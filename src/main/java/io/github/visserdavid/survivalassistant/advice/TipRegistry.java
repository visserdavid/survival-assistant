package io.github.visserdavid.survivalassistant.advice;

import java.util.List;

/**
 * All tips. Urgent tips are listed in priority order; milestones in progression order.
 * The order and quantities follow docs/research-first-days.md (verified against the 26.3 game data).
 */
public final class TipRegistry {
	private TipRegistry() {}

	// --- Urgent tips and warnings (highest priority first) ---
	/** A creeper explodes ~1.5 s after it starts hissing: run. */
	public static final Tip CREEPER = Tip.urgent("creeper", 2,
			SurvivalContext::creeperNearby);
	/** Breaking the block under your own feet twice in a row: you're digging straight down. */
	public static final Tip DIG_DOWN = Tip.urgent("dig_down", 2,
			c -> c.digDownStreak() >= 2);
	/** Lava within a few blocks while underground. */
	public static final Tip LAVA_NEAR = Tip.urgent("lava_near", 2,
			SurvivalContext::lavaNearby);
	public static final Tip DANGER = Tip.urgent("danger", 2,
			c -> c.health() <= 6f && c.hostilesNearby() > 0);
	/** At food <= 6 you can't sprint; at 0 you starve. */
	public static final Tip HUNGRY_EAT = Tip.urgent("hungry_eat", 2,
			c -> c.food() <= 6 && c.foodItems() > 0);
	public static final Tip HUNGRY_HUNT = Tip.urgent("hungry_hunt", 2,
			c -> c.food() <= 6 && c.foodItems() == 0);
	public static final Tip DUSK = Tip.urgent("dusk", 3,
			c -> c.isDusk() && !c.sheltered());
	public static final Tip NIGHT_OUTSIDE = Tip.urgent("night_outside", 2,
			c -> c.isNight() && !c.sheltered());
	/** Phantoms after 3 nights without sleep (verified: PhantomSpawner, 72000 ticks); warn from the 2nd night. */
	public static final Tip PHANTOMS = Tip.warning("phantoms", 2,
			c -> c.isNight() && c.ticksSinceRest() >= SurvivalContext.PHANTOM_WARNING_TICKS);
	/** Sand or gravel right above your head underground can fall on you. */
	public static final Tip FALLING_BLOCK = Tip.warning("falling_block", 1,
			c -> c.underground() && c.fallingBlockAbove());
	/** Monsters can spawn in daylight during thunderstorms. */
	public static final Tip THUNDER = Tip.warning("thunder", 1,
			c -> c.thundering() && !c.sheltered());
	/** Underground without light: never dig straight down, bring torches. */
	public static final Tip UNDERGROUND = Tip.warning("underground", 2,
			c -> c.underground() && c.torches() == 0);

	public static final List<Tip> URGENT = List.of(CREEPER, DIG_DOWN, LAVA_NEAR, DANGER, HUNGRY_EAT, HUNGRY_HUNT, DUSK, NIGHT_OUTSIDE,
			PHANTOMS, THUNDER, FALLING_BLOCK, UNDERGROUND);

	// --- Day 1: progression milestones. "done" checks include later progress, so skipping ahead counts. ---
	public static final Tip WOOD = Tip.milestone("wood", 3,
			c -> c.logs() >= 10 || c.planks() >= 24 || c.hasCraftingTable() || c.pickaxeTier() >= 1,
			SurvivalContext::logs, 10);
	public static final Tip CRAFTING_TABLE = Tip.milestone("crafting_table", 2,
			c -> c.hasCraftingTable() || c.pickaxeTier() >= 1);
	public static final Tip WOODEN_PICKAXE = Tip.milestone("wooden_pickaxe", 2,
			c -> c.pickaxeTier() >= 1);
	public static final Tip STONE_TOOLS = Tip.milestone("stone_tools", 3,
			c -> c.pickaxeTier() >= 2 && c.swordTier() >= 2,
			c -> (c.pickaxeTier() >= 2 ? 1 : 0) + (c.swordTier() >= 2 ? 1 : 0), 2);
	public static final Tip FURNACE = Tip.milestone("furnace", 2,
			SurvivalContext::hasFurnace);
	public static final Tip FOOD = Tip.milestone("food", 3,
			c -> c.foodItems() >= 5,
			SurvivalContext::foodItems, 5);
	public static final Tip BED = Tip.milestone("bed", 2,
			SurvivalContext::hasBed,
			SurvivalContext::wool, 3);
	public static final Tip TORCHES = Tip.milestone("torches", 2,
			c -> c.torches() >= 8,
			SurvivalContext::torches, 8);
	public static final Tip SHELTER = Tip.milestone("shelter", 3,
			c -> c.dayIndex() >= 1 || (c.sheltered() && c.timeOfDay() >= SurvivalContext.SUNSET));

	// --- Day 2 ---
	public static final Tip CHEST = Tip.milestone("chest", 1,
			SurvivalContext::hasChest);
	public static final Tip IRON = Tip.milestone("iron", 3,
			c -> c.iron() >= 3 || c.pickaxeTier() >= 3 || c.hasShield() || c.hasIronArmor(),
			SurvivalContext::iron, 3);
	public static final Tip SHIELD = Tip.milestone("shield", 2,
			SurvivalContext::hasShield);
	public static final Tip IRON_PICKAXE = Tip.milestone("iron_pickaxe", 1,
			c -> c.pickaxeTier() >= 3);

	// --- Day 3 ---
	public static final Tip IRON_ARMOR = Tip.milestone("iron_armor", 2,
			SurvivalContext::hasIronArmor);
	public static final Tip WATER_BUCKET = Tip.milestone("water_bucket", 1,
			SurvivalContext::hasWaterBucket);

	public static final List<Tip> MILESTONES = List.of(
			WOOD, CRAFTING_TABLE, WOODEN_PICKAXE, STONE_TOOLS, FURNACE, FOOD, BED, TORCHES, SHELTER,
			CHEST, IRON, SHIELD, IRON_PICKAXE,
			IRON_ARMOR, WATER_BUCKET);

	// --- Chapter 2: settling in (docs/research-chapter-2.md, verified against the 26.3 game data) ---
	public static final Tip FARM = Tip.milestone("farm", 2,
			c -> c.cropsNearby() >= 6,
			SurvivalContext::cropsNearby, 6);
	public static final Tip BREAD = Tip.milestone("bread", 1,
			c -> c.bread() >= 3,
			SurvivalContext::bread, 3);
	public static final Tip BREED = Tip.milestone("breed", 2,
			c -> c.animalsBred() >= 2,
			SurvivalContext::animalsBred, 2);
	public static final Tip FULL_IRON_ARMOR = Tip.milestone("full_iron_armor", 1,
			c -> c.armorPieces() >= 4,
			SurvivalContext::armorPieces, 4);
	public static final Tip BOW = Tip.milestone("bow", 2,
			c -> c.hasBow() && c.arrows() >= 16,
			SurvivalContext::arrows, 16);
	public static final Tip DIAMONDS = Tip.milestone("diamonds", 2,
			c -> c.diamonds() >= 3 || c.pickaxeTier() >= 4,
			SurvivalContext::diamonds, 3);
	public static final Tip DIAMOND_PICKAXE = Tip.milestone("diamond_pickaxe", 1,
			c -> c.pickaxeTier() >= 4);
	public static final Tip OBSIDIAN = Tip.milestone("obsidian", 2,
			c -> c.obsidian() >= 10,
			SurvivalContext::obsidian, 10);
	public static final Tip FLINT_AND_STEEL = Tip.milestone("flint_and_steel", 1,
			SurvivalContext::hasFlintAndSteel);
	public static final Tip ENCHANTING_TABLE = Tip.milestone("enchanting_table", 1,
			SurvivalContext::hasEnchantingTable);

	public static final List<Tip> CHAPTER_2 = List.of(
			FARM, BREAD, BREED, FULL_IRON_ARMOR, BOW, DIAMONDS, DIAMOND_PICKAXE, OBSIDIAN, FLINT_AND_STEEL, ENCHANTING_TABLE);

	public static final int LAST_CHAPTER = 2;

	/** The goals of a chapter (1 or 2). */
	public static List<Tip> milestones(int chapter) {
		return chapter >= 2 ? CHAPTER_2 : MILESTONES;
	}

	/** True when every goal of the chapter is in {@code completed}. */
	public static boolean chapterDone(int chapter, java.util.Set<String> completed) {
		for (Tip m : milestones(chapter)) if (!completed.contains(m.id())) return false;
		return true;
	}
}
