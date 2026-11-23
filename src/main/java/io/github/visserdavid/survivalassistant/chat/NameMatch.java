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
			boolean startOk = i == 0 || boundary(t.codePointBefore(i), n.codePointAt(0));
			boolean endOk = end == t.length() || boundary(t.codePointAt(end), n.codePointBefore(n.length()));
			if (startOk && endOk) return true;
		}
		return false;
	}

	/**
	 * Is there a word boundary between the name's edge and the character next to it? Chinese, Japanese and Korean
	 * write words without spaces, so next to (or inside) such a script every edge counts ("向导你好", "Pim你好").
	 */
	private static boolean boundary(int neighbour, int nameEdge) {
		return !isWordChar(neighbour) || isCjk(neighbour) || isCjk(nameEdge);
	}

	private static boolean isCjk(int cp) {
		Character.UnicodeScript s = Character.UnicodeScript.of(cp);
		return s == Character.UnicodeScript.HAN || s == Character.UnicodeScript.HIRAGANA
				|| s == Character.UnicodeScript.KATAKANA || s == Character.UnicodeScript.HANGUL;
	}

	private static boolean isWordChar(int cp) {
		return Character.isLetterOrDigit(cp) || cp == '_';
	}
}
