package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import static io.github.visserdavid.survivalassistant.advice.GuideLifecycle.Step.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GuideLifecycleTest {
	private static GuideLifecycle.Step decide(int day, int extra, int extend, boolean done, long asked, boolean reminded, long now) {
		return GuideLifecycle.decide(day, 3, extra, extend, done, asked, reminded, now);
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
	void allGoalsDoneAlwaysCongratulates() {
		assertEquals(CONGRATULATE, decide(0, 0, 3, true, -1, false, 100));
		assertEquals(CONGRATULATE, decide(3, 0, 3, true, 72000, false, 80000), "also while the question is open");
	}
}
