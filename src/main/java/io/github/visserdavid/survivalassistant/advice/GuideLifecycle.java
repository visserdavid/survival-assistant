package io.github.visserdavid.survivalassistant.advice;

/**
 * Decides whether the guide keeps helping, asks to stay longer, offers the next chapter, says goodbye, or
 * congratulates the player. Pure logic, no Minecraft types.
 */
public final class GuideLifecycle {
	private GuideLifecycle() {}

	/** Without an answer to an open question, the guide leaves after one full day. */
	public static final long ANSWER_TICKS = 24000;
	/** One reminder halfway through that day. */
	public static final long REMIND_TICKS = 12000;

	public enum Step {
		/** Keep helping as usual. */
		CONTINUE,
		/** All goals of the last chapter reached: congratulate and leave. */
		CONGRATULATE,
		/** All goals of this chapter reached and another chapter exists: congratulate and offer it. */
		OFFER_CHAPTER,
		/** The period is over: ask whether the player still needs the guide. */
		ASK,
		/** A question is still open: remind once. */
		REMIND,
		/** A question is open; keep helping while waiting for an answer. */
		WAITING,
		/** Leave (period over without extensions, or no answer in time). */
		FAREWELL
	}

	/**
	 * Everything the decision needs.
	 *
	 * @param dayIndex         days since the guide (or this chapter) started (0 = first day)
	 * @param baseDays         configured period for this chapter; 0 or less = no limit (stays while the chapter runs)
	 * @param extraDays        days added by extensions so far
	 * @param extendDays       days one extension adds; 0 or less = never ask, just leave
	 * @param allDone          every goal of the current chapter reached
	 * @param nextChapter      another chapter is available after this one
	 * @param chapterOffered   the next chapter was already offered (question open)
	 * @param finished         the final congratulation already happened (guide was called back afterwards)
	 * @param askedAtTick      overworld clock tick of the open question, or -1 if none
	 * @param reminded         the reminder for the open question was already given
	 * @param now              current overworld clock tick
	 */
	public record State(int dayIndex, int baseDays, int extraDays, int extendDays, boolean allDone, boolean nextChapter,
			boolean chapterOffered, boolean finished, long askedAtTick, boolean reminded, long now) {}

	public static Step decide(State s) {
		if (s.allDone() && !s.finished()) {
			if (!s.nextChapter()) return Step.CONGRATULATE;
			if (!s.chapterOffered()) return Step.OFFER_CHAPTER;
			return waitForAnswer(s);
		}
		if (s.baseDays() <= 0 || s.dayIndex() < s.baseDays() + s.extraDays()) return Step.CONTINUE;
		if (s.extendDays() <= 0) return Step.FAREWELL;
		if (s.askedAtTick() < 0) return Step.ASK;
		return waitForAnswer(s);
	}

	private static Step waitForAnswer(State s) {
		long waited = s.now() - s.askedAtTick();
		if (s.askedAtTick() < 0 || waited >= ANSWER_TICKS) return Step.FAREWELL;
		if (waited >= REMIND_TICKS && !s.reminded()) return Step.REMIND;
		return Step.WAITING;
	}
}
