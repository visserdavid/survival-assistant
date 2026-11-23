package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Chapters 9-10: redstone and the End. */
class Chapters910Test {
	private static SurvivalContext.Builder b() {
		return SurvivalContext.builder();
	}

	private static void completes(Tip goal, SurvivalContext ctx) {
		assertTrue(goal.done().test(ctx), goal.id());
		assertFalse(goal.done().test(SurvivalContext.builder().build()), goal.id() + " is not done in an empty context");
	}

	@Test
	void redstoneGoals() {
		completes(TipRegistry.REDSTONE_DUST, b().redstone(16).build());
		assertFalse(TipRegistry.REDSTONE_DUST.done().test(b().redstone(15).build()));
		completes(TipRegistry.IRON_DOOR, b().ironDoorOpen(true).build());
		completes(TipRegistry.PISTON, b().pistonExtended(true).build());
		completes(TipRegistry.REPEATER, b().repeater(true).build());
		completes(TipRegistry.HOPPER, b().hopper(true).build());
	}

	@Test
	void endGoals() {
		completes(TipRegistry.ENDER_PEARLS, b().enderPearls(12).build());
		completes(TipRegistry.EYES_OF_ENDER, b().eyes(12).build());
		completes(TipRegistry.FIND_STRONGHOLD, b().advancement(TipRegistry.ADV_STRONGHOLD).build());
		completes(TipRegistry.ENTER_END, b().dimension(SurvivalContext.END).build());
		completes(TipRegistry.DRAGON, b().advancement(TipRegistry.ADV_DRAGON).build());
		completes(TipRegistry.END_CITY, b().advancement(TipRegistry.ADV_END_CITY).build());
		completes(TipRegistry.ELYTRA, b().elytra(true).build());
		// pearls thrown and eyes used: the earlier goals stay done once the player is further
		assertTrue(TipRegistry.ENDER_PEARLS.done().test(b().advancement(TipRegistry.ADV_STRONGHOLD).build()));
		assertTrue(TipRegistry.EYES_OF_ENDER.done().test(b().advancement(TipRegistry.ADV_ENTER_END).build()));
	}

	@Test
	void endNotesComeOnce() {
		assertEquals("stronghold", PlaceNotices.next(b().structure("stronghold").build(), Set.of()));
		assertEquals("end", PlaceNotices.next(b().dimension(SurvivalContext.END).build(), Set.of()));
		assertEquals("end_city", PlaceNotices.next(b().dimension(SurvivalContext.END).structure("end_city").build(), Set.of("end")));
		assertNull(PlaceNotices.next(b().dimension(SurvivalContext.END).build(), Set.of("end")));
	}

	@Test
	void noOverworldNightWarningsInTheEnd() {
		assertNull(AdviceEngine.activeUrgent(b().dimension(SurvivalContext.END).time(15000).thundering(true).build()));
	}
}
