package io.github.visserdavid.survivalassistant.advice;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The tutorial: chapters 1 and 2 are the base, after that the player picks a themed chapter from a menu.
 * Pure logic, no Minecraft types.
 */
public final class Chapters {
	private Chapters() {}

	public static final Chapter FIRST_DAYS = new Chapter(1, "first_days", TipRegistry.MILESTONES, List.of(), true);
	public static final Chapter SETTLING_IN = new Chapter(2, "settling_in", TipRegistry.CHAPTER_2, List.of(1), false);
	public static final Chapter VILLAGE = new Chapter(3, "village", TipRegistry.CHAPTER_VILLAGE, List.of(2), false);
	public static final Chapter NETHER = new Chapter(4, "nether", TipRegistry.CHAPTER_NETHER, List.of(2), false);
	public static final Chapter MAGIC = new Chapter(5, "magic", TipRegistry.CHAPTER_MAGIC, List.of(4), false);
	public static final Chapter HUSBANDRY = new Chapter(6, "husbandry", TipRegistry.CHAPTER_HUSBANDRY, List.of(2), false);
	public static final Chapter OCEAN = new Chapter(7, "ocean", TipRegistry.CHAPTER_OCEAN, List.of(2), false);
	public static final Chapter EXPLORER = new Chapter(8, "explorer", TipRegistry.CHAPTER_EXPLORER, List.of(2), false);

	/** In menu order; the list index is number - 1. */
	public static final List<Chapter> ALL = List.of(FIRST_DAYS, SETTLING_IN, VILLAGE, NETHER, MAGIC, HUSBANDRY, OCEAN, EXPLORER);

	/** The chapter with this number; unknown numbers (an old or newer save) fall back to the nearest one. */
	public static Chapter get(int number) {
		return ALL.get(Math.max(1, Math.min(ALL.size(), number)) - 1);
	}

	public static @Nullable Chapter byId(String id) {
		for (Chapter c : ALL) if (c.id().equals(id)) return c;
		return null;
	}

	/** Status of a chapter for one player, as shown in the menu. */
	public enum Status { DONE, CURRENT, AVAILABLE, LOCKED }

	public static Status status(Chapter c, int current, Set<String> completed) {
		if (c.done(completed)) return Status.DONE;
		if (c.number() == current) return Status.CURRENT;
		return c.unlocked(completed) ? Status.AVAILABLE : Status.LOCKED;
	}

	/** Chapters the player can start now (unlocked, not finished, not the current one). */
	public static List<Chapter> available(int current, Set<String> completed) {
		List<Chapter> out = new ArrayList<>();
		for (Chapter c : ALL) if (status(c, current, completed) == Status.AVAILABLE) out.add(c);
		return out;
	}

	/** Vanilla advancement ids (without namespace) the goals and warnings check. */
	public static Set<String> trackedAdvancements() {
		return TRACKED;
	}

	private static final Set<String> TRACKED = new LinkedHashSet<>(List.of(
			TipRegistry.ADV_TRADE, TipRegistry.ADV_IRON_GOLEM, TipRegistry.ADV_ENTER_NETHER, TipRegistry.ADV_FORTRESS,
			TipRegistry.ADV_BREW, TipRegistry.ADV_ENCHANT, TipRegistry.ADV_TAME, TipRegistry.ADV_FISH, TipRegistry.ADV_HONEY,
			TipRegistry.ADV_TRIDENT, TipRegistry.ADV_TRIAL_CHAMBERS, TipRegistry.ADV_VAULT));
}
