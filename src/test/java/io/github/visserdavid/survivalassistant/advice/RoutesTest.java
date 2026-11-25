package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutesTest {
	private static Set<String> done(int... chapters) {
		Set<String> completed = new LinkedHashSet<>();
		for (int n : chapters) for (Tip t : Chapters.get(n).goals()) completed.add(t.id());
		return completed;
	}

	@Test
	void everyRouteListsValidChaptersOnceWithTheirPrerequisitesFirst() {
		for (Route r : Routes.ALL) {
			Set<Integer> seen = new HashSet<>(Set.of(1, 2));
			for (int n : r.chapters()) {
				assertTrue(n > 2 && n <= Chapters.ALL.size(), r.id() + ": chapter " + n);
				for (int required : Chapters.get(n).requires()) {
					assertTrue(seen.contains(required), r.id() + ": chapter " + n + " needs " + required + " earlier");
				}
				assertTrue(seen.add(n), r.id() + ": chapter " + n + " twice");
			}
		}
	}

	@Test
	void idsAreUniqueAndTheChoicesStartWithFree() {
		assertEquals(Routes.ALL.size(), Routes.ALL.stream().map(Route::id).distinct().count());
		assertEquals(Routes.FREE, Routes.CHOICES.getFirst());
		assertEquals(Routes.ALL.size() + 1, Routes.CHOICES.size());
		for (Route r : Routes.ALL) assertEquals(r, Routes.byId(r.id()));
		assertNull(Routes.byId(Routes.FREE));
		assertTrue(Routes.valid(Routes.FREE));
		assertFalse(Routes.valid(""));
		assertFalse(Routes.valid("wizard"));
	}

	@Test
	void nextIsTheFirstUnfinishedChapterThatCanStart() {
		Route farmer = Routes.FARMER;
		assertEquals(Optional.of(Chapters.HUSBANDRY), farmer.next(done(1, 2)));
		assertEquals(Optional.of(Chapters.VILLAGE), farmer.next(done(1, 2, 6)));
		// progress made elsewhere counts: the village was already done
		assertEquals(Optional.of(Chapters.NETHER), farmer.next(done(1, 2, 3, 6)));
		assertEquals(2, farmer.doneCount(done(1, 2, 3, 6, 7))); // the ocean is not on the route
		assertFalse(farmer.done(done(1, 2, 3, 6)));
	}

	@Test
	void aFinishedRouteHasNoNextChapter() {
		Set<String> all = done(3, 4, 5, 6, 9, 17, 18, 1, 2);
		assertTrue(Routes.FARMER.done(all));
		assertEquals(Optional.empty(), Routes.FARMER.next(all));
		assertFalse(Routes.FIGHTER.done(all));
	}

	@Test
	void theRecommendationFollowsTheChosenRoute() {
		GuideProgress p = new GuideProgress();
		p.completed.addAll(done(1, 2));
		assertTrue(Routes.undecided(p));
		assertEquals(Optional.empty(), Routes.recommended(p));
		p.route = Routes.FREE;
		assertFalse(Routes.undecided(p));
		assertEquals(Optional.empty(), Routes.recommended(p));
		p.route = Routes.EXPLORER.id();
		assertEquals(Optional.of(Chapters.EXPLORER), Routes.recommended(p));
		// switching keeps every goal: the fighter starts in the Nether
		p.route = Routes.FIGHTER.id();
		assertEquals(Optional.of(Chapters.NETHER), Routes.recommended(p));
	}

	@Test
	void choicesShowNoRouteYetAsFree() {
		GuideProgress p = new GuideProgress();
		assertEquals(Routes.FREE, GuideChoices.of(p, true, true).route());
		p.route = "researcher";
		assertEquals("researcher", GuideChoices.of(p, true, true).route());
		assertEquals(Routes.FREE, new GuideChoices(true, "", GuideChoices.MUSINGS_ON, true, "wizard").route());
	}
}
