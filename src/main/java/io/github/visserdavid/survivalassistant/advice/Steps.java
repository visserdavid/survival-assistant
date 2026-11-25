package io.github.visserdavid.survivalassistant.advice;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Smaller steps inside the goals that hide many actions behind one line ("Brew a potion", "Enter the Nether"). The task
 * card shows the current step under the goal, and the guide says the step instead of the goal's general tip.
 *
 * <p>Rules:
 * <ul>
 *   <li>The current step is the first one that isn't done. A step's {@code done} also holds for later progress
 *       ("skipping ahead counts", as with the goals in {@link TipRegistry}).</li>
 *   <li>The last step is the finishing action; it is never done by itself, the goal completes it.</li>
 *   <li>Steps are worked out from the world every time and never saved: when the player uses something up, the step
 *       may go back. That is correct, and old worlds need nothing new.</li>
 *   <li>Step ids are {@code <goal>.<step>}; the texts are {@code survivalassistant.step.<id>} (the card) and
 *       {@code survivalassistant.step.<id>.tip} (the guide).</li>
 * </ul>
 * Pure logic, no Minecraft types.
 */
public final class Steps {
	private Steps() {}

	/**
	 * @param id      {@code <goal>.<step>}
	 * @param done    when the step is behind the player
	 * @param counter optional live progress for the card; null = no counter
	 * @param target  value at which the counter is "full"; 0 = no counter
	 */
	public record Step(String id, Predicate<SurvivalContext> done, @Nullable ToIntFunction<SurvivalContext> counter, int target) {
		public boolean hasCounter() {
			return counter != null && target > 0;
		}

		/** The counter, clamped to 0..target (0 without a counter). */
		public int value(SurvivalContext ctx) {
			return hasCounter() ? Math.min(target, Math.max(0, counter.applyAsInt(ctx))) : 0;
		}

		/** The goal this step belongs to. */
		public String goal() {
			return id.substring(0, id.indexOf('.'));
		}

		public String labelKey() {
			return "survivalassistant.step." + id;
		}

		public String tipKey() {
			return labelKey() + ".tip";
		}
	}

	private static Step step(String id, Predicate<SurvivalContext> done) {
		return new Step(id, done, null, 0);
	}

	/** Done when the counter reaches the target, or when {@code alsoDone} holds (later progress). */
	private static Step count(String id, ToIntFunction<SurvivalContext> counter, int target, Predicate<SurvivalContext> alsoDone) {
		return new Step(id, c -> counter.applyAsInt(c) >= target || alsoDone.test(c), counter, target);
	}

	/** The finishing action: the goal itself completes it. */
	private static Step finish(String id) {
		return step(id, c -> false);
	}

	/** The finishing action, with a counter on the card. */
	private static Step finish(String id, ToIntFunction<SurvivalContext> counter, int target) {
		return new Step(id, c -> false, counter, target);
	}

	// ---------------------------------------------------------------- what the steps look at

	/** Every hoe tills the soil. */
	private static final List<String> HOES = List.of("wooden_hoe", "stone_hoe", "copper_hoe", "iron_hoe", "golden_hoe",
			"diamond_hoe", "netherite_hoe");
	/** Diamond gear a smithing table turns into netherite. */
	private static final List<String> DIAMOND_GEAR = List.of("diamond_sword", "diamond_pickaxe", "diamond_axe",
			"diamond_shovel", "diamond_hoe", "diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots");
	/** What a farm can be planted with. */
	private static final List<String> SEEDS = List.of("wheat_seeds", "carrot", "potato", "beetroot_seeds");

	/**
	 * Groups of items the collector counts under these keys in {@link SurvivalContext.Inventory#count}: sherds and
	 * bricks for a pot, trim materials, armor that takes a trim (worn counts too), and armor trim templates.
	 */
	public static final String POT_PARTS = "#pot_parts", TRIM_MATERIALS = "#trim_materials",
			TRIMMABLE_ARMOR = "#trimmable_armor", TRIM_TEMPLATES = "#trim_templates";

	/** Items whose carried count the collector reports ({@link SurvivalContext.Inventory#count}). */
	public static final Set<String> TRACKED_ITEMS = Stream.concat(
			Stream.of(HOES, SEEDS, DIAMOND_GEAR).flatMap(List::stream), Stream.of(
			"glass_bottle", "blaze_powder", "paper", "leather", "book", "bookshelf", "iron_block", "iron_ingot",
			"carved_pumpkin", "ancient_debris", "netherite_scrap", "gold_ingot", "glass", "beacon", "hopper",
			"saddle", "crying_obsidian", "glowstone", "glowstone_dust", "respawn_anchor", "compass", "seagrass",
			"turtle_scute", "ghast_tear", "copper_block", "copper_ingot", "copper_bulb", "crafting_table", "dropper",
			"string", "flint", "feather", "wheat", "bone", "cod", "salmon", "bucket", "fishing_rod", "lapis_lazuli",
			"smithing_table", "trial_key", "ominous_trial_key", "piston", "sticky_piston", "cobblestone", "stick", "chest",
			"shears", "pumpkin", "campfire"))
			.collect(Collectors.toUnmodifiableSet());
	/** Blocks whose placed count nearby the collector reports ({@link SurvivalContext.Nearby#placed}). */
	public static final Set<String> TRACKED_BLOCKS = Set.of(
			"obsidian", "nether_portal", "iron_block", "carved_pumpkin", "beacon", "hopper", "nether_wart",
			"respawn_anchor", "turtle_egg", "copper_bulb", "farmland", "water", "smithing_table", "piston",
			"sticky_piston", "sculk_sensor", "calibrated_sculk_sensor", "sculk", "sculk_shrieker", "pumpkin");

	/** The pyramid has at most 4 levels: 9 + 25 + 49 + 81 = 164 blocks. */
	public static final int MAX_BEACON_LEVEL = 4;
	/** The central island of the End has 10 obsidian pillars, each with a crystal. */
	public static final int END_CRYSTALS = 10;
	/** Within this many blocks (horizontally) of a treasure map's red X the player is "there". */
	public static final int TREASURE_NEAR = 8;
	/** A respawn anchor holds 4 charges; the advancement wants it full. */
	public static final int ANCHOR_CHARGES = 4;
	/** One flint, stick and feather make 4 arrows; the bow goal wants 16. */
	private static final int ARROWS_PER_CRAFT = 4, ARROW_CRAFTS = 4;
	/** Iron for each armor piece: helmet, chestplate, leggings, boots. */
	private static final int HELMET_IRON = 5, CHESTPLATE_IRON = 8, LEGGINGS_IRON = 7, BOOTS_IRON = 4;
	/** Bits of {@link SurvivalContext.Inventory#armorSlots}. */
	private static final int HEAD = 1, CHEST = 2, LEGS = 4;
	/** Diamonds are most common deep down; above this height there are hardly any. */
	public static final int DIAMOND_DEPTH = 16;
	/** The cheapest enchantment costs 1 lapis, the best 3; levels as much. */
	private static final int ENCHANT_LAPIS = 3, ENCHANT_LEVELS = 3;
	/** 3 wheat make a bread; the bread goal wants 3. */
	private static final int WHEAT_PER_BREAD = 3, BREADS = 3;
	/** The farm goal wants 6 crops. */
	private static final int FARM_CROPS = 6;
	/** A dried ghast in water swells up in 3 stages before the ghastling comes out. */
	public static final int GHAST_HYDRATION = 3;
	/** The crafter's own recipe fills all 9 slots of a crafter. */
	private static final int CRAFTER_SLOTS = 9;
	/** 8 ghast tears around soul sand make a dried ghast. */
	private static final int GHAST_TEARS = 8;
	/** One glowstone block is 4 glowstone dust. */
	private static final int DUST_PER_GLOWSTONE = 4;
	/** One bookshelf takes 3 books; one book takes 3 paper and 1 leather. */
	private static final int BOOKS_PER_SHELF = 3, PAPER_PER_BOOK = 3;

	private static int item(SurvivalContext c, String id) {
		return c.inv().count(id);
	}

	private static boolean hasBeacon(SurvivalContext c) {
		return item(c, "beacon") > 0 || c.nearby().placed("beacon") > 0;
	}

	private static boolean carriesShelves(SurvivalContext c) {
		return item(c, "bookshelf") > 0;
	}

	private static boolean portalLit(SurvivalContext c) {
		return c.nearby().placed("nether_portal") > 0;
	}

	private static boolean inNether(SurvivalContext c) {
		return c.world().inNether();
	}

	private static boolean hasAnchor(SurvivalContext c) {
		return item(c, "respawn_anchor") > 0 || c.nearby().placed("respawn_anchor") > 0;
	}

	/** Turtle eggs on the beach, a baby growing up, or scutes already: the breeding is behind the player. */
	private static boolean turtlesBred(SurvivalContext c) {
		return c.nearby().placed("turtle_egg") > 0 || c.creatures().babyTurtleNearby() || item(c, "turtle_scute") > 0;
	}

	/** Putting the recipe into the crafter uses up the carried materials, so from then on the filling counts. */
	private static boolean fillingCrafter(SurvivalContext c) {
		return c.nearby().crafterSlots() > 0;
	}

	private static int sum(SurvivalContext c, List<String> ids) {
		return ids.stream().mapToInt(id -> item(c, id)).sum();
	}

	/** Crops in the ground or tilled soil: the farm is started. */
	private static boolean planting(SurvivalContext c) {
		return c.nearby().crops() > 0 || c.nearby().placed("farmland") > 0;
	}

	/** Arrows already made count as the flint or feathers that went into them (1 of each makes 4). */
	private static int arrowSets(SurvivalContext c) {
		return c.inv().arrows() / ARROWS_PER_CRAFT;
	}

	private static boolean wears(SurvivalContext c, int slot) {
		return (c.inv().armorSlots() & slot) != 0;
	}

	private static int ironIngots(SurvivalContext c) {
		return item(c, "iron_ingot");
	}

	/** An armor piece: done once the player has it (not when the iron is there), the counter shows the iron for it. */
	private static Step piece(String id, int slot, int iron) {
		return new Step(id, c -> wears(c, slot), Steps::ironIngots, iron);
	}

	/** Wheat carried, plus the wheat already baked into bread. */
	private static int wheat(SurvivalContext c) {
		return item(c, "wheat") + WHEAT_PER_BREAD * c.inv().bread();
	}

	private static boolean inTrials(SurvivalContext c) {
		return c.world().in(Structures.TRIAL_CHAMBERS);
	}

	private static boolean ominousKey(SurvivalContext c) {
		return item(c, "ominous_trial_key") > 0;
	}

	private static int sensors(SurvivalContext c) {
		return c.nearby().placed("sculk_sensor") + c.nearby().placed("calibrated_sculk_sensor");
	}

	private static boolean hasHoe(SurvivalContext c) {
		return sum(c, HOES) > 0 || planting(c);
	}

	private static boolean hasSmithingTable(SurvivalContext c) {
		return item(c, "smithing_table") > 0 || c.nearby().placed("smithing_table") > 0;
	}

	private static boolean carvedPumpkin(SurvivalContext c) {
		return item(c, "carved_pumpkin") > 0 || c.nearby().placed("carved_pumpkin") > 0;
	}

	private static boolean hasSaddle(SurvivalContext c) {
		return item(c, "saddle") > 0 || c.creatures().saddledHorseNearby();
	}

	/** Hoppers carried or placed; each holds 5 iron and a chest. */
	private static int hoppers(SurvivalContext c) {
		return item(c, "hopper") + c.nearby().placed("hopper");
	}

	private static boolean hasPiston(SurvivalContext c) {
		return item(c, "piston") + item(c, "sticky_piston") + placedPistons(c) > 0;
	}

	private static int placedPistons(SurvivalContext c) {
		return c.nearby().placed("piston") + c.nearby().placed("sticky_piston");
	}

	private static boolean hasBulb(SurvivalContext c) {
		return item(c, "copper_bulb") > 0 || c.nearby().placed("copper_bulb") > 0;
	}

	private static boolean wartGrowing(SurvivalContext c) {
		return c.nearby().placed("nether_wart") > 0;
	}

	// ---------------------------------------------------------------- the steps per goal

	public static final Map<String, List<Step>> BY_GOAL = Map.ofEntries(
			// chapter 1: iron for the chestplate (it protects most), craft it, put it on (the goal counts worn armor)
			Map.entry("iron_armor", List.of(
					count("iron_armor.iron", c -> item(c, "iron_ingot"), CHESTPLATE_IRON, c -> c.inv().armorPieces() > 0),
					step("iron_armor.craft", c -> c.inv().armorPieces() > 0),
					finish("iron_armor.wear"))),
			// chapter 2: every piece still missing, with the iron it takes (pieces the player has are skipped)
			Map.entry("full_iron_armor", List.of(
					piece("full_iron_armor.helmet", HEAD, HELMET_IRON),
					piece("full_iron_armor.chestplate", CHEST, CHESTPLATE_IRON),
					piece("full_iron_armor.leggings", LEGS, LEGGINGS_IRON),
					finish("full_iron_armor.boots", Steps::ironIngots, BOOTS_IRON))),
			// chapter 2: an iron pickaxe mines diamonds; they lie deep down; 3 of them make the pickaxe
			Map.entry("diamond_pickaxe", List.of(
					step("diamond_pickaxe.iron_pickaxe", c -> c.inv().pickaxeTier() >= 3),
					step("diamond_pickaxe.deep", c -> c.player().y() <= DIAMOND_DEPTH || c.inv().diamonds() >= 3),
					count("diamond_pickaxe.diamonds", c -> c.inv().diamonds(), 3, c -> false),
					count("diamond_pickaxe.sticks", c -> item(c, "stick"), 2, c -> false),
					finish("diamond_pickaxe.craft"))),
			// chapter 2: ripe wheat, 9 of it (baked bread counts), then 3 in a row
			Map.entry("bread", List.of(
					step("bread.ripe", c -> c.nearby().ripeWheat() > 0 || wheat(c) >= WHEAT_PER_BREAD * BREADS),
					count("bread.harvest", Steps::wheat, WHEAT_PER_BREAD * BREADS, c -> false),
					finish("bread.bake"))),
			// chapter 2: a hoe, seeds (planted ones count), tilled soil by the water, then plant
			Map.entry("farm", List.of(
					count("farm.planks", c -> c.inv().planks(), 2, Steps::hasHoe),
					count("farm.sticks", c -> item(c, "stick"), 2, Steps::hasHoe),
					step("farm.hoe", Steps::hasHoe),
					count("farm.seeds", c -> sum(c, SEEDS) + c.nearby().crops(), FARM_CROPS, c -> false),
					step("farm.till", Steps::planting),
					finish("farm.plant"))),
			// chapter 2: string from spiders makes the bow; flint, a stick and a feather make 4 arrows
			Map.entry("bow", List.of(
					count("bow.string", c -> item(c, "string"), 3, c -> c.inv().hasBow()),
					count("bow.sticks", c -> item(c, "stick"), 3, c -> c.inv().hasBow()),
					step("bow.craft", c -> c.inv().hasBow()),
					count("bow.flint", c -> item(c, "flint") + arrowSets(c), ARROW_CRAFTS, c -> false),
					count("bow.feathers", c -> item(c, "feather") + arrowSets(c), ARROW_CRAFTS, c -> false),
					count("bow.arrow_sticks", c -> item(c, "stick") + arrowSets(c), ARROW_CRAFTS, c -> false),
					finish("bow.arrows"))),
			// chapter 2: a book, 2 diamonds and 4 obsidian (the diamond pickaxe often used up the diamonds)
			Map.entry("enchanting_table", List.of(
					count("enchanting_table.diamonds", c -> c.inv().diamonds(), 2, c -> false),
					count("enchanting_table.obsidian", c -> c.inv().obsidian(), 4, c -> false),
					count("enchanting_table.paper", c -> item(c, "paper") + PAPER_PER_BOOK * item(c, "book"), PAPER_PER_BOOK, c -> false),
					count("enchanting_table.leather", c -> item(c, "leather") + item(c, "book"), 1, c -> false),
					count("enchanting_table.book", c -> item(c, "book"), 1, c -> false),
					finish("enchanting_table.craft"))),
			// chapter 3: 4 iron blocks in a T, a carved pumpkin on top
			Map.entry("iron_golem", List.of(
					count("iron_golem.iron_blocks",
							c -> item(c, "iron_block") + c.nearby().placed("iron_block") + item(c, "iron_ingot") / 9, 4, c -> false),
					count("iron_golem.find_pumpkin", c -> item(c, "pumpkin") + c.nearby().placed("pumpkin"), 1, Steps::carvedPumpkin),
					count("iron_golem.shears", c -> item(c, "shears"), 1, Steps::carvedPumpkin),
					step("iron_golem.pumpkin", Steps::carvedPumpkin),
					finish("iron_golem.build"))),
			// chapter 4: obsidian (carried or already in a frame), light the portal, step through
			Map.entry("enter_nether", List.of(
					count("enter_nether.obsidian", c -> c.inv().obsidian() + c.nearby().placed("obsidian"), 10, Steps::portalLit),
					step("enter_nether.light", Steps::portalLit),
					finish("enter_nether.step_in"))),
			// chapter 4: back into the Nether, search until a fortress is close, then walk into it
			Map.entry("find_fortress", List.of(
					step("find_fortress.nether", Steps::inNether),
					step("find_fortress.search", c -> c.nearby().fortressNear()),
					finish("find_fortress.enter"))),
			// chapter 4: blazes live only in fortresses, around their cage
			Map.entry("blaze_rods", List.of(
					step("blaze_rods.nether", Steps::inNether),
					step("blaze_rods.find", c -> c.creatures().blazeNearby()),
					finish("blaze_rods.fight"))),
			// chapter 4: wart grows on soul sand in fortresses; a wart farm at home counts too
			Map.entry("nether_wart", List.of(
					step("nether_wart.nether", c -> inNether(c) || wartGrowing(c)),
					step("nether_wart.find", c -> wartGrowing(c) || c.inv().netherWart() > 0),
					finish("nether_wart.pick"))),
			// chapter 5: bottles, water, fuel, then brew (bottles in the stand still count)
			Map.entry("brew_potion", List.of(
					count("brew_potion.glass", c -> item(c, "glass") + item(c, "glass_bottle") + c.inv().waterBottles()
							+ c.nearby().standBottles(), 3, c -> false),
					count("brew_potion.bottles",
							c -> item(c, "glass_bottle") + c.inv().waterBottles() + c.nearby().standBottles(), 3, c -> false),
					count("brew_potion.water", c -> c.inv().waterBottles() + c.nearby().standBottles(), 3, c -> false),
					step("brew_potion.fuel", c -> item(c, "blaze_powder") > 0 || c.nearby().standFuel()),
					count("brew_potion.wart", c -> c.inv().netherWart(), 1, c -> c.nearby().standIngredient()),
					finish("brew_potion.brew"))),
			// chapter 5: an enchanting table, lapis and levels, then enchant
			Map.entry("enchant", List.of(
					step("enchant.table", c -> c.inv().hasEnchantingTable()),
					count("enchant.lapis", c -> item(c, "lapis_lazuli"), ENCHANT_LAPIS, c -> false),
					count("enchant.levels", c -> c.player().xpLevel(), ENCHANT_LEVELS, c -> false),
					finish("enchant.enchant"))),
			// chapter 5: one bookshelf at a time; books count as the paper and leather in them, so crafting doesn't go back
			Map.entry("bookshelves", List.of(
					count("bookshelves.paper", c -> item(c, "paper") + PAPER_PER_BOOK * item(c, "book"),
							BOOKS_PER_SHELF * PAPER_PER_BOOK, Steps::carriesShelves),
					count("bookshelves.leather", c -> item(c, "leather") + item(c, "book"), BOOKS_PER_SHELF, Steps::carriesShelves),
					count("bookshelves.books", c -> item(c, "book"), BOOKS_PER_SHELF, Steps::carriesShelves),
					count("bookshelves.planks", c -> c.inv().planks(), 6, Steps::carriesShelves),
					step("bookshelves.craft", Steps::carriesShelves),
					finish("bookshelves.place"))),
			// chapter 6: an untamed wolf or cat, bones or raw fish, then feed it until hearts appear
			Map.entry("tame_animal", List.of(
					step("tame_animal.find", c -> c.creatures().tameableNearby()),
					step("tame_animal.food", c -> item(c, "bone") + item(c, "cod") + item(c, "salmon") > 0),
					finish("tame_animal.feed"))),
			// chapter 6: 3 iron make a bucket; a cow, mooshroom or goat gives milk
			Map.entry("milk", List.of(
					count("milk.iron", c -> item(c, "iron_ingot"), 3, c -> item(c, "bucket") > 0),
					step("milk.bucket", c -> item(c, "bucket") > 0),
					step("milk.cow", c -> c.creatures().milkableNearby()),
					finish("milk.milk"))),
			// chapter 6: 3 sticks and 2 string make a rod; any water will do
			Map.entry("fishing", List.of(
					count("fishing.string", c -> item(c, "string"), 2, c -> item(c, "fishing_rod") > 0),
					count("fishing.sticks", c -> item(c, "stick"), 3, c -> item(c, "fishing_rod") > 0),
					step("fishing.rod", c -> item(c, "fishing_rod") > 0),
					step("fishing.water", c -> c.nearby().placed("water") > 0),
					finish("fishing.cast"))),
			// chapter 6: a bottle, a hive, a campfire under it (calm bees), wait until it's full, then the bottle
			Map.entry("honey", List.of(
					count("honey.glass", c -> item(c, "glass"), 3, c -> item(c, "glass_bottle") > 0),
					count("honey.bottle", c -> item(c, "glass_bottle"), 1, c -> false),
					step("honey.hive", c -> c.nearby().hive()),
					count("honey.campfire_item", c -> item(c, "campfire"), 1, c -> c.nearby().hiveSmoked()),
					step("honey.campfire", c -> c.nearby().hiveSmoked()),
					step("honey.wait", c -> c.nearby().hiveFull()),
					finish("honey.collect"))),
			// chapter 6: find a horse, tame it, a saddle, ride
			Map.entry("horse", List.of(
					step("horse.find", c -> c.creatures().horseNearby()),
					step("horse.tame", c -> c.creatures().tamedHorseNearby()),
					count("horse.leather", c -> item(c, "leather"), 3, Steps::hasSaddle),
					count("horse.iron", c -> item(c, "iron_ingot"), 1, Steps::hasSaddle),
					step("horse.saddle", Steps::hasSaddle),
					finish("horse.ride"))),
			// chapter 7: a treasure map from a shipwreck or ruin, its red X, then dig
			Map.entry("treasure", List.of(
					step("treasure.map", c -> c.inv().treasureDistance() >= 0),
					step("treasure.follow", c -> c.inv().treasureDistance() >= 0 && c.inv().treasureDistance() <= TREASURE_NEAR),
					finish("treasure.dig"))),
			// chapter 7: feed two turtles seagrass, they lay eggs; every baby drops a scute as it grows up; 5 scutes
			Map.entry("turtle_helmet", List.of(
					step("turtle_helmet.turtles", c -> c.creatures().turtleNearby() || turtlesBred(c)),
					count("turtle_helmet.seagrass", c -> item(c, "seagrass"), 2, Steps::turtlesBred),
					step("turtle_helmet.eggs", Steps::turtlesBred),
					count("turtle_helmet.scutes", c -> item(c, "turtle_scute"), 5, c -> false),
					finish("turtle_helmet.craft"))),
			// chapter 7: only drowned carry tridents; find one that holds a trident and beat it at the waterline
			Map.entry("trident", List.of(
					step("trident.drowned", c -> c.creatures().drownedNearby()),
					step("trident.armed", c -> c.creatures().tridentDrownedNearby()),
					finish("trident.fight"))),
			// chapter 13: 8 echo shards around a compass
			Map.entry("recovery_compass", List.of(
					count("recovery_compass.shards", c -> c.inv().echoShards(), 8, c -> false),
					count("recovery_compass.iron", c -> item(c, "iron_ingot"), 4, c -> item(c, "compass") > 0),
					count("recovery_compass.redstone", c -> c.inv().redstone(), 1, c -> item(c, "compass") > 0),
					count("recovery_compass.compass", c -> item(c, "compass"), 1, c -> false),
					finish("recovery_compass.craft"))),
			// chapter 16: an ominous bottle, Bad Omen turns into Trial Omen in the chambers, an ominous key, an ominous vault
			Map.entry("ominous_vault", List.of(
					step("ominous_vault.bottle", c -> c.inv().hasOminousBottle() || c.player().omen() || c.player().trialOmen()
							|| ominousKey(c)),
					step("ominous_vault.omen", c -> c.player().omen() || c.player().trialOmen() || ominousKey(c)),
					step("ominous_vault.chambers", c -> c.player().trialOmen() || ominousKey(c)),
					step("ominous_vault.key", Steps::ominousKey),
					finish("ominous_vault.open"))),
			// chapter 16: the heavy core on top of a breeze rod
			Map.entry("mace", List.of(
					count("mace.rod", c -> c.inv().breezeRods(), 1, c -> false),
					step("mace.core", c -> c.inv().hasHeavyCore()),
					finish("mace.craft"))),
			// chapter 8: the trial chambers, a trial key from a beaten trial spawner, then a vault
			Map.entry("open_vault", List.of(
					step("open_vault.chambers", c -> inTrials(c) || item(c, "trial_key") > 0),
					step("open_vault.key", c -> item(c, "trial_key") > 0),
					finish("open_vault.open"))),
			// chapter 10: after the dragon a gateway leads to the outer islands, where the end cities stand
			Map.entry("end_city", List.of(
					step("end_city.dragon", c -> c.player().has(Advancements.DRAGON) || c.nearby().outerEnd()),
					step("end_city.gateway", c -> c.nearby().outerEnd()),
					step("end_city.search", c -> c.nearby().endCityNear()),
					finish("end_city.enter"))),
			// chapter 9: every part of the recipe (top row down), craft, place, then power it
			Map.entry("piston", List.of(
					count("piston.planks", c -> c.inv().planks(), 3, Steps::hasPiston),
					count("piston.cobblestone", c -> item(c, "cobblestone"), 4, Steps::hasPiston),
					count("piston.iron", c -> item(c, "iron_ingot"), 1, Steps::hasPiston),
					count("piston.redstone", c -> c.inv().redstone(), 1, Steps::hasPiston),
					step("piston.craft", Steps::hasPiston),
					step("piston.place", c -> placedPistons(c) > 0),
					finish("piston.power"))),
			// chapter 13: the deep dark, a sculk sensor, sneak past it
			Map.entry("sneak_past", List.of(
					step("sneak_past.deep_dark", c -> c.world().in(Structures.ANCIENT_CITY) || c.nearby().placed("sculk") > 0
							|| sensors(c) > 0),
					step("sneak_past.sensor", c -> sensors(c) > 0),
					finish("sneak_past.sneak"))),
			// chapter 10: in the End, an end city, its ship, the elytra in the item frame
			Map.entry("elytra", List.of(
					step("elytra.end", c -> c.world().inEnd()),
					step("elytra.city", c -> c.nearby().endCityNear() || c.nearby().elytraFrame()),
					step("elytra.ship", c -> c.nearby().elytraFrame()),
					finish("elytra.take"))),
			// chapter 10: blaze powder for every eye still to make
			Map.entry("eyes_of_ender", List.of(
					count("eyes_of_ender.pearls", c -> c.inv().enderPearls(), 12, c -> false),
					count("eyes_of_ender.powder", c -> item(c, "blaze_powder") + c.inv().eyesOfEnder(), 12, c -> false),
					finish("eyes_of_ender.craft"))),
			Map.entry("find_stronghold", List.of(
					step("find_stronghold.throw", c -> c.player().eyesThrown() > 0),
					finish("find_stronghold.follow"))),
			// chapter 10: the crystals heal the dragon, so they go first
			Map.entry("dragon", List.of(
					count("dragon.crystals", c -> c.world().inEnd() ? END_CRYSTALS - c.creatures().endCrystals() : 0, END_CRYSTALS,
							c -> false),
					finish("dragon.fight"))),
			// chapter 11: debris, smelted into scrap, gold, then the ingot (4 scrap + 4 gold)
			Map.entry("netherite_ingot", List.of(
					count("netherite_ingot.debris", c -> item(c, "ancient_debris") + item(c, "netherite_scrap"), 4, c -> false),
					count("netherite_ingot.smelt", c -> item(c, "netherite_scrap"), 4, c -> false),
					count("netherite_ingot.gold", c -> item(c, "gold_ingot"), 4, c -> false),
					finish("netherite_ingot.craft"))),
			// chapter 11: bastions keep the template in their chests, almost always in the treasure room
			Map.entry("upgrade_template", List.of(
					step("upgrade_template.nether", Steps::inNether),
					step("upgrade_template.bastion", c -> c.nearby().bastionNear()),
					finish("upgrade_template.loot"))),
			// chapter 11: template, diamond gear and an ingot on a smithing table
			Map.entry("netherite_gear", List.of(
					step("netherite_gear.template", c -> c.inv().hasUpgradeTemplate()),
					step("netherite_gear.diamond", c -> sum(c, DIAMOND_GEAR) > 0),
					step("netherite_gear.ingot", c -> c.inv().hasNetheriteIngot()),
					count("netherite_gear.table_iron", c -> item(c, "iron_ingot"), 2, Steps::hasSmithingTable),
					count("netherite_gear.planks", c -> c.inv().planks(), 4, Steps::hasSmithingTable),
					step("netherite_gear.table", Steps::hasSmithingTable),
					finish("netherite_gear.upgrade"))),
			// chapter 11: 6 crying obsidian and 3 glowstone make the anchor; in the Nether, 4 glowstone fill it up
			Map.entry("respawn_anchor", List.of(
					count("respawn_anchor.crying", c -> item(c, "crying_obsidian"), 6, Steps::hasAnchor),
					count("respawn_anchor.glowstone", c -> item(c, "glowstone") + item(c, "glowstone_dust") / DUST_PER_GLOWSTONE, 3,
							Steps::hasAnchor),
					step("respawn_anchor.craft", Steps::hasAnchor),
					step("respawn_anchor.place", c -> inNether(c) && c.nearby().placed("respawn_anchor") > 0),
					finish("respawn_anchor.charge", c -> c.nearby().anchorCharge(), ANCHOR_CHARGES))),
			// chapter 12: a safe spot far from home, the T of soul sand, then the skulls (the last one wakes it)
			Map.entry("summon_wither", List.of(
					step("summon_wither.spot", c -> c.world().underground() || inNether(c) || c.nearby().witherBase()),
					step("summon_wither.base", c -> c.nearby().witherBase()),
					finish("summon_wither.skulls"))),
			// chapter 12: the summoned Wither, defeat it, the star it drops
			Map.entry("nether_star", List.of(
					step("nether_star.wither", c -> c.creatures().witherNearby() || c.nearby().droppedStar()),
					step("nether_star.defeat", c -> c.nearby().droppedStar()),
					finish("nether_star.pick"))),
			// chapter 12: 5 glass, 3 obsidian and the nether star make a beacon; it needs a 3x3 base to work
			Map.entry("beacon", List.of(
					count("beacon.glass", c -> item(c, "glass"), 5, Steps::hasBeacon),
					count("beacon.obsidian", c -> c.inv().obsidian(), 3, Steps::hasBeacon),
					step("beacon.star", c -> c.inv().hasNetherStar() || hasBeacon(c)),
					step("beacon.craft", Steps::hasBeacon),
					finish("beacon.base"))),
			Map.entry("full_beacon", List.of(
					step("full_beacon.level2", c -> c.nearby().beaconLevel() >= 2),
					step("full_beacon.level3", c -> c.nearby().beaconLevel() >= 3),
					finish("full_beacon.level4"))),
			// chapter 14: an outpost, its captain (the illager with a banner), the bottle it drops
			Map.entry("ominous_bottle", List.of(
					step("ominous_bottle.outpost", c -> c.world().in(Structures.PILLAGER_OUTPOST) || c.creatures().captainNearby()
							|| c.nearby().droppedBottle()),
					step("ominous_bottle.captain", c -> c.creatures().captainNearby() || c.nearby().droppedBottle()),
					finish("ominous_bottle.pick"))),
			// chapter 14: the captain's ominous bottle, drink it (Bad Omen), walk into a village, win every wave
			Map.entry("win_raid", List.of(
					step("win_raid.bottle", c -> c.inv().hasOminousBottle() || c.player().omen() || c.nearby().raidActive()),
					step("win_raid.omen", c -> c.player().omen() || c.nearby().raidActive()),
					step("win_raid.village", c -> c.nearby().raidActive()),
					finish("win_raid.fight"))),
			// chapter 15: 4 sherds or bricks in a diamond shape
			Map.entry("decorated_pot", List.of(
					count("decorated_pot.parts", c -> item(c, POT_PARTS), 4, c -> false),
					finish("decorated_pot.craft"))),
			// chapter 15: a trim template, a material and an armor piece on a smithing table
			Map.entry("armor_trim", List.of(
					step("armor_trim.template", c -> item(c, TRIM_TEMPLATES) > 0),
					step("armor_trim.material", c -> item(c, TRIM_MATERIALS) > 0),
					step("armor_trim.armor", c -> item(c, TRIMMABLE_ARMOR) > 0),
					count("armor_trim.table_iron", c -> item(c, "iron_ingot"), 2, Steps::hasSmithingTable),
					count("armor_trim.planks", c -> c.inv().planks(), 4, Steps::hasSmithingTable),
					step("armor_trim.table", Steps::hasSmithingTable),
					finish("armor_trim.trim"))),
			// chapter 15: a brush, a warm ocean ruin, brush its suspicious sand
			Map.entry("sniffer_egg", List.of(
					count("sniffer_egg.feather", c -> item(c, "feather"), 1, c -> c.inv().hasBrush()),
					count("sniffer_egg.copper", c -> item(c, "copper_ingot"), 1, c -> c.inv().hasBrush()),
					count("sniffer_egg.stick", c -> item(c, "stick"), 1, c -> c.inv().hasBrush()),
					step("sniffer_egg.brush", c -> c.inv().hasBrush()),
					step("sniffer_egg.ruin", c -> c.world().in(Structures.OCEAN_RUIN_WARM)),
					finish("sniffer_egg.dig"))),
			// chapter 17: ghast tears from the Nether around soul sand (a dried ghast from a fossil completes it too)
			Map.entry("dried_ghast", List.of(
					step("dried_ghast.nether", c -> inNether(c) || item(c, "ghast_tear") >= GHAST_TEARS),
					count("dried_ghast.tears", c -> item(c, "ghast_tear"), GHAST_TEARS, c -> false),
					count("dried_ghast.soul_sand", c -> c.inv().soulSand(), 1, c -> false),
					finish("dried_ghast.craft"))),
			// chapter 17: the dried ghast into water (the advancement), it swells up, then the grown ghast with a harness
			Map.entry("hydrate_ghast", List.of(
					step("hydrate_ghast.ghast", c -> c.inv().hasDriedGhast() || c.nearby().driedGhast()),
					step("hydrate_ghast.water", c -> c.nearby().placed("water") > 0 || c.inv().hasWaterBucket()),
					finish("hydrate_ghast.place"))),
			Map.entry("ghastling", List.of(
					step("ghastling.wet", c -> c.nearby().ghastHydration() >= 0),
					finish("ghastling.swell", c -> c.nearby().ghastHydration(), GHAST_HYDRATION))),
			Map.entry("ride_happy_ghast", List.of(
					step("ride_happy_ghast.grown", c -> c.creatures().grownHappyGhast()),
					step("ride_happy_ghast.harness", c -> c.inv().hasHarness() || c.creatures().harnessedGhast()),
					step("ride_happy_ghast.equip", c -> c.creatures().harnessedGhast()),
					finish("ride_happy_ghast.ride"))),
			// chapter 17: 3 leather, 2 glass and 1 wool (its color is the harness's color)
			Map.entry("harness", List.of(
					count("harness.leather", c -> item(c, "leather"), 3, c -> false),
					count("harness.glass", c -> item(c, "glass"), 2, c -> false),
					count("harness.wool", c -> c.inv().wool(), 1, c -> false),
					finish("harness.craft"))),
			// chapter 18: 3 copper blocks, a blaze rod and redstone make 4 bulbs; each rising pulse switches one
			Map.entry("copper_bulb", List.of(
					count("copper_bulb.copper", c -> item(c, "copper_block") + item(c, "copper_ingot") / 9, 3, Steps::hasBulb),
					count("copper_bulb.blaze_rod", c -> c.inv().blazeRods(), 1, Steps::hasBulb),
					count("copper_bulb.redstone", c -> c.inv().redstone(), 1, Steps::hasBulb),
					step("copper_bulb.craft", Steps::hasBulb),
					step("copper_bulb.place", c -> c.nearby().placed("copper_bulb") > 0),
					finish("copper_bulb.pulse"))),
			// chapter 18: the crafter's own recipe in a crafter (5 iron, a crafting table, 2 redstone, a dropper), then a pulse
			Map.entry("crafter_crafts", List.of(
					step("crafter_crafts.crafter", c -> c.nearby().crafter()),
					count("crafter_crafts.iron", c -> item(c, "iron_ingot"), 5, Steps::fillingCrafter),
					count("crafter_crafts.dropper", c -> item(c, "dropper"), 1, Steps::fillingCrafter),
					count("crafter_crafts.table", c -> item(c, "crafting_table"), 1, Steps::fillingCrafter),
					count("crafter_crafts.redstone", c -> c.inv().redstone(), 2, Steps::fillingCrafter),
					count("crafter_crafts.fill", c -> c.nearby().crafterSlots(), CRAFTER_SLOTS, c -> false),
					finish("crafter_crafts.pulse"))),
			// chapter 18: a hopper on top feeds the furnace, one below takes the result out
			Map.entry("auto_smelter", List.of(
					count("auto_smelter.iron", c -> item(c, "iron_ingot") + 5 * hoppers(c), 10, c -> hoppers(c) >= 2),
					count("auto_smelter.chests", c -> item(c, "chest") + hoppers(c), 2, c -> hoppers(c) >= 2),
					count("auto_smelter.hoppers", Steps::hoppers, 2, c -> false),
					step("auto_smelter.top", c -> c.nearby().hopperOnFurnace()),
					finish("auto_smelter.bottom")))
	);

	/** Position of every step in its goal's list. */
	private static final Map<String, Integer> INDEX = new HashMap<>();

	static {
		BY_GOAL.forEach((goal, steps) -> {
			for (int i = 0; i < steps.size(); i++) INDEX.put(steps.get(i).id(), i);
		});
	}

	/** Every step of every goal. */
	public static List<Step> all() {
		return BY_GOAL.values().stream().flatMap(List::stream).toList();
	}

	/** The step the player is on for this goal, or null when the goal has no steps. */
	public static @Nullable Step current(Tip goal, SurvivalContext ctx) {
		List<Step> steps = BY_GOAL.get(goal.id());
		if (steps == null) return null;
		for (Step s : steps) if (!s.done().test(ctx)) return s;
		return steps.getLast();
	}

	/** The player moved on to a later step of the same goal (going back, or another goal, doesn't count). */
	public static boolean advanced(String previousId, @Nullable Step now) {
		if (now == null || previousId.isEmpty() || previousId.equals(now.id())) return false;
		Integer before = INDEX.get(previousId);
		return before != null && previousId.startsWith(now.goal() + ".") && INDEX.get(now.id()) > before;
	}
}
