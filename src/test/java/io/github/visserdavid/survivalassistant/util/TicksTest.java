package io.github.visserdavid.survivalassistant.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TicksTest {
	@Test
	void secondsMinutesAndDays() {
		assertEquals(20, Ticks.seconds(1));
		assertEquals(1200, Ticks.minutes(1));
		assertEquals(0, Ticks.day(23_999));
		assertEquals(1, Ticks.day(24_000));
	}

	@Test
	void timeOfDayWrapsAndStaysPositive() {
		assertEquals(0, Ticks.timeOfDay(0));
		assertEquals(11_000, Ticks.timeOfDay(24_000 * 3 + 11_000));
		assertEquals(23_000, Ticks.timeOfDay(-1_000), "a clock set back before 0 still gives a time of day");
	}
}
