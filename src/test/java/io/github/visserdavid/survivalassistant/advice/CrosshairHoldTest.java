package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosshairHoldTest {
	@Test
	void theCardKeepsTheInfoForAWhile() {
		CrosshairHold h = new CrosshairHold();
		assertTrue(h.tick(true, true));
		for (int i = 0; i < CrosshairHold.HOLD_TICKS; i++) assertTrue(h.tick(false, true), "tick " + i);
		assertFalse(h.tick(false, true));
	}

	@Test
	void lookingAtSomethingNewStartsTheTimeAgain() {
		CrosshairHold h = new CrosshairHold();
		h.tick(true, true);
		for (int i = 0; i < 30; i++) h.tick(false, true);
		assertTrue(h.tick(true, true));
		for (int i = 0; i < CrosshairHold.HOLD_TICKS; i++) assertTrue(h.tick(false, true));
		assertFalse(h.tick(false, true));
	}

	@Test
	void underTheCrosshairItGoesAtOnce() {
		CrosshairHold h = new CrosshairHold();
		assertTrue(h.tick(true, false));
		assertFalse(h.tick(false, false));
	}

	@Test
	void switchingToTheCrosshairEndsTheHold() {
		CrosshairHold h = new CrosshairHold();
		h.tick(true, true);
		h.tick(false, true);
		assertFalse(h.tick(false, false));
		assertFalse(h.tick(false, true));
	}
}
