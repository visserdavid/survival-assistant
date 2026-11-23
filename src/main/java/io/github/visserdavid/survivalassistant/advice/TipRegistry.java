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
			c -> c.inOverworld() && c.isDusk() && !c.sheltered());
	public static final Tip NIGHT_OUTSIDE = Tip.urgent("night_outside", 2,
			c -> c.inOverworld() && c.isNight() && !c.sheltered());
	/** Phantoms after 3 nights without sleep (verified: PhantomSpawner, 72000 ticks); warn from the 2nd night. */
	public static final Tip PHANTOMS = Tip.warning("phantoms", 2,
			c -> c.inOverworld() && c.isNight() && c.ticksSinceRest() >= SurvivalContext.PHANTOM_WARNING_TICKS);
	/** A ghast nearby in the Nether: its fireballs can be hit back. */
	public static final Tip GHAST = Tip.warning("ghast", 2,
			c -> c.inNether() && c.ghastNearby());
	/** Piglins attack players without golden armor (item tag piglin_safe_armor). */
	public static final Tip PIGLINS = Tip.warning("piglins", 2,
			c -> c.inNether() && c.piglinNearby() && !c.goldArmorWorn());
	/** Sand or gravel right above your head underground can fall on you. */
	public static final Tip FALLING_BLOCK = Tip.warning("falling_block", 1,
			c -> c.underground() && c.fallingBlockAbove());
	/** Monsters can spawn in daylight during thunderstorms. */
	public static final Tip THUNDER = Tip.warning("thunder", 1,
			c -> c.inOverworld() && c.thundering() && !c.sheltered());
	/** Underground without light: never dig straight down, bring torches. */
	public static final Tip UNDERGROUND = Tip.warning("underground", 2,
			c -> c.underground() && c.torches() == 0);

	/** A warden nearby: never fight it, sneak away (it hunts by sound). */
	public static final Tip WARDEN = Tip.urgent("warden", 2,
			SurvivalContext::wardenNearby);

	public static final List<Tip> URGENT = List.of(WARDEN, CREEPER, DIG_DOWN, LAVA_NEAR, DANGER, HUNGRY_EAT, HUNGRY_HUNT, DUSK, NIGHT_OUTSIDE,
			PHANTOMS, GHAST, PIGLINS, THUNDER, FALLING_BLOCK, UNDERGROUND);

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

	// --- Vanilla advancements used as goals (ids checked in the 26.3 data: data/minecraft/advancement) ---
	public static final String ADV_TRADE = "adventure/trade";
	public static final String ADV_IRON_GOLEM = "adventure/summon_iron_golem";
	public static final String ADV_ENTER_NETHER = "story/enter_the_nether";
	public static final String ADV_FORTRESS = "nether/find_fortress";
	public static final String ADV_BREW = "nether/brew_potion";
	public static final String ADV_ENCHANT = "story/enchant_item";
	public static final String ADV_TAME = "husbandry/tame_an_animal";
	public static final String ADV_FISH = "husbandry/fishy_business";
	public static final String ADV_HONEY = "husbandry/safely_harvest_honey";
	public static final String ADV_TRIDENT = "adventure/throw_trident";
	public static final String ADV_TRIAL_CHAMBERS = "adventure/minecraft_trials_edition";
	public static final String ADV_VAULT = "adventure/under_lock_and_key";
	public static final String ADV_STRONGHOLD = "story/follow_ender_eye";
	public static final String ADV_ENTER_END = "story/enter_the_end";
	public static final String ADV_DRAGON = "end/kill_dragon";
	public static final String ADV_END_CITY = "end/find_end_city";
	public static final String ADV_ELYTRA = "end/elytra";
	public static final String ADV_DEBRIS = "nether/obtain_ancient_debris";
	public static final String ADV_BASTION = "nether/find_bastion";
	public static final String ADV_ANCHOR = "nether/charge_respawn_anchor";
	public static final String ADV_WITHER_SKULL = "nether/get_wither_skull";
	public static final String ADV_SUMMON_WITHER = "nether/summon_wither";
	public static final String ADV_BEACON = "nether/create_beacon";
	public static final String ADV_FULL_BEACON = "nether/create_full_beacon";
	public static final String ADV_SNEAK = "adventure/avoid_vibration";
	public static final String ADV_RAID_CAPTAIN = "adventure/voluntary_exile";
	public static final String ADV_RAID = "adventure/hero_of_the_village";
	public static final String ADV_TOTEM = "adventure/totem_of_undying";
	public static final String ADV_SHERD = "adventure/salvage_sherd";
	public static final String ADV_TRIM = "adventure/trim_with_any_armor_pattern";
	public static final String ADV_SNIFFER_EGG = "husbandry/obtain_sniffer_egg";
	public static final String ADV_OMINOUS_VAULT = "adventure/revaulting";
	public static final String ADV_DRIED_GHAST = "husbandry/place_dried_ghast_in_water";
	public static final String ADV_CRAFTER = "adventure/crafters_crafting_crafters";

	// --- Chapter 3: villages and trading (docs/research-chapters-3-5.md) ---
	public static final Tip FIND_VILLAGE = Tip.milestone("find_village", 2,
			c -> c.inVillage() || c.has(ADV_TRADE));
	public static final Tip FIRST_TRADE = Tip.milestone("first_trade", 2,
			c -> c.has(ADV_TRADE) || c.trades() >= 1);
	public static final Tip TRADES = Tip.milestone("trades", 2,
			c -> c.trades() >= 5,
			SurvivalContext::trades, 5);
	public static final Tip EMERALDS = Tip.milestone("emeralds", 2,
			c -> c.emeralds() >= 10,
			SurvivalContext::emeralds, 10);
	public static final Tip ENCHANTED_BOOK = Tip.milestone("enchanted_book", 2,
			SurvivalContext::hasEnchantedBook);
	public static final Tip IRON_GOLEM = Tip.milestone("iron_golem", 2,
			c -> c.has(ADV_IRON_GOLEM));

	public static final List<Tip> CHAPTER_VILLAGE = List.of(FIND_VILLAGE, FIRST_TRADE, TRADES, EMERALDS, ENCHANTED_BOOK, IRON_GOLEM);

	// --- Chapter 4: the Nether ---
	public static final Tip GOLD_ARMOR = Tip.milestone("gold_armor", 2,
			SurvivalContext::goldArmorWorn);
	public static final Tip ENTER_NETHER = Tip.milestone("enter_nether", 2,
			c -> c.has(ADV_ENTER_NETHER) || c.inNether());
	public static final Tip FIND_FORTRESS = Tip.milestone("find_fortress", 2,
			c -> c.has(ADV_FORTRESS));
	public static final Tip BLAZE_RODS = Tip.milestone("blaze_rods", 2,
			c -> c.blazeRods() >= 3,
			SurvivalContext::blazeRods, 3);
	public static final Tip NETHER_WART = Tip.milestone("nether_wart", 2,
			c -> c.netherWart() >= 4,
			SurvivalContext::netherWart, 4);
	public static final Tip HOME_SAFE = Tip.milestone("home_safe", 2,
			c -> c.inOverworld() && c.blazeRods() >= 1 && c.has(ADV_ENTER_NETHER));

	public static final List<Tip> CHAPTER_NETHER = List.of(GOLD_ARMOR, ENTER_NETHER, FIND_FORTRESS, BLAZE_RODS, NETHER_WART, HOME_SAFE);

	// --- Chapter 5: brewing and enchanting ---
	public static final Tip BREWING_STAND = Tip.milestone("brewing_stand", 2,
			c -> c.hasBrewingStand() || c.has(ADV_BREW));
	public static final Tip BREW_POTION = Tip.milestone("brew_potion", 2,
			c -> c.has(ADV_BREW));
	public static final Tip BOOKSHELVES = Tip.milestone("bookshelves", 2,
			c -> c.bookshelves() >= 15,
			SurvivalContext::bookshelves, 15);
	public static final Tip LEVEL_30 = Tip.milestone("level_30", 2,
			c -> c.xpLevel() >= 30,
			SurvivalContext::xpLevel, 30);
	public static final Tip ENCHANT = Tip.milestone("enchant", 2,
			c -> c.has(ADV_ENCHANT));
	public static final Tip ANVIL = Tip.milestone("anvil", 2,
			SurvivalContext::hasAnvil);

	public static final List<Tip> CHAPTER_MAGIC = List.of(BREWING_STAND, BREW_POTION, BOOKSHELVES, LEVEL_30, ENCHANT, ANVIL);

	// --- Chapter 6: farming and animals (docs/research-chapters-6-8.md) ---
	public static final Tip TAME_ANIMAL = Tip.milestone("tame_animal", 2,
			c -> c.has(ADV_TAME));
	public static final Tip HORSE = Tip.milestone("horse", 2,
			SurvivalContext::ridingTamedHorse);
	public static final Tip MILK = Tip.milestone("milk", 2,
			SurvivalContext::hasMilk);
	public static final Tip SUGAR_CANE = Tip.milestone("sugar_cane", 2,
			c -> c.sugarCane() >= 16,
			SurvivalContext::sugarCane, 16);
	public static final Tip FISHING = Tip.milestone("fishing", 2,
			c -> c.has(ADV_FISH));
	public static final Tip HONEY = Tip.milestone("honey", 2,
			c -> c.has(ADV_HONEY));

	public static final List<Tip> CHAPTER_HUSBANDRY = List.of(TAME_ANIMAL, MILK, SUGAR_CANE, FISHING, HONEY, HORSE);

	// --- Chapter 7: the ocean ---
	public static final Tip BOAT = Tip.milestone("boat", 2,
			SurvivalContext::hasBoat);
	public static final Tip FIND_WRECK = Tip.milestone("find_wreck", 2,
			SurvivalContext::inWreck);
	public static final Tip TREASURE = Tip.milestone("treasure", 2,
			SurvivalContext::hasHeartOfTheSea);
	public static final Tip TURTLE_HELMET = Tip.milestone("turtle_helmet", 2,
			SurvivalContext::hasTurtleHelmet);
	public static final Tip TRIDENT = Tip.milestone("trident", 2,
			c -> c.hasTrident() || c.has(ADV_TRIDENT));

	public static final List<Tip> CHAPTER_OCEAN = List.of(BOAT, FIND_WRECK, TREASURE, TURTLE_HELMET, TRIDENT);

	// --- Chapter 8: exploring ---
	public static final Tip MAP = Tip.milestone("map", 2,
			SurvivalContext::hasFilledMap);
	public static final Tip SPYGLASS = Tip.milestone("spyglass", 2,
			SurvivalContext::hasSpyglass);
	public static final Tip FIND_TEMPLE = Tip.milestone("find_temple", 2,
			SurvivalContext::inTemple);
	public static final Tip TRIAL_CHAMBERS = Tip.milestone("trial_chambers", 2,
			c -> c.has(ADV_TRIAL_CHAMBERS) || c.structure().equals("trial_chambers"));
	public static final Tip OPEN_VAULT = Tip.milestone("open_vault", 2,
			c -> c.has(ADV_VAULT));

	public static final List<Tip> CHAPTER_EXPLORER = List.of(MAP, SPYGLASS, FIND_TEMPLE, TRIAL_CHAMBERS, OPEN_VAULT);

	// --- Chapter 9: redstone (docs/research-chapters-9-10.md) ---
	public static final Tip REDSTONE_DUST = Tip.milestone("redstone_dust", 2,
			c -> c.redstone() >= 16,
			SurvivalContext::redstone, 16);
	public static final Tip IRON_DOOR = Tip.milestone("iron_door", 2,
			SurvivalContext::ironDoorOpen);
	public static final Tip PISTON = Tip.milestone("piston", 2,
			SurvivalContext::pistonExtended);
	public static final Tip REPEATER = Tip.milestone("repeater", 2,
			SurvivalContext::repeaterPlaced);
	public static final Tip HOPPER = Tip.milestone("hopper", 2,
			SurvivalContext::hopperPlaced);

	public static final List<Tip> CHAPTER_REDSTONE = List.of(REDSTONE_DUST, IRON_DOOR, PISTON, REPEATER, HOPPER);

	// --- Chapter 10: the End ---
	public static final Tip ENDER_PEARLS = Tip.milestone("ender_pearls", 2,
			c -> c.enderPearls() >= 12 || c.has(ADV_STRONGHOLD),
			SurvivalContext::enderPearls, 12);
	public static final Tip EYES_OF_ENDER = Tip.milestone("eyes_of_ender", 2,
			c -> c.eyesOfEnder() >= 12 || c.has(ADV_ENTER_END),
			SurvivalContext::eyesOfEnder, 12);
	public static final Tip FIND_STRONGHOLD = Tip.milestone("find_stronghold", 2,
			c -> c.has(ADV_STRONGHOLD));
	public static final Tip ENTER_END = Tip.milestone("enter_end", 2,
			c -> c.has(ADV_ENTER_END) || c.inEnd());
	public static final Tip DRAGON = Tip.milestone("dragon", 2,
			c -> c.has(ADV_DRAGON));
	public static final Tip END_CITY = Tip.milestone("end_city", 2,
			c -> c.has(ADV_END_CITY));
	public static final Tip ELYTRA = Tip.milestone("elytra", 2,
			c -> c.hasElytra() || c.has(ADV_ELYTRA));

	public static final List<Tip> CHAPTER_END = List.of(ENDER_PEARLS, EYES_OF_ENDER, FIND_STRONGHOLD, ENTER_END, DRAGON, END_CITY, ELYTRA);

	// --- Chapter 11: netherite and bastions (docs/research-chapters-11-14.md) ---
	public static final Tip ANCIENT_DEBRIS = Tip.milestone("ancient_debris", 2,
			c -> c.has(ADV_DEBRIS));
	public static final Tip NETHERITE_INGOT = Tip.milestone("netherite_ingot", 2,
			c -> c.hasNetheriteIngot() || c.hasNetheriteGear());
	public static final Tip FIND_BASTION = Tip.milestone("find_bastion", 2,
			c -> c.has(ADV_BASTION));
	public static final Tip UPGRADE_TEMPLATE = Tip.milestone("upgrade_template", 2,
			c -> c.hasUpgradeTemplate() || c.hasNetheriteGear());
	public static final Tip NETHERITE_GEAR = Tip.milestone("netherite_gear", 2,
			SurvivalContext::hasNetheriteGear);
	public static final Tip RESPAWN_ANCHOR = Tip.milestone("respawn_anchor", 2,
			c -> c.has(ADV_ANCHOR));

	public static final List<Tip> CHAPTER_NETHERITE = List.of(ANCIENT_DEBRIS, NETHERITE_INGOT, FIND_BASTION, UPGRADE_TEMPLATE,
			NETHERITE_GEAR, RESPAWN_ANCHOR);

	// --- Chapter 12: the Wither and the beacon ---
	public static final Tip WITHER_SKULLS = Tip.milestone("wither_skulls", 2,
			c -> c.witherSkulls() >= 3 || c.has(ADV_SUMMON_WITHER),
			SurvivalContext::witherSkulls, 3);
	public static final Tip SOUL_SAND = Tip.milestone("soul_sand", 2,
			c -> c.soulSand() >= 4 || c.has(ADV_SUMMON_WITHER),
			SurvivalContext::soulSand, 4);
	public static final Tip SUMMON_WITHER = Tip.milestone("summon_wither", 2,
			c -> c.has(ADV_SUMMON_WITHER));
	public static final Tip NETHER_STAR = Tip.milestone("nether_star", 2,
			c -> c.hasNetherStar() || c.has(ADV_BEACON));
	public static final Tip BEACON = Tip.milestone("beacon", 2,
			c -> c.has(ADV_BEACON));
	public static final Tip FULL_BEACON = Tip.milestone("full_beacon", 2,
			c -> c.has(ADV_FULL_BEACON));

	public static final List<Tip> CHAPTER_WITHER = List.of(WITHER_SKULLS, SOUL_SAND, SUMMON_WITHER, NETHER_STAR, BEACON, FULL_BEACON);

	// --- Chapter 13: the Deep Dark and the ancient city ---
	public static final Tip FIND_ANCIENT_CITY = Tip.milestone("find_ancient_city", 2,
			c -> c.structure().equals("ancient_city"));
	public static final Tip SNEAK_PAST = Tip.milestone("sneak_past", 2,
			c -> c.has(ADV_SNEAK));
	public static final Tip ECHO_SHARDS = Tip.milestone("echo_shards", 2,
			c -> c.echoShards() >= 8 || c.hasRecoveryCompass(),
			SurvivalContext::echoShards, 8);
	public static final Tip RECOVERY_COMPASS = Tip.milestone("recovery_compass", 2,
			SurvivalContext::hasRecoveryCompass);

	public static final List<Tip> CHAPTER_DEEP_DARK = List.of(FIND_ANCIENT_CITY, SNEAK_PAST, ECHO_SHARDS, RECOVERY_COMPASS);

	// --- Chapter 14: raids and illagers ---
	public static final Tip FIND_OUTPOST = Tip.milestone("find_outpost", 2,
			c -> c.structure().equals("pillager_outpost") || c.has(ADV_RAID_CAPTAIN));
	public static final Tip RAID_CAPTAIN = Tip.milestone("raid_captain", 2,
			c -> c.has(ADV_RAID_CAPTAIN));
	public static final Tip OMINOUS_BOTTLE = Tip.milestone("ominous_bottle", 2,
			c -> c.hasOminousBottle() || c.has(ADV_RAID));
	public static final Tip WIN_RAID = Tip.milestone("win_raid", 2,
			c -> c.has(ADV_RAID));
	public static final Tip TOTEM = Tip.milestone("totem", 2,
			c -> c.hasTotem() || c.has(ADV_TOTEM));

	public static final List<Tip> CHAPTER_RAIDS = List.of(FIND_OUTPOST, RAID_CAPTAIN, OMINOUS_BOTTLE, WIN_RAID, TOTEM);

	// --- Chapter 15: archaeology and trims (docs/research-chapters-15-18.md) ---
	public static final Tip BRUSH = Tip.milestone("brush", 2,
			c -> c.hasBrush() || c.has(ADV_SHERD));
	public static final Tip FIND_TRAIL_RUINS = Tip.milestone("find_trail_ruins", 2,
			c -> c.structure().equals("trail_ruins"));
	public static final Tip SHERD = Tip.milestone("sherd", 2,
			c -> c.has(ADV_SHERD));
	public static final Tip DECORATED_POT = Tip.milestone("decorated_pot", 2,
			SurvivalContext::hasDecoratedPot);
	public static final Tip ARMOR_TRIM = Tip.milestone("armor_trim", 2,
			c -> c.has(ADV_TRIM));
	public static final Tip SNIFFER_EGG = Tip.milestone("sniffer_egg", 2,
			c -> c.has(ADV_SNIFFER_EGG));

	public static final List<Tip> CHAPTER_ARCHAEOLOGY = List.of(BRUSH, FIND_TRAIL_RUINS, SHERD, DECORATED_POT, ARMOR_TRIM, SNIFFER_EGG);

	// --- Chapter 16: trial challenges ---
	public static final Tip BREEZE_RODS = Tip.milestone("breeze_rods", 2,
			c -> c.breezeRods() >= 2 || c.hasMace(),
			SurvivalContext::breezeRods, 2);
	public static final Tip WIND_CHARGE = Tip.milestone("wind_charge", 2,
			SurvivalContext::hasWindCharge);
	public static final Tip OMINOUS_VAULT = Tip.milestone("ominous_vault", 2,
			c -> c.has(ADV_OMINOUS_VAULT));
	public static final Tip HEAVY_CORE = Tip.milestone("heavy_core", 2,
			c -> c.hasHeavyCore() || c.hasMace());
	public static final Tip MACE = Tip.milestone("mace", 2,
			SurvivalContext::hasMace);

	public static final List<Tip> CHAPTER_TRIALS = List.of(BREEZE_RODS, WIND_CHARGE, OMINOUS_VAULT, HEAVY_CORE, MACE);

	// --- Chapter 17: the happy ghast ---
	public static final Tip DRIED_GHAST = Tip.milestone("dried_ghast", 2,
			c -> c.hasDriedGhast() || c.has(ADV_DRIED_GHAST));
	public static final Tip HYDRATE_GHAST = Tip.milestone("hydrate_ghast", 2,
			c -> c.has(ADV_DRIED_GHAST));
	public static final Tip GHASTLING = Tip.milestone("ghastling", 2,
			c -> c.ghastlingNearby() || c.ridingHappyGhast());
	public static final Tip HARNESS = Tip.milestone("harness", 2,
			c -> c.hasHarness() || c.ridingHappyGhast());
	public static final Tip RIDE_HAPPY_GHAST = Tip.milestone("ride_happy_ghast", 2,
			SurvivalContext::ridingHappyGhast);

	public static final List<Tip> CHAPTER_HAPPY_GHAST = List.of(DRIED_GHAST, HYDRATE_GHAST, GHASTLING, HARNESS, RIDE_HAPPY_GHAST);

	// --- Chapter 18: automation ---
	public static final Tip OBSERVER = Tip.milestone("observer", 2,
			SurvivalContext::observerPlaced);
	public static final Tip AUTO_SMELTER = Tip.milestone("auto_smelter", 2,
			SurvivalContext::autoSmelter);
	public static final Tip COPPER_BULB = Tip.milestone("copper_bulb", 2,
			SurvivalContext::copperBulbLit);
	public static final Tip CRAFTER = Tip.milestone("crafter", 2,
			c -> c.crafterPlaced() || c.has(ADV_CRAFTER));
	public static final Tip CRAFTER_CRAFTS = Tip.milestone("crafter_crafts", 2,
			c -> c.has(ADV_CRAFTER));

	public static final List<Tip> CHAPTER_AUTOMATION = List.of(OBSERVER, AUTO_SMELTER, COPPER_BULB, CRAFTER, CRAFTER_CRAFTS);

	/** The goals of a chapter (see {@link Chapters}). */
	public static List<Tip> milestones(int chapter) {
		return Chapters.get(chapter).goals();
	}

	/** True when every goal of the chapter is in {@code completed}. */
	public static boolean chapterDone(int chapter, java.util.Set<String> completed) {
		return Chapters.get(chapter).done(completed);
	}
}
