package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import static io.github.visserdavid.survivalassistant.advice.GuideLifecycle.Step.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GuideLifecycleTest {
	private static GuideLifecycle.Step decide(boolean done, boolean next, boolean offered, boolean finished, long asked,
			boolean reminded, long now) {
		return GuideLifecycle.decide(new GuideLifecycle.State(done, next, offered, finished, asked, reminded, now, false));
	}

	@Test
	void keepsHelpingAsLongAsTheChapterRuns() {
		assertEquals(CONTINUE, decide(false, true, false, false, -1, false, 0));
		assertEquals(CONTINUE, decide(false, true, false, false, -1, false, 10 * 24000), "no time limit, also not on day 10");
	}

	@Test
	void allGoalsDoneInTheLastChapterCongratulates() {
		assertEquals(CONGRATULATE, decide(true, false, false, false, -1, false, 100));
	}

	@Test
	void allGoalsDoneWithAnotherChapterOffersIt() {
		assertEquals(OFFER_CHAPTER, decide(true, true, false, false, -1, false, 1000));
		assertEquals(WAITING, decide(true, true, true, false, 1000, false, 2000));
	}

	@Test
	void remindsOnceThenSaysGoodbyeWithoutAnAnswer() {
		long asked = 1000;
		assertEquals(REMIND, decide(true, true, true, false, asked, false, asked + GuideLifecycle.REMIND_TICKS));
		assertEquals(WAITING, decide(true, true, true, false, asked, true, asked + GuideLifecycle.REMIND_TICKS + 20));
		assertEquals(FAREWELL, decide(true, true, true, false, asked, true, asked + GuideLifecycle.ANSWER_TICKS));
	}

	@Test
	void afterTheFinalCongratulationTheGuideJustHelps() {
		assertEquals(CONTINUE, decide(true, false, false, true, -1, false, 1000), "called back after finishing: no second party");
	}

	@Test
	void afterChapterOneTheNextChapterStartsByItself() {
		long asked = 1000;
		GuideLifecycle.State waiting = new GuideLifecycle.State(true, true, true, false, asked, false,
				asked + GuideLifecycle.AUTO_START_TICKS - 20, true);
		assertEquals(WAITING, GuideLifecycle.decide(waiting));
		GuideLifecycle.State late = new GuideLifecycle.State(true, true, true, false, asked, false,
				asked + GuideLifecycle.AUTO_START_TICKS, true);
		assertEquals(START_NEXT, GuideLifecycle.decide(late));
		assertEquals(WAITING, decide(true, true, true, false, asked, false, asked + GuideLifecycle.AUTO_START_TICKS),
				"other chapters wait for the player");
	}

	@Test
	void theCountdownShowsTheSecondsLeft() {
		long asked = 1000;
		long total = GuideLifecycle.AUTO_START_SECONDS;
		assertEquals(total - 1, GuideLifecycle.autoStartSecondsLeft(
				new GuideLifecycle.State(true, true, true, false, asked, false, asked + 20, true)));
		assertEquals(1, GuideLifecycle.autoStartSecondsLeft(
				new GuideLifecycle.State(true, true, true, false, asked, false, asked + GuideLifecycle.AUTO_START_TICKS - 20, true)));
		assertEquals(-1, GuideLifecycle.autoStartSecondsLeft(
				new GuideLifecycle.State(true, true, true, false, asked, false, asked + GuideLifecycle.AUTO_START_TICKS, true)),
				"time is up: the chapter starts, no countdown");
		assertEquals(-1, GuideLifecycle.autoStartSecondsLeft(
				new GuideLifecycle.State(true, true, true, false, asked, false, asked + 20, false)), "other chapters don't count down");
	}
}
