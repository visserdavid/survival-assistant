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
import io.github.visserdavid.survivalassistant.util.Ticks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * Small talk with the guide through a local language model.
 *
 * <p>The guide remarks on changes around the player (weather, time of day, a new biome, a mob it hasn't talked
 * about yet) and answers when the player talks to it in chat. It stays quiet while something urgent is going on
 * or the guide is muted: warnings remain the advice engine's job. Everything runs on the server thread except
 * the HTTP request itself.
 */
public final class GuideChat {
	/** No model server: leave it alone for a minute, then longer and longer up to half an hour (wall clock). */
	private static final long FIRST_BACKOFF_MS = 60_000;
	private static final long MAX_BACKOFF_MS = 30 * 60_000;
	private static final String ABOUT_PREFIX = "survivalassistant.about.";
	/**
	 * One request at a time for the whole server: a small local model answers one by one anyway (the README advises
	 * {@code --parallel 1}), and queued requests would only time out.
	 */
	private static final Semaphore SLOT = new Semaphore(1);

	private static final Map<UUID, State> STATES = new HashMap<>();
	private static volatile long offlineUntil;
	private static volatile long backoffMs = FIRST_BACKOFF_MS;
	/** A problem was logged; nothing more until a request works again (no log spam without a model server). */
	private static volatile boolean reported;

	private GuideChat() {}

	private static final class State {
		final ChangeDetector detector;
		final List<ChatPrompt.Turn> history = new ArrayList<>();
		/** The situation at the last evaluation, so a chat message doesn't need a fresh scan of the world. */
		@Nullable SurvivalContext lastContext;
		boolean waiting;
		/** The running request, cancelled when the player leaves or the server stops. */
		@Nullable CompletableFuture<String> pending;

		State(long cooldownTicks) {
			detector = new ChangeDetector(cooldownTicks);
		}
	}

	/**
	 * One question to the model.
	 *
	 * @param userLine  what goes into the history as the player's turn
	 * @param apologize the player called the guide by name: an unreachable model then gets a short apology
	 * @param fallback  said instead when the model gives no usable answer (a musing's handwritten line), or null
	 */
	private record Request(List<ChatPrompt.Turn> messages, String userLine, String guideName, boolean apologize,
			@Nullable Component fallback) {}

	// ---------------------------------------------------------------- spontaneous remarks

	/** Called with every fresh situation evaluation (every few seconds). */
	public static void observe(ServerPlayer player, @Nullable GuideEntity guide, SurvivalContext ctx, GuideProgress progress,
			Supplier<Surroundings> seen) {
		ModConfig cfg = ModConfig.get();
		if (!progress.chatEnabled(cfg.chatEnabled)) return;
		State state = state(player);
		state.lastContext = ctx;
		// Without the character the task card is the main UI and only urgent warnings go to chat: no small talk either.
		if (!cfg.chatSpontaneous || guide == null) return;

		long now = player.level().getGameTime();
		Surroundings surroundings = seen.get();
		boolean blocked = state.waiting || modelBusy() || offline() || now < progress.muteUntil
				|| AdviceEngine.activeUrgent(ctx) != null;
		state.detector.setCooldown(Ticks.seconds(cfg.chatCooldownSeconds));
		// the musings already greet a new biome with a building idea
		state.detector.ignoreNewBiome(GuideMusings.enabled(progress));

		state.detector.update(surroundings, now, blocked).ifPresent(change -> {
			ChatPrompt.Scene scene = scene(player, surroundings, progress);
			String note = ChatPrompt.changeNote(scene, change);
			ask(player, state, new Request(ChatPrompt.forLine(scene, state.history, note), note, scene.guideName(), false, null));
		});
	}

	// ---------------------------------------------------------------- the player talks

	/** Called for every chat message a player sends. */
	public static void onChat(ServerPlayer player, String text) {
		ModConfig cfg = ModConfig.get();
		if (text.isBlank()) return;
		GuideProgress progress = GuideState.of(player);
		if (!progress.chatEnabled(cfg.chatEnabled) || progress.retired || !progress.met()) return;

		GuideEntity guide = GuideManager.guideOf(player);
		String guideName = GuideManager.guideName(player);
		boolean named = named(text, guideName, language(player));
		if (!addressed(player, guide, named, cfg)) return;

		State state = state(player);
		if (state.waiting) return; // still thinking about the previous message
		state.detector.conversationAt(player.level().getGameTime());
		if (guide != null) guide.pointAt(player.blockPosition().above()); // turn towards the player

		if (offline()) {
			// Only when called by name: someone without a model server shouldn't get this after every chat line.
			if (named) GuideManager.speak(player, guide, unavailable());
			return;
		}

		ChatPrompt.Scene scene = scene(player, state, progress);
		String said = text.strip();
		ask(player, state, new Request(ChatPrompt.forLine(scene, state.history, said), said, guideName, named, null));
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

	/** The message mentions the guide: its name, "guide", or the word for guide in the player's language. */
	private static boolean named(String text, String guideName, String language) {
		String localized = LangFile.get(language, "entity.survivalassistant.guide");
		return NameMatch.mentions(text, guideName) || NameMatch.mentions(text, "guide")
				|| (localized != null && NameMatch.mentions(text, localized));
	}

	// ---------------------------------------------------------------- musings

	/** True while a request is running (for this player, or for anyone: the model answers one at a time). */
	public static boolean busy(ServerPlayer player) {
		State state = STATES.get(player.getUUID());
		return (state != null && state.waiting) || modelBusy();
	}

	private static boolean modelBusy() {
		return SLOT.availablePermits() == 0;
	}

	/**
	 * Lets the language model put a musing in its own words (the English line of {@code key} is the idea).
	 *
	 * @return false when small talk or spontaneous remarks are off, or the model is offline or busy: the caller then
	 *         says the handwritten line
	 */
	public static boolean muse(ServerPlayer player, GuideProgress progress, String key) {
		ModConfig cfg = ModConfig.get();
		if (!progress.chatEnabled(cfg.chatEnabled) || !cfg.chatSpontaneous || offline()) return false;
		String idea = LangFile.get("en_us", key);
		if (idea == null) return false;
		State state = state(player);
		if (state.waiting) return false;
		state.detector.conversationAt(player.level().getGameTime());

		ChatPrompt.Scene scene = scene(player, state, progress);
		String note = key.startsWith(ABOUT_PREFIX) ? ChatPrompt.selfNote(scene, idea) : ChatPrompt.museNote(scene, idea);
		return ask(player, state, new Request(ChatPrompt.forLine(scene, state.history, note), note, scene.guideName(), false,
				Component.translatable(key)));
	}

	// ---------------------------------------------------------------- request and reply

	/** Sends the request when the model is free; false when another request is still running. */
	private static boolean ask(ServerPlayer player, State state, Request request) {
		if (!SLOT.tryAcquire()) return false;
		ModConfig cfg = ModConfig.get();
		MinecraftServer server = player.level().getServer();
		UUID id = player.getUUID();
		String body = ChatPrompt.toRequestJson(request.messages(), cfg.chatModel, cfg.chatMaxTokens, cfg.chatTemperature);

		state.waiting = true;
		try {
			CompletableFuture<String> future = ChatClient.complete(cfg.chatEndpoint, body, cfg.chatTimeoutSeconds);
			state.pending = future;
			future.whenComplete((raw, error) -> {
				SLOT.release();
				server.execute(() -> deliver(server, id, state, request, raw, error));
			});
			return true;
		} catch (RuntimeException e) {
			SLOT.release();
			state.waiting = false;
			throw e;
		}
	}

	private static void deliver(MinecraftServer server, UUID id, State state, Request request, @Nullable String raw,
			@Nullable Throwable error) {
		// A stopped server runs late tasks on the calling (HTTP) thread: never touch the game from there.
		if (!server.isSameThread()) return;
		state.waiting = false;
		state.pending = null;
		// The player left (or the server stopped) while the model was thinking: this answer belongs to nobody now.
		if (STATES.get(id) != state || error instanceof CancellationException) return;
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		if (player == null) return;
		GuideEntity guide = GuideManager.guideOf(player);

		if (error != null) {
			failed(error);
			if (request.apologize()) GuideManager.speak(player, guide, unavailable());
			else if (request.fallback() != null) GuideManager.speak(player, guide, request.fallback());
			return;
		}
		recovered();

		String reply = ChatPrompt.cleanReply(raw, request.guideName());
		if (reply.isEmpty()) {
			if (request.fallback() != null) GuideManager.speak(player, guide, request.fallback());
			return;
		}
		state.history.add(new ChatPrompt.Turn(ChatPrompt.Role.USER, request.userLine()));
		state.history.add(new ChatPrompt.Turn(ChatPrompt.Role.ASSISTANT, reply));
		ChatPrompt.trimHistory(state.history, ModConfig.get().chatHistoryTurns);
		GuideManager.speak(player, guide, Component.literal(reply));
	}

	/**
	 * A request failed. Only "nobody listens there" (connection refused) makes the guide stop asking for a while, a
	 * little longer each time; a slow or strange answer just counts as no answer. Logged once until a request works.
	 */
	private static void failed(Throwable error) {
		Throwable cause = error;
		while (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
		boolean noServer = cause instanceof ConnectException || cause instanceof HttpConnectTimeoutException;
		if (!reported) {
			reported = true;
			String endpoint = ModConfig.get().chatEndpoint;
			if (noServer) {
				SurvivalAssistant.LOGGER.warn("Guide chat: no model server at {}. Is llama-server running? Trying again later.", endpoint);
			} else if (cause instanceof ChatClient.BadResponseException bad) {
				SurvivalAssistant.LOGGER.warn("Guide chat: unusable answer from {} ({}).", endpoint, bad.getMessage());
			} else {
				SurvivalAssistant.LOGGER.warn("Guide chat: request to {} failed ({}).", endpoint, cause.getClass().getSimpleName());
			}
		}
		if (noServer) {
			offlineUntil = System.currentTimeMillis() + backoffMs;
			backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
		}
	}

	/** A request worked: back to normal, and the next problem is logged again. */
	private static void recovered() {
		if (reported) SurvivalAssistant.LOGGER.info("Guide chat: the model server answers again.");
		reported = false;
		backoffMs = FIRST_BACKOFF_MS;
	}

	private static boolean offline() {
		return System.currentTimeMillis() < offlineUntil;
	}

	// ---------------------------------------------------------------- helpers

	private static Component unavailable() {
		return Component.translatable("survivalassistant.chat.unavailable").withStyle(ChatFormatting.GRAY);
	}

	/** The scene for a message or musing: from the last evaluation when there is one, otherwise from a fresh look. */
	private static ChatPrompt.Scene scene(ServerPlayer player, State state, GuideProgress progress) {
		SurvivalContext ctx = state.lastContext != null ? state.lastContext : ContextCollector.collect(player, progress);
		return scene(player, SurroundingsCollector.collect(player, ctx, ModConfig.get().chatSightRadius), progress);
	}

	private static ChatPrompt.Scene scene(ServerPlayer player, Surroundings s, GuideProgress progress) {
		String language = language(player);
		String group = s.biomeGroup();
		Tip next = AdviceEngine.nextMilestone(progress);
		return new ChatPrompt.Scene(language, GuideManager.guideName(player), player.getName().getString(), s,
				LangFile.get(language, "survivalassistant.biome." + group + ".pro"),
				LangFile.get(language, "survivalassistant.biome." + group + ".con"),
				next == null ? null : LangFile.get(language, "survivalassistant.task." + next.id()));
	}

	/** The player's game language when the mod ships it, else English (the client chooses this text freely). */
	private static String language(ServerPlayer player) {
		String code = player.clientInformation().language();
		return code != null && LangFile.has(code) ? code.toLowerCase(java.util.Locale.ROOT) : "en_us";
	}

	private static State state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(),
				id -> new State(Ticks.seconds(ModConfig.get().chatCooldownSeconds)));
	}

	public static void forget(UUID playerId) {
		State state = STATES.remove(playerId);
		if (state != null && state.pending != null) state.pending.cancel(true);
	}

	public static void clear() {
		for (State state : STATES.values()) {
			if (state.pending != null) state.pending.cancel(true);
		}
		STATES.clear();
		offlineUntil = 0;
		backoffMs = FIRST_BACKOFF_MS;
		reported = false;
	}
}
