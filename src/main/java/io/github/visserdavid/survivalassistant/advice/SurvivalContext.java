package io.github.visserdavid.survivalassistant.advice;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable snapshot of everything the advice engine needs to know about a player, in five groups.
 * Deliberately free of Minecraft types so it can be unit tested in plain JUnit.
 *
 * @param world     where and when: day, time, dimension, structure, weather, shelter
 * @param player    the player's own state: health, food, experience, statistics and advancements
 * @param inv       what the player has: items carried (and stations placed nearby), tools and armor
 * @param nearby    blocks around the player: farms, redstone, lava, light
 * @param creatures mobs around the player and what the player rides
 */
public record SurvivalContext(World world, Player player, Inventory inv, Nearby nearby, Creatures creatures) {
	public static final String OVERWORLD = "overworld";
	public static final String NETHER = "nether";
	public static final String END = "end";

	public static final int SUNSET = 11000;
	public static final int NIGHT_START = 13000;
	public static final int NIGHT_END = 23000;

	/** Phantoms can spawn after 72000 ticks (3 days) without sleep; we warn from the second night on. */
	public static final int PHANTOM_WARNING_TICKS = 48000;

	public SurvivalContext {
		Objects.requireNonNull(world, "world");
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(inv, "inv");
		Objects.requireNonNull(nearby, "nearby");
		Objects.requireNonNull(creatures, "creatures");
	}

	/**
	 * @param dayIndex    days since the guide arrived (0 = first day)
	 * @param timeOfDay   0..23999, 0 = sunrise, 11000 = sunset ({@link #SUNSET}), 13000 = night
	 * @param dimension   {@link #OVERWORLD}, {@link #NETHER}, {@link #END} (or the path of another dimension id)
	 * @param structure   id path of the notable structure the player stands in (see {@link Structures}), or ""
	 * @param sheltered   the player has a roof above them
	 * @param underground no sky above and well below the surface (overworld only)
	 */
	public record World(int dayIndex, int timeOfDay, String dimension, String structure, boolean thundering,
			boolean sheltered, boolean underground) {
		public World {
			Objects.requireNonNull(dimension, "dimension");
			Objects.requireNonNull(structure, "structure");
		}

		public boolean inOverworld() {
			return OVERWORLD.equals(dimension);
		}

		public boolean inNether() {
			return NETHER.equals(dimension);
		}

		public boolean inEnd() {
			return END.equals(dimension);
		}

		public boolean isNight() {
			return timeOfDay >= NIGHT_START && timeOfDay < NIGHT_END;
		}

		public boolean isDusk() {
			return timeOfDay >= SUNSET && timeOfDay < NIGHT_START;
		}

		public boolean isDuskOrNight() {
			return duskOrNight(timeOfDay);
		}

		public boolean in(String structureId) {
			return structure.equals(structureId);
		}

		/** Standing inside a village (any of the village structures). */
		public boolean inVillage() {
			return Structures.isVillage(structure);
		}

		/** In a shipwreck or an ocean ruin. */
		public boolean inWreck() {
			return Structures.isWreck(structure);
		}

		/** In a desert or jungle temple. */
		public boolean inTemple() {
			return in(Structures.DESERT_PYRAMID) || in(Structures.JUNGLE_PYRAMID);
		}
	}

	/**
	 * @param ticksSinceRest statistic time_since_rest (ticks since the player last slept)
	 * @param animalsBred    statistic animals_bred
	 * @param trades         statistic traded_with_villager
	 * @param digDownStreak  recent breaks of the block directly under the player's feet
	 * @param advancements   the vanilla advancements the chapters track that this player has completed
	 */
	public record Player(float health, int food, int xpLevel, int ticksSinceRest, int animalsBred, int trades,
			int digDownStreak, Set<String> advancements) {
		public Player {
			advancements = Set.copyOf(advancements);
		}

		/** True when the player completed this vanilla advancement (only ids from {@link Chapters#trackedAdvancements()}). */
		public boolean has(String advancement) {
			return advancements.contains(advancement);
		}
	}

	/**
	 * Counts are items carried. "has" flags for placeable blocks (crafting table, furnace, chest, bed, enchanting
	 * table, brewing stand, anvil, decorated pot) are true when the item is carried <em>or</em> the block stands
	 * within a few blocks of the player.
	 *
	 * @param pickaxeTier     0 none, 1 wood/gold, 2 stone/copper, 3 iron, 4 diamond+
	 * @param swordTier       same scale as pickaxeTier (axes count as weapons too)
	 * @param iron            raw iron + iron ingots
	 * @param enderPearls     ender pearls plus eyes of ender (an eye is made from a pearl)
	 * @param armorPieces     armor slots (head, chest, legs, feet) covered by iron or better, worn or carried
	 * @param hasIronArmor    wearing at least one piece of iron (or better) armor
	 * @param goldArmorWorn   wearing at least one piece of piglin-safe (golden) armor
	 * @param bedInInventory  a bed carried (not a placed one)
	 * @param hasNetheriteGear a netherite tool, weapon or armor piece (not the ingot, scrap or template)
	 */
	public record Inventory(
			int logs, int planks, int cobblestone, int foodItems, int wool, int torches, int iron, int bread, int arrows,
			int diamonds, int obsidian, int emeralds, int blazeRods, int netherWart, int sugarCane, int redstone,
			int enderPearls, int eyesOfEnder, int witherSkulls, int soulSand, int echoShards, int breezeRods,
			int pickaxeTier, int swordTier, int armorPieces,
			boolean hasIronArmor, boolean goldArmorWorn,
			boolean hasCraftingTable, boolean hasFurnace, boolean hasChest, boolean hasBed, boolean bedInInventory,
			boolean hasShield, boolean hasWaterBucket, boolean hasBow, boolean hasFlintAndSteel, boolean hasEnchantingTable,
			boolean hasEnchantedBook, boolean hasBrewingStand, boolean hasAnvil,
			boolean hasBoat, boolean hasMilk, boolean hasHeartOfTheSea, boolean hasTurtleHelmet, boolean hasTrident,
			boolean hasFilledMap, boolean hasSpyglass, boolean hasElytra,
			boolean hasNetheriteIngot, boolean hasUpgradeTemplate, boolean hasNetheriteGear, boolean hasNetherStar,
			boolean hasRecoveryCompass, boolean hasOminousBottle, boolean hasTotem,
			boolean hasBrush, boolean hasDecoratedPot, boolean hasWindCharge, boolean hasHeavyCore, boolean hasMace,
			boolean hasDriedGhast, boolean hasHarness) {}

	/**
	 * @param crops             planted crops (block tag crops) within a few blocks
	 * @param bookshelves       valid bookshelves around the best enchanting table nearby (15 is the maximum that counts)
	 * @param lava              underground (or in the Nether) with lava within a few blocks
	 * @param fallingBlockAbove underground with sand/gravel (a falling block) right above the head
	 * @param litArea           no monster can spawn within a few blocks (see {@link LightScan#LIT_RADIUS}); only
	 *                          checked when the dark matters ({@link #darkMatters()}), false otherwise
	 * @param ironDoorOpen      an iron door nearby stands open (only redstone can open one)
	 * @param pistonExtended    a piston nearby is pushed out (so it is powered)
	 * @param autoSmelter       a furnace nearby with a hopper above it (feeding in) and a hopper below it (taking out)
	 * @param copperBulbLit     a lit copper bulb nearby (any oxidation stage)
	 */
	public record Nearby(int crops, int bookshelves, boolean lava, boolean fallingBlockAbove, boolean litArea,
			boolean ironDoorOpen, boolean pistonExtended, boolean repeater, boolean hopper, boolean observer,
			boolean crafter, boolean autoSmelter, boolean copperBulbLit) {}

	/**
	 * @param hostiles  number of hostile mobs within ~16 blocks
	 * @param creeper   a creeper within ~7 blocks
	 * @param warden    a warden within 24 blocks
	 * @param ghast     a ghast within 32 blocks (outside the overworld)
	 * @param piglin    a piglin within 16 blocks (outside the overworld)
	 * @param ghastling a baby happy ghast within 16 blocks
	 * @param ridingTamedHorse riding a tamed horse, donkey, mule or camel
	 */
	public record Creatures(int hostiles, boolean creeper, boolean warden, boolean ghast, boolean piglin,
			boolean ghastling, boolean ridingTamedHorse, boolean ridingHappyGhast) {}

	/** Monsters could spawn around the player: underground, or outside without a roof at dusk, at night or in a thunderstorm. */
	public boolean darkMatters() {
		return darkMatters(world.underground(), world.inOverworld(), world.sheltered(), world.isDuskOrNight(),
				world.thundering());
	}

	/** {@link #darkMatters()} from its parts, for the collector (which decides whether to check the light at all). */
	/** From sunset until the night ends. */
	public static boolean duskOrNight(int timeOfDay) {
		return timeOfDay >= SUNSET && timeOfDay < NIGHT_END;
	}

	public static boolean darkMatters(boolean underground, boolean overworld, boolean sheltered, boolean duskOrNight, boolean thundering) {
		return underground || (overworld && !sheltered && (duskOrNight || thundering));
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builds a context field by field; every setter is named after its component. Defaults: day 0, morning, the
	 * overworld, full health and food, empty inventory, outside, nobody around.
	 */
	public static final class Builder {
		// world
		private int dayIndex, timeOfDay = 1000;
		private String dimension = OVERWORLD, structure = "";
		private boolean thundering, sheltered, underground;
		// player
		private float health = 20f;
		private int food = 20, xpLevel, ticksSinceRest, animalsBred, trades, digDownStreak;
		private final Set<String> advancements = new HashSet<>();
		// inventory
		private int logs, planks, cobblestone, foodItems, wool, torches, iron, bread, arrows, diamonds, obsidian, emeralds,
				blazeRods, netherWart, sugarCane, redstone, enderPearls, eyesOfEnder, witherSkulls, soulSand, echoShards,
				breezeRods, pickaxeTier, swordTier, armorPieces;
		private boolean hasIronArmor, goldArmorWorn, hasCraftingTable, hasFurnace, hasChest, hasBed, bedInInventory,
				hasShield, hasWaterBucket, hasBow, hasFlintAndSteel, hasEnchantingTable, hasEnchantedBook, hasBrewingStand,
				hasAnvil, hasBoat, hasMilk, hasHeartOfTheSea, hasTurtleHelmet, hasTrident, hasFilledMap, hasSpyglass,
				hasElytra, hasNetheriteIngot, hasUpgradeTemplate, hasNetheriteGear, hasNetherStar, hasRecoveryCompass,
				hasOminousBottle, hasTotem, hasBrush, hasDecoratedPot, hasWindCharge, hasHeavyCore, hasMace, hasDriedGhast,
				hasHarness;
		// nearby
		private int crops, bookshelves;
		private boolean lava, fallingBlockAbove, litArea, ironDoorOpen, pistonExtended, repeater, hopper, observer,
				crafter, autoSmelter, copperBulbLit;
		// creatures
		private int hostiles;
		private boolean creeper, warden, ghast, piglin, ghastling, ridingTamedHorse, ridingHappyGhast;

		private Builder() {}

		// world
		public Builder dayIndex(int v) { dayIndex = v; return this; }
		public Builder timeOfDay(int v) { timeOfDay = v; return this; }
		public Builder dimension(String v) { dimension = v; return this; }
		public Builder structure(String v) { structure = v; return this; }
		public Builder thundering(boolean v) { thundering = v; return this; }
		public Builder sheltered(boolean v) { sheltered = v; return this; }
		public Builder underground(boolean v) { underground = v; return this; }
		// player
		public Builder health(float v) { health = v; return this; }
		public Builder food(int v) { food = v; return this; }
		public Builder xpLevel(int v) { xpLevel = v; return this; }
		public Builder ticksSinceRest(int v) { ticksSinceRest = v; return this; }
		public Builder animalsBred(int v) { animalsBred = v; return this; }
		public Builder trades(int v) { trades = v; return this; }
		public Builder digDownStreak(int v) { digDownStreak = v; return this; }
		public Builder advancement(String v) { advancements.add(v); return this; }
		public Builder advancements(Set<String> v) { advancements.addAll(v); return this; }
		// inventory: counts
		public Builder logs(int v) { logs = v; return this; }
		public Builder planks(int v) { planks = v; return this; }
		public Builder cobblestone(int v) { cobblestone = v; return this; }
		public Builder foodItems(int v) { foodItems = v; return this; }
		public Builder wool(int v) { wool = v; return this; }
		public Builder torches(int v) { torches = v; return this; }
		public Builder iron(int v) { iron = v; return this; }
		public Builder bread(int v) { bread = v; return this; }
		public Builder arrows(int v) { arrows = v; return this; }
		public Builder diamonds(int v) { diamonds = v; return this; }
		public Builder obsidian(int v) { obsidian = v; return this; }
		public Builder emeralds(int v) { emeralds = v; return this; }
		public Builder blazeRods(int v) { blazeRods = v; return this; }
		public Builder netherWart(int v) { netherWart = v; return this; }
		public Builder sugarCane(int v) { sugarCane = v; return this; }
		public Builder redstone(int v) { redstone = v; return this; }
		public Builder enderPearls(int v) { enderPearls = v; return this; }
		public Builder eyesOfEnder(int v) { eyesOfEnder = v; return this; }
		public Builder witherSkulls(int v) { witherSkulls = v; return this; }
		public Builder soulSand(int v) { soulSand = v; return this; }
		public Builder echoShards(int v) { echoShards = v; return this; }
		public Builder breezeRods(int v) { breezeRods = v; return this; }
		public Builder pickaxeTier(int v) { pickaxeTier = v; return this; }
		public Builder swordTier(int v) { swordTier = v; return this; }
		public Builder armorPieces(int v) { armorPieces = v; return this; }
		// inventory: flags
		public Builder hasIronArmor(boolean v) { hasIronArmor = v; return this; }
		public Builder goldArmorWorn(boolean v) { goldArmorWorn = v; return this; }
		public Builder hasCraftingTable(boolean v) { hasCraftingTable = v; return this; }
		public Builder hasFurnace(boolean v) { hasFurnace = v; return this; }
		public Builder hasChest(boolean v) { hasChest = v; return this; }
		public Builder hasBed(boolean v) { hasBed = v; return this; }
		public Builder bedInInventory(boolean v) { bedInInventory = v; return this; }
		public Builder hasShield(boolean v) { hasShield = v; return this; }
		public Builder hasWaterBucket(boolean v) { hasWaterBucket = v; return this; }
		public Builder hasBow(boolean v) { hasBow = v; return this; }
		public Builder hasFlintAndSteel(boolean v) { hasFlintAndSteel = v; return this; }
		public Builder hasEnchantingTable(boolean v) { hasEnchantingTable = v; return this; }
		public Builder hasEnchantedBook(boolean v) { hasEnchantedBook = v; return this; }
		public Builder hasBrewingStand(boolean v) { hasBrewingStand = v; return this; }
		public Builder hasAnvil(boolean v) { hasAnvil = v; return this; }
		public Builder hasBoat(boolean v) { hasBoat = v; return this; }
		public Builder hasMilk(boolean v) { hasMilk = v; return this; }
		public Builder hasHeartOfTheSea(boolean v) { hasHeartOfTheSea = v; return this; }
		public Builder hasTurtleHelmet(boolean v) { hasTurtleHelmet = v; return this; }
		public Builder hasTrident(boolean v) { hasTrident = v; return this; }
		public Builder hasFilledMap(boolean v) { hasFilledMap = v; return this; }
		public Builder hasSpyglass(boolean v) { hasSpyglass = v; return this; }
		public Builder hasElytra(boolean v) { hasElytra = v; return this; }
		public Builder hasNetheriteIngot(boolean v) { hasNetheriteIngot = v; return this; }
		public Builder hasUpgradeTemplate(boolean v) { hasUpgradeTemplate = v; return this; }
		public Builder hasNetheriteGear(boolean v) { hasNetheriteGear = v; return this; }
		public Builder hasNetherStar(boolean v) { hasNetherStar = v; return this; }
		public Builder hasRecoveryCompass(boolean v) { hasRecoveryCompass = v; return this; }
		public Builder hasOminousBottle(boolean v) { hasOminousBottle = v; return this; }
		public Builder hasTotem(boolean v) { hasTotem = v; return this; }
		public Builder hasBrush(boolean v) { hasBrush = v; return this; }
		public Builder hasDecoratedPot(boolean v) { hasDecoratedPot = v; return this; }
		public Builder hasWindCharge(boolean v) { hasWindCharge = v; return this; }
		public Builder hasHeavyCore(boolean v) { hasHeavyCore = v; return this; }
		public Builder hasMace(boolean v) { hasMace = v; return this; }
		public Builder hasDriedGhast(boolean v) { hasDriedGhast = v; return this; }
		public Builder hasHarness(boolean v) { hasHarness = v; return this; }
		// nearby
		public Builder crops(int v) { crops = v; return this; }
		public Builder bookshelves(int v) { bookshelves = v; return this; }
		public Builder lava(boolean v) { lava = v; return this; }
		public Builder fallingBlockAbove(boolean v) { fallingBlockAbove = v; return this; }
		public Builder litArea(boolean v) { litArea = v; return this; }
		public Builder ironDoorOpen(boolean v) { ironDoorOpen = v; return this; }
		public Builder pistonExtended(boolean v) { pistonExtended = v; return this; }
		public Builder repeater(boolean v) { repeater = v; return this; }
		public Builder hopper(boolean v) { hopper = v; return this; }
		public Builder observer(boolean v) { observer = v; return this; }
		public Builder crafter(boolean v) { crafter = v; return this; }
		public Builder autoSmelter(boolean v) { autoSmelter = v; return this; }
		public Builder copperBulbLit(boolean v) { copperBulbLit = v; return this; }
		// creatures
		public Builder hostiles(int v) { hostiles = v; return this; }
		public Builder creeper(boolean v) { creeper = v; return this; }
		public Builder warden(boolean v) { warden = v; return this; }
		public Builder ghast(boolean v) { ghast = v; return this; }
		public Builder piglin(boolean v) { piglin = v; return this; }
		public Builder ghastling(boolean v) { ghastling = v; return this; }
		public Builder ridingTamedHorse(boolean v) { ridingTamedHorse = v; return this; }
		public Builder ridingHappyGhast(boolean v) { ridingHappyGhast = v; return this; }

		public SurvivalContext build() {
			return new SurvivalContext(
					new World(dayIndex, timeOfDay, dimension, structure, thundering, sheltered, underground),
					new Player(health, food, xpLevel, ticksSinceRest, animalsBred, trades, digDownStreak, advancements),
					new Inventory(logs, planks, cobblestone, foodItems, wool, torches, iron, bread, arrows, diamonds,
							obsidian, emeralds, blazeRods, netherWart, sugarCane, redstone, enderPearls, eyesOfEnder,
							witherSkulls, soulSand, echoShards, breezeRods, pickaxeTier, swordTier, armorPieces,
							hasIronArmor, goldArmorWorn, hasCraftingTable, hasFurnace, hasChest, hasBed, bedInInventory,
							hasShield, hasWaterBucket, hasBow, hasFlintAndSteel, hasEnchantingTable, hasEnchantedBook,
							hasBrewingStand, hasAnvil, hasBoat, hasMilk, hasHeartOfTheSea, hasTurtleHelmet, hasTrident,
							hasFilledMap, hasSpyglass, hasElytra, hasNetheriteIngot, hasUpgradeTemplate, hasNetheriteGear,
							hasNetherStar, hasRecoveryCompass, hasOminousBottle, hasTotem, hasBrush, hasDecoratedPot,
							hasWindCharge, hasHeavyCore, hasMace, hasDriedGhast, hasHarness),
					new Nearby(crops, bookshelves, lava, fallingBlockAbove, litArea, ironDoorOpen, pistonExtended,
							repeater, hopper, observer, crafter, autoSmelter, copperBulbLit),
					new Creatures(hostiles, creeper, warden, ghast, piglin, ghastling, ridingTamedHorse, ridingHappyGhast));
		}
	}
}
