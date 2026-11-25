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
		assertEquals(new GuideChoices(true, "", GuideChoices.MUSINGS_ON, false, "free"), c);
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
		GuideChoices c = GuideChoices.of(p, true, true);
		assertEquals(new GuideChoices(false, "Bram", GuideChoices.MUSINGS_ALL, true, "free"), c);
		assertEquals("Bram", c.storedName());
		assertEquals(GuideChoices.MUSINGS_ALL, c.storedMusings());
	}

	@Test
	void unknownMusingsAndBlankNamesAreCleanedUp() {
		GuideChoices c = new GuideChoices(true, "  ", "sometimes", false, "free");
		assertEquals("", c.name());
		assertEquals(GuideChoices.MUSINGS_ON, c.musings());
		assertTrue(c.nameValid());
	}

	@Test
	void invalidNamesAreRefused() {
		assertFalse(new GuideChoices(true, "!!!", GuideChoices.MUSINGS_ON, false, "free").nameValid());
		assertFalse(new GuideChoices(true, "a".repeat(GuideNames.MAX_LENGTH + 1), GuideChoices.MUSINGS_ON, false, "free").nameValid());
		assertTrue(new GuideChoices(true, " Pim ", GuideChoices.MUSINGS_ON, false, "free").nameValid());
	}

	@Test
	void onlyTheServerCanAllowSmallTalk() {
		GuideProgress p = new GuideProgress();
		assertTrue(p.chatEnabled(true), "allowed by the server: on unless the player switched it off");
		assertFalse(p.chatEnabled(false));
		p.chatEnabled = true;
		assertFalse(p.chatEnabled(false), "a player can't switch it on when the server doesn't allow it");
		p.chatEnabled = false;
		assertFalse(p.chatEnabled(true), "a player can always switch it off");
	}
}
