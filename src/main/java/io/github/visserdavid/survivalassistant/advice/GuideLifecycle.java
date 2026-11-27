package io.github.visserdavid.survivalassistant.advice;

import io.github.visserdavid.survivalassistant.util.Ticks;

/**
 * Decides whether the guide keeps helping, offers the next chapter, says goodbye, or congratulates the player.
 * There is no time limit: every chapter lasts as long as the player needs. Pure logic, no Minecraft types.
 */
public final class GuideLifecycle {
	private GuideLifecycle() {}

	/** Without an answer to the chapter menu, the guide leaves after one full in-game day of played time (20 min). */
	public static final long ANSWER_TICKS = Ticks.PER_DAY;
	/** One reminder halfway through that day. */
	public static final long REMIND_TICKS = Ticks.PER_DAY / 2;
	/** After chapter 1 the guide starts chapter 2 by itself when the player doesn't choose within this time. */
	public static final long AUTO_START_SECONDS = 15;
	public static final long AUTO_START_TICKS = Ticks.seconds(AUTO_START_SECONDS);

	public enum Step {
		/** Keep helping as usual. */
		CONTINUE,
		/** All goals of the last chapter reached: congratulate and leave. */
		CONGRATULATE,
		/** All goals of this chapter reached and another chapter exists: congratulate and offer it. */
		OFFER_CHAPTER,
		/** The chapter menu is still open: remind once. */
		REMIND,
		/** The chapter menu after chapter 1 got no answer in time: start the next chapter (chapter 2 is part of the basics). */
		START_NEXT,
		/** The chapter menu is open; keep helping while waiting for an answer. */
		WAITING,
		/** No answer to the chapter menu in time: leave. */
		FAREWELL
	}

	/**
	 * Everything the decision needs.
	 *
	 * @param allDone        every goal of the current chapter reached
	 * @param nextChapter    another chapter is available after this one
	 * @param chapterOffered the next chapter was already offered (question open)
	 * @param finished       the final congratulation already happened (guide was called back afterwards)
	 * @param askedAtTick    played ticks when the chapter menu was offered, or -1 if none
	 * @param reminded       the reminder for the open question was already given
	 * @param now            played ticks now (see {@link GuideProgress#playedTicks})
	 * @param autoStart      the next chapter starts by itself without an answer (after chapter 1)
	 */
	public record State(boolean allDone, boolean nextChapter, boolean chapterOffered, boolean finished, long askedAtTick,
			boolean reminded, long now, boolean autoStart) {}

	/**
	 * Whole seconds left before the next chapter starts by itself (for the countdown), or -1 when nothing counts down.
	 * Rounded up, so the countdown never shows 0 while waiting.
	 */
	public static long autoStartSecondsLeft(State s) {
		if (decide(s) != Step.WAITING || !s.autoStart()) return -1;
		long left = AUTO_START_TICKS - (s.now() - s.askedAtTick());
		return (left + Ticks.PER_SECOND - 1) / Ticks.PER_SECOND;
	}

	public static Step decide(State s) {
		if (!s.allDone() || s.finished()) return Step.CONTINUE;
		if (!s.nextChapter()) return Step.CONGRATULATE;
		if (!s.chapterOffered()) return Step.OFFER_CHAPTER;
		long waited = s.now() - s.askedAtTick();
		if (s.askedAtTick() < 0 || waited >= ANSWER_TICKS) return Step.FAREWELL;
		if (s.autoStart() && waited >= AUTO_START_TICKS) return Step.START_NEXT;
		if (waited >= REMIND_TICKS && !s.reminded()) return Step.REMIND;
		return Step.WAITING;
	}
}
