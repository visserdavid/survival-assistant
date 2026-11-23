package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlaceNoticesTest {
	@Test
	void eachPlaceIsMentionedOnce() {
		SurvivalContext nether = SurvivalContext.builder().dimension(SurvivalContext.NETHER).build();
		Set<String> seen = new HashSet<>();
		assertEquals("nether", PlaceNotices.next(nether, seen));
		seen.add("nether");
		assertNull(PlaceNotices.next(nether, seen));
	}

	@Test
	void aVillageByDayThenAgainAtNight() {
		Set<String> seen = new HashSet<>();
		SurvivalContext day = SurvivalContext.builder().structure(Structures.VILLAGE_PLAINS).timeOfDay(6000).build();
		assertEquals("village", PlaceNotices.next(day, seen));
		seen.add("village");
		assertNull(PlaceNotices.next(day, seen));
		SurvivalContext night = SurvivalContext.builder().structure(Structures.VILLAGE_PLAINS).timeOfDay(18000).build();
		assertEquals("village_night", PlaceNotices.next(night, seen));
	}

	@Test
	void theFirstWitherSkullAndWrecks() {
		assertEquals("wither", PlaceNotices.next(SurvivalContext.builder().witherSkulls(1).build(), Set.of()));
		assertEquals("wreck", PlaceNotices.next(SurvivalContext.builder().structure(Structures.SHIPWRECK).build(), Set.of()));
		assertNull(PlaceNotices.next(SurvivalContext.builder().build(), Set.of()));
	}
}
