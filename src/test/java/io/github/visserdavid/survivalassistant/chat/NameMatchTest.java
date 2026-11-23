package io.github.visserdavid.survivalassistant.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameMatchTest {
	@Test
	void wholeWordsOnly() {
		assertTrue(NameMatch.mentions("Pim, look at this", "Pim"));
		assertTrue(NameMatch.mentions("hey pim!", "Pim"));
		assertTrue(NameMatch.mentions("pim", "Pim"));
		assertTrue(NameMatch.mentions("where is my guide?", "guide"));
		assertFalse(NameMatch.mentions("pimple", "Pim"));
		assertFalse(NameMatch.mentions("read the guidelines", "guide"));
		assertFalse(NameMatch.mentions("that is also fine", "Al"));
	}

	@Test
	void namesWithSpacesAndOtherScripts() {
		assertTrue(NameMatch.mentions("Hello Old Tom, how are you", "Old Tom"));
		assertTrue(NameMatch.mentions("Привет, Гид!", "гид"));
		assertFalse(NameMatch.mentions("Гидра", "гид"));
	}

	@Test
	void secondOccurrenceCounts() {
		assertTrue(NameMatch.mentions("pimple? no, Pim", "Pim"));
	}

	@Test
	void emptyNameNeverMatches() {
		assertFalse(NameMatch.mentions("anything", "  "));
	}
}
