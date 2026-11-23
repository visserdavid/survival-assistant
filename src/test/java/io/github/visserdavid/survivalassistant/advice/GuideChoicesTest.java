package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideChoicesTest {
	@Test
	void serverDefaultsFillTheGaps() {
		GuideChoices c = GuideChoices.of(new GuideProgress(), true, false);
		assertEquals(new GuideChoices(true, "", GuideChoices.MUSINGS_ON, false), c);
		assertNull(c.storedName());
		assertNull(c.storedMusings());
	}

	@Test
	void ownChoicesWin() {
		GuideProgress p = new GuideProgress();
		p.npcEnabled = false;
		p.chatEnabled = true;
		p.musings = GuideChoices.MUSINGS_ALL;
		p.guideName = "Bram";
		GuideChoices c = GuideChoices.of(p, true, false);
		assertEquals(new GuideChoices(false, "Bram", GuideChoices.MUSINGS_ALL, true), c);
		assertEquals("Bram", c.storedName());
		assertEquals(GuideChoices.MUSINGS_ALL, c.storedMusings());
	}

	@Test
	void unknownMusingsAndBlankNamesAreCleanedUp() {
		GuideChoices c = new GuideChoices(true, "  ", "sometimes", false);
		assertEquals("", c.name());
		assertEquals(GuideChoices.MUSINGS_ON, c.musings());
		assertTrue(c.nameValid());
	}

	@Test
	void invalidNamesAreRefused() {
		assertFalse(new GuideChoices(true, "!!!", GuideChoices.MUSINGS_ON, false).nameValid());
		assertFalse(new GuideChoices(true, "a".repeat(GuideNames.MAX_LENGTH + 1), GuideChoices.MUSINGS_ON, false).nameValid());
		assertTrue(new GuideChoices(true, " Pim ", GuideChoices.MUSINGS_ON, false).nameValid());
	}
}
