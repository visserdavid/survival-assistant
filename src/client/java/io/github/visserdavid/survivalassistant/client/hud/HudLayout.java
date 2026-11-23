package io.github.visserdavid.survivalassistant.client.hud;

/**
 * What the task card and the environment card tell each other every frame, so they share one width and the
 * environment card sits right under (or above) the task card. One instance is shared by both cards.
 */
public final class HudLayout {
	/** Width the environment card wanted last frame (0 when hidden); the task card takes it into account. */
	int environmentWanted;

	/** The task card is drawn this frame; its screen rectangle and unscaled width follow. */
	boolean taskVisible;
	int taskX, taskY, taskW, taskH;
	int taskCardWidth = HudText.MIN_WIDTH;

	/** The environment card is drawn this frame; its screen rectangle and unscaled width follow. */
	boolean envVisible;
	int envX, envY, envW, envH;
	int envCardWidth = HudText.MIN_WIDTH;

	void envHidden() {
		envVisible = false;
	}

	void envShown(int x, int y, int w, int h, int cardWidth) {
		envVisible = true;
		envX = x;
		envY = y;
		envW = w;
		envH = h;
		envCardWidth = cardWidth;
	}

	void taskHidden() {
		taskVisible = false;
	}

	void taskShown(int x, int y, int w, int h, int cardWidth) {
		taskVisible = true;
		taskX = x;
		taskY = y;
		taskW = w;
		taskH = h;
		taskCardWidth = cardWidth;
	}
}
