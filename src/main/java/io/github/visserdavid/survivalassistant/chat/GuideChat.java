package io.github.visserdavid.survivalassistant.chat;

import io.github.visserdavid.survivalassistant.GuideManager;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.advice.AdviceEngine;
import io.github.visserdavid.survivalassistant.advice.ContextCollector;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.advice.SurvivalContext;
import io.github.visserdavid.survivalassistant.advice.Tip;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import io.github.visserdavid.survivalassistant.state.GuideState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Small talk with the guide through a local language model.
 *
 * <p>The guide remarks on changes around the player (weather, time of day, a new biome, a mob it hasn't talked
 * about yet) and answers when the player talks to it in chat. It stays quiet while something urgent is going on
 * or the guide is muted: warnings remain the advice engine's job. Everything runs on the server thread except
 * the HTTP request itself.
 */
public final class GuideChat {
	private static final class State {
		final ChangeDetector detector;
		final List<ChatPrompt.Turn> history = new ArrayList<>();
		boolean waiting;

		State(long cooldownTicks) {
			detector = new ChangeDetector(cooldownTicks);
		}
	}

	private static final Map<UUID, State> STATES = new HashMap<>();
	/** After a failed connection we leave the model alone for a minute (wall clock, shared by all players). */
	private static final long OFFLINE_BACKOFF_MS = 60_000;
	private static volatile long offlineUntil;

	private GuideChat() {}

	// ---------------------------------------------------------------- spontaneous remarks

	/** Called with every fresh situation evaluation (every few seconds). */
	public static void observe(ServerPlayer player, @Nullable GuideEntity guide, SurvivalContext ctx, GuideProgress progress) {
		ModConfig cfg = ModConfig.get();
		if (!progress.chatEnabled(cfg.chatEnabled) || !cfg.chatSpontaneous) return;
		// Without the character the task card is the main UI and only urgent warnings go to chat: no small talk either.
		if (guide == null) return;

		State state = state(player);
		long now = player.level().getGameTime();
		Surroundings surroundings = SurroundingsCollector.collect(player, ctx, cfg.chatSightRadius);
		boolean blocked = state.waiting || offline() || now < progress.muteUntil || AdviceEngine.activeUrgent(ctx) != null;

		state.detector.update(surroundings, now, blocked).ifPresent(change -> {
			// the musings already greet a new biome with a building idea
			if (change.kind() == ChangeDetector.Kind.NEW_BIOME && GuideMusings.enabled(progress)) return;
			ChatPrompt.Scene scene = scene(player, surroundings, progress);
			String note = ChatPrompt.changeNote(scene, change);
			ask(player, state, ChatPrompt.forChange(scene, state.history, change), note, scene.guideName(), false, null);
		});
	}

	// ---------------------------------------------------------------- the player talks

	/** Called for every chat message a player sends. */
	public static void onChat(ServerPlayer player, String text) {
		ModConfig cfg = ModConfig.get();
		if (text.isBlank()) return;
		GuideProgress progress = GuideState.of(player);
		if (!progress.chatEnabled(cfg.chatEnabled) || progress.retired || progress.startTick < 0) return;

		GuideEntity guide = GuideManager.guideOf(player);
		String language = language(player);
		String guideName = GuideManager.guideName(player);
		boolean named = named(text, guideName, language);
		if (!addressed(player, guide, named, cfg)) return;

		State state = state(player);
		if (state.waiting) return; // still thinking about the previous message
		state.detector.conversationAt(player.level().getGameTime());
		if (guide != null) guide.pointAt(player.blockPosition().above()); // turn towards the player

		if (offline()) {
			// Only when called by name: someone without a model server shouldn't get this after every chat line.
			if (named) GuideManager.speak(player, guide, Component.translatable("survivalassistant.chat.unavailable").withStyle(ChatFormatting.GRAY));
			return;
		}

		SurvivalContext ctx = ContextCollector.collect(player, progress);
		Surroundings surroundings = SurroundingsCollector.collect(player, ctx, cfg.chatSightRadius);
		ChatPrompt.Scene scene = scene(player, surroundings, progress);
		String said = text.strip();
		ask(player, state, ChatPrompt.forPlayer(scene, state.history, said), said, guideName, true, null);
	}

	/**
	 * Is the player talking to the guide? With nobody else around, everything said near the guide is meant for it.
	 * With other players in earshot (or without the character) the guide must be named.
	 */
	private static boolean addressed(ServerPlayer player, @Nullable GuideEntity guide, boolean named, ModConfig cfg) {
		double hearing = (double) cfg.chatHearingDistance * cfg.chatHearingDistance;
		if (guide == null) return named;
		if (guide.distanceToSqr(player) > hearing) return false;
		boolean othersNearby = player.level().players().stream()
				.anyMatch(p -> p != player && !p.isSpectator() && p.distanceToSqr(player) <= hearing);
		return !othersNearby || named;
	}

	private static boolean named(String text, String guideName, String language) {
		String localized = LangFile.get(language, "entity.survivalassistant.guide");
		return NameMatch.mentions(text, guideName) || NameMatch.mentions(text, "guide")
				|| (localized != null && NameMatch.mentions(text, localized));
	}

	// ---------------------------------------------------------------- request and reply

	// ---------------------------------------------------------------- musings

	/** True while a request for this player is running. */
	public static boolean busy(ServerPlayer player) {
		State state = STATES.get(player.getUUID());
		return state != null && state.waiting;
	}

	/**
	 * Lets the language model put a musing in its own words (the English line of {@code key} is the idea).
	 *
	 * @return false when small talk is off or the model is offline: the caller then says the handwritten line
	 */
	public static boolean muse(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress, String key) {
		ModConfig cfg = ModConfig.get();
		if (!progress.chatEnabled(cfg.chatEnabled) || offline()) return false;
		String idea = LangFile.get("en_us", key);
		if (idea == null) return false;
		State state = state(player);
		if (state.waiting) return false;
		state.detector.conversationAt(player.level().getGameTime());

		SurvivalContext ctx = ContextCollector.collect(player, progress);
		Surroundings surroundings = SurroundingsCollector.collect(player, ctx, cfg.chatSightRadius);
		ChatPrompt.Scene scene = scene(player, surroundings, progress);
		String note = key.startsWith("survivalassistant.about.") ? ChatPrompt.selfNote(scene, idea) : ChatPrompt.museNote(scene, idea);
		ask(player, state, ChatPrompt.forPlayer(scene, state.history, note), note, scene.guideName(), false,
				Component.translatable(key));
		return true;
	}

	private static void ask(ServerPlayer player, State state, List<ChatPrompt.Turn> messages, String userLine,
			String guideName, boolean direct, @Nullable Component fallback) {
		ModConfig cfg = ModConfig.get();
		MinecraftServer server = player.level().getServer();
		UUID id = player.getUUID();
		String body = ChatPrompt.toRequestJson(messages, cfg.chatModel, cfg.chatMaxTokens, cfg.chatTemperature);

		state.waiting = true;
		try {
			ChatClient.complete(cfg.chatEndpoint, body, cfg.chatTimeoutSeconds)
					.whenComplete((raw, error) -> server.execute(() -> deliver(server, id, state, userLine, raw, error, guideName, direct, fallback)));
		} catch (RuntimeException e) {
			state.waiting = false;
			throw e;
		}
	}

	private static void deliver(MinecraftServer server, UUID id, State state, String userLine, @Nullable String raw,
			@Nullable Throwable error, String guideName, boolean direct, @Nullable Component fallback) {
		state.waiting = false;
		// The player left (or the server stopped) while the model was thinking: this answer belongs to nobody now.
		if (STATES.get(id) != state) return;
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		if (player == null) return;
		GuideEntity guide = GuideManager.guideOf(player);

		if (error != null) {
			failed(error);
			if (direct) {
				GuideManager.speak(player, guide, Component.translatable("survivalassistant.chat.unavailable").withStyle(ChatFormatting.GRAY));
			} else if (fallback != null) {
				GuideManager.speak(player, guide, fallback);
			}
			return;
		}

		String reply = ChatPrompt.cleanReply(raw, guideName);
		if (reply.isEmpty()) {
			if (fallback != null) GuideManager.speak(player, guide, fallback);
			return;
		}
		state.history.add(new ChatPrompt.Turn(ChatPrompt.Role.USER, userLine));
		state.history.add(new ChatPrompt.Turn(ChatPrompt.Role.ASSISTANT, reply));
		ChatPrompt.trimHistory(state.history, ModConfig.get().chatHistoryTurns);
		GuideManager.speak(player, guide, Component.literal(reply));
	}

	private static void failed(Throwable error) {
		// Connection refused, timeout, dropped connection: all I/O trouble means "no server", so back off for a while.
		IOException io = null;
		for (Throwable t = error; t != null && io == null; t = t.getCause() == t ? null : t.getCause()) {
			if (t instanceof IOException e) io = e;
		}
		if (io != null) {
			if (offline()) {
				// already reported
			} else if (io instanceof ChatClient.BadResponseException bad) {
				SurvivalAssistant.LOGGER.warn("Guide chat: unusable answer from {} ({}). Trying again in a minute.",
						ModConfig.get().chatEndpoint, bad.getMessage());
			} else {
				SurvivalAssistant.LOGGER.warn("Guide chat: no answer from {} ({}). Is llama-server running? Trying again in a minute.",
						ModConfig.get().chatEndpoint, io.getClass().getSimpleName());
			}
			offlineUntil = System.currentTimeMillis() + OFFLINE_BACKOFF_MS;
		} else {
			SurvivalAssistant.LOGGER.warn("Guide chat request failed", error);
		}
	}

	private static boolean offline() {
		return System.currentTimeMillis() < offlineUntil;
	}

	// ---------------------------------------------------------------- helpers

	private static ChatPrompt.Scene scene(ServerPlayer player, Surroundings s, GuideProgress progress) {
		String language = language(player);
		String group = s.biomeGroup();
		Tip next = AdviceEngine.nextMilestone(progress);
		return new ChatPrompt.Scene(language, GuideManager.guideName(player), player.getName().getString(), s,
				LangFile.get(language, "survivalassistant.biome." + group + ".pro"),
				LangFile.get(language, "survivalassistant.biome." + group + ".con"),
				next == null ? null : LangFile.get(language, "survivalassistant.task." + next.id()));
	}

	private static String language(ServerPlayer player) {
		String code = player.clientInformation().language();
		return code == null || code.isBlank() ? "en_us" : code;
	}

	private static State state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(),
				id -> new State(ModConfig.get().chatCooldownSeconds * 20L));
	}

	public static void forget(UUID playerId) {
		STATES.remove(playerId);
	}

	public static void clear() {
		STATES.clear();
		offlineUntil = 0;
	}
}
