package io.github.visserdavid.survivalassistant.advice;

import io.github.visserdavid.survivalassistant.config.ModConfig;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NavigationTest {
	private static final String OW = "minecraft:overworld";
	private static final Navigation.Mark HOME = new Navigation.Mark(OW, 0, 64, 0);

	private static Navigation.Mark at(int x, int z) {
		return new Navigation.Mark(OW, x, 64, z);
	}

	@Test
	void deathSpotWinsWhileItemsExist() {
		Navigation.Mark death = at(100, 0);
		Optional<Navigation.Waypoint> w = Navigation.pick(death, 1000, HOME, at(500, 0), 1000 + 1200, false, 64);
		assertTrue(w.isPresent());
		assertEquals(Navigation.Kind.DEATH, w.get().kind());
		assertEquals(Navigation.ITEM_LIFETIME_TICKS - 1200, w.get().ticksLeft());
	}

	@Test
	void deathSpotExpiresAfterItemLifetime() {
		Optional<Navigation.Waypoint> w = Navigation.pick(at(100, 0), 0, HOME, at(500, 0), Navigation.ITEM_LIFETIME_TICKS, false, 64);
		assertEquals(Navigation.Kind.HOME, w.orElseThrow().kind(), "after 6000 ticks the items are gone; fall back to home");
	}

	@Test
	void homeOnlyWhenFarOrAtNight() {
		assertTrue(Navigation.pick(null, 0, HOME, at(40, 0), 0, false, 64).isEmpty(), "40 blocks by day: no need");
		assertTrue(Navigation.pick(null, 0, HOME, at(80, 0), 0, false, 64).isPresent(), "80 blocks: show");
		assertTrue(Navigation.pick(null, 0, HOME, at(30, 0), 0, true, 64).isPresent(), "30 blocks at night: show");
		assertTrue(Navigation.pick(null, 0, HOME, at(10, 0), 0, true, 64).isEmpty(), "close to home: nothing");
	}

	@Test
	void homeIgnoredInOtherDimensionOrWhenUnknown() {
		Navigation.Mark nether = new Navigation.Mark("minecraft:the_nether", 500, 64, 0);
		assertTrue(Navigation.pick(null, 0, HOME, nether, 0, true, 64).isEmpty());
		assertTrue(Navigation.pick(null, 0, null, at(500, 0), 0, true, 64).isEmpty());
	}

	@Test
	void deathSpotInOtherDimensionIsStillShown() {
		Navigation.Mark netherDeath = new Navigation.Mark("minecraft:the_nether", 5, 40, 5);
		assertEquals(Navigation.Kind.DEATH, Navigation.pick(netherDeath, 0, HOME, at(0, 0), 100, false, 64).orElseThrow().kind());
	}

	@Test
	void reachedNeedsSameDimensionAndCloseBy() {
		assertTrue(Navigation.reached(at(10, 10), new Navigation.Mark(OW, 11, 65, 10)));
		assertFalse(Navigation.reached(at(10, 10), at(20, 10)));
		assertFalse(Navigation.reached(at(10, 10), new Navigation.Mark("minecraft:the_nether", 10, 64, 10)));
	}


	@Test
	void safetyWarnings() {
		GuideProgress p = new GuideProgress();
		assertEquals(TipRegistry.DIG_DOWN, firstTip(SurvivalContext.builder().digDownStreak(2).build(), p));
		assertEquals(TipRegistry.LAVA_NEAR, firstTip(SurvivalContext.builder().lava(true).build(), new GuideProgress()));
		assertEquals(TipRegistry.PHANTOMS, firstTip(SurvivalContext.builder().timeOfDay(15000).sheltered(true)
				.ticksSinceRest(SurvivalContext.PHANTOM_WARNING_TICKS).build(), new GuideProgress()));
		assertNotEquals(TipRegistry.PHANTOMS, firstTip(SurvivalContext.builder().timeOfDay(15000).sheltered(true)
				.ticksSinceRest(20000).build(), new GuideProgress()), "slept recently: no phantom warning");
		assertEquals(TipRegistry.FALLING_BLOCK, firstTip(SurvivalContext.builder().sheltered(true).underground(true)
				.fallingBlockAbove(true).torches(5).build(), new GuideProgress()));
		assertNotEquals(TipRegistry.DIG_DOWN, firstTip(SurvivalContext.builder().digDownStreak(1).build(), new GuideProgress()),
				"one break under your feet is fine (e.g. placing a block)");
	}

	private static Tip firstTip(SurvivalContext c, GuideProgress p) {
		return AdviceEngine.evaluate(c, p, 0, AdviceEngine.Settings.from(new ModConfig()), false).stream()
				.filter(m -> !m.praise()).findFirst().orElseThrow().tip();
	}
}
