package io.github.visserdavid.survivalassistant.skin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkinNamesTest {
	@Test
	void slimSkin() {
		SkinNames.Skin s = SkinNames.parse("Elf_Slim.png").orElseThrow();
		assertEquals("elf_slim", s.name());
		assertTrue(s.slim());
		assertEquals("skins/elf_slim", s.texturePath());
	}

	@Test
	void wideSkinAndCleanup() {
		SkinNames.Skin s = SkinNames.parse("My Skin!.PNG").orElseThrow();
		assertEquals("my_skin", s.name());
		assertFalse(s.slim());
	}

	@Test
	void skipsOtherFilesAndReservedNames() {
		assertTrue(SkinNames.parse("README.txt").isEmpty());
		assertTrue(SkinNames.parse("!!!.png").isEmpty());
		assertTrue(SkinNames.parse("Default.png").isEmpty(), "'default' is the built-in skin");
	}

	@Test
	void onlyModernSkinSize() {
		assertTrue(SkinNames.validSize(64, 64));
		assertFalse(SkinNames.validSize(64, 32));
		assertFalse(SkinNames.validSize(128, 128));
	}
}
