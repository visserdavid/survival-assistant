package io.github.visserdavid.survivalassistant.advice;

import java.util.List;
import java.util.Set;

/**
 * One chapter of the tutorial: a list of goals the guide helps with.
 *
 * @param number   stable number (saved in the player data, shown in the menu); never renumber a chapter
 * @param id       stable name used in commands and language keys ({@code survivalassistant.chapter.<id>.title})
 * @param goals    milestone tips in the order the guide suggests them
 * @param requires numbers of chapters that must be finished first
 * @param limited  true when the chapter has a day limit (only the first days; later chapters last as long as needed)
 */
public record Chapter(int number, String id, List<Tip> goals, List<Integer> requires, boolean limited) {
	/** Every goal is in {@code completed}. */
	public boolean done(Set<String> completed) {
		for (Tip m : goals) if (!completed.contains(m.id())) return false;
		return true;
	}

	/** Every chapter this one needs is finished. */
	public boolean unlocked(Set<String> completed) {
		for (int n : requires) if (!Chapters.get(n).done(completed)) return false;
		return true;
	}

	public String titleKey() {
		return "survivalassistant.chapter." + id + ".title";
	}
}
