package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import static io.github.visserdavid.survivalassistant.advice.GuideLifecycle.Step.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GuideLifecycleTest {
	/** Chapter 1 with a chapter 2 available unless stated otherwise. */
	private static GuideLifecycle.Step decide(int day, int extra, int extend, boolean done, long asked, boolean reminded, long now) {
		return GuideLifecycle.decide(new GuideLifecycle.State(day, 3, extra, extend, done, false, false, false, asked, reminded, now));
	}

	private static GuideLifecycle.Step chapter(boolean done, boolean next, boolean offered, boolean finished, long asked, boolean reminded, long now) {
		return GuideLifecycle.decide(new GuideLifecycle.State(1, 3, 0, 3, done, next, offered, finished, asked, reminded, now));
	}

	@Test
	void keepsHelpingWithinThePeriod() {
		assertEquals(CONTINUE, decide(0, 0, 3, false, -1, false, 0));
		assertEquals(CONTINUE, decide(2, 0, 3, false, -1, false, 0));
	}

	@Test
	void asksWhenThePeriodEnds() {
		assertEquals(ASK, decide(3, 0, 3, false, -1, false, 72000));
	}

	@Test
	void remindsOnceThenSaysGoodbyeWithoutAnswer() {
		long asked = 72000;
		assertEquals(WAITING, decide(3, 0, 3, false, asked, false, asked + 100));
		assertEquals(REMIND, decide(3, 0, 3, false, asked, false, asked + GuideLifecycle.REMIND_TICKS));
		assertEquals(WAITING, decide(3, 0, 3, false, asked, true, asked + GuideLifecycle.REMIND_TICKS + 20));
		assertEquals(FAREWELL, decide(4, 0, 3, false, asked, true, asked + GuideLifecycle.ANSWER_TICKS));
	}

	@Test
	void extensionAddsDays() {
		assertEquals(CONTINUE, decide(3, 3, 3, false, -1, false, 72000));
		assertEquals(CONTINUE, decide(5, 3, 3, false, -1, false, 130000));
		assertEquals(ASK, decide(6, 3, 3, false, -1, false, 144000));
	}

	@Test
	void noExtensionsConfiguredMeansOldBehaviour() {
		assertEquals(FAREWELL, decide(3, 0, 0, false, -1, false, 72000));
	}

	@Test
	void allGoalsDoneInTheLastChapterCongratulates() {
		assertEquals(CONGRATULATE, decide(0, 0, 3, true, -1, false, 100));
		assertEquals(CONGRATULATE, decide(3, 0, 3, true, 72000, false, 80000), "also while the extend question is open");
	}

	@Test
	void allGoalsDoneWithAnotherChapterOffersIt() {
		assertEquals(OFFER_CHAPTER, chapter(true, true, false, false, -1, false, 1000));
		assertEquals(WAITING, chapter(true, true, true, false, 1000, false, 2000));
		assertEquals(REMIND, chapter(true, true, true, false, 1000, false, 1000 + GuideLifecycle.REMIND_TICKS));
		assertEquals(FAREWELL, chapter(true, true, true, false, 1000, true, 1000 + GuideLifecycle.ANSWER_TICKS));
	}

	@Test
	void afterTheFinalCongratulationTheGuideJustHelps() {
		assertEquals(CONTINUE, chapter(true, false, false, true, -1, false, 1000), "called back after finishing: no second party");
	}
}
