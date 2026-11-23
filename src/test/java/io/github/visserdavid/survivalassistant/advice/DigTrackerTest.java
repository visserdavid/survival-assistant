package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DigTrackerTest {
	@Test
	void countsQuickBreaksUnderTheFeet() {
		DigTracker t = new DigTracker();
		assertEquals(0, t.current(0));
		assertEquals(1, t.record(100));
		assertEquals(2, t.record(150));
		assertEquals(2, t.current(200));
		assertEquals(0, t.current(150 + DigTracker.WINDOW_TICKS + 1), "streak expires");
		assertEquals(1, t.record(1000), "a slow break starts over");
	}
}
