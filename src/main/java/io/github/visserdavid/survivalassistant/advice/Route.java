package io.github.visserdavid.survivalassistant.advice;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A specialization: an order over the existing chapters that the guide recommends after chapter 2. A route adds no
 * goals; it only decides which open chapter the guide suggests next. Pure logic, no Minecraft types.
 *
 * @param id       stable name, saved with the player and used in commands and language keys
 *                 ({@code survivalassistant.route.<id>.name}); never rename it
 * @param chapters chapter numbers in the recommended order; every chapter's prerequisites come earlier in the list
 *                 (or are chapter 1 or 2)
 */
public record Route(String id, List<Integer> chapters) {
	public Route {
		chapters = List.copyOf(chapters);
	}

	/** Every chapter of the route is finished. */
	public boolean done(Set<String> completed) {
		for (int n : chapters) if (!Chapters.get(n).done(completed)) return false;
		return true;
	}

	/** The first chapter of the route that isn't finished and can be started (or is the current one). */
	public Optional<Chapter> next(Set<String> completed) {
		for (int n : chapters) {
			Chapter c = Chapters.get(n);
			if (!c.done(completed) && c.unlocked(completed)) return Optional.of(c);
		}
		return Optional.empty();
	}

	/** How many chapters of the route are finished. */
	public int doneCount(Set<String> completed) {
		int n = 0;
		for (int c : chapters) if (Chapters.get(c).done(completed)) n++;
		return n;
	}

	public String nameKey() {
		return "survivalassistant.route." + id + ".name";
	}
}
