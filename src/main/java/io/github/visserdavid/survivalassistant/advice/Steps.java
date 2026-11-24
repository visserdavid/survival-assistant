package io.github.visserdavid.survivalassistant.advice;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

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

	// ---------------------------------------------------------------- what the steps look at

	/** Items whose carried count the collector reports ({@link SurvivalContext.Inventory#count}). */
	public static final Set<String> TRACKED_ITEMS = Set.of("glass_bottle", "blaze_powder", "paper", "leather", "book",
			"bookshelf", "iron_block", "iron_ingot", "carved_pumpkin", "ancient_debris", "netherite_scrap", "gold_ingot",
			"glass", "beacon", "hopper", "saddle");
	/** Blocks whose placed count nearby the collector reports ({@link SurvivalContext.Nearby#placed}). */
	public static final Set<String> TRACKED_BLOCKS = Set.of("obsidian", "nether_portal", "iron_block", "carved_pumpkin",
			"beacon", "hopper");

	/** The pyramid has at most 4 levels: 9 + 25 + 49 + 81 = 164 blocks. */
	public static final int MAX_BEACON_LEVEL = 4;
	/** The central island of the End has 10 obsidian pillars, each with a crystal. */
	public static final int END_CRYSTALS = 10;
	/** Within this many blocks (horizontally) of a treasure map's red X the player is "there". */
	public static final int TREASURE_NEAR = 8;
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

	// ---------------------------------------------------------------- the steps per goal

	public static final Map<String, List<Step>> BY_GOAL = Map.ofEntries(
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
					step("iron_golem.pumpkin", c -> item(c, "carved_pumpkin") > 0 || c.nearby().placed("carved_pumpkin") > 0),
					finish("iron_golem.build"))),
			// chapter 4: obsidian (carried or already in a frame), light the portal, step through
			Map.entry("enter_nether", List.of(
					count("enter_nether.obsidian", c -> c.inv().obsidian() + c.nearby().placed("obsidian"), 10, Steps::portalLit),
					step("enter_nether.light", Steps::portalLit),
					finish("enter_nether.step_in"))),
			// chapter 5: bottles, water, fuel, then brew (bottles in the stand still count)
			Map.entry("brew_potion", List.of(
					count("brew_potion.bottles",
							c -> item(c, "glass_bottle") + c.inv().waterBottles() + c.nearby().standBottles(), 3, c -> false),
					count("brew_potion.water", c -> c.inv().waterBottles() + c.nearby().standBottles(), 3, c -> false),
					step("brew_potion.fuel", c -> item(c, "blaze_powder") > 0 || c.nearby().standFuel()),
					finish("brew_potion.brew"))),
			// chapter 5: one bookshelf at a time; books count as the paper and leather in them, so crafting doesn't go back
			Map.entry("bookshelves", List.of(
					count("bookshelves.paper", c -> item(c, "paper") + PAPER_PER_BOOK * item(c, "book"),
							BOOKS_PER_SHELF * PAPER_PER_BOOK, Steps::carriesShelves),
					count("bookshelves.leather", c -> item(c, "leather") + item(c, "book"), BOOKS_PER_SHELF, Steps::carriesShelves),
					count("bookshelves.books", c -> item(c, "book"), BOOKS_PER_SHELF, Steps::carriesShelves),
					step("bookshelves.craft", Steps::carriesShelves),
					finish("bookshelves.place"))),
			// chapter 6: a bottle, a hive, a campfire under it (calm bees), wait until it's full, then the bottle
			Map.entry("honey", List.of(
					count("honey.bottle", c -> item(c, "glass_bottle"), 1, c -> false),
					step("honey.hive", c -> c.nearby().hive()),
					step("honey.campfire", c -> c.nearby().hiveSmoked()),
					step("honey.wait", c -> c.nearby().hiveFull()),
					finish("honey.collect"))),
			// chapter 6: find a horse, tame it, a saddle, ride
			Map.entry("horse", List.of(
					step("horse.find", c -> c.creatures().horseNearby()),
					step("horse.tame", c -> c.creatures().tamedHorseNearby()),
					step("horse.saddle", c -> item(c, "saddle") > 0 || c.creatures().saddledHorseNearby()),
					finish("horse.ride"))),
			// chapter 7: a treasure map from a shipwreck or ruin, its red X, then dig
			Map.entry("treasure", List.of(
					step("treasure.map", c -> c.inv().treasureDistance() >= 0),
					step("treasure.follow", c -> c.inv().treasureDistance() >= 0 && c.inv().treasureDistance() <= TREASURE_NEAR),
					finish("treasure.dig"))),
			// chapter 10: blaze powder for every eye still to make
			Map.entry("eyes_of_ender", List.of(
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
			// chapter 12: 5 glass, 3 obsidian and the nether star make a beacon; it needs a 3x3 base to work
			Map.entry("beacon", List.of(
					count("beacon.glass", c -> item(c, "glass"), 5, Steps::hasBeacon),
					count("beacon.obsidian", c -> c.inv().obsidian(), 3, Steps::hasBeacon),
					step("beacon.craft", Steps::hasBeacon),
					finish("beacon.base"))),
			Map.entry("full_beacon", List.of(
					step("full_beacon.level2", c -> c.nearby().beaconLevel() >= 2),
					step("full_beacon.level3", c -> c.nearby().beaconLevel() >= 3),
					finish("full_beacon.level4"))),
			// chapter 17: 3 leather, 2 glass and 1 wool (its color is the harness's color)
			Map.entry("harness", List.of(
					count("harness.leather", c -> item(c, "leather"), 3, c -> false),
					count("harness.glass", c -> item(c, "glass"), 2, c -> false),
					count("harness.wool", c -> c.inv().wool(), 1, c -> false),
					finish("harness.craft"))),
			// chapter 18: a hopper on top feeds the furnace, one below takes the result out
			Map.entry("auto_smelter", List.of(
					count("auto_smelter.hoppers", c -> item(c, "hopper") + c.nearby().placed("hopper"), 2, c -> false),
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
