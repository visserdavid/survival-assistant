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
}
