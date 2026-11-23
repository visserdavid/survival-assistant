package io.github.visserdavid.survivalassistant.advice;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Groups the 67 biomes of Minecraft 26.3 into families that share the same survival advice.
 * The pros/cons texts live in the language files ({@code survivalassistant.biome.<group>.pro/.con})
 * and are checked against docs/biomes-and-heights.md (generated from the game data).
 *
 * @param icon item id used as icon on the environment card
 */
public enum BiomeGroup {
	PLAINS("minecraft:short_grass", "plains", "sunflower_plains"),
	FOREST("minecraft:oak_sapling", "forest", "flower_forest"),
	BIRCH_FOREST("minecraft:birch_sapling", "birch_forest", "old_growth_birch_forest"),
	DARK_FOREST("minecraft:dark_oak_sapling", "dark_forest"),
	PALE_GARDEN("minecraft:pale_oak_sapling", "pale_garden"),
	DAPPLED_FOREST("minecraft:brown_mushroom", "dappled_forest"),
	TAIGA("minecraft:spruce_sapling", "taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga"),
	SNOWY("minecraft:snowball", "snowy_plains", "ice_spikes", "snowy_taiga", "snowy_beach", "frozen_river"),
	MOUNTAINS("minecraft:emerald", "windswept_hills", "windswept_gravelly_hills", "windswept_forest",
			"jagged_peaks", "frozen_peaks", "stony_peaks", "snowy_slopes", "grove"),
	MEADOW("minecraft:cherry_sapling", "meadow", "cherry_grove"),
	DESERT("minecraft:sand", "desert"),
	BADLANDS("minecraft:red_sand", "badlands", "eroded_badlands", "wooded_badlands"),
	SAVANNA("minecraft:acacia_sapling", "savanna", "savanna_plateau", "windswept_savanna"),
	JUNGLE("minecraft:cocoa_beans", "jungle", "sparse_jungle", "bamboo_jungle"),
	SWAMP("minecraft:lily_pad", "swamp", "mangrove_swamp"),
	SHORE("minecraft:sugar_cane", "beach", "river", "stony_shore"),
	OCEAN("minecraft:kelp", "ocean", "deep_ocean", "cold_ocean", "deep_cold_ocean", "frozen_ocean", "deep_frozen_ocean",
			"lukewarm_ocean", "deep_lukewarm_ocean", "warm_ocean"),
	MUSHROOM("minecraft:red_mushroom", "mushroom_fields"),
	LUSH_CAVES("minecraft:glow_berries", "lush_caves"),
	DRIPSTONE("minecraft:pointed_dripstone", "dripstone_caves"),
	DEEP_DARK("minecraft:sculk", "deep_dark"),
	SULFUR_CAVES("minecraft:yellow_dye", "sulfur_caves"),
	NETHER("minecraft:netherrack", "nether_wastes", "crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas"),
	END("minecraft:end_stone", "the_end", "end_barrens", "end_highlands", "end_midlands", "small_end_islands"),
	OTHER("minecraft:compass");

	private static final Map<String, BiomeGroup> BY_BIOME = new HashMap<>();

	static {
		for (BiomeGroup g : values()) {
			for (String b : g.biomes) BY_BIOME.put("minecraft:" + b, g);
		}
	}

	private final String icon;
	private final String[] biomes;

	BiomeGroup(String icon, String... biomes) {
		this.icon = icon;
		this.biomes = biomes;
	}

	public String icon() {
		return icon;
	}

	/** @param biomeId full id such as {@code minecraft:desert}; unknown or modded biomes give {@link #OTHER} */
	public static BiomeGroup of(String biomeId) {
		return BY_BIOME.getOrDefault(biomeId, OTHER);
	}

	public static boolean isKnown(String biomeId) {
		return BY_BIOME.containsKey(biomeId);
	}

	/** Biomes where emerald ore generates (verified: ore_emerald feature). */
	public boolean hasEmerald() {
		return this == MOUNTAINS || this == MEADOW;
	}

	/** Badlands get the extra gold feature (Y 32..256). */
	public boolean hasExtraGold() {
		return this == BADLANDS;
	}

	public boolean isMountain() {
		return this == MOUNTAINS || this == MEADOW;
	}

	public boolean isOverworld() {
		return this != NETHER && this != END;
	}

	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}
}
