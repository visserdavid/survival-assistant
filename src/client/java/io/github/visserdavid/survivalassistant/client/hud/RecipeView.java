package io.github.visserdavid.survivalassistant.client.hud;

import io.github.visserdavid.survivalassistant.advice.Recipes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;


/** Draws a recipe as a 3x3 grid of item icons, an arrow and the result. Used by the recipe card and the full task list. */
public final class RecipeView {
	private RecipeView() {}

	/** Grid slots at this size or larger draw items at full size with the game's own count label. */
	private static final int FULL_SLOT = 18;
	private static final int GRID = 3;

	/** Width of the drawing for the given slot size (18 = full size, 9 = half size). */
	public static int width(int slot) {
		return GRID * slot + slot / 2 + PixelIcons.PLAY_W + slot / 2 + slot;
	}

	public static int height(int slot) {
		return GRID * slot;
	}

	/** Draws the grid with its top-left corner at (x, y). */
	public static void draw(GuiGraphicsExtractor g, Font font, Recipes.Recipe recipe, int x, int y, int slot) {
		for (int row = 0; row < GRID; row++) {
			for (int col = 0; col < GRID; col++) {
				int sx = x + col * slot, sy = y + row * slot;
				slot(g, sx, sy, slot, HudColors.SLOT_EDGE);
				String id = recipe.at(row, col);
				if (id != null) icon(g, ItemIcons.of(id), sx + 1, sy + 1, slot);
			}
		}
		int arrowX = x + GRID * slot + slot / 2;
		int midY = y + height(slot) / 2;
		PixelIcons.play(g, arrowX, midY - PixelIcons.PLAY_H / 2, HudColors.GOLD);

		int rx = arrowX + PixelIcons.PLAY_W + slot / 2, ry = midY - slot / 2;
		slot(g, rx, ry, slot, HudColors.BORDER_TASK);
		ItemStack result = ItemIcons.of(recipe.resultId());
		icon(g, result, rx + 1, ry + 1, slot);
		if (recipe.count() > 1) {
			if (slot >= FULL_SLOT) {
				g.itemDecorations(font, result, rx + 1, ry + 1, String.valueOf(recipe.count()));
			} else {
				g.text(font, "x" + recipe.count(), rx + slot + 1, ry + 1, HudColors.WHITE, true);
			}
		}
	}

	/** Extra width taken by the "x4" label next to a half-size result. */
	public static int countLabelWidth(Font font, Recipes.Recipe recipe, int slot) {
		return slot < FULL_SLOT && recipe.count() > 1 ? 2 + font.width("x" + recipe.count()) : 0;
	}

	/** An empty slot: a frame in {@code edge} with the dark slot background inside. */
	private static void slot(GuiGraphicsExtractor g, int x, int y, int size, int edge) {
		g.fill(x, y, x + size - 1, y + size - 1, edge);
		g.fill(x + 1, y + 1, x + size - 1, y + size - 1, HudColors.SLOT_BG);
	}

	private static void icon(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int slot) {
		if (stack.isEmpty()) return;
		if (slot >= FULL_SLOT) g.item(stack, x, y);
		else HudDraw.scaledIcon(g, stack, x, y, (slot - 1) / 16f);
	}
}
