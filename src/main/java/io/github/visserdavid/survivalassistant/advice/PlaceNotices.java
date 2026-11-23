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
 *   <li>{@code desert_temple}: the pressure plate in the treasure room is wired to TNT.</li>
 *   <li>{@code jungle_temple}: tripwires fire arrows.</li>
 *   <li>{@code trial_chambers}: trial spawners, and keys that open the vaults.</li>
 *   <li>{@code wreck}: drowned around shipwrecks and ocean ruins; watch your air.</li>
 *   <li>{@code stronghold}: the portal room, silverfish hiding in stone bricks.</li>
 *   <li>{@code end}: endermen, the end crystals that heal the dragon, the void.</li>
 *   <li>{@code end_city}: shulker bullets make you float; the ship holds an elytra.</li>
 * </ul>
 */
public final class PlaceNotices {
	private PlaceNotices() {}

	public static @Nullable String next(SurvivalContext ctx, Set<String> seen) {
		if (ctx.inNether() && !seen.contains("nether")) return "nether";
		if (ctx.inVillage() && !seen.contains("village")) return "village";
		if (ctx.inVillage() && ctx.isNight() && !seen.contains("village_night")) return "village_night";
		if (ctx.structure().equals("desert_pyramid") && !seen.contains("desert_temple")) return "desert_temple";
		if (ctx.structure().equals("jungle_pyramid") && !seen.contains("jungle_temple")) return "jungle_temple";
		if (ctx.structure().equals("trial_chambers") && !seen.contains("trial_chambers")) return "trial_chambers";
		if (ctx.inWreck() && !seen.contains("wreck")) return "wreck";
		if (ctx.structure().equals("stronghold") && !seen.contains("stronghold")) return "stronghold";
		if (ctx.inEnd() && !seen.contains("end")) return "end";
		if (ctx.structure().equals("end_city") && !seen.contains("end_city")) return "end_city";
		return null;
	}
}
