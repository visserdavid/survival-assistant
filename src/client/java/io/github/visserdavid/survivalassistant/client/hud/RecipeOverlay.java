package io.github.visserdavid.survivalassistant.client.hud;

import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.Recipes;
import io.github.visserdavid.survivalassistant.state.HudState;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * A small recipe card next to the inventory or crafting table window, showing how to craft the item for the
 * current task, right when the player needs it. The window's position comes through the access widener
 * ({@code survivalassistant.accesswidener}).
 */
public final class RecipeOverlay {
	private static final int PAD = 5;
	private static final int SLOT = 18;
	private static final int TITLE_H = 12;
	private static final int NOTE_H = 11;
	/** Space between the container window and the card, and the minimum distance to the screen edge. */
	private static final int SPACING = 4;
	private static final int EDGE = 2;

	private RecipeOverlay() {}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof InventoryScreen || screen instanceof CraftingScreen) {
				AbstractContainerScreen<?> container = (AbstractContainerScreen<?>) screen;
				ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> draw(container, g));
			}
		});
	}

	private static void draw(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !ClientConfig.get().recipeCard) return;
		HudSnapshot snap = mc.player.getAttached(HudState.HUD);
		if (snap == null || !snap.active() || snap.next().isEmpty()) return;
		Optional<Recipes.Recipe> found = Recipes.forTask(snap.next().getFirst().id());
		if (found.isEmpty()) return;
		Recipes.Recipe recipe = found.get();

		Font font = mc.font;
		Component title = Component.translatable("survivalassistant.recipe.title", ItemIcons.of(recipe.resultId()).getHoverName());
		boolean noteTable = recipe.needsTable() && screen instanceof InventoryScreen;
		Component note = Component.translatable("survivalassistant.recipe.needs_table");
		int w = Math.max(RecipeView.width(SLOT), Math.max(font.width(title), noteTable ? font.width(note) : 0)) + 2 * PAD;
		int h = PAD + TITLE_H + RecipeView.height(SLOT) + (noteTable ? NOTE_H : 0) + PAD;

		// right of the container window, or left of it when there is no room, or the top-left corner as a last resort
		int y = Math.max(EDGE, Math.min(screen.topPos, screen.height - h - EDGE));
		int right = screen.leftPos + screen.imageWidth + SPACING, left = screen.leftPos - SPACING - w;
		int x;
		if (right + w <= screen.width - EDGE) {
			x = right;
		} else if (left >= EDGE) {
			x = left;
		} else {
			x = EDGE;
			y = EDGE;
		}

		HudDraw.card(g, x, y, w, h, HudColors.BG_SOLID, HudColors.BORDER_TASK);
		g.text(font, title, x + PAD, y + PAD, HudColors.GOLD, true);
		RecipeView.draw(g, font, recipe, x + PAD, y + PAD + TITLE_H, SLOT);
		if (noteTable) {
			g.text(font, note, x + PAD, y + PAD + TITLE_H + RecipeView.height(SLOT) + 3, HudColors.SALMON, true);
		}
	}
}
