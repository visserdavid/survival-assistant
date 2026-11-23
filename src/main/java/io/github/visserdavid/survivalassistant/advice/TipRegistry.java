package io.github.visserdavid.survivalassistant.advice;

import java.util.List;

import static io.github.visserdavid.survivalassistant.advice.Tip.count;
import static io.github.visserdavid.survivalassistant.advice.Tip.milestone;
import static io.github.visserdavid.survivalassistant.advice.Tip.urgent;
import static io.github.visserdavid.survivalassistant.advice.Tip.warning;

/**
 * All tips: the urgent tips and warnings in priority order, then the goals of every chapter in progression order.
 * The order and quantities follow the research documents in docs/ (verified against the 26.3 game data).
 * The chapters themselves are put together in {@link Chapters}.
 */
public final class TipRegistry {
	private TipRegistry() {}

	// ---------------------------------------------------------------- urgent tips and warnings (highest priority first)

	/** A warden nearby: never fight it, sneak away (it hunts by sound). */
	public static final Tip WARDEN = urgent("warden", 2,
			c -> c.creatures().warden());
	/** A creeper explodes ~1.5 s after it starts hissing: run. */
	public static final Tip CREEPER = urgent("creeper", 2,
			c -> c.creatures().creeper());
	/** Breaking the block under your own feet twice in a row: you're digging straight down. */
	public static final Tip DIG_DOWN = urgent("dig_down", 2,
			c -> c.player().digDownStreak() >= 2);
	/** Lava within a few blocks while underground. */
	public static final Tip LAVA_NEAR = urgent("lava_near", 2,
			c -> c.nearby().lava());
	public static final Tip DANGER = urgent("danger", 2,
			c -> c.player().health() <= 6f && c.creatures().hostiles() > 0);
	/** At food <= 6 you can't sprint; at 0 you starve. */
	public static final Tip HUNGRY_EAT = urgent("hungry_eat", 2,
			c -> c.player().food() <= 6 && c.inv().foodItems() > 0);
	public static final Tip HUNGRY_HUNT = urgent("hungry_hunt", 2,
			c -> c.player().food() <= 6 && c.inv().foodItems() == 0);
	public static final Tip DUSK = urgent("dusk", 3,
			c -> c.world().inOverworld() && c.world().isDusk() && !c.world().sheltered() && !c.nearby().litArea());
	public static final Tip NIGHT_OUTSIDE = urgent("night_outside", 2,
			c -> c.world().inOverworld() && c.world().isNight() && !c.world().sheltered() && !c.nearby().litArea());
	/** Phantoms after 3 nights without sleep (verified: PhantomSpawner, 72000 ticks); warn from the 2nd night. */
	public static final Tip PHANTOMS = warning("phantoms", 2,
			c -> c.world().inOverworld() && c.world().isNight()
					&& c.player().ticksSinceRest() >= SurvivalContext.PHANTOM_WARNING_TICKS);
	/** A ghast nearby in the Nether: its fireballs can be hit back. */
	public static final Tip GHAST = warning("ghast", 2,
			c -> c.world().inNether() && c.creatures().ghast());
	/** Piglins attack players without golden armor (item tag piglin_safe_armor). */
	public static final Tip PIGLINS = warning("piglins", 2,
			c -> c.world().inNether() && c.creatures().piglin() && !c.inv().goldArmorWorn());
	/** Monsters can spawn in daylight during thunderstorms (not where it's well lit). */
	public static final Tip THUNDER = warning("thunder", 1,
			c -> c.world().inOverworld() && c.world().thundering() && !c.world().sheltered() && !c.nearby().litArea());
	/** Sand or gravel right above your head underground can fall on you. */
	public static final Tip FALLING_BLOCK = warning("falling_block", 1,
			c -> c.world().underground() && c.nearby().fallingBlockAbove());
	/** Underground in the dark without torches: never dig straight down, bring torches. Not in a well-lit cave. */
	public static final Tip UNDERGROUND = warning("underground", 2,
			c -> c.world().underground() && c.inv().torches() == 0 && !c.nearby().litArea());

	/** Checked in this order: the first one that applies wins. */
	public static final List<Tip> URGENT = List.of(WARDEN, CREEPER, DIG_DOWN, LAVA_NEAR, DANGER, HUNGRY_EAT, HUNGRY_HUNT,
			DUSK, NIGHT_OUTSIDE, PHANTOMS, GHAST, PIGLINS, THUNDER, FALLING_BLOCK, UNDERGROUND);

	// ---------------------------------------------------------------- chapter 1: the first days (docs/research-first-days.md)
	// "done" checks include later progress, so skipping ahead counts.

	public static final Tip WOOD = count("wood", 3, c -> c.inv().logs(), 10,
			c -> c.inv().planks() >= 24 || c.inv().hasCraftingTable() || c.inv().pickaxeTier() >= 1);
	public static final Tip CRAFTING_TABLE = milestone("crafting_table", 2,
			c -> c.inv().hasCraftingTable() || c.inv().pickaxeTier() >= 1);
	public static final Tip WOODEN_PICKAXE = milestone("wooden_pickaxe", 2,
			c -> c.inv().pickaxeTier() >= 1);
	/** A stone (or better) pickaxe and sword: the counter shows how many of the two you have. */
	public static final Tip STONE_TOOLS = count("stone_tools", 3,
			c -> (c.inv().pickaxeTier() >= 2 ? 1 : 0) + (c.inv().swordTier() >= 2 ? 1 : 0), 2);
	public static final Tip FURNACE = milestone("furnace", 2,
			c -> c.inv().hasFurnace());
	public static final Tip FOOD = count("food", 3, c -> c.inv().foodItems(), 5);
	/** Done with a bed; the counter shows the wool for one. */
	public static final Tip BED = milestone("bed", 2,
			c -> c.inv().hasBed(),
			c -> c.inv().wool(), 3);
	public static final Tip TORCHES = count("torches", 2, c -> c.inv().torches(), 8);
	public static final Tip SHELTER = milestone("shelter", 3,
			c -> c.world().dayIndex() >= 1 || (c.world().sheltered() && c.world().timeOfDay() >= SurvivalContext.SUNSET));
	public static final Tip CHEST = milestone("chest", 1,
			c -> c.inv().hasChest());
	public static final Tip IRON = count("iron", 3, c -> c.inv().iron(), 3,
			c -> c.inv().pickaxeTier() >= 3 || c.inv().hasShield() || c.inv().hasIronArmor());
	public static final Tip SHIELD = milestone("shield", 2,
			c -> c.inv().hasShield());
	public static final Tip IRON_PICKAXE = milestone("iron_pickaxe", 1,
			c -> c.inv().pickaxeTier() >= 3);
	public static final Tip IRON_ARMOR = milestone("iron_armor", 2,
			c -> c.inv().hasIronArmor());
	public static final Tip WATER_BUCKET = milestone("water_bucket", 1,
			c -> c.inv().hasWaterBucket());

	public static final List<Tip> CHAPTER_FIRST_DAYS = List.of(
			WOOD, CRAFTING_TABLE, WOODEN_PICKAXE, STONE_TOOLS, FURNACE, FOOD, BED, TORCHES, SHELTER,
			CHEST, IRON, SHIELD, IRON_PICKAXE,
			IRON_ARMOR, WATER_BUCKET);

	// ---------------------------------------------------------------- chapter 2: settling in (docs/research-chapter-2.md)

	public static final Tip FARM = count("farm", 2, c -> c.nearby().crops(), 6);
	public static final Tip BREAD = count("bread", 1, c -> c.inv().bread(), 3);
	public static final Tip BREED = count("breed", 2, c -> c.player().animalsBred(), 2);
	public static final Tip FULL_IRON_ARMOR = count("full_iron_armor", 1, c -> c.inv().armorPieces(), 4);
	/** A bow and 16 arrows: the counter shows the arrows. */
	public static final Tip BOW = milestone("bow", 2,
			c -> c.inv().hasBow() && c.inv().arrows() >= 16,
			c -> c.inv().arrows(), 16);
	public static final Tip DIAMONDS = count("diamonds", 2, c -> c.inv().diamonds(), 3,
			c -> c.inv().pickaxeTier() >= 4);
	public static final Tip DIAMOND_PICKAXE = milestone("diamond_pickaxe", 1,
			c -> c.inv().pickaxeTier() >= 4);
	public static final Tip OBSIDIAN = count("obsidian", 2, c -> c.inv().obsidian(), 10);
	public static final Tip FLINT_AND_STEEL = milestone("flint_and_steel", 1,
			c -> c.inv().hasFlintAndSteel());
	public static final Tip ENCHANTING_TABLE = milestone("enchanting_table", 1,
			c -> c.inv().hasEnchantingTable());

	public static final List<Tip> CHAPTER_SETTLING_IN = List.of(
			FARM, BREAD, BREED, FULL_IRON_ARMOR, BOW, DIAMONDS, DIAMOND_PICKAXE, OBSIDIAN, FLINT_AND_STEEL, ENCHANTING_TABLE);

	// ---------------------------------------------------------------- chapter 3: villages and trading (docs/research-chapters-3-5.md)

	public static final Tip FIND_VILLAGE = milestone("find_village", 2,
			c -> c.world().inVillage() || c.player().has(Advancements.TRADE));
	public static final Tip FIRST_TRADE = milestone("first_trade", 2,
			c -> c.player().has(Advancements.TRADE) || c.player().trades() >= 1);
	public static final Tip TRADES = count("trades", 2, c -> c.player().trades(), 5);
	public static final Tip EMERALDS = count("emeralds", 2, c -> c.inv().emeralds(), 10);
	public static final Tip ENCHANTED_BOOK = milestone("enchanted_book", 2,
			c -> c.inv().hasEnchantedBook());
	public static final Tip IRON_GOLEM = milestone("iron_golem", 2,
			c -> c.player().has(Advancements.IRON_GOLEM));

	public static final List<Tip> CHAPTER_VILLAGE = List.of(FIND_VILLAGE, FIRST_TRADE, TRADES, EMERALDS, ENCHANTED_BOOK, IRON_GOLEM);

	// ---------------------------------------------------------------- chapter 4: the Nether

	public static final Tip GOLD_ARMOR = milestone("gold_armor", 2,
			c -> c.inv().goldArmorWorn());
	public static final Tip ENTER_NETHER = milestone("enter_nether", 2,
			c -> c.player().has(Advancements.ENTER_NETHER) || c.world().inNether());
	public static final Tip FIND_FORTRESS = milestone("find_fortress", 2,
			c -> c.player().has(Advancements.FORTRESS));
	public static final Tip BLAZE_RODS = count("blaze_rods", 2, c -> c.inv().blazeRods(), 3);
	public static final Tip NETHER_WART = count("nether_wart", 2, c -> c.inv().netherWart(), 4);
	public static final Tip HOME_SAFE = milestone("home_safe", 2,
			c -> c.world().inOverworld() && c.inv().blazeRods() >= 1 && c.player().has(Advancements.ENTER_NETHER));

	public static final List<Tip> CHAPTER_NETHER = List.of(GOLD_ARMOR, ENTER_NETHER, FIND_FORTRESS, BLAZE_RODS, NETHER_WART, HOME_SAFE);

	// ---------------------------------------------------------------- chapter 5: brewing and enchanting

	public static final Tip BREWING_STAND = milestone("brewing_stand", 2,
			c -> c.inv().hasBrewingStand() || c.player().has(Advancements.BREW));
	public static final Tip BREW_POTION = milestone("brew_potion", 2,
			c -> c.player().has(Advancements.BREW));
	public static final Tip BOOKSHELVES = count("bookshelves", 2, c -> c.nearby().bookshelves(), 15);
	public static final Tip LEVEL_30 = count("level_30", 2, c -> c.player().xpLevel(), 30);
	public static final Tip ENCHANT = milestone("enchant", 2,
			c -> c.player().has(Advancements.ENCHANT));
	public static final Tip ANVIL = milestone("anvil", 2,
			c -> c.inv().hasAnvil());

	public static final List<Tip> CHAPTER_MAGIC = List.of(BREWING_STAND, BREW_POTION, BOOKSHELVES, LEVEL_30, ENCHANT, ANVIL);

	// ---------------------------------------------------------------- chapter 6: farming and animals (docs/research-chapters-6-8.md)

	public static final Tip TAME_ANIMAL = milestone("tame_animal", 2,
			c -> c.player().has(Advancements.TAME));
	public static final Tip MILK = milestone("milk", 2,
			c -> c.inv().hasMilk());
	public static final Tip SUGAR_CANE = count("sugar_cane", 2, c -> c.inv().sugarCane(), 16);
	public static final Tip FISHING = milestone("fishing", 2,
			c -> c.player().has(Advancements.FISH));
	public static final Tip HONEY = milestone("honey", 2,
			c -> c.player().has(Advancements.HONEY));
	public static final Tip HORSE = milestone("horse", 2,
			c -> c.creatures().ridingTamedHorse());

	public static final List<Tip> CHAPTER_HUSBANDRY = List.of(TAME_ANIMAL, MILK, SUGAR_CANE, FISHING, HONEY, HORSE);

	// ---------------------------------------------------------------- chapter 7: the ocean

	public static final Tip BOAT = milestone("boat", 2,
			c -> c.inv().hasBoat());
	public static final Tip FIND_WRECK = milestone("find_wreck", 2,
			c -> c.world().inWreck());
	public static final Tip TREASURE = milestone("treasure", 2,
			c -> c.inv().hasHeartOfTheSea());
	public static final Tip TURTLE_HELMET = milestone("turtle_helmet", 2,
			c -> c.inv().hasTurtleHelmet());
	public static final Tip TRIDENT = milestone("trident", 2,
			c -> c.inv().hasTrident() || c.player().has(Advancements.TRIDENT));

	public static final List<Tip> CHAPTER_OCEAN = List.of(BOAT, FIND_WRECK, TREASURE, TURTLE_HELMET, TRIDENT);

	// ---------------------------------------------------------------- chapter 8: exploring

	public static final Tip MAP = milestone("map", 2,
			c -> c.inv().hasFilledMap());
	public static final Tip SPYGLASS = milestone("spyglass", 2,
			c -> c.inv().hasSpyglass());
	public static final Tip FIND_TEMPLE = milestone("find_temple", 2,
			c -> c.world().inTemple());
	public static final Tip TRIAL_CHAMBERS = milestone("trial_chambers", 2,
			c -> c.player().has(Advancements.TRIAL_CHAMBERS) || c.world().in(Structures.TRIAL_CHAMBERS));
	public static final Tip OPEN_VAULT = milestone("open_vault", 2,
			c -> c.player().has(Advancements.VAULT));

	public static final List<Tip> CHAPTER_EXPLORER = List.of(MAP, SPYGLASS, FIND_TEMPLE, TRIAL_CHAMBERS, OPEN_VAULT);

	// ---------------------------------------------------------------- chapter 9: redstone (docs/research-chapters-9-10.md)

	public static final Tip REDSTONE_DUST = count("redstone_dust", 2, c -> c.inv().redstone(), 16);
	public static final Tip IRON_DOOR = milestone("iron_door", 2,
			c -> c.nearby().ironDoorOpen());
	public static final Tip PISTON = milestone("piston", 2,
			c -> c.nearby().pistonExtended());
	public static final Tip REPEATER = milestone("repeater", 2,
			c -> c.nearby().repeater());
	public static final Tip HOPPER = milestone("hopper", 2,
			c -> c.nearby().hopper());

	public static final List<Tip> CHAPTER_REDSTONE = List.of(REDSTONE_DUST, IRON_DOOR, PISTON, REPEATER, HOPPER);

	// ---------------------------------------------------------------- chapter 10: the End

	public static final Tip ENDER_PEARLS = count("ender_pearls", 2, c -> c.inv().enderPearls(), 12,
			c -> c.player().has(Advancements.STRONGHOLD));
	public static final Tip EYES_OF_ENDER = count("eyes_of_ender", 2, c -> c.inv().eyesOfEnder(), 12,
			c -> c.player().has(Advancements.ENTER_END));
	public static final Tip FIND_STRONGHOLD = milestone("find_stronghold", 2,
			c -> c.player().has(Advancements.STRONGHOLD));
	public static final Tip ENTER_END = milestone("enter_end", 2,
			c -> c.player().has(Advancements.ENTER_END) || c.world().inEnd());
	public static final Tip DRAGON = milestone("dragon", 2,
			c -> c.player().has(Advancements.DRAGON));
	public static final Tip END_CITY = milestone("end_city", 2,
			c -> c.player().has(Advancements.END_CITY));
	public static final Tip ELYTRA = milestone("elytra", 2,
			c -> c.inv().hasElytra() || c.player().has(Advancements.ELYTRA));

	public static final List<Tip> CHAPTER_END = List.of(ENDER_PEARLS, EYES_OF_ENDER, FIND_STRONGHOLD, ENTER_END, DRAGON, END_CITY, ELYTRA);

	// ---------------------------------------------------------------- chapter 11: netherite and bastions (docs/research-chapters-11-14.md)

	public static final Tip ANCIENT_DEBRIS = milestone("ancient_debris", 2,
			c -> c.player().has(Advancements.DEBRIS));
	public static final Tip NETHERITE_INGOT = milestone("netherite_ingot", 2,
			c -> c.inv().hasNetheriteIngot() || c.inv().hasNetheriteGear());
	public static final Tip FIND_BASTION = milestone("find_bastion", 2,
			c -> c.player().has(Advancements.BASTION));
	public static final Tip UPGRADE_TEMPLATE = milestone("upgrade_template", 2,
			c -> c.inv().hasUpgradeTemplate() || c.inv().hasNetheriteGear());
	public static final Tip NETHERITE_GEAR = milestone("netherite_gear", 2,
			c -> c.inv().hasNetheriteGear());
	public static final Tip RESPAWN_ANCHOR = milestone("respawn_anchor", 2,
			c -> c.player().has(Advancements.ANCHOR));

	public static final List<Tip> CHAPTER_NETHERITE = List.of(ANCIENT_DEBRIS, NETHERITE_INGOT, FIND_BASTION, UPGRADE_TEMPLATE,
			NETHERITE_GEAR, RESPAWN_ANCHOR);

	// ---------------------------------------------------------------- chapter 12: the Wither and the beacon

	public static final Tip WITHER_SKULLS = count("wither_skulls", 2, c -> c.inv().witherSkulls(), 3,
			c -> c.player().has(Advancements.SUMMON_WITHER));
	public static final Tip SOUL_SAND = count("soul_sand", 2, c -> c.inv().soulSand(), 4,
			c -> c.player().has(Advancements.SUMMON_WITHER));
	public static final Tip SUMMON_WITHER = milestone("summon_wither", 2,
			c -> c.player().has(Advancements.SUMMON_WITHER));
	public static final Tip NETHER_STAR = milestone("nether_star", 2,
			c -> c.inv().hasNetherStar() || c.player().has(Advancements.BEACON));
	public static final Tip BEACON = milestone("beacon", 2,
			c -> c.player().has(Advancements.BEACON));
	public static final Tip FULL_BEACON = milestone("full_beacon", 2,
			c -> c.player().has(Advancements.FULL_BEACON));

	public static final List<Tip> CHAPTER_WITHER = List.of(WITHER_SKULLS, SOUL_SAND, SUMMON_WITHER, NETHER_STAR, BEACON, FULL_BEACON);

	// ---------------------------------------------------------------- chapter 13: the Deep Dark and the ancient city

	public static final Tip FIND_ANCIENT_CITY = milestone("find_ancient_city", 2,
			c -> c.world().in(Structures.ANCIENT_CITY));
	public static final Tip SNEAK_PAST = milestone("sneak_past", 2,
			c -> c.player().has(Advancements.SNEAK));
	public static final Tip ECHO_SHARDS = count("echo_shards", 2, c -> c.inv().echoShards(), 8,
			c -> c.inv().hasRecoveryCompass());
	public static final Tip RECOVERY_COMPASS = milestone("recovery_compass", 2,
			c -> c.inv().hasRecoveryCompass());

	public static final List<Tip> CHAPTER_DEEP_DARK = List.of(FIND_ANCIENT_CITY, SNEAK_PAST, ECHO_SHARDS, RECOVERY_COMPASS);

	// ---------------------------------------------------------------- chapter 14: raids and illagers

	public static final Tip FIND_OUTPOST = milestone("find_outpost", 2,
			c -> c.world().in(Structures.PILLAGER_OUTPOST) || c.player().has(Advancements.RAID_CAPTAIN));
	public static final Tip RAID_CAPTAIN = milestone("raid_captain", 2,
			c -> c.player().has(Advancements.RAID_CAPTAIN));
	public static final Tip OMINOUS_BOTTLE = milestone("ominous_bottle", 2,
			c -> c.inv().hasOminousBottle() || c.player().has(Advancements.RAID));
	public static final Tip WIN_RAID = milestone("win_raid", 2,
			c -> c.player().has(Advancements.RAID));
	public static final Tip TOTEM = milestone("totem", 2,
			c -> c.inv().hasTotem() || c.player().has(Advancements.TOTEM));

	public static final List<Tip> CHAPTER_RAIDS = List.of(FIND_OUTPOST, RAID_CAPTAIN, OMINOUS_BOTTLE, WIN_RAID, TOTEM);

	// ---------------------------------------------------------------- chapter 15: archaeology and trims (docs/research-chapters-15-18.md)

	public static final Tip BRUSH = milestone("brush", 2,
			c -> c.inv().hasBrush() || c.player().has(Advancements.SHERD));
	public static final Tip FIND_TRAIL_RUINS = milestone("find_trail_ruins", 2,
			c -> c.world().in(Structures.TRAIL_RUINS));
	public static final Tip SHERD = milestone("sherd", 2,
			c -> c.player().has(Advancements.SHERD));
	public static final Tip DECORATED_POT = milestone("decorated_pot", 2,
			c -> c.inv().hasDecoratedPot());
	public static final Tip ARMOR_TRIM = milestone("armor_trim", 2,
			c -> c.player().has(Advancements.TRIM));
	public static final Tip SNIFFER_EGG = milestone("sniffer_egg", 2,
			c -> c.player().has(Advancements.SNIFFER_EGG));

	public static final List<Tip> CHAPTER_ARCHAEOLOGY = List.of(BRUSH, FIND_TRAIL_RUINS, SHERD, DECORATED_POT, ARMOR_TRIM, SNIFFER_EGG);

	// ---------------------------------------------------------------- chapter 16: trial challenges

	public static final Tip BREEZE_RODS = count("breeze_rods", 2, c -> c.inv().breezeRods(), 2,
			c -> c.inv().hasMace());
	public static final Tip WIND_CHARGE = milestone("wind_charge", 2,
			c -> c.inv().hasWindCharge());
	public static final Tip OMINOUS_VAULT = milestone("ominous_vault", 2,
			c -> c.player().has(Advancements.OMINOUS_VAULT));
	public static final Tip HEAVY_CORE = milestone("heavy_core", 2,
			c -> c.inv().hasHeavyCore() || c.inv().hasMace());
	public static final Tip MACE = milestone("mace", 2,
			c -> c.inv().hasMace());

	public static final List<Tip> CHAPTER_TRIALS = List.of(BREEZE_RODS, WIND_CHARGE, OMINOUS_VAULT, HEAVY_CORE, MACE);

	// ---------------------------------------------------------------- chapter 17: the happy ghast

	public static final Tip DRIED_GHAST = milestone("dried_ghast", 2,
			c -> c.inv().hasDriedGhast() || c.player().has(Advancements.DRIED_GHAST));
	public static final Tip HYDRATE_GHAST = milestone("hydrate_ghast", 2,
			c -> c.player().has(Advancements.DRIED_GHAST));
	public static final Tip GHASTLING = milestone("ghastling", 2,
			c -> c.creatures().ghastling() || c.creatures().ridingHappyGhast());
	public static final Tip HARNESS = milestone("harness", 2,
			c -> c.inv().hasHarness() || c.creatures().ridingHappyGhast());
	public static final Tip RIDE_HAPPY_GHAST = milestone("ride_happy_ghast", 2,
			c -> c.creatures().ridingHappyGhast());

	public static final List<Tip> CHAPTER_HAPPY_GHAST = List.of(DRIED_GHAST, HYDRATE_GHAST, GHASTLING, HARNESS, RIDE_HAPPY_GHAST);

	// ---------------------------------------------------------------- chapter 18: automation

	public static final Tip OBSERVER = milestone("observer", 2,
			c -> c.nearby().observer());
	public static final Tip AUTO_SMELTER = milestone("auto_smelter", 2,
			c -> c.nearby().autoSmelter());
	public static final Tip COPPER_BULB = milestone("copper_bulb", 2,
			c -> c.nearby().copperBulbLit());
	public static final Tip CRAFTER = milestone("crafter", 2,
			c -> c.nearby().crafter() || c.player().has(Advancements.CRAFTER));
	public static final Tip CRAFTER_CRAFTS = milestone("crafter_crafts", 2,
			c -> c.player().has(Advancements.CRAFTER));

	public static final List<Tip> CHAPTER_AUTOMATION = List.of(OBSERVER, AUTO_SMELTER, COPPER_BULB, CRAFTER, CRAFTER_CRAFTS);
}
