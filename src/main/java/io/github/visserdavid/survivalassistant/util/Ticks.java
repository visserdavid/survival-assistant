package io.github.visserdavid.survivalassistant.util;

/** Game time: 20 ticks per second, 24000 ticks per day. */
public final class Ticks {
	private Ticks() {}

	public static final int PER_SECOND = 20;
	public static final int PER_MINUTE = 60 * PER_SECOND;
	public static final int PER_DAY = 24000;

	public static long seconds(long seconds) {
		return seconds * PER_SECOND;
	}

	public static long minutes(long minutes) {
		return minutes * PER_MINUTE;
	}

	/** The day number of a (non-negative) clock tick. */
	public static long day(long clock) {
		return clock / PER_DAY;
	}

	/** 0..23999 within the day: 0 is sunrise, 12000 sunset. */
	public static int timeOfDay(long clock) {
		return (int) Math.floorMod(clock, (long) PER_DAY);
	}
}
