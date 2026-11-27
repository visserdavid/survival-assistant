package io.github.visserdavid.survivalassistant.advice;

import org.jspecify.annotations.Nullable;

/** The guide's name: a server default that every player can replace with their own choice. Pure logic. */
public final class GuideNames {
	private GuideNames() {}

	public static final String DEFAULT = "Pim";
	public static final int MAX_LENGTH = 16;
	/** What the default name stands for; only told when the guide really is called Pim. */
	public static final String ACRONYM = "Player Introduction Mentor";

	/** True when the guide carries the default name (any case), so its name stands for {@link #ACRONYM}. */
	public static boolean isPim(@Nullable String name) {
		return name != null && name.strip().equalsIgnoreCase(DEFAULT);
	}

	/** Letters (any script), digits, spaces, '-' and '_'; 1 to {@link #MAX_LENGTH} characters, not blank. */
	public static boolean valid(@Nullable String name) {
		if (name == null) return false;
		String s = name.strip();
		if (s.isEmpty() || s.codePointCount(0, s.length()) > MAX_LENGTH) return false;
		return s.codePoints().allMatch(cp -> Character.isLetterOrDigit(cp) || Character.getType(cp) == Character.NON_SPACING_MARK
				|| Character.getType(cp) == Character.COMBINING_SPACING_MARK || cp == ' ' || cp == '-' || cp == '_');
	}

	/** The player's own name for the guide, else the server's, else {@link #DEFAULT}. */
	public static String effective(@Nullable String playerChoice, @Nullable String serverDefault) {
		if (valid(playerChoice)) return playerChoice.strip();
		if (valid(serverDefault)) return serverDefault.strip();
		return DEFAULT;
	}
}
