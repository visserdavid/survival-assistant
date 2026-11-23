package io.github.visserdavid.survivalassistant.advice;

/**
 * How long the mob/ore info stays after the player looks away. In the card it stays a little while, so there is
 * time to read it; under the crosshair it goes at once (text hanging around the crosshair is confusing).
 */
public final class CrosshairHold {
	/** About 2 seconds. */
	public static final int HOLD_TICKS = 40;

	private int left;

	/**
	 * Called every client tick.
	 *
	 * @param hasInfo there is info for what the player looks at now
	 * @param hold    the info is shown in the card (and may stay a while)
	 * @return the last info should still be shown
	 */
	public boolean tick(boolean hasInfo, boolean hold) {
		if (hasInfo) {
			left = hold ? HOLD_TICKS : 0;
			return true;
		}
		if (hold && left > 0) {
			left--;
			return true;
		}
		left = 0;
		return false;
	}
}
