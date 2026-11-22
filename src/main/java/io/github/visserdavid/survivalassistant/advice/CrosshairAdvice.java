package io.github.visserdavid.survivalassistant.advice;

import java.util.Set;

/**
 * What the crosshair info line says about the thing the player looks at: a short tip per mob, and for ores
 * which pickaxe is needed. Pure logic; the client looks up the block tags and the held tool.
 */
public final class CrosshairAdvice {
	private CrosshairAdvice() {}

	/** Ticks the player must keep looking at the same thing before the line appears (about 0.3 s). */
	public static final int DWELL_TICKS = 6;

	/** Mobs with a tip (lang key {@code survivalassistant.mob.<id>}); ids are the entity type paths. */
	public static final Set<String> MOBS = Set.of(
			// hostile
			"creeper", "zombie", "zombie_villager", "husk", "drowned", "skeleton", "stray", "bogged", "parched",
			"spider", "cave_spider", "enderman", "witch", "slime", "phantom", "pillager", "silverfish", "creaking",
			// useful animals
			"sheep", "cow", "pig", "chicken", "wolf");

	public static boolean hasMobTip(String entityPath) {
		return MOBS.contains(entityPath);
	}

	/** Blocks worth a tool hint: ores and the obsidian family. Plain stone would make the line pop up all the time. */
	public static boolean isInterestingBlock(String blockPath) {
		return blockPath.endsWith("_ore") || blockPath.equals("obsidian") || blockPath.equals("crying_obsidian")
				|| blockPath.equals("ancient_debris");
	}

	public enum Pickaxe {
		WOODEN, STONE, IRON, DIAMOND;

		public String langKey() {
			return "survivalassistant.tool." + name().toLowerCase(java.util.Locale.ROOT) + "_pickaxe";
		}
	}

	/** The weakest pickaxe that still gives drops, from the vanilla "needs ... tool" block tags. */
	public static Pickaxe requiredPickaxe(boolean needsDiamond, boolean needsIron, boolean needsStone) {
		if (needsDiamond) return Pickaxe.DIAMOND;
		if (needsIron) return Pickaxe.IRON;
		if (needsStone) return Pickaxe.STONE;
		return Pickaxe.WOODEN;
	}
}
