package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuideNamesTest {
	@Test
	void validNames() {
		assertTrue(GuideNames.valid("Pim"));
		assertTrue(GuideNames.valid("Oude Bram"));
		assertTrue(GuideNames.valid("Jan-Willem_2"));
		assertTrue(GuideNames.valid("Ёжик"));
		assertTrue(GuideNames.valid("小明"));
		assertTrue(GuideNames.valid("गाइड"), "combining marks of Devanagari");
		assertTrue(GuideNames.valid("ABCDEFGHIJKLMNOP"), "16 characters");
	}

	@Test
	void invalidNames() {
		assertFalse(GuideNames.valid(null));
		assertFalse(GuideNames.valid(""));
		assertFalse(GuideNames.valid("   "));
		assertFalse(GuideNames.valid("ABCDEFGHIJKLMNOPQ"), "17 characters");
		assertFalse(GuideNames.valid("§cRood"), "no formatting codes");
		assertFalse(GuideNames.valid("Pim!"));
		assertFalse(GuideNames.valid("Pim\nBram"));
	}

	@Test
	void playerChoiceBeatsServerDefault() {
		assertEquals("Bas", GuideNames.effective("Bas", "Wim"));
		assertEquals("Wim", GuideNames.effective(null, "Wim"));
		assertEquals("Wim", GuideNames.effective("§bad", "Wim"));
		assertEquals(GuideNames.DEFAULT, GuideNames.effective(null, ""));
		assertEquals("Bas", GuideNames.effective("  Bas ", null), "trimmed");
	}
}
