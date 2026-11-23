package io.github.visserdavid.survivalassistant.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import io.github.visserdavid.survivalassistant.advice.LightScan;
import io.github.visserdavid.survivalassistant.advice.SpawnSpots;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Light check (key L): marks nearby spots where monsters could spawn with small red dust particles, so players
 * can see where their base still needs torches. Client only; nothing is sent to the server.
 */
public final class LightCheck {
	private LightCheck() {}

	private static final DustParticleOptions MARKER = new DustParticleOptions(0xFF2020, 1.0f);

	private static boolean enabled;
	private static int ticks;
	/** Markers found by the last scan, or -1 before the first scan after switching on. */
	private static int lastCount = -1;
	private static final List<BlockPos> spots = new ArrayList<>();

	public static void toggle(Minecraft mc) {
		enabled = !enabled;
		ticks = 0;
		lastCount = -1;
		spots.clear();
		if (mc.player == null || mc.level == null) return;
		String key = !enabled ? "survivalassistant.light.off"
				: LightScan.lightMatters(limit(mc.level)) ? "survivalassistant.light.on" : "survivalassistant.light.no_effect";
		mc.player.sendOverlayMessage(Component.translatable(key));
	}

	public static void tick(Minecraft mc) {
		if (!enabled) return;
		if (mc.player == null || mc.level == null) {
			enabled = false;
			return;
		}
		if (ticks++ % LightScan.INTERVAL_TICKS != 0) return;
		ClientLevel level = mc.level;
		if (!LightScan.lightMatters(limit(level))) return;

		scan(mc, level);
		for (BlockPos p : spots) {
			level.addParticle(MARKER, p.getX() + 0.5, p.getY() + 0.15, p.getZ() + 0.5, 0, 0, 0);
		}
		if (spots.isEmpty() && lastCount != 0) {
			mc.player.sendOverlayMessage(Component.translatable("survivalassistant.light.all_lit"));
		}
		lastCount = spots.size();
	}

	private static int limit(ClientLevel level) {
		return level.dimensionType().monsterSpawnBlockLightLimit();
	}

	private static void scan(Minecraft mc, ClientLevel level) {
		spots.clear();
		BlockPos origin = mc.player.blockPosition();
		SpawnSpots.scan(level, origin, LightScan.RADIUS, LightScan.VERTICAL, limit(level), Integer.MAX_VALUE, spots::add);
		if (spots.size() > LightScan.MAX_MARKERS) {
			spots.sort(Comparator.comparingDouble(p -> p.distSqr(origin)));
			spots.subList(LightScan.MAX_MARKERS, spots.size()).clear();
		}
	}
}
