package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Chapters 6-8: every goal completes in a fitting situation; structures give their one-time notes. */
class Chapters68Test {
	private static SurvivalContext.Builder b() {
		return SurvivalContext.builder();
	}

	private static void completes(Tip goal, SurvivalContext ctx) {
		assertTrue(goal.done().test(ctx), goal.id());
		assertFalse(goal.done().test(SurvivalContext.builder().build()), goal.id() + " is not done in an empty context");
	}

	@Test
	void farmingGoals() {
		completes(TipRegistry.TAME_ANIMAL, b().advancement(TipRegistry.ADV_TAME).build());
		completes(TipRegistry.MILK, b().milk(true).build());
		completes(TipRegistry.SUGAR_CANE, b().sugarCane(16).build());
		assertFalse(TipRegistry.SUGAR_CANE.done().test(b().sugarCane(15).build()));
		completes(TipRegistry.FISHING, b().advancement(TipRegistry.ADV_FISH).build());
		completes(TipRegistry.HONEY, b().advancement(TipRegistry.ADV_HONEY).build());
		completes(TipRegistry.HORSE, b().ridingHorse(true).build());
	}

	@Test
	void oceanGoals() {
		completes(TipRegistry.BOAT, b().boat(true).build());
		completes(TipRegistry.FIND_WRECK, b().structure("shipwreck_beached").build());
		completes(TipRegistry.FIND_WRECK, b().structure("ocean_ruin_warm").build());
		assertFalse(TipRegistry.FIND_WRECK.done().test(b().structure("village_plains").build()));
		completes(TipRegistry.TREASURE, b().heartOfTheSea(true).build());
		completes(TipRegistry.TURTLE_HELMET, b().turtleHelmet(true).build());
		completes(TipRegistry.TRIDENT, b().trident(true).build());
		completes(TipRegistry.TRIDENT, b().advancement(TipRegistry.ADV_TRIDENT).build());
	}

	@Test
	void explorerGoals() {
		completes(TipRegistry.MAP, b().filledMap(true).build());
		completes(TipRegistry.SPYGLASS, b().spyglass(true).build());
		completes(TipRegistry.FIND_TEMPLE, b().structure("desert_pyramid").build());
		completes(TipRegistry.FIND_TEMPLE, b().structure("jungle_pyramid").build());
		completes(TipRegistry.TRIAL_CHAMBERS, b().structure("trial_chambers").build());
		completes(TipRegistry.TRIAL_CHAMBERS, b().advancement(TipRegistry.ADV_TRIAL_CHAMBERS).build());
		completes(TipRegistry.OPEN_VAULT, b().advancement(TipRegistry.ADV_VAULT).build());
	}

	@Test
	void structureNotesComeOnce() {
		assertEquals("desert_temple", PlaceNotices.next(b().structure("desert_pyramid").build(), Set.of()));
		assertEquals("jungle_temple", PlaceNotices.next(b().structure("jungle_pyramid").build(), Set.of()));
		assertEquals("trial_chambers", PlaceNotices.next(b().structure("trial_chambers").build(), Set.of()));
		assertEquals("wreck", PlaceNotices.next(b().structure("shipwreck").build(), Set.of()));
		assertNull(PlaceNotices.next(b().structure("shipwreck").build(), Set.of("wreck")));
		assertEquals("village", PlaceNotices.next(b().structure("village_taiga").build(), Set.of()), "villages via the structure too");
	}

	@Test
	void everyKnownStructureExistsIn263() {
		// ids from data/minecraft/worldgen/structure in the 26.3 jar
		assertTrue(SurvivalContext.STRUCTURES.containsAll(Set.of("shipwreck", "shipwreck_beached", "ocean_ruin_cold",
				"ocean_ruin_warm", "desert_pyramid", "jungle_pyramid", "trial_chambers", "village_plains")));
	}
}
