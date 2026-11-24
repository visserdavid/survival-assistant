package io.github.visserdavid.survivalassistant.advice;

import io.github.visserdavid.survivalassistant.config.ModConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class StepsTest {
	private static String current(Tip goal, UnaryOperator<SurvivalContext.Builder> setup) {
		Steps.Step s = Steps.current(goal, setup.apply(SurvivalContext.builder()).build());
		assertNotNull(s, goal.id() + " has steps");
		return s.id();
	}

	@Test
	void goalsWithoutStepsHaveNone() {
		assertNull(Steps.current(TipRegistry.WOOD, SurvivalContext.builder().build()));
	}

	@Test
	void theLastStepIsNeverDoneByItself() {
		for (List<Steps.Step> steps : Steps.BY_GOAL.values()) {
			assertFalse(steps.getLast().done().test(SurvivalContext.builder().build()), steps.getLast().id());
		}
	}

	@Test
	void brewingGoesStepByStep() {
		Tip g = TipRegistry.BREW_POTION;
		assertEquals("brew_potion.bottles", current(g, b -> b));
		assertEquals("brew_potion.bottles", current(g, b -> b.item("glass_bottle", 2)));
		assertEquals("brew_potion.water", current(g, b -> b.item("glass_bottle", 3)));
		assertEquals("brew_potion.water", current(g, b -> b.item("glass_bottle", 1).waterBottles(2)));
		assertEquals("brew_potion.fuel", current(g, b -> b.waterBottles(3)));
		assertEquals("brew_potion.brew", current(g, b -> b.waterBottles(3).item("blaze_powder", 1)));
	}

	@Test
	void bottlesInTheStandStillCount() {
		Tip g = TipRegistry.BREW_POTION;
		assertEquals("brew_potion.fuel", current(g, b -> b.standBottles(3)));
		assertEquals("brew_potion.brew", current(g, b -> b.standBottles(3).standFuel(true)),
				"the powder is used up, but the stand still has fuel");
	}

	@Test
	void netherPortal() {
		Tip g = TipRegistry.ENTER_NETHER;
		assertEquals("enter_nether.obsidian", current(g, b -> b.obsidian(4)));
		assertEquals("enter_nether.light", current(g, b -> b.obsidian(4).block("obsidian", 6)),
				"obsidian in the frame counts while building it");
		assertEquals("enter_nether.step_in", current(g, b -> b.block("nether_portal", 6)), "a lit portal skips ahead");
	}

	@Test
	void ironGolem() {
		Tip g = TipRegistry.IRON_GOLEM;
		assertEquals("iron_golem.iron_blocks", current(g, b -> b.item("iron_block", 3).item("iron_ingot", 8)));
		assertEquals("iron_golem.pumpkin", current(g, b -> b.item("iron_block", 3).item("iron_ingot", 9)), "9 ingots are a block");
		assertEquals("iron_golem.pumpkin", current(g, b -> b.block("iron_block", 4)), "placed blocks count");
		assertEquals("iron_golem.build", current(g, b -> b.block("iron_block", 4).item("carved_pumpkin", 1)));
	}

	@Test
	void bookshelvesCountBooksAsTheirMaterials() {
		Tip g = TipRegistry.BOOKSHELVES;
		assertEquals("bookshelves.paper", current(g, b -> b.item("paper", 8)));
		assertEquals("bookshelves.leather", current(g, b -> b.item("paper", 9)));
		assertEquals("bookshelves.books", current(g, b -> b.item("paper", 9).item("leather", 3)));
		// after crafting one book: 6 paper, 2 leather, 1 book is still enough for 3 books
		assertEquals("bookshelves.books", current(g, b -> b.item("paper", 6).item("leather", 2).item("book", 1)));
		assertEquals("bookshelves.craft", current(g, b -> b.item("book", 3)));
		assertEquals("bookshelves.place", current(g, b -> b.item("bookshelf", 1)));
	}

	@Test
	void eyesAndStronghold() {
		assertEquals("eyes_of_ender.powder", current(TipRegistry.EYES_OF_ENDER, b -> b.item("blaze_powder", 6).eyesOfEnder(5)));
		assertEquals("eyes_of_ender.craft", current(TipRegistry.EYES_OF_ENDER, b -> b.item("blaze_powder", 7).eyesOfEnder(5)));
		assertEquals("find_stronghold.throw", current(TipRegistry.FIND_STRONGHOLD, b -> b));
		assertEquals("find_stronghold.follow", current(TipRegistry.FIND_STRONGHOLD, b -> b.eyesThrown(1)));
	}

	@Test
	void dragonCrystalsOnlyCountInTheEnd() {
		Tip g = TipRegistry.DRAGON;
		assertEquals("dragon.crystals", current(g, b -> b));
		assertEquals("dragon.crystals", current(g, b -> b.dimension(SurvivalContext.END).endCrystals(3)));
		assertEquals("dragon.fight", current(g, b -> b.dimension(SurvivalContext.END)));
		Steps.Step crystals = Steps.BY_GOAL.get("dragon").getFirst();
		assertEquals(7, crystals.value(SurvivalContext.builder().dimension(SurvivalContext.END).endCrystals(3).build()));
		assertEquals(0, crystals.value(SurvivalContext.builder().build()), "no false progress outside the End");
	}

	@Test
	void netheriteBeaconAndSmelter() {
		assertEquals("netherite_ingot.smelt", current(TipRegistry.NETHERITE_INGOT, b -> b.item("ancient_debris", 2).item("netherite_scrap", 2)));
		assertEquals("netherite_ingot.gold", current(TipRegistry.NETHERITE_INGOT, b -> b.item("netherite_scrap", 4)));
		assertEquals("netherite_ingot.craft", current(TipRegistry.NETHERITE_INGOT, b -> b.item("netherite_scrap", 4).item("gold_ingot", 4)));

		assertEquals("beacon.obsidian", current(TipRegistry.BEACON, b -> b.item("glass", 5)));
		assertEquals("beacon.base", current(TipRegistry.BEACON, b -> b.block("beacon", 1)), "a placed beacon skips the crafting");

		assertEquals("full_beacon.level2", current(TipRegistry.FULL_BEACON, b -> b.beaconLevel(1)));
		assertEquals("full_beacon.level4", current(TipRegistry.FULL_BEACON, b -> b.beaconLevel(3)));

		assertEquals("auto_smelter.top", current(TipRegistry.AUTO_SMELTER, b -> b.item("hopper", 1).block("hopper", 1)));
		assertEquals("auto_smelter.bottom", current(TipRegistry.AUTO_SMELTER, b -> b.block("hopper", 2).hopperOnFurnace(true)));
	}

	@Test
	void enchantingTable() {
		Tip g = TipRegistry.ENCHANTING_TABLE;
		assertEquals("enchanting_table.diamonds", current(g, b -> b.diamonds(1).obsidian(10)), "the pickaxe used the diamonds");
		assertEquals("enchanting_table.obsidian", current(g, b -> b.diamonds(2).obsidian(3)));
		assertEquals("enchanting_table.paper", current(g, b -> b.diamonds(2).obsidian(4).item("paper", 2)));
		assertEquals("enchanting_table.leather", current(g, b -> b.diamonds(2).obsidian(4).item("paper", 3)));
		assertEquals("enchanting_table.book", current(g, b -> b.diamonds(2).obsidian(4).item("paper", 3).item("leather", 1)));
		assertEquals("enchanting_table.craft", current(g, b -> b.diamonds(2).obsidian(4).item("book", 1)),
				"a book counts as its paper and leather");
	}

	@Test
	void treasure() {
		Tip g = TipRegistry.TREASURE;
		assertEquals("treasure.map", current(g, b -> b));
		assertEquals("treasure.follow", current(g, b -> b.treasureDistance(300)));
		assertEquals("treasure.dig", current(g, b -> b.treasureDistance(Steps.TREASURE_NEAR)));
		assertEquals("treasure.dig", current(g, b -> b.treasureDistance(0)), "standing on the X");
	}

	@Test
	void harness() {
		Tip g = TipRegistry.HARNESS;
		assertEquals("harness.leather", current(g, b -> b.item("leather", 2).item("glass", 2).wool(1)));
		assertEquals("harness.glass", current(g, b -> b.item("leather", 3).item("glass", 1)));
		assertEquals("harness.wool", current(g, b -> b.item("leather", 3).item("glass", 2)));
		assertEquals("harness.craft", current(g, b -> b.item("leather", 3).item("glass", 2).wool(1)));
	}

	@Test
	void countersAreClamped() {
		Steps.Step bottles = Steps.BY_GOAL.get("brew_potion").getFirst();
		assertEquals(3, bottles.target());
		assertEquals(3, bottles.value(SurvivalContext.builder().item("glass_bottle", 9).build()));
		assertEquals(0, Steps.BY_GOAL.get("brew_potion").get(2).value(SurvivalContext.builder().build()), "no counter");
	}

	@Test
	void advancedOnlyCountsMovingForwardInTheSameGoal() {
		List<Steps.Step> brew = Steps.BY_GOAL.get("brew_potion");
		assertTrue(Steps.advanced("brew_potion.bottles", brew.get(1)));
		assertFalse(Steps.advanced("brew_potion.water", brew.get(0)), "going back (bottles used up) is no news");
		assertFalse(Steps.advanced("brew_potion.water", brew.get(1)));
		assertFalse(Steps.advanced("", brew.get(1)), "the first look is no news");
		assertFalse(Steps.advanced("bookshelves.paper", brew.get(1)), "another goal");
		assertFalse(Steps.advanced("brew_potion.bottles", null));
	}

	// ---------------------------------------------------------------- the engine and the card

	private static GuideProgress inChapter(Chapter chapter, Tip upTo) {
		GuideProgress p = new GuideProgress();
		p.chapter = chapter.number();
		for (Tip t : chapter.goals()) {
			if (t == upTo) break;
			p.completed.add(t.id());
		}
		return p;
	}

	private static List<AdviceEngine.Message> evaluate(SurvivalContext c, GuideProgress p, long now) {
		return AdviceEngine.evaluate(c, p, now, AdviceEngine.Settings.from(new ModConfig()), false);
	}

	@Test
	void theGuideSaysTheStepAndMovesOnRightAway() {
		GuideProgress p = inChapter(Chapters.MAGIC, TipRegistry.BREW_POTION);
		List<AdviceEngine.Message> first = evaluate(SurvivalContext.builder().build(), p, 0);
		assertEquals(1, first.size());
		assertEquals(TipRegistry.BREW_POTION, first.getFirst().tip());
		assertEquals("brew_potion.bottles", first.getFirst().step().id());

		assertTrue(evaluate(SurvivalContext.builder().build(), p, 20).isEmpty(), "same step: wait for the cooldown");

		List<AdviceEngine.Message> next = evaluate(SurvivalContext.builder().item("glass_bottle", 3).build(), p, 40);
		assertEquals(1, next.size(), "the next step is said at once");
		assertEquals("brew_potion.water", next.getFirst().step().id());

		assertTrue(evaluate(SurvivalContext.builder().item("glass_bottle", 1).build(), p, 60).isEmpty(),
				"going back is not repeated before the cooldown");
	}

	@Test
	void theCardShowsTheStepOfTheCurrentTaskOnly() {
		GuideProgress p = inChapter(Chapters.MAGIC, TipRegistry.BREW_POTION);
		HudSnapshot s = HudSnapshot.of(SurvivalContext.builder().item("glass_bottle", 3).waterBottles(1).build(), p);
		assertEquals("brew_potion", s.next().getFirst().id());
		assertEquals(new HudSnapshot.Entry("brew_potion.water", 1, 3), s.step());

		GuideProgress ch1 = new GuideProgress();
		assertEquals(HudSnapshot.NO_STEP, HudSnapshot.of(SurvivalContext.builder().build(), ch1).step(), "wood has no steps");
	}
}
