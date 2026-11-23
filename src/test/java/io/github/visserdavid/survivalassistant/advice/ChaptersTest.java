package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ChaptersTest {
	private static Set<String> done(Chapter... chapters) {
		Set<String> s = new HashSet<>();
		for (Chapter c : chapters) c.goals().forEach(t -> s.add(t.id()));
		return s;
	}

	@Test
	void numbersMatchThePositionAndNeverChange() {
		for (int i = 0; i < Chapters.ALL.size(); i++) assertEquals(i + 1, Chapters.ALL.get(i).number());
		// saved player data stores these numbers: 1 and 2 must stay the base chapters
		assertEquals("first_days", Chapters.get(1).id());
		assertEquals("settling_in", Chapters.get(2).id());
		assertEquals(Chapters.ALL.get(Chapters.ALL.size() - 1), Chapters.get(99), "unknown numbers fall back to the nearest chapter");
	}

	@Test
	void goalIdsAreUniqueAcrossChapters() {
		Set<String> ids = new HashSet<>();
		for (Chapter c : Chapters.ALL) c.goals().forEach(t -> assertTrue(ids.add(t.id()), t.id()));
		for (Tip u : TipRegistry.URGENT) assertFalse(ids.contains(u.id()), u.id());
	}

	@Test
	void prerequisites() {
		// fresh player in chapter 1: nothing else is open
		assertEquals(List.of(), Chapters.available(1, Set.of()));
		// chapter 1 done: only chapter 2
		assertEquals(List.of(Chapters.SETTLING_IN), Chapters.available(1, done(Chapters.FIRST_DAYS)));
		// base done: villages and the Nether, brewing waits for the Nether
		Set<String> base = done(Chapters.FIRST_DAYS, Chapters.SETTLING_IN);
		assertEquals(List.of(Chapters.VILLAGE, Chapters.NETHER, Chapters.HUSBANDRY, Chapters.OCEAN, Chapters.EXPLORER,
				Chapters.REDSTONE), Chapters.available(2, base));
		assertEquals(Chapters.Status.LOCKED, Chapters.status(Chapters.MAGIC, 2, base));
		Set<String> nether = done(Chapters.FIRST_DAYS, Chapters.SETTLING_IN, Chapters.NETHER);
		assertEquals(Chapters.Status.AVAILABLE, Chapters.status(Chapters.MAGIC, 4, nether));
		// the End needs both the Nether and brewing
		assertEquals(Chapters.Status.LOCKED, Chapters.status(Chapters.END, 4, nether));
		assertEquals(Chapters.Status.AVAILABLE, Chapters.status(Chapters.END, 5,
				done(Chapters.FIRST_DAYS, Chapters.SETTLING_IN, Chapters.NETHER, Chapters.MAGIC)));
	}

	@Test
	void statusShowsDoneCurrentAvailableLocked() {
		Set<String> base = done(Chapters.FIRST_DAYS, Chapters.SETTLING_IN);
		assertEquals(Chapters.Status.DONE, Chapters.status(Chapters.FIRST_DAYS, 3, base));
		assertEquals(Chapters.Status.CURRENT, Chapters.status(Chapters.VILLAGE, 3, base));
		assertEquals(Chapters.Status.AVAILABLE, Chapters.status(Chapters.NETHER, 3, base));
		assertEquals(Chapters.Status.LOCKED, Chapters.status(Chapters.MAGIC, 3, base));
	}

	@Test
	void everyTrackedAdvancementIsUsed() {
		assertEquals(Set.of("adventure/trade", "adventure/summon_iron_golem", "story/enter_the_nether",
				"nether/find_fortress", "nether/brew_potion", "story/enchant_item", "husbandry/tame_an_animal",
				"husbandry/fishy_business", "husbandry/safely_harvest_honey", "adventure/throw_trident",
				"adventure/minecraft_trials_edition", "adventure/under_lock_and_key", "story/follow_ender_eye",
				"story/enter_the_end", "end/kill_dragon", "end/find_end_city", "end/elytra", "nether/obtain_ancient_debris",
				"nether/find_bastion", "nether/charge_respawn_anchor", "nether/summon_wither",
				"nether/create_beacon", "nether/create_full_beacon", "adventure/avoid_vibration", "adventure/voluntary_exile",
				"adventure/hero_of_the_village", "adventure/totem_of_undying", "adventure/salvage_sherd",
				"adventure/trim_with_any_armor_pattern", "husbandry/obtain_sniffer_egg", "adventure/revaulting",
				"husbandry/place_dried_ghast_in_water", "adventure/crafters_crafting_crafters"), Chapters.trackedAdvancements());
	}

	@Test
	void unlimitedChaptersNeverSendTheGuideAway() {
		GuideLifecycle.State late = new GuideLifecycle.State(false, true, false, false, -1, false, 50 * 24000L);
		assertEquals(GuideLifecycle.Step.CONTINUE, GuideLifecycle.decide(late));
		GuideLifecycle.State doneNoMore = new GuideLifecycle.State(true, false, false, false, -1, false, 50 * 24000L);
		assertEquals(GuideLifecycle.Step.CONGRATULATE, GuideLifecycle.decide(doneNoMore));
		GuideLifecycle.State doneMore = new GuideLifecycle.State(true, true, false, false, -1, false, 50 * 24000L);
		assertEquals(GuideLifecycle.Step.OFFER_CHAPTER, GuideLifecycle.decide(doneMore));
	}
}
