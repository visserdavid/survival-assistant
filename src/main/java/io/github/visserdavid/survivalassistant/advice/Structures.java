package io.github.visserdavid.survivalassistant.advice;

import java.util.Set;

/** The structures the guide knows about (ids in data/minecraft/worldgen/structure of the 26.3 data). */
public final class Structures {
	private Structures() {}

	public static final String VILLAGE_PLAINS = "village_plains";
	public static final String VILLAGE_DESERT = "village_desert";
	public static final String VILLAGE_SAVANNA = "village_savanna";
	public static final String VILLAGE_SNOWY = "village_snowy";
	public static final String VILLAGE_TAIGA = "village_taiga";
	public static final String SHIPWRECK = "shipwreck";
	public static final String SHIPWRECK_BEACHED = "shipwreck_beached";
	public static final String OCEAN_RUIN_COLD = "ocean_ruin_cold";
	public static final String OCEAN_RUIN_WARM = "ocean_ruin_warm";
	public static final String DESERT_PYRAMID = "desert_pyramid";
	public static final String JUNGLE_PYRAMID = "jungle_pyramid";
	public static final String TRIAL_CHAMBERS = "trial_chambers";
	public static final String STRONGHOLD = "stronghold";
	public static final String END_CITY = "end_city";
	public static final String BASTION_REMNANT = "bastion_remnant";
	public static final String ANCIENT_CITY = "ancient_city";
	public static final String PILLAGER_OUTPOST = "pillager_outpost";
	public static final String TRAIL_RUINS = "trail_ruins";

	public static final Set<String> ALL = Set.of(
			VILLAGE_PLAINS, VILLAGE_DESERT, VILLAGE_SAVANNA, VILLAGE_SNOWY, VILLAGE_TAIGA,
			SHIPWRECK, SHIPWRECK_BEACHED, OCEAN_RUIN_COLD, OCEAN_RUIN_WARM,
			DESERT_PYRAMID, JUNGLE_PYRAMID, TRIAL_CHAMBERS, STRONGHOLD, END_CITY,
			BASTION_REMNANT, ANCIENT_CITY, PILLAGER_OUTPOST, TRAIL_RUINS);

	public static boolean isVillage(String id) {
		return id.startsWith("village_");
	}

	/** A shipwreck or an ocean ruin. */
	public static boolean isWreck(String id) {
		return id.startsWith(SHIPWRECK) || id.startsWith("ocean_ruin");
	}
}
