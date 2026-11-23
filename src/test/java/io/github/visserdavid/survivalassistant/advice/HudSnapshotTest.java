package io.github.visserdavid.survivalassistant.advice;

import io.github.visserdavid.survivalassistant.config.ModConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HudSnapshotTest {
	private static HudSnapshot snap(SurvivalContext c, GuideProgress p) {
		// The server always evaluates (which records completed milestones) before building the snapshot.
		AdviceEngine.evaluate(c, p, 0, AdviceEngine.Settings.from(new ModConfig()), false);
		return HudSnapshot.of(c, p);
	}

	private static List<String> ids(HudSnapshot s) {
		return s.next().stream().map(HudSnapshot.Entry::id).toList();
	}

	@Test
	void freshStartShowsFirstThreeTasks() {
		HudSnapshot s = snap(SurvivalContext.builder().build(), new GuideProgress());
		assertTrue(s.active());
		assertEquals(List.of("wood", "crafting_table", "wooden_pickaxe"), ids(s));
		assertEquals(new HudSnapshot.Entry("wood", 0, 10), s.next().get(0));
		assertEquals(0, s.next().get(1).target(), "crafting table has no counter");
		assertEquals("", s.urgentId());
		assertEquals(1, s.chapter());
	}

	@Test
	void counterShowsLiveProgress() {
		HudSnapshot s = snap(SurvivalContext.builder().logs(6).build(), new GuideProgress());
		assertEquals(new HudSnapshot.Entry("wood", 6, 10), s.next().get(0));
	}

	@Test
	void counterIsClampedToTarget() {
		GuideProgress p = new GuideProgress();
		p.completed.addAll(List.of("wood", "crafting_table", "wooden_pickaxe", "stone_tools", "furnace"));
		HudSnapshot s = snap(SurvivalContext.builder().pickaxeTier(2).swordTier(2).hasFurnace(true).foodItems(3).build(), p);
		assertEquals(new HudSnapshot.Entry("food", 3, 5), s.next().get(0));
	}

	@Test
	void stoneToolsLeadsToFurnace() {
		HudSnapshot s = snap(SurvivalContext.builder().pickaxeTier(2).swordTier(2).build(), new GuideProgress());
		assertEquals("furnace", ids(s).get(0));
		assertTrue(s.completed().containsAll(List.of("wood", "crafting_table", "wooden_pickaxe", "stone_tools")));
	}

	@Test
	void bedCountsWool() {
		GuideProgress p = new GuideProgress();
		p.completed.addAll(List.of("wood", "crafting_table", "wooden_pickaxe", "stone_tools", "furnace", "food"));
		HudSnapshot s = snap(SurvivalContext.builder().wool(2).build(), p);
		assertEquals(new HudSnapshot.Entry("bed", 2, 3), s.next().get(0));
	}

	@Test
	void lastTasksShowFewerLines() {
		GuideProgress p = new GuideProgress();
		TipRegistry.CHAPTER_FIRST_DAYS.stream().limit(TipRegistry.CHAPTER_FIRST_DAYS.size() - 1).forEach(m -> p.completed.add(m.id()));
		HudSnapshot s = HudSnapshot.of(SurvivalContext.builder().build(), p);
		assertEquals(List.of("water_bucket"), ids(s));
	}

	@Test
	void allDoneMeansNoTasks() {
		GuideProgress p = new GuideProgress();
		TipRegistry.CHAPTER_FIRST_DAYS.forEach(m -> p.completed.add(m.id()));
		assertTrue(HudSnapshot.of(SurvivalContext.builder().build(), p).next().isEmpty());
	}

	@Test
	void urgentIdReflectsSituationRegardlessOfCooldown() {
		GuideProgress p = new GuideProgress();
		SurvivalContext creeper = SurvivalContext.builder().creeper(true).build();
		assertEquals("creeper", snap(creeper, p).urgentId());
		assertEquals("creeper", snap(creeper, p).urgentId(), "still shown while the chat tip is on cooldown");
	}

	@Test
	void duskInsideShelterIsCalm() {
		assertEquals("", snap(SurvivalContext.builder().timeOfDay(11500).sheltered(true).build(), new GuideProgress()).urgentId());
		assertEquals("dusk", snap(SurvivalContext.builder().timeOfDay(11500).build(), new GuideProgress()).urgentId());
	}

	@Test
	void snapshotsWithSameContentAreEqual() {
		SurvivalContext c = SurvivalContext.builder().logs(3).build();
		assertEquals(snap(c, new GuideProgress()), snap(c, new GuideProgress()), "equality drives 'only sync when changed'");
	}

	@Test
	void completedOnlyListsTheCurrentChapter() {
		GuideProgress p = new GuideProgress();
		for (Chapter c : Chapters.ALL) for (Tip t : c.goals()) p.completed.add(t.id());
		for (Chapter c : Chapters.ALL) {
			p.chapter = c.number();
			HudSnapshot s = HudSnapshot.of(SurvivalContext.builder().build(), p);
			assertEquals(c.goals().stream().map(Tip::id).toList(), s.completed(), c.id());
			assertTrue(s.completed().size() <= HudSnapshot.MAX_COMPLETED, "fits on the wire: " + c.id());
		}
	}
}
