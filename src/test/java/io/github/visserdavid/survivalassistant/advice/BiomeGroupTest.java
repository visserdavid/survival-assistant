package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BiomeGroupTest {
	/** All biomes in Minecraft 26.3 (data/minecraft/worldgen/biome), see docs/biomes-and-heights.md. */
	private static final List<String> ALL_26_3 = List.of(
			"badlands", "bamboo_jungle", "basalt_deltas", "beach", "birch_forest", "cherry_grove", "cold_ocean",
			"crimson_forest", "dappled_forest", "dark_forest", "deep_cold_ocean", "deep_dark", "deep_frozen_ocean",
			"deep_lukewarm_ocean", "deep_ocean", "desert", "dripstone_caves", "end_barrens", "end_highlands", "end_midlands",
			"eroded_badlands", "flower_forest", "forest", "frozen_ocean", "frozen_peaks", "frozen_river", "grove",
			"ice_spikes", "jagged_peaks", "jungle", "lukewarm_ocean", "lush_caves", "mangrove_swamp", "meadow",
			"mushroom_fields", "nether_wastes", "ocean", "old_growth_birch_forest", "old_growth_pine_taiga",
			"old_growth_spruce_taiga", "pale_garden", "plains", "river", "savanna", "savanna_plateau", "small_end_islands",
			"snowy_beach", "snowy_plains", "snowy_slopes", "snowy_taiga", "soul_sand_valley", "sparse_jungle", "stony_peaks",
			"stony_shore", "sulfur_caves", "sunflower_plains", "swamp", "taiga", "the_end", "the_void", "warm_ocean",
			"warped_forest", "windswept_forest", "windswept_gravelly_hills", "windswept_hills", "windswept_savanna",
			"wooded_badlands");

	@Test
	void everyBiomeHasAGroupExceptTheVoid() {
		assertEquals(67, ALL_26_3.size());
		for (String b : ALL_26_3) {
			if (b.equals("the_void")) continue;
			assertTrue(BiomeGroup.isKnown("minecraft:" + b), "unmapped biome: " + b);
		}
	}

	@Test
	void unknownAndModdedBiomesFallBack() {
		assertEquals(BiomeGroup.OTHER, BiomeGroup.of("minecraft:the_void"));
		assertEquals(BiomeGroup.OTHER, BiomeGroup.of("somemod:crystal_valley"));
	}

	@Test
	void emeraldGroupsMatchGameData() {
		// ore_emerald feature is present in exactly these biomes (26.3 data)
		for (String b : List.of("cherry_grove", "frozen_peaks", "grove", "jagged_peaks", "meadow", "snowy_slopes",
				"stony_peaks", "windswept_forest", "windswept_gravelly_hills", "windswept_hills")) {
			assertTrue(BiomeGroup.of("minecraft:" + b).hasEmerald(), b);
		}
		assertFalse(BiomeGroup.of("minecraft:windswept_savanna").hasEmerald(), "windswept savanna has no emerald ore");
	}

	@Test
	void extraGoldOnlyInBadlands() {
		for (String b : List.of("badlands", "eroded_badlands", "wooded_badlands")) {
			assertTrue(BiomeGroup.of("minecraft:" + b).hasExtraGold(), b);
		}
		assertFalse(BiomeGroup.of("minecraft:desert").hasExtraGold());
	}
}
