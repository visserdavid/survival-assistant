package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuresTest {
	@Test
	void villagesAndWrecks() {
		for (String id : Structures.ALL) {
			if (id.startsWith("village_")) assertTrue(Structures.isVillage(id), id);
		}
		assertTrue(Structures.isWreck(Structures.SHIPWRECK));
		assertTrue(Structures.isWreck(Structures.SHIPWRECK_BEACHED));
		assertTrue(Structures.isWreck(Structures.OCEAN_RUIN_WARM));
		assertFalse(Structures.isWreck(Structures.STRONGHOLD));
		assertFalse(Structures.isVillage(Structures.PILLAGER_OUTPOST));
	}
}
