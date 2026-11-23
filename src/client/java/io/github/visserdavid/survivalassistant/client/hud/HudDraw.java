package io.github.visserdavid.survivalassistant.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/** Drawing helpers shared by the cards. */
final class HudDraw {
	/** Bottom-corner cards keep this far above the screen edge: clear of the hotbar, health/food bars and chat line. */
	static final int HOTBAR_MARGIN = 52;
	/** An item drawn at half size is 8 GUI pixels. */
	static final int SMALL_ICON = 8;

	private HudDraw() {}

	/** The HUD is hidden (F1) or the debug screen (F3) is open: no cards. */
	static boolean hidden(Minecraft mc) {
		return mc.gui.hud.isHidden() || mc.getDebugOverlay().showDebugScreen();
	}

	/** An item at half size with its top-left corner at (x, y). */
	static void smallIcon(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
		scaledIcon(g, stack, x, y, 0.5f);
	}

	/** An item at {@code scale} (1 = 16 pixels) with its top-left corner at (x, y). */
	static void scaledIcon(GuiGraphicsExtractor g, ItemStack stack, int x, int y, float scale) {
		if (stack.isEmpty()) return;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.item(stack, 0, 0);
		g.pose().popMatrix();
	}

	/** A one-pixel frame around the rectangle (x0, y0)-(x1, y1). */
	static void outline(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
		g.fill(x0, y0, x1, y0 + 1, color);
		g.fill(x0, y1 - 1, x1, y1, color);
		g.fill(x0, y0, x0 + 1, y1, color);
		g.fill(x1 - 1, y0, x1, y1, color);
	}

	/** A filled card with a frame. */
	static void card(GuiGraphicsExtractor g, int x, int y, int w, int h, int background, int border) {
		g.fill(x, y, x + w, y + h, background);
		outline(g, x, y, x + w, y + h, border);
	}

	/** Left edge of a card of width {@code w} (already scaled) in the configured corner. */
	static int cornerX(ClientConfig cfg, int guiWidth, int w) {
		return switch (cfg.corner) {
			case TOP_LEFT, BOTTOM_LEFT -> cfg.margin;
			case TOP_RIGHT, BOTTOM_RIGHT -> guiWidth - w - cfg.margin;
		};
	}

	/** Top edge of a card of height {@code h} (already scaled) in the configured corner. */
	static int cornerY(ClientConfig cfg, int guiHeight, int h) {
		return switch (cfg.corner) {
			case TOP_LEFT, TOP_RIGHT -> cfg.margin;
			case BOTTOM_LEFT, BOTTOM_RIGHT -> guiHeight - h - cfg.margin - HOTBAR_MARGIN;
		};
	}

	/** Top edge of a card of height {@code sh} stacked under (top corner) or above (bottom corner) another card. */
	static int stack(boolean top, int anchorY, int anchorH, int sh) {
		return top ? anchorY + anchorH + STACK_GAP : anchorY - sh - STACK_GAP;
	}

	/** Space between two stacked cards. */
	static final int STACK_GAP = 2;

	static boolean topCorner(ClientConfig cfg) {
		return cfg.corner == ClientConfig.Corner.TOP_LEFT || cfg.corner == ClientConfig.Corner.TOP_RIGHT;
	}
}
