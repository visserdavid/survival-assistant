package io.github.visserdavid.survivalassistant.advice;

/**
 * How the collector rates tools and armor, from the item id path (such as {@code iron_pickaxe}). Pure logic, so it
 * can be tested without the game; the collector only asks for items it already knows are tools or armor.
 */
public final class Gear {
	private Gear() {}

	/** 1 wood or gold, 2 stone or copper, 3 iron, 4 diamond, netherite or anything else (modded tools count as top). */
	public static int toolTier(String path) {
		if (path.startsWith("wooden_") || path.startsWith("golden_")) return 1;
		if (path.startsWith("stone_") || path.startsWith("copper_")) return 2;
		if (path.startsWith("iron_")) return 3;
		return 4;
	}

	/** Iron, diamond or netherite: armor that counts for the "iron armor" goals. */
	public static boolean ironOrBetter(String path) {
		return path.startsWith("iron_") || path.startsWith("diamond_") || path.startsWith("netherite_");
	}

	/** A netherite tool, weapon or armor piece (made in a smithing table, so not the ingot, scrap, block or template). */
	public static boolean isNetheriteGear(String path) {
		return path.startsWith("netherite_") && !path.equals("netherite_ingot") && !path.equals("netherite_scrap")
				&& !path.equals("netherite_block") && !path.endsWith("smithing_template");
	}
}
