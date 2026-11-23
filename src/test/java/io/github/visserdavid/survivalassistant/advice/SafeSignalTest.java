package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeSignalTest {
	private static final int NIGHT = 15000;

	private static SurvivalContext.Builder litCave() {
		return SurvivalContext.builder().sheltered(true).underground(true).litArea(true);
	}

	@Test
	void darkWarningsGoQuietWhereItIsWellLit() {
		assertTrue(TipRegistry.UNDERGROUND.applies().test(SurvivalContext.builder().sheltered(true).underground(true).build()));
		assertFalse(TipRegistry.UNDERGROUND.applies().test(litCave().build()));
		SurvivalContext litNight = SurvivalContext.builder().time(NIGHT).litArea(true).build();
		assertTrue(TipRegistry.NIGHT_OUTSIDE.applies().test(SurvivalContext.builder().time(NIGHT).build()));
		assertFalse(TipRegistry.NIGHT_OUTSIDE.applies().test(litNight));
		assertFalse(TipRegistry.DUSK.applies().test(SurvivalContext.builder().time(11500).litArea(true).build()));
		assertFalse(TipRegistry.THUNDER.applies().test(SurvivalContext.builder().thundering(true).litArea(true).build()));
	}

	@Test
	void realDangersStillWarnInTheLight() {
		assertTrue(TipRegistry.CREEPER.applies().test(litCave().creeper(true).build()));
		assertTrue(TipRegistry.LAVA_NEAR.applies().test(litCave().lava(true).build()));
		assertTrue(TipRegistry.DIG_DOWN.applies().test(litCave().digDown(2).build()));
	}

	@Test
	void showsInALitCaveAndOnALitSpotAtNight() {
		assertTrue(SafeSignal.show(litCave().build()));
		assertTrue(SafeSignal.show(SurvivalContext.builder().time(NIGHT).litArea(true).build()));
	}

	@Test
	void notShownWhenTheDarkDoesNotMatterOrSomethingIsGoingOn() {
		assertFalse(SafeSignal.show(litCave().litArea(false).build()), "dark cave");
		assertFalse(SafeSignal.show(SurvivalContext.builder().litArea(true).build()), "daytime outside");
		assertFalse(SafeSignal.show(SurvivalContext.builder().time(NIGHT).sheltered(true).litArea(true).build()), "indoors");
		assertFalse(SafeSignal.show(litCave().hostiles(1).build()), "a monster nearby");
		assertFalse(SafeSignal.show(litCave().lava(true).build()), "an urgent tip");
	}

	@Test
	void theGuideSaysItOnlyWhenComingIntoTheLight() {
		assertTrue(SafeSignal.notice(false, true, 1000, SafeSignal.NEVER));
		assertFalse(SafeSignal.notice(true, true, 1000, SafeSignal.NEVER), "already safe");
		assertFalse(SafeSignal.notice(false, false, 1000, SafeSignal.NEVER), "still dark");
	}

	@Test
	void andNotAgainWithinTheCooldown() {
		long said = 1000;
		assertFalse(SafeSignal.notice(false, true, said + SafeSignal.NOTICE_COOLDOWN_TICKS - 1, said));
		assertTrue(SafeSignal.notice(false, true, said + SafeSignal.NOTICE_COOLDOWN_TICKS, said));
	}

	@Test
	void theTaskCardCarriesTheSignal() {
		assertTrue(HudSnapshot.of(litCave().build(), new GuideProgress(), 3).safe());
		assertFalse(HudSnapshot.of(SurvivalContext.builder().build(), new GuideProgress(), 3).safe());
	}
}
