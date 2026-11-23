package io.github.visserdavid.survivalassistant.advice;

import io.github.visserdavid.survivalassistant.util.Ticks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideProgressTest {
	@Test
	void meetingStartsTheFirstChapterOnce() {
		GuideProgress p = new GuideProgress();
		assertFalse(p.met());
		p.playedTicks = 100;
		p.meet(5000);
		assertTrue(p.met());
		assertEquals(5000, p.startTick);
		assertEquals(100, p.chapterStartedAt);
		p.playedTicks = 900;
		p.meet(9999); // meeting again changes nothing
		assertEquals(5000, p.startTick);
		assertEquals(100, p.chapterStartedAt);
	}

	@Test
	void daysCountPlayedTimeSinceTheChapterStarted() {
		GuideProgress p = new GuideProgress();
		assertEquals(0, p.daysInChapter(), "not met yet");
		p.meet(0);
		p.playedTicks = Ticks.PER_DAY - 1;
		assertEquals(0, p.daysInChapter());
		p.playedTicks = Ticks.PER_DAY;
		assertEquals(1, p.daysInChapter(), "a frozen daylight cycle doesn't stop played time");
	}

	@Test
	void restartKeepsTheMeetingButStartsTheChapterAgain() {
		GuideProgress p = new GuideProgress();
		p.meet(42);
		p.playedTicks = 3 * Ticks.PER_DAY;
		p.retired = true;
		p.chapterOffered = true;
		p.askedAtTick = 10;
		p.reminded = true;
		p.restart();
		assertTrue(p.met());
		assertEquals(42, p.startTick);
		assertEquals(0, p.daysInChapter());
		assertFalse(p.retired);
		assertFalse(p.chapterOffered);
		assertEquals(-1, p.askedAtTick);
		assertFalse(p.reminded);
	}
}
