package io.github.visserdavid.survivalassistant.advice;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The tutorial: chapters 1 and 2 are the base, after that the player picks a themed chapter from a menu.
 * Pure logic, no Minecraft types.
 */
public final class Chapters {
	private Chapters() {}

	public static final Chapter FIRST_DAYS = new Chapter(1, "first_days", TipRegistry.CHAPTER_FIRST_DAYS, List.of(), true);
	public static final Chapter SETTLING_IN = new Chapter(2, "settling_in", TipRegistry.CHAPTER_SETTLING_IN, List.of(1), false);
	public static final Chapter VILLAGE = new Chapter(3, "village", TipRegistry.CHAPTER_VILLAGE, List.of(2), false);
	public static final Chapter NETHER = new Chapter(4, "nether", TipRegistry.CHAPTER_NETHER, List.of(2), false);
	public static final Chapter MAGIC = new Chapter(5, "magic", TipRegistry.CHAPTER_MAGIC, List.of(4), false);
	public static final Chapter HUSBANDRY = new Chapter(6, "husbandry", TipRegistry.CHAPTER_HUSBANDRY, List.of(2), false);
	public static final Chapter OCEAN = new Chapter(7, "ocean", TipRegistry.CHAPTER_OCEAN, List.of(2), false);
	public static final Chapter EXPLORER = new Chapter(8, "explorer", TipRegistry.CHAPTER_EXPLORER, List.of(2), false);
	public static final Chapter REDSTONE = new Chapter(9, "redstone", TipRegistry.CHAPTER_REDSTONE, List.of(2), false);
	public static final Chapter END = new Chapter(10, "end", TipRegistry.CHAPTER_END, List.of(4, 5), false);
	public static final Chapter NETHERITE = new Chapter(11, "netherite", TipRegistry.CHAPTER_NETHERITE, List.of(4), false);
	public static final Chapter WITHER = new Chapter(12, "wither", TipRegistry.CHAPTER_WITHER, List.of(4, 5), false);
	public static final Chapter DEEP_DARK = new Chapter(13, "deep_dark", TipRegistry.CHAPTER_DEEP_DARK, List.of(8), false);
	public static final Chapter RAIDS = new Chapter(14, "raids", TipRegistry.CHAPTER_RAIDS, List.of(3), false);
	public static final Chapter ARCHAEOLOGY = new Chapter(15, "archaeology", TipRegistry.CHAPTER_ARCHAEOLOGY, List.of(8), false);
	public static final Chapter TRIALS = new Chapter(16, "trials", TipRegistry.CHAPTER_TRIALS, List.of(8), false);
	public static final Chapter HAPPY_GHAST = new Chapter(17, "happy_ghast", TipRegistry.CHAPTER_HAPPY_GHAST, List.of(4), false);
	public static final Chapter AUTOMATION = new Chapter(18, "automation", TipRegistry.CHAPTER_AUTOMATION, List.of(9, 4), false);

	/** In menu order; the list index is number - 1. */
	public static final List<Chapter> ALL = List.of(FIRST_DAYS, SETTLING_IN, VILLAGE, NETHER, MAGIC, HUSBANDRY, OCEAN, EXPLORER,
			REDSTONE, END, NETHERITE, WITHER, DEEP_DARK, RAIDS, ARCHAEOLOGY, TRIALS, HAPPY_GHAST, AUTOMATION);

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

	/** Vanilla advancement ids (without namespace) the goals check. */
	public static Set<String> trackedAdvancements() {
		return Advancements.ALL;
	}
}
