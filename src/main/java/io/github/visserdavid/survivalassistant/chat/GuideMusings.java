package io.github.visserdavid.survivalassistant.chat;

import io.github.visserdavid.survivalassistant.advice.AdviceEngine;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.advice.SurvivalContext;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import io.github.visserdavid.survivalassistant.GuideManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The guide's musings: a building idea when you enter a new biome, a remark about the animals around you, a scene
 * or a little story. Handwritten lines in every language; with a local language model (/guide chat on) the model
 * puts the same idea in its own words. Now and then the guide tells something about itself ({@link Persona}). See {@link MuseScheduler} for when and {@link BiomeThemes} for what.
 */
public final class GuideMusings {
	private GuideMusings() {}

	/** Per-player setting values stored in {@link GuideProgress#musings}. */
	public static final String OFF = "off";
	public static final String ALL = "all";

	private static final Map<UUID, MuseScheduler> SCHEDULERS = new HashMap<>();

	/** Musings are on for this player (server default and the player's own choice). */
	public static boolean enabled(GuideProgress progress) {
		return ModConfig.get().musings && !OFF.equals(progress.musings);
	}

	/** Called with every fresh situation evaluation (every few seconds). */
	public static void observe(ServerPlayer player, @Nullable GuideEntity guide, SurvivalContext ctx, GuideProgress progress) {
		if (!enabled(progress)) return;
		ModConfig cfg = ModConfig.get();
		long now = player.level().getGameTime();
		MuseScheduler scheduler = SCHEDULERS.computeIfAbsent(player.getUUID(),
				id -> new MuseScheduler(cfg.museIntervalSeconds * 20L));

		Surroundings s = SurroundingsCollector.collect(player, ctx, cfg.chatSightRadius);
		// Without the character only urgent warnings go to chat, so no musings either.
		boolean blocked = guide == null || now < progress.muteUntil || AdviceEngine.activeUrgent(ctx) != null
				|| GuideChat.busy(player);
		boolean dangerZones = cfg.musingsInDangerZones || ALL.equals(progress.musings);
		MuseScheduler.Input in = new MuseScheduler.Input(s.biomeGroup(), !ctx.isNight(), ctx.underground(),
				s.friendly().keySet(), blocked, dangerZones, Persona.aboutKey(progress.aboutIndex));

		scheduler.update(in, now).ifPresent(key -> {
			if (key.equals(in.selfKey())) GuideManager.nextAbout(progress); // the next story is a different one
			if (!GuideChat.muse(player, guide, progress, key)) {
				GuideManager.speak(player, guide, Component.translatable(key));
			}
		});
	}

	/** The guide said something else: the musings keep quiet for a while. */
	public static void noteRemark(ServerPlayer player) {
		MuseScheduler s = SCHEDULERS.get(player.getUUID());
		if (s != null) s.noteRemark(player.level().getGameTime());
	}

	public static void forget(UUID playerId) {
		SCHEDULERS.remove(playerId);
	}

	public static void clear() {
		SCHEDULERS.clear();
	}
}
