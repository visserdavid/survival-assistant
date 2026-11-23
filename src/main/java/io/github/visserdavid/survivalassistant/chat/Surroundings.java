package io.github.visserdavid.survivalassistant.chat;

import java.util.Comparator;
import java.util.Map;

/**
 * What the guide can see around the player, for small talk. Free of Minecraft types so it can be unit tested.
 *
 * @param biomeId   full biome id, for example {@code minecraft:forest}
 * @param biomeGroup lower-case {@link io.github.visserdavid.survivalassistant.advice.BiomeGroup} name, for example {@code forest}
 * @param raining   rain falls at the player's position (false in deserts and while it snows)
 * @param thundering a thunderstorm is going on
 * @param phase     time of day
 * @param underground no sky above and well below the surface
 * @param friendly  non-hostile mobs within sight, by entity id path ({@code cow}) with their count
 * @param hostile   hostile mobs within sight, same format
 */
public record Surroundings(
		String biomeId,
		String biomeGroup,
		boolean raining,
		boolean thundering,
		Phase phase,
		boolean underground,
		Map<String, Integer> friendly,
		Map<String, Integer> hostile
) {
	public enum Phase { DAY, DUSK, NIGHT }

	/** Mob counts, most numerous first (alphabetical on a tie, for predictability). */
	public static final Comparator<Map.Entry<String, Integer>> MOST_FIRST =
			Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey());

	public Surroundings {
		friendly = Map.copyOf(friendly);
		hostile = Map.copyOf(hostile);
	}

	/** Biome id without the {@code minecraft:} namespace. */
	public String biomeName() {
		int colon = biomeId.indexOf(':');
		return colon < 0 ? biomeId : biomeId.substring(colon + 1);
	}

	public boolean sees(String mobId) {
		return friendly.containsKey(mobId) || hostile.containsKey(mobId);
	}
}
