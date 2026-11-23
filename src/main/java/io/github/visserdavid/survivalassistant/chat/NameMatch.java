package io.github.visserdavid.survivalassistant.chat;

import java.util.Locale;

/** Does a chat line mention a name? Whole words only, ignoring case. Pure logic. */
public final class NameMatch {
	private NameMatch() {}

	/** True when {@code name} appears in {@code text} as a whole word ("Pim, look" yes, "Pimple" no). */
	public static boolean mentions(String text, String name) {
		String n = name.strip().toLowerCase(Locale.ROOT);
		if (n.isEmpty()) return false;
		String t = text.toLowerCase(Locale.ROOT);
		for (int i = t.indexOf(n); i >= 0; i = t.indexOf(n, i + 1)) {
			int end = i + n.length();
			boolean startOk = i == 0 || !isWordChar(t.codePointBefore(i));
			boolean endOk = end == t.length() || !isWordChar(t.codePointAt(end));
			if (startOk && endOk) return true;
		}
		return false;
	}

	private static boolean isWordChar(int cp) {
		return Character.isLetterOrDigit(cp) || cp == '_';
	}
}
