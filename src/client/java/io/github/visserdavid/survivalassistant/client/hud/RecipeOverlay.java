package io.github.visserdavid.survivalassistant.client.hud;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.Recipes;
import io.github.visserdavid.survivalassistant.state.HudState;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * A small recipe card next to the inventory or crafting table window, showing how to craft the item for the
 * current task, right when the player needs it.
 */
public final class RecipeOverlay {
	private RecipeOverlay() {}

	private static final int PAD = 5;
	private static final int SLOT = 18;
	private static final int TITLE_H = 12;
	private static final int NOTE_H = 11;
	private static final int BG = 0xE0101010;
	private static final int BORDER = 0xFF6B5320;

	private static Field leftPos, topPos, imageWidth;
	private static boolean reflectionFailed;

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof InventoryScreen || screen instanceof CraftingScreen) {
				ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> draw(s, g));
			}
		});
	}

	private static void draw(Screen screen, GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !HudConfig.get().recipeCard) return;
		HudSnapshot snap = mc.player.getAttached(HudState.HUD);
		if (snap == null || !snap.active() || snap.next().isEmpty()) return;
		String task = snap.next().get(0).id();
		Optional<Recipes.Recipe> found = Recipes.forTask(task);
		if (found.isEmpty()) return;
		Recipes.Recipe recipe = found.get();

		Font font = mc.font;
		Component title = Component.translatable("survivalassistant.recipe.title", RecipeView.stack(recipe.resultId()).getHoverName());
		boolean noteTable = recipe.needsTable() && screen instanceof InventoryScreen;
		Component note = Component.translatable("survivalassistant.recipe.needs_table");
		int w = Math.max(RecipeView.width(SLOT), Math.max(font.width(title), noteTable ? font.width(note) : 0)) + 2 * PAD;
		int h = PAD + TITLE_H + RecipeView.height(SLOT) + (noteTable ? NOTE_H : 0) + PAD;

		int[] pos = position(screen, w, h);
		int x = pos[0], y = pos[1];
		g.fill(x, y, x + w, y + h, BG);
		g.fill(x, y, x + w, y + 1, BORDER);
		g.fill(x, y + h - 1, x + w, y + h, BORDER);
		g.fill(x, y, x + 1, y + h, BORDER);
		g.fill(x + w - 1, y, x + w, y + h, BORDER);

		g.text(font, title, x + PAD, y + PAD, 0xFFFFC94D, true);
		RecipeView.draw(g, font, recipe, x + PAD, y + PAD + TITLE_H, SLOT);
		if (noteTable) {
			g.text(font, note, x + PAD, y + PAD + TITLE_H + RecipeView.height(SLOT) + 3, 0xFFFF9A6B, true);
		}
	}

	/** Right of the container window, or left of it when there is no room, or the top-left corner as a last resort. */
	private static int[] position(Screen screen, int w, int h) {
		int left = (screen.width - 176) / 2, top = (screen.height - 166) / 2, imgW = 176;
		if (screen instanceof AbstractContainerScreen<?> acs && lookupFields()) {
			try {
				left = leftPos.getInt(acs);
				top = topPos.getInt(acs);
				imgW = imageWidth.getInt(acs);
			} catch (IllegalAccessException e) {
				reflectionFailed = true;
			}
		}
		int y = Math.max(2, Math.min(top, screen.height - h - 2));
		if (left + imgW + 4 + w <= screen.width - 2) return new int[]{left + imgW + 4, y};
		if (left - 4 - w >= 2) return new int[]{left - 4 - w, y};
		return new int[]{2, 2};
	}

	private static boolean lookupFields() {
		if (reflectionFailed) return false;
		if (leftPos != null) return true;
		try {
			leftPos = AbstractContainerScreen.class.getDeclaredField("leftPos");
			topPos = AbstractContainerScreen.class.getDeclaredField("topPos");
			imageWidth = AbstractContainerScreen.class.getDeclaredField("imageWidth");
			leftPos.setAccessible(true);
			topPos.setAccessible(true);
			imageWidth.setAccessible(true);
			return true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			SurvivalAssistant.LOGGER.warn("Recipe card: container position unavailable, using a default position", e);
			reflectionFailed = true;
			leftPos = null;
			return false;
		}
	}
}
