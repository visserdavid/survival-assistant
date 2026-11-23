package io.github.visserdavid.survivalassistant.advice;

import io.github.visserdavid.survivalassistant.config.ModConfig;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Chapter2Test {
	private static GuideProgress chapter2() {
		GuideProgress p = new GuideProgress();
		TipRegistry.CHAPTER_FIRST_DAYS.forEach(m -> p.completed.add(m.id()));
		p.chapter = 2;
		return p;
	}

	@Test
	void idsAreUniqueAcrossChapters() {
		Set<String> ids = new HashSet<>();
		TipRegistry.CHAPTER_FIRST_DAYS.forEach(m -> assertTrue(ids.add(m.id()), m.id()));
		TipRegistry.CHAPTER_SETTLING_IN.forEach(m -> assertTrue(ids.add(m.id()), m.id()));
	}

	@Test
	void chapter2StartsWithTheFarm() {
		assertEquals(TipRegistry.FARM, AdviceEngine.nextMilestone(chapter2()));
		HudSnapshot s = HudSnapshot.of(SurvivalContext.builder().crops(2).build(), chapter2());
		assertEquals(2, s.chapter());
		assertEquals("farm", s.next().get(0).id());
		assertEquals(2, s.next().get(0).value());
		assertEquals(6, s.next().get(0).target());
	}

	@Test
	void chapter1PlayersNeverSeeChapter2Goals() {
		GuideProgress p = new GuideProgress();
		SurvivalContext rich = SurvivalContext.builder().crops(10).bread(5).diamonds(5).obsidian(20).build();
		AdviceEngine.evaluate(rich, p, 0, AdviceEngine.Settings.from(new ModConfig()), false);
		assertFalse(p.completed.contains("farm"), "chapter-2 goals are only tracked in chapter 2");
	}

	@Test
	void goalsCompleteInChapter2() {
		GuideProgress p = chapter2();
		SurvivalContext c = SurvivalContext.builder().crops(6).bread(3).animalsBred(2).armorPieces(4).hasBow(true).arrows(16)
				.diamonds(3).pickaxeTier(4).obsidian(10).hasFlintAndSteel(true).hasEnchantingTable(true).build();
		AdviceEngine.evaluate(c, p, 0, AdviceEngine.Settings.from(new ModConfig()), false);
		assertTrue(Chapters.get(2).done(p.completed));
	}

	@Test
	void bowNeedsArrowsToo() {
		GuideProgress p = chapter2();
		AdviceEngine.evaluate(SurvivalContext.builder().hasBow(true).arrows(4).build(), p, 0, AdviceEngine.Settings.from(new ModConfig()), false);
		assertFalse(p.completed.contains("bow"));
	}

	@Test
	void chapterDone() {
		Set<String> done = new HashSet<>();
		assertFalse(Chapters.get(1).done(done));
		TipRegistry.CHAPTER_FIRST_DAYS.forEach(m -> done.add(m.id()));
		assertTrue(Chapters.get(1).done(done));
		assertFalse(Chapters.get(2).done(done));
	}
}
