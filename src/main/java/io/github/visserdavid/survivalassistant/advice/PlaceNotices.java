package io.github.visserdavid.survivalassistant.advice;

import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * One-time notes about special places, like the depth notes: said once per player, the first time it applies.
 * Language keys: {@code survivalassistant.place.<id>}. Pure logic.
 *
 * <ul>
 *   <li>{@code nether}: beds explode there, piglins leave you alone in golden armor, ghast fireballs can be hit back.</li>
 *   <li>{@code village}: the first village; villagers trade and a bell rings when raiders come.</li>
 *   <li>{@code village_night}: the first night in a village; zombies hunt villagers.</li>
 * </ul>
 */
public final class PlaceNotices {
	private PlaceNotices() {}

	public static @Nullable String next(SurvivalContext ctx, Set<String> seen) {
		if (ctx.inNether() && !seen.contains("nether")) return "nether";
		if (ctx.inVillage() && !seen.contains("village")) return "village";
		if (ctx.inVillage() && ctx.isNight() && !seen.contains("village_night")) return "village_night";
		return null;
	}
}
