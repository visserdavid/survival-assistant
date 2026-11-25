package io.github.visserdavid.survivalassistant.skin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Turns a skin file name from the skins folder into a skin name the player can type, and tells whether it uses
 * the slim (Alex) arm model. Pure logic so it can be unit tested.
 */
public final class SkinNames {
	private SkinNames() {}

	/** The built-in guide skin: the Keeper of the Ancients. */
	public static final String DEFAULT = "default";
	/** The built-in traveller that was the default before 0.42. */
	public static final String CLASSIC = "classic";
	/** The built-in skins, in the order they are offered. */
	public static final List<String> BUILT_IN = List.of(DEFAULT, CLASSIC);
	private static final String SLIM_SUFFIX = "_slim";

	/**
	 * @param name    lowercase name made of a-z, 0-9 and _, without ".png"
	 * @param slim    the name ends with "_slim": use the slim arm model
	 */
	public record Skin(String name, boolean slim) {
		/** Texture path for the Identifier, e.g. "skins/elf_slim". */
		public String texturePath() {
			return "skins/" + name;
		}
	}

	/** Parses a file name; empty for non-PNG files, names without usable characters and the built-in names. */
	public static Optional<Skin> parse(String fileName) {
		String lower = fileName.toLowerCase(Locale.ROOT);
		if (!lower.endsWith(".png")) return Optional.empty();
		String base = lower.substring(0, lower.length() - 4);
		String name = base.replaceAll("[^a-z0-9_]+", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
		if (name.isEmpty() || BUILT_IN.contains(name)) return Optional.empty();
		return Optional.of(new Skin(name, name.endsWith(SLIM_SUFFIX)));
	}

	/** Minecraft skins are 64x64 (the old 64x32 layout isn't supported). */
	public static boolean validSize(int width, int height) {
		return width == 64 && height == 64;
	}
}
