package io.github.visserdavid.survivalassistant.client.hud;

/**
 * What the three cards tell each other every frame (drawn in this order: task card, environment card, target card),
 * so they share one width and stack in the chosen corner. One instance is shared by all cards.
 */
public final class HudLayout {
	/** Width the environment card wanted last frame (0 when hidden); the task card takes it into account. */
	int environmentWanted;

	/** The task card is drawn this frame: its top and height on screen (scaled) and its unscaled width. */
	boolean taskVisible;
	int taskY, taskH;
	int taskCardWidth = HudText.MIN_WIDTH;

	/** The environment card is drawn this frame: its top and height on screen (scaled) and its unscaled width. */
	boolean envVisible;
	int envY, envH;
	int envCardWidth = HudText.MIN_WIDTH;

	void taskHidden() {
		taskVisible = false;
	}

	void taskShown(int y, int h, int cardWidth) {
		taskVisible = true;
		taskY = y;
		taskH = h;
		taskCardWidth = cardWidth;
	}

	void envHidden() {
		envVisible = false;
	}

	void envShown(int y, int h, int cardWidth) {
		envVisible = true;
		envY = y;
		envH = h;
		envCardWidth = cardWidth;
	}
}
