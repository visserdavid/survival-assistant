package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Chapters 15-18: archaeology, trials, the happy ghast and automation. */
class Chapters1518Test {
	private static SurvivalContext.Builder b() {
		return SurvivalContext.builder();
	}

	private static void completes(Tip goal, SurvivalContext ctx) {
		assertTrue(goal.done().test(ctx), goal.id());
		assertFalse(goal.done().test(SurvivalContext.builder().build()), goal.id() + " is not done in an empty context");
	}

	@Test
	void archaeologyGoals() {
		completes(TipRegistry.BRUSH, b().hasBrush(true).build());
		completes(TipRegistry.FIND_TRAIL_RUINS, b().structure("trail_ruins").build());
		completes(TipRegistry.SHERD, b().advancement(Advancements.SHERD).build());
		completes(TipRegistry.DECORATED_POT, b().hasDecoratedPot(true).build());
		completes(TipRegistry.ARMOR_TRIM, b().advancement(Advancements.TRIM).build());
		completes(TipRegistry.SNIFFER_EGG, b().advancement(Advancements.SNIFFER_EGG).build());
	}

	@Test
	void trialGoals() {
		completes(TipRegistry.BREEZE_RODS, b().breezeRods(2).build());
		assertFalse(TipRegistry.BREEZE_RODS.done().test(b().breezeRods(1).build()));
		completes(TipRegistry.WIND_CHARGE, b().hasWindCharge(true).build());
		completes(TipRegistry.OMINOUS_VAULT, b().advancement(Advancements.OMINOUS_VAULT).build());
		completes(TipRegistry.HEAVY_CORE, b().hasHeavyCore(true).build());
		completes(TipRegistry.MACE, b().hasMace(true).build());
		assertTrue(TipRegistry.HEAVY_CORE.done().test(b().hasMace(true).build()), "the core went into the mace");
	}

	@Test
	void happyGhastGoals() {
		completes(TipRegistry.DRIED_GHAST, b().hasDriedGhast(true).build());
		completes(TipRegistry.HYDRATE_GHAST, b().advancement(Advancements.DRIED_GHAST).build());
		completes(TipRegistry.GHASTLING, b().ghastling(true).build());
		completes(TipRegistry.HARNESS, b().hasHarness(true).build());
		completes(TipRegistry.RIDE_HAPPY_GHAST, b().ridingHappyGhast(true).build());
		assertTrue(TipRegistry.HARNESS.done().test(b().ridingHappyGhast(true).build()), "the harness is on the ghast");
	}

	@Test
	void automationGoals() {
		completes(TipRegistry.OBSERVER, b().observer(true).build());
		completes(TipRegistry.AUTO_SMELTER, b().autoSmelter(true).build());
		completes(TipRegistry.COPPER_BULB, b().copperBulbLit(true).build());
		completes(TipRegistry.CRAFTER, b().crafter(true).build());
		completes(TipRegistry.CRAFTER_CRAFTS, b().advancement(Advancements.CRAFTER).build());
	}

	@Test
	void trailRuinsNoteComesOnce() {
		assertEquals("trail_ruins", PlaceNotices.next(b().structure("trail_ruins").build(), Set.of()));
		assertNull(PlaceNotices.next(b().structure("trail_ruins").build(), Set.of("trail_ruins")));
	}

	@Test
	void automationNeedsRedstoneAndTheNether() {
		Set<String> done = new HashSet<>();
		for (Chapter c : java.util.List.of(Chapters.FIRST_DAYS, Chapters.SETTLING_IN, Chapters.REDSTONE)) c.goals().forEach(t -> done.add(t.id()));
		assertEquals(Chapters.Status.LOCKED, Chapters.status(Chapters.AUTOMATION, 2, done), "quartz and blaze rods come from the Nether");
		Chapters.NETHER.goals().forEach(t -> done.add(t.id()));
		assertEquals(Chapters.Status.AVAILABLE, Chapters.status(Chapters.AUTOMATION, 2, done));
		assertEquals(Chapters.Status.AVAILABLE, Chapters.status(Chapters.HAPPY_GHAST, 2, done));
		assertEquals(Chapters.Status.LOCKED, Chapters.status(Chapters.TRIALS, 2, done), "needs exploring");
	}

	@Test
	void eighteenChaptersWithUniqueGoals() {
		assertEquals(18, Chapters.ALL.size());
		Set<String> ids = new HashSet<>();
		for (Chapter c : Chapters.ALL) c.goals().forEach(t -> assertTrue(ids.add(t.id()), t.id()));
	}
}
