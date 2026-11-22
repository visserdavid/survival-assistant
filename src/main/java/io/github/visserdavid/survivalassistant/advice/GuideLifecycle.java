package io.github.visserdavid.survivalassistant.advice;

/**
 * Decides whether the guide keeps helping, asks to stay longer, says goodbye, or congratulates the player.
 * Pure logic, no Minecraft types.
 */
public final class GuideLifecycle {
	private GuideLifecycle() {}

	/** Without an answer to "do you still need me?", the guide leaves after one full day. */
	public static final long ANSWER_TICKS = 24000;
	/** One reminder halfway through that day. */
	public static final long REMIND_TICKS = 12000;

	public enum Step {
		/** Keep helping as usual. */
		CONTINUE,
		/** All goals reached: congratulate and leave. */
		CONGRATULATE,
		/** The period is over: ask whether the player still needs the guide. */
		ASK,
		/** The question is still open: remind once. */
		REMIND,
		/** The question is open; keep helping while waiting for an answer. */
		WAITING,
		/** Leave (period over without extensions, or no answer in time). */
		FAREWELL
	}

	/**
	 * @param dayIndex    days since the guide arrived (0 = first day)
	 * @param baseDays    configured guide period (maxDays)
	 * @param extraDays   days added by extensions so far
	 * @param extendDays  days one extension adds; 0 or less = never ask, just leave
	 * @param allDone     every goal reached
	 * @param askedAtTick overworld clock tick of the open question, or -1 if none
	 * @param reminded    the reminder for the open question was already given
	 * @param now         current overworld clock tick
	 */
	public static Step decide(int dayIndex, int baseDays, int extraDays, int extendDays, boolean allDone,
			long askedAtTick, boolean reminded, long now) {
		if (allDone) return Step.CONGRATULATE;
		if (dayIndex < baseDays + extraDays) return Step.CONTINUE;
		if (extendDays <= 0) return Step.FAREWELL;
		if (askedAtTick < 0) return Step.ASK;
		long waited = now - askedAtTick;
		if (waited >= ANSWER_TICKS) return Step.FAREWELL;
		if (waited >= REMIND_TICKS && !reminded) return Step.REMIND;
		return Step.WAITING;
	}
}
