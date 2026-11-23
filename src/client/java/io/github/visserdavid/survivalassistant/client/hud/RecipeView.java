package io.github.visserdavid.survivalassistant.client.hud;

import io.github.visserdavid.survivalassistant.advice.Recipes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Draws a recipe as a 3x3 grid of item icons, an arrow and the result. Used by the recipe card and the full task list. */
public final class RecipeView {
	private RecipeView() {}

	private static final Map<String, ItemStack> STACKS = new HashMap<>();
	private static final int SLOT_BG = 0xFF2A2A2A;
	private static final int SLOT_EDGE = 0xFF555555;

	/** Width of the drawing for the given slot size (18 = full size, 9 = half size). */
	public static int width(int slot) {
		return 3 * slot + slot / 2 + PixelIcons.PLAY_W + slot / 2 + slot;
	}

	public static int height(int slot) {
		return 3 * slot;
	}

	/** Draws the grid with its top-left corner at (x, y). */
	public static void draw(GuiGraphicsExtractor g, Font font, Recipes.Recipe recipe, int x, int y, int slot) {
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int sx = x + col * slot, sy = y + row * slot;
				g.fill(sx, sy, sx + slot - 1, sy + slot - 1, SLOT_EDGE);
				g.fill(sx + 1, sy + 1, sx + slot - 1, sy + slot - 1, SLOT_BG);
				String id = recipe.at(row, col);
				if (id != null) icon(g, stack(id), sx + 1, sy + 1, slot);
			}
		}
		int arrowX = x + 3 * slot + slot / 2;
		int midY = y + height(slot) / 2;
		PixelIcons.play(g, arrowX, midY - 3, 0xFFFFC94D);

		int rx = arrowX + PixelIcons.PLAY_W + slot / 2, ry = midY - slot / 2;
		g.fill(rx, ry, rx + slot - 1, ry + slot - 1, 0xFF6B5320);
		g.fill(rx + 1, ry + 1, rx + slot - 1, ry + slot - 1, SLOT_BG);
		ItemStack result = stack(recipe.resultId());
		icon(g, result, rx + 1, ry + 1, slot);
		if (recipe.count() > 1) {
			if (slot >= 18) {
				g.itemDecorations(font, result, rx + 1, ry + 1, String.valueOf(recipe.count()));
			} else {
				g.text(font, "x" + recipe.count(), rx + slot + 1, ry + 1, 0xFFFFFFFF, true);
			}
		}
	}

	/** Extra width taken by the "x4" label next to a half-size result. */
	public static int countLabelWidth(Font font, Recipes.Recipe recipe, int slot) {
		return slot < 18 && recipe.count() > 1 ? 2 + font.width("x" + recipe.count()) : 0;
	}

	public static ItemStack stack(String id) {
		return STACKS.computeIfAbsent(id, k -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(k))));
	}

	private static void icon(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int slot) {
		if (stack.isEmpty()) return;
		if (slot >= 18) {
			g.item(stack, x, y);
			return;
		}
		float s = (slot - 1) / 16f;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(s, s);
		g.item(stack, 0, 0);
		g.pose().popMatrix();
	}
}
