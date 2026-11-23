package io.github.visserdavid.survivalassistant.advice;

/**
 * Immutable snapshot of everything the advice engine needs to know about a player.
 * Deliberately free of Minecraft types so it can be unit tested in plain JUnit.
 *
 * <p>"has" flags for placeable blocks (crafting table, furnace, chest, bed) are true when the item is in the
 * inventory <em>or</em> the block stands within a few blocks of the player.
 *
 * @param dayIndex       days since the guide arrived (0 = first day)
 * @param timeOfDay      0..23999, 0 = sunrise, 12000 = sunset, 13000 = night
 * @param pickaxeTier    0 none, 1 wood/gold, 2 stone/copper, 3 iron, 4 diamond+
 * @param swordTier      same scale as pickaxeTier (axes count as weapons too)
 * @param iron           raw iron + iron ingots
 * @param sheltered      the player has a roof above them
 * @param underground    no sky above and well below the surface
 * @param hostilesNearby number of hostile mobs within ~16 blocks
 * @param creeperNearby  a creeper within ~7 blocks
 * @param ticksSinceRest statistic time_since_rest (ticks since the player last slept)
 * @param lavaNearby     underground with lava within ~3 blocks
 * @param fallingBlockAbove underground with sand/gravel (a falling block) right above the head
 * @param digDownStreak  recent breaks of the block directly under the player's feet
 * @param cropsNearby    planted crops (block tag crops) within a few blocks
 * @param animalsBred    statistic animals_bred
 * @param armorPieces    armor slots (head, chest, legs, feet) covered by iron or better, worn or carried
 * @param dimension      "overworld", "nether", "end" (or the path of another dimension id)
 * @param inVillage      standing inside a village (structure check)
 * @param trades         statistic traded_with_villager
 * @param goldArmorWorn  wearing at least one piece of piglin-safe (golden) armor
 * @param bedInInventory a bed in the inventory (not a placed one)
 * @param bookshelves    valid bookshelves around the best enchanting table nearby (15 is the maximum that counts)
 * @param advancements   the vanilla advancements the chapters track that this player has completed
 * @param structure      id path of the notable structure the player stands in ("village_plains", "shipwreck",
 *                       "desert_pyramid", "trial_chambers", ...), or "" (see {@link #STRUCTURES})
 * @param ridingTamedHorse riding a tamed horse, donkey, mule or camel
 * @param ironDoorOpen   an iron door nearby stands open (only redstone can open one)
 * @param pistonExtended a piston nearby is pushed out (so it is powered)
 * @param enderPearls    ender pearls plus eyes of ender (an eye is made from a pearl)
 * @param hasNetheriteGear a netherite tool, weapon or armor piece (not the ingot, scrap or template)
 * @param wardenNearby   a warden within 24 blocks
 */
public record SurvivalContext(
		int dayIndex,
		int timeOfDay,
		float health,
		int food,
		int logs,
		int planks,
		boolean hasCraftingTable,
		int pickaxeTier,
		int swordTier,
		int cobblestone,
		boolean hasFurnace,
		int foodItems,
		boolean hasBed,
		int wool,
		int torches,
		boolean hasChest,
		int iron,
		boolean hasShield,
		boolean hasIronArmor,
		boolean hasWaterBucket,
		boolean sheltered,
		boolean underground,
		boolean thundering,
		int hostilesNearby,
		boolean creeperNearby,
		int ticksSinceRest,
		boolean lavaNearby,
		boolean fallingBlockAbove,
		int digDownStreak,
		// chapter 2
		int cropsNearby,
		int bread,
		int animalsBred,
		int armorPieces,
		boolean hasBow,
		int arrows,
		int diamonds,
		int obsidian,
		boolean hasFlintAndSteel,
		boolean hasEnchantingTable,
		// chapters 3-5 (village, Nether, brewing and enchanting)
		String dimension,
		boolean inVillage,
		int emeralds,
		int trades,
		boolean hasEnchantedBook,
		boolean goldArmorWorn,
		int blazeRods,
		int netherWart,
		boolean bedInInventory,
		boolean ghastNearby,
		boolean piglinNearby,
		boolean hasBrewingStand,
		int bookshelves,
		boolean hasAnvil,
		int xpLevel,
		java.util.Set<String> advancements,
		// chapters 6-8 (farming and animals, the ocean, exploring)
		String structure,
		boolean hasBoat,
		boolean ridingTamedHorse,
		boolean hasMilk,
		int sugarCane,
		boolean hasHeartOfTheSea,
		boolean hasTurtleHelmet,
		boolean hasTrident,
		boolean hasFilledMap,
		boolean hasSpyglass,
		// chapters 9-10 (redstone, the End)
		int redstone,
		boolean ironDoorOpen,
		boolean pistonExtended,
		boolean repeaterPlaced,
		boolean hopperPlaced,
		int enderPearls,
		int eyesOfEnder,
		boolean hasElytra,
		// chapters 11-14 (netherite, the Wither, the Deep Dark, raids)
		boolean hasNetheriteIngot,
		boolean hasUpgradeTemplate,
		boolean hasNetheriteGear,
		int witherSkulls,
		int soulSand,
		boolean hasNetherStar,
		int echoShards,
		boolean hasRecoveryCompass,
		boolean hasOminousBottle,
		boolean hasTotem,
		boolean wardenNearby
) {
	/** Structures the guide knows about (ids in data/minecraft/worldgen/structure of the 26.3 data). */
	public static final java.util.Set<String> STRUCTURES = java.util.Set.of(
			"village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga",
			"shipwreck", "shipwreck_beached", "ocean_ruin_cold", "ocean_ruin_warm",
			"desert_pyramid", "jungle_pyramid", "trial_chambers", "stronghold", "end_city",
			"bastion_remnant", "ancient_city", "pillager_outpost");

	public boolean inEnd() {
		return END.equals(dimension);
	}

	/** In a shipwreck or an ocean ruin. */
	public boolean inWreck() {
		return structure.startsWith("shipwreck") || structure.startsWith("ocean_ruin");
	}

	/** In a desert or jungle temple. */
	public boolean inTemple() {
		return structure.equals("desert_pyramid") || structure.equals("jungle_pyramid");
	}

	public static final String OVERWORLD = "overworld";
	public static final String NETHER = "nether";
	public static final String END = "end";

	public boolean inOverworld() {
		return OVERWORLD.equals(dimension);
	}

	public boolean inNether() {
		return NETHER.equals(dimension);
	}

	/** True when the player completed this vanilla advancement (only ids from {@link Chapters#trackedAdvancements()}). */
	public boolean has(String advancement) {
		return advancements.contains(advancement);
	}

	/** Phantoms can spawn after 72000 ticks (3 days) without sleep; we warn from the second night on. */
	public static final int PHANTOM_WARNING_TICKS = 48000;

	public static final int SUNSET = 11000;
	public static final int NIGHT_START = 13000;
	public static final int NIGHT_END = 23000;

	public boolean isNight() {
		return timeOfDay >= NIGHT_START && timeOfDay < NIGHT_END;
	}

	public boolean isDusk() {
		return timeOfDay >= SUNSET && timeOfDay < NIGHT_START;
	}

	public static Builder builder() {
		return new Builder();
	}

	/** Convenience builder, mainly for tests. Defaults: day 0, morning, full health and food, empty inventory, outside. */
	public static final class Builder {
		private int dayIndex, timeOfDay = 1000, food = 20, logs, planks, pickaxeTier, swordTier, cobblestone, foodItems, wool, torches, iron, hostilesNearby, ticksSinceRest, digDownStreak,
				cropsNearby, bread, animalsBred, armorPieces, arrows, diamonds, obsidian,
				emeralds, trades, blazeRods, netherWart, bookshelves, xpLevel;
		private String dimension = OVERWORLD;
		private java.util.Set<String> advancements = new java.util.HashSet<>();
		private String structure = "";
		private boolean hasBoat, ridingTamedHorse, hasMilk, hasHeartOfTheSea, hasTurtleHelmet, hasTrident, hasFilledMap, hasSpyglass;
		private int sugarCane, redstone, enderPearls, eyesOfEnder;
		private boolean ironDoorOpen, pistonExtended, repeaterPlaced, hopperPlaced, hasElytra;
		private boolean hasNetheriteIngot, hasUpgradeTemplate, hasNetheriteGear, hasNetherStar, hasRecoveryCompass,
				hasOminousBottle, hasTotem, wardenNearby;
		private int witherSkulls, soulSand, echoShards;
		private boolean inVillage, hasEnchantedBook, goldArmorWorn, bedInInventory, ghastNearby, piglinNearby, hasBrewingStand, hasAnvil;
		private float health = 20f;
		private boolean hasCraftingTable, hasFurnace, hasBed, hasChest, hasShield, hasIronArmor, hasWaterBucket,
				sheltered, underground, thundering, creeperNearby, lavaNearby, fallingBlockAbove,
				hasBow, hasFlintAndSteel, hasEnchantingTable;

		public Builder day(int v) { dayIndex = v; return this; }
		public Builder time(int v) { timeOfDay = v; return this; }
		public Builder health(float v) { health = v; return this; }
		public Builder food(int v) { food = v; return this; }
		public Builder logs(int v) { logs = v; return this; }
		public Builder planks(int v) { planks = v; return this; }
		public Builder craftingTable(boolean v) { hasCraftingTable = v; return this; }
		public Builder pickaxe(int v) { pickaxeTier = v; return this; }
		public Builder sword(int v) { swordTier = v; return this; }
		public Builder cobblestone(int v) { cobblestone = v; return this; }
		public Builder furnace(boolean v) { hasFurnace = v; return this; }
		public Builder foodItems(int v) { foodItems = v; return this; }
		public Builder bed(boolean v) { hasBed = v; return this; }
		public Builder wool(int v) { wool = v; return this; }
		public Builder torches(int v) { torches = v; return this; }
		public Builder chest(boolean v) { hasChest = v; return this; }
		public Builder iron(int v) { iron = v; return this; }
		public Builder shield(boolean v) { hasShield = v; return this; }
		public Builder ironArmor(boolean v) { hasIronArmor = v; return this; }
		public Builder waterBucket(boolean v) { hasWaterBucket = v; return this; }
		public Builder sheltered(boolean v) { sheltered = v; return this; }
		public Builder underground(boolean v) { underground = v; return this; }
		public Builder thundering(boolean v) { thundering = v; return this; }
		public Builder hostiles(int v) { hostilesNearby = v; return this; }
		public Builder creeper(boolean v) { creeperNearby = v; return this; }
		public Builder sinceRest(int v) { ticksSinceRest = v; return this; }
		public Builder lava(boolean v) { lavaNearby = v; return this; }
		public Builder fallingAbove(boolean v) { fallingBlockAbove = v; return this; }
		public Builder digDown(int v) { digDownStreak = v; return this; }
		public Builder crops(int v) { cropsNearby = v; return this; }
		public Builder bread(int v) { bread = v; return this; }
		public Builder bred(int v) { animalsBred = v; return this; }
		public Builder armorPieces(int v) { armorPieces = v; return this; }
		public Builder bow(boolean v) { hasBow = v; return this; }
		public Builder arrows(int v) { arrows = v; return this; }
		public Builder diamonds(int v) { diamonds = v; return this; }
		public Builder obsidian(int v) { obsidian = v; return this; }
		public Builder flintAndSteel(boolean v) { hasFlintAndSteel = v; return this; }
		public Builder enchantingTable(boolean v) { hasEnchantingTable = v; return this; }
		public Builder dimension(String v) { dimension = v; return this; }
		public Builder village(boolean v) { inVillage = v; return this; }
		public Builder emeralds(int v) { emeralds = v; return this; }
		public Builder trades(int v) { trades = v; return this; }
		public Builder enchantedBook(boolean v) { hasEnchantedBook = v; return this; }
		public Builder goldArmor(boolean v) { goldArmorWorn = v; return this; }
		public Builder blazeRods(int v) { blazeRods = v; return this; }
		public Builder netherWart(int v) { netherWart = v; return this; }
		public Builder bedInInventory(boolean v) { bedInInventory = v; return this; }
		public Builder ghast(boolean v) { ghastNearby = v; return this; }
		public Builder piglin(boolean v) { piglinNearby = v; return this; }
		public Builder brewingStand(boolean v) { hasBrewingStand = v; return this; }
		public Builder bookshelves(int v) { bookshelves = v; return this; }
		public Builder anvil(boolean v) { hasAnvil = v; return this; }
		public Builder xpLevel(int v) { xpLevel = v; return this; }
		public Builder advancement(String v) { advancements.add(v); return this; }
		public Builder structure(String v) { structure = v; inVillage = v.startsWith("village_"); return this; }
		public Builder boat(boolean v) { hasBoat = v; return this; }
		public Builder ridingHorse(boolean v) { ridingTamedHorse = v; return this; }
		public Builder milk(boolean v) { hasMilk = v; return this; }
		public Builder sugarCane(int v) { sugarCane = v; return this; }
		public Builder heartOfTheSea(boolean v) { hasHeartOfTheSea = v; return this; }
		public Builder turtleHelmet(boolean v) { hasTurtleHelmet = v; return this; }
		public Builder trident(boolean v) { hasTrident = v; return this; }
		public Builder filledMap(boolean v) { hasFilledMap = v; return this; }
		public Builder spyglass(boolean v) { hasSpyglass = v; return this; }
		public Builder redstone(int v) { redstone = v; return this; }
		public Builder ironDoorOpen(boolean v) { ironDoorOpen = v; return this; }
		public Builder pistonExtended(boolean v) { pistonExtended = v; return this; }
		public Builder repeater(boolean v) { repeaterPlaced = v; return this; }
		public Builder hopper(boolean v) { hopperPlaced = v; return this; }
		public Builder enderPearls(int v) { enderPearls = v; return this; }
		public Builder eyes(int v) { eyesOfEnder = v; return this; }
		public Builder elytra(boolean v) { hasElytra = v; return this; }
		public Builder netheriteIngot(boolean v) { hasNetheriteIngot = v; return this; }
		public Builder upgradeTemplate(boolean v) { hasUpgradeTemplate = v; return this; }
		public Builder netheriteGear(boolean v) { hasNetheriteGear = v; return this; }
		public Builder witherSkulls(int v) { witherSkulls = v; return this; }
		public Builder soulSand(int v) { soulSand = v; return this; }
		public Builder netherStar(boolean v) { hasNetherStar = v; return this; }
		public Builder echoShards(int v) { echoShards = v; return this; }
		public Builder recoveryCompass(boolean v) { hasRecoveryCompass = v; return this; }
		public Builder ominousBottle(boolean v) { hasOminousBottle = v; return this; }
		public Builder totem(boolean v) { hasTotem = v; return this; }
		public Builder warden(boolean v) { wardenNearby = v; return this; }

		public SurvivalContext build() {
			return new SurvivalContext(dayIndex, timeOfDay, health, food, logs, planks, hasCraftingTable, pickaxeTier, swordTier,
					cobblestone, hasFurnace, foodItems, hasBed, wool, torches, hasChest, iron, hasShield, hasIronArmor, hasWaterBucket,
					sheltered, underground, thundering, hostilesNearby, creeperNearby, ticksSinceRest, lavaNearby, fallingBlockAbove,
					digDownStreak, cropsNearby, bread, animalsBred, armorPieces, hasBow, arrows, diamonds, obsidian, hasFlintAndSteel,
					hasEnchantingTable, dimension, inVillage, emeralds, trades, hasEnchantedBook, goldArmorWorn, blazeRods, netherWart,
					bedInInventory, ghastNearby, piglinNearby, hasBrewingStand, bookshelves, hasAnvil, xpLevel, java.util.Set.copyOf(advancements),
					structure, hasBoat, ridingTamedHorse, hasMilk, sugarCane, hasHeartOfTheSea, hasTurtleHelmet, hasTrident,
					hasFilledMap, hasSpyglass, redstone, ironDoorOpen, pistonExtended, repeaterPlaced, hopperPlaced, enderPearls,
					eyesOfEnder, hasElytra, hasNetheriteIngot, hasUpgradeTemplate, hasNetheriteGear, witherSkulls, soulSand,
					hasNetherStar, echoShards, hasRecoveryCompass, hasOminousBottle, hasTotem, wardenNearby);
		}
	}
}
