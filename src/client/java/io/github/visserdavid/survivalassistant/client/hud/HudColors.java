package io.github.visserdavid.survivalassistant.client.hud;

/** The colour palette of every card (ARGB). */
final class HudColors {
	private HudColors() {}

	/** Card background: dark and slightly see-through. */
	static final int BG = 0xB0101010;
	/** Background of the recipe card next to a container window (more opaque, it sits on a busy screen). */
	static final int BG_SOLID = 0xE0101010;
	/** Border of the task card and the recipe card. */
	static final int BORDER_TASK = 0xFF6B5320;
	/** Border of the environment card. */
	static final int BORDER_ENV = 0xFF3E5A6B;
	/** The thin separator line inside a card. */
	static final int SEPARATOR = 0x40FFFFFF;

	static final int WHITE = 0xFFFFFFFF;
	static final int GOLD = 0xFFFFC94D;
	static final int GRAY = 0xFF9A9A9A;
	static final int DARK_GRAY = 0xFF5A5A5A;
	static final int GREEN = 0xFF6BD96B;
	static final int RED = 0xFFFF5050;
	static final int AQUA = 0xFF6FD3E8;
	static final int ORANGE = 0xFFFFA040;
	/** Salmon: the death spot, the "needs a crafting table" note. */
	static final int SALMON = 0xFFFF9A6B;
	/** Pale red: the blinking alert text between its red frames. */
	static final int PALE_RED = 0xFFFFB0B0;
	/** Dark red: the pulsing border of an urgent card at its dimmest. */
	static final int DARK_RED = 0xFF7A1010;
	/** The glow of the environment card when you enter a new biome. */
	static final int GLOW = 0xFFFFE066;

	static final int BAR_BG = 0xFF303030;
	static final int SLOT_BG = 0xFF2A2A2A;
	static final int SLOT_EDGE = 0xFF555555;

	/** The clock: day, dusk and night segments. */
	static final int CLOCK_DAY = 0xFFFFD54F;
	static final int CLOCK_DUSK = 0xFFFF8C1A;
	static final int CLOCK_NIGHT = RED;

	/** The crosshair info background. */
	static final int CROSSHAIR_BG = 0x90000000;

	/** The colour with the alpha of {@code alpha} (0..1, at least a faint trace). */
	static int withAlpha(int color, float alpha) {
		return (Math.max(0x20, Math.round(alpha * 255)) << 24) | (color & 0xFFFFFF);
	}

	/** Linear mix of two opaque colours: t = 0 gives a, t = 1 gives b. */
	static int lerp(int a, int b, float t) {
		int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return 0xFF000000 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
	}
}
