package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LightScanTest {
	private static final int OVERWORLD = 0, NETHER = 15;

	@Test
	void darkFloorWithRoomSpawns() {
		assertTrue(LightScan.monsterCanSpawn(0, OVERWORLD, true, true, true));
	}

	@Test
	void anyBlockLightStopsOverworldSpawns() {
		assertFalse(LightScan.monsterCanSpawn(1, OVERWORLD, true, true, true), "one torch level is enough");
	}

	@Test
	void needsFloorAndTwoFreeBlocks() {
		assertFalse(LightScan.monsterCanSpawn(0, OVERWORLD, false, true, true));
		assertFalse(LightScan.monsterCanSpawn(0, OVERWORLD, true, false, true));
		assertFalse(LightScan.monsterCanSpawn(0, OVERWORLD, true, true, false));
	}

	@Test
	void lightDoesNotMatterInTheNether() {
		assertTrue(LightScan.lightMatters(OVERWORLD));
		assertFalse(LightScan.lightMatters(NETHER));
	}
}
