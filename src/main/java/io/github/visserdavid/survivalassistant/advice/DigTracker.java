package io.github.visserdavid.survivalassistant.advice;

/**
 * Counts how often a player breaks the block directly under their feet in quick succession ("digging straight
 * down", the number one beginner mistake). Pure logic, unit tested.
 */
public final class DigTracker {
	/** Breaks further apart than this (10 seconds) start a new streak. */
	public static final long WINDOW_TICKS = 200;

	private long lastTick = Long.MIN_VALUE;
	private int streak = 0;

	/** Records a break under the feet and returns the current streak. */
	public int record(long now) {
		streak = (lastTick != Long.MIN_VALUE && now - lastTick <= WINDOW_TICKS) ? streak + 1 : 1;
		lastTick = now;
		return streak;
	}

	/** The streak if it is still recent, otherwise 0. */
	public int current(long now) {
		return lastTick != Long.MIN_VALUE && now - lastTick <= WINDOW_TICKS ? streak : 0;
	}
}
