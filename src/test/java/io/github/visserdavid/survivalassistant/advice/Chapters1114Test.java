package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Chapters 11-14: netherite, the Wither, the Deep Dark and raids. */
class Chapters1114Test {
	private static SurvivalContext.Builder b() {
		return SurvivalContext.builder();
	}

	private static void completes(Tip goal, SurvivalContext ctx) {
		assertTrue(goal.done().test(ctx), goal.id());
		assertFalse(goal.done().test(SurvivalContext.builder().build()), goal.id() + " is not done in an empty context");
	}

	@Test
	void netheriteGoals() {
		completes(TipRegistry.ANCIENT_DEBRIS, b().advancement(Advancements.DEBRIS).build());
		completes(TipRegistry.NETHERITE_INGOT, b().hasNetheriteIngot(true).build());
		completes(TipRegistry.FIND_BASTION, b().advancement(Advancements.BASTION).build());
		completes(TipRegistry.UPGRADE_TEMPLATE, b().hasUpgradeTemplate(true).build());
		completes(TipRegistry.NETHERITE_GEAR, b().hasNetheriteGear(true).build());
		completes(TipRegistry.RESPAWN_ANCHOR, b().advancement(Advancements.ANCHOR).build());
		// using the ingot and template on gear keeps those goals done
		assertTrue(TipRegistry.NETHERITE_INGOT.done().test(b().hasNetheriteGear(true).build()));
		assertTrue(TipRegistry.UPGRADE_TEMPLATE.done().test(b().hasNetheriteGear(true).build()));
	}

	@Test
	void witherGoals() {
		completes(TipRegistry.WITHER_SKULLS, b().witherSkulls(3).build());
		assertFalse(TipRegistry.WITHER_SKULLS.done().test(b().witherSkulls(2).build()));
		completes(TipRegistry.SOUL_SAND, b().soulSand(4).build());
		completes(TipRegistry.SUMMON_WITHER, b().advancement(Advancements.SUMMON_WITHER).build());
		completes(TipRegistry.NETHER_STAR, b().hasNetherStar(true).build());
		completes(TipRegistry.BEACON, b().advancement(Advancements.BEACON).build());
		completes(TipRegistry.FULL_BEACON, b().advancement(Advancements.FULL_BEACON).build());
		// skulls and sand are used up by summoning: still done afterwards
		assertTrue(TipRegistry.WITHER_SKULLS.done().test(b().advancement(Advancements.SUMMON_WITHER).build()));
		assertTrue(TipRegistry.SOUL_SAND.done().test(b().advancement(Advancements.SUMMON_WITHER).build()));
	}

	@Test
	void deepDarkGoals() {
		completes(TipRegistry.FIND_ANCIENT_CITY, b().structure("ancient_city").build());
		completes(TipRegistry.SNEAK_PAST, b().advancement(Advancements.SNEAK).build());
		completes(TipRegistry.ECHO_SHARDS, b().echoShards(8).build());
		completes(TipRegistry.RECOVERY_COMPASS, b().hasRecoveryCompass(true).build());
		assertTrue(TipRegistry.ECHO_SHARDS.done().test(b().hasRecoveryCompass(true).build()), "shards crafted into the compass");
	}

	@Test
	void raidGoals() {
		completes(TipRegistry.FIND_OUTPOST, b().structure("pillager_outpost").build());
		completes(TipRegistry.RAID_CAPTAIN, b().advancement(Advancements.RAID_CAPTAIN).build());
		completes(TipRegistry.OMINOUS_BOTTLE, b().hasOminousBottle(true).build());
		completes(TipRegistry.WIN_RAID, b().advancement(Advancements.RAID).build());
		completes(TipRegistry.TOTEM, b().hasTotem(true).build());
		completes(TipRegistry.TOTEM, b().advancement(Advancements.TOTEM).build());
	}

	@Test
	void wardenIsTheMostUrgent() {
		assertEquals(TipRegistry.WARDEN, AdviceEngine.activeUrgent(b().warden(true).creeper(true).health(4).hostiles(3).build()));
	}

	@Test
	void notesComeOnce() {
		assertEquals("bastion", PlaceNotices.next(b().dimension(SurvivalContext.NETHER).structure("bastion_remnant").build(),
				Set.of("nether")));
		assertEquals("ancient_city", PlaceNotices.next(b().structure("ancient_city").build(), Set.of()));
		assertEquals("outpost", PlaceNotices.next(b().structure("pillager_outpost").build(), Set.of()));
		assertEquals("wither", PlaceNotices.next(b().witherSkulls(1).build(), Set.of()));
		assertNull(PlaceNotices.next(b().witherSkulls(1).build(), Set.of("wither")));
	}

	@Test
	void prerequisites() {
		Set<String> base = new HashSet<>();
		for (Chapter c : java.util.List.of(Chapters.FIRST_DAYS, Chapters.SETTLING_IN)) c.goals().forEach(t -> base.add(t.id()));
		assertEquals(Chapters.Status.LOCKED, Chapters.status(Chapters.NETHERITE, 2, base));
		assertEquals(Chapters.Status.LOCKED, Chapters.status(Chapters.RAIDS, 2, base));
		Chapters.NETHER.goals().forEach(t -> base.add(t.id()));
		Chapters.VILLAGE.goals().forEach(t -> base.add(t.id()));
		assertEquals(Chapters.Status.AVAILABLE, Chapters.status(Chapters.NETHERITE, 2, base));
		assertEquals(Chapters.Status.AVAILABLE, Chapters.status(Chapters.RAIDS, 2, base));
		assertEquals(Chapters.Status.LOCKED, Chapters.status(Chapters.WITHER, 2, base), "also needs brewing");
		assertEquals(Chapters.Status.LOCKED, Chapters.status(Chapters.DEEP_DARK, 2, base), "needs exploring");
	}
}
