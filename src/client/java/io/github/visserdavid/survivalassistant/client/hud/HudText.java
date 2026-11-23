package io.github.visserdavid.survivalassistant.client.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Shared layout for the HUD cards: one common width (so the task card and environment card line up)
 * and text that never overlaps its neighbours. Text that is wider than its slot scrolls slowly back and
 * forth inside a clipped area, like a marquee, instead of running into the counter or the clock.
 */
public final class HudText {
	/** Cards grow with their content between these widths (GUI pixels, before the extra config scale). */
	public static final int MIN_WIDTH = 160;
	public static final int MAX_WIDTH = 230;

	private static final float SCROLL_PX_PER_SEC = 22f;
	private static final long PAUSE_MS = 1400;

	private HudText() {}

	public static int clampWidth(int wanted) {
		return Math.clamp(wanted, MIN_WIDTH, MAX_WIDTH);
	}

	/**
	 * Draws {@code text} at (x, y). If it is wider than {@code maxWidth}, it is clipped to that slot and scrolls.
	 */
	public static void fit(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int maxWidth, int color, boolean shadow, long now) {
		if (maxWidth <= 0) return; // no room at all: drawing it would overlap what it should leave room for
		int w = font.width(text);
		if (w <= maxWidth || maxWidth <= 0) {
			g.text(font, text, x, y, color, shadow);
			return;
		}
		int offset = Math.round(marquee(w - maxWidth, now));
		g.enableScissor(x, y - 1, x + maxWidth, y + font.lineHeight + 1);
		g.text(font, text, x - offset, y, color, shadow);
		g.disableScissor();
	}

	/** Back-and-forth scroll position: pause at the start, scroll to the end, pause, scroll back. */
	static float marquee(int overflow, long now) {
		long travel = Math.max(1, (long) (overflow / SCROLL_PX_PER_SEC * 1000));
		long cycle = 2 * PAUSE_MS + 2 * travel;
		long t = now % cycle;
		if (t < PAUSE_MS) return 0;
		t -= PAUSE_MS;
		if (t < travel) return overflow * (t / (float) travel);
		t -= travel;
		if (t < PAUSE_MS) return overflow;
		t -= PAUSE_MS;
		return overflow * (1 - t / (float) travel);
	}
}
