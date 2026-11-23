package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Chapters 3-5: every goal completes in a fitting situation, and the Nether warnings stay in the Nether. */
class NewChaptersTest {
	private static SurvivalContext.Builder b() {
		return SurvivalContext.builder();
	}

	private static void completes(Tip goal, SurvivalContext ctx) {
		assertTrue(goal.done().test(ctx), goal.id());
		assertFalse(goal.done().test(SurvivalContext.builder().build()), goal.id() + " is not done in an empty context");
	}

	@Test
	void villageGoals() {
		completes(TipRegistry.FIND_VILLAGE, b().structure(Structures.VILLAGE_PLAINS).build());
		completes(TipRegistry.FIRST_TRADE, b().advancement(Advancements.TRADE).build());
		completes(TipRegistry.TRADES, b().trades(5).build());
		assertFalse(TipRegistry.TRADES.done().test(b().trades(4).build()));
		completes(TipRegistry.EMERALDS, b().emeralds(10).build());
		completes(TipRegistry.ENCHANTED_BOOK, b().hasEnchantedBook(true).build());
		completes(TipRegistry.IRON_GOLEM, b().advancement(Advancements.IRON_GOLEM).build());
		assertTrue(TipRegistry.FIND_VILLAGE.done().test(b().advancement(Advancements.TRADE).build()), "skipping ahead counts");
	}

	@Test
	void netherGoals() {
		completes(TipRegistry.GOLD_ARMOR, b().goldArmorWorn(true).build());
		completes(TipRegistry.ENTER_NETHER, b().dimension(SurvivalContext.NETHER).build());
		completes(TipRegistry.FIND_FORTRESS, b().advancement(Advancements.FORTRESS).build());
		completes(TipRegistry.BLAZE_RODS, b().blazeRods(3).build());
		completes(TipRegistry.NETHER_WART, b().netherWart(4).build());
		completes(TipRegistry.HOME_SAFE, b().blazeRods(1).advancement(Advancements.ENTER_NETHER).build());
		assertFalse(TipRegistry.HOME_SAFE.done().test(b().blazeRods(1).advancement(Advancements.ENTER_NETHER)
				.dimension(SurvivalContext.NETHER).build()), "still in the Nether");
	}

	@Test
	void magicGoals() {
		completes(TipRegistry.BREWING_STAND, b().hasBrewingStand(true).build());
		completes(TipRegistry.BREW_POTION, b().advancement(Advancements.BREW).build());
		completes(TipRegistry.BOOKSHELVES, b().bookshelves(15).build());
		assertFalse(TipRegistry.BOOKSHELVES.done().test(b().bookshelves(14).build()));
		completes(TipRegistry.LEVEL_30, b().xpLevel(30).build());
		completes(TipRegistry.ENCHANT, b().advancement(Advancements.ENCHANT).build());
		completes(TipRegistry.ANVIL, b().hasAnvil(true).build());
	}

	@Test
	void netherWarningsOnlyInTheNether() {
		SurvivalContext nether = b().dimension(SurvivalContext.NETHER).sheltered(true).timeOfDay(15000).ghast(true).build();
		assertEquals(TipRegistry.GHAST, AdviceEngine.activeUrgent(nether));
		assertEquals(TipRegistry.PIGLINS, AdviceEngine.activeUrgent(b().dimension(SurvivalContext.NETHER).piglin(true).build()));
		assertNull(AdviceEngine.activeUrgent(b().dimension(SurvivalContext.NETHER).piglin(true).goldArmorWorn(true).build()),
				"gold keeps piglins calm");
		assertNull(AdviceEngine.activeUrgent(b().ghast(true).piglin(true).build()), "not in the overworld");
	}

	@Test
	void overworldNightWarningsStayOutOfTheNetherAndTheEnd() {
		SurvivalContext endNight = b().dimension(SurvivalContext.END).timeOfDay(15000).ticksSinceRest(60000).build();
		assertNull(AdviceEngine.activeUrgent(endNight));
		assertEquals(TipRegistry.NIGHT_OUTSIDE, AdviceEngine.activeUrgent(b().timeOfDay(15000).build()));
	}

	@Test
	void placeNoticesComeOnce() {
		SurvivalContext nether = b().dimension(SurvivalContext.NETHER).build();
		assertEquals("nether", PlaceNotices.next(nether, Set.of()));
		assertNull(PlaceNotices.next(nether, Set.of("nether")));
		SurvivalContext villageNight = b().structure(Structures.VILLAGE_PLAINS).timeOfDay(15000).build();
		assertEquals("village", PlaceNotices.next(villageNight, Set.of()));
		assertEquals("village_night", PlaceNotices.next(villageNight, Set.of("village")));
		assertNull(PlaceNotices.next(b().structure(Structures.VILLAGE_PLAINS).build(), Set.of("village")), "daytime");
	}

	@Test
	void hudCardFollowsTheChapter() {
		GuideProgress p = new GuideProgress();
		p.chapter = Chapters.NETHER.number();
		HudSnapshot s = HudSnapshot.of(b().build(), p, 0);
		assertEquals(4, s.chapter());
		assertEquals("gold_armor", s.next().get(0).id());
		assertEquals(TipRegistry.GOLD_ARMOR, AdviceEngine.nextMilestone(p));
	}
}
