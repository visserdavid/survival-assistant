package io.github.visserdavid.survivalassistant.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Tiny symbols drawn pixel by pixel, so the HUD never depends on Unicode glyphs that may be missing
 * from Minecraft's font (those show up as boxes or wrong characters). One pattern pixel = one GUI pixel.
 */
public final class PixelIcons {
	private PixelIcons() {}

	private static final String[] CHECK = {
			"......#",
			".....#.",
			"#...#..",
			".#.#...",
			"..#....",
	};

	private static final String[] PLAY = {
			"#...",
			"##..",
			"###.",
			"####",
			"###.",
			"##..",
			"#...",
	};

	private static final String[] ARROW = {
			"...#...",
			"..###..",
			".#####.",
			"#######",
			"..###..",
			"..###..",
			"..###..",
	};

	public static final int CHECK_W = CHECK[0].length();
	public static final int PLAY_W = PLAY[0].length();
	public static final int PLAY_H = PLAY.length;
	public static final int ARROW_SIZE = ARROW.length;

	/**
	 * An arrow centred on (cx, cy), pointing up when {@code angle} is 0 and turning clockwise for positive angles
	 * (radians). Used to point towards a place in the world relative to where the player is looking.
	 */
	public static void arrow(GuiGraphicsExtractor g, float cx, float cy, float angle, int color) {
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().rotate(angle);
		g.pose().translate(-ARROW_SIZE / 2f, -ARROW_SIZE / 2f);
		draw(g, ARROW, 0, 0, color);
		g.pose().popMatrix();
	}

	/** A check mark, 7x5, top-left at (x, y). */
	public static void check(GuiGraphicsExtractor g, int x, int y, int color) {
		draw(g, CHECK, x, y, color);
	}

	/** A right-pointing triangle, 4x7, top-left at (x, y). */
	public static void play(GuiGraphicsExtractor g, int x, int y, int color) {
		draw(g, PLAY, x, y, color);
	}

	/** A 2x2 dot. */
	public static void dot(GuiGraphicsExtractor g, int x, int y, int color) {
		g.fill(x, y, x + 2, y + 2, color);
	}

	private static void draw(GuiGraphicsExtractor g, String[] pattern, int x, int y, int color) {
		for (int row = 0; row < pattern.length; row++) {
			String line = pattern[row];
			for (int col = 0; col < line.length(); col++) {
				if (line.charAt(col) == '#') g.fill(x + col, y + row, x + col + 1, y + row + 1, color);
			}
		}
	}
}
