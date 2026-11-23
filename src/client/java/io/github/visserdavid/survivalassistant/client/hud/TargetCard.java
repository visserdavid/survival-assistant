package io.github.visserdavid.survivalassistant.client.hud;

import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.state.HudState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static io.github.visserdavid.survivalassistant.client.hud.HudColors.BG;
import static io.github.visserdavid.survivalassistant.client.hud.HudColors.BORDER_ENV;

/**
 * The mob/ore info as a third card, under the environment card (or above it in a bottom corner). Used when the
 * settings put the info in a card instead of under the crosshair; the info stays a moment after looking away
 * (see {@link io.github.visserdavid.survivalassistant.advice.CrosshairHold}), so there is time to read it.
 */
public final class TargetCard implements HudElement {
	private static final int PAD = 4;
	private static final int LINE_H = 10;
	/** Text indent after the small icon on the first line. */
	private static final int ICON_INDENT = 10;
	private static final int MAX_LINES = 4;

	private final HudLayout layout;
	private final CrosshairInfo info;

	/** Wrapped rows, rebuilt only when the info or the width changes. */
	private CrosshairInfo.@Nullable Info wrappedInfo;
	private int wrappedWidth = -1;
	private List<FormattedCharSequence> rows = List.of();

	public TargetCard(HudLayout layout, CrosshairInfo info) {
		this.layout = layout;
		this.info = info;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		ClientConfig cfg = ClientConfig.get();
		CrosshairInfo.Info shown = info.shown();
		if (shown == null || mc.player == null || !cfg.crosshairInfo || cfg.crosshairPlace != ClientConfig.Place.CARD) return;
		if (HudDraw.hidden(mc) || mc.gui.screen() != null) return;
		HudSnapshot snap = mc.player.getAttached(HudState.HUD);
		if (snap == null || !snap.active()) return;

		// the same width as the cards above, so the three line up
		int width = layout.taskVisible ? layout.taskCardWidth : layout.envVisible ? layout.envCardWidth : HudText.MIN_WIDTH;
		Font font = mc.font;
		boolean icon = !shown.icon().isEmpty();
		int indent = icon ? ICON_INDENT : 0;
		if (!shown.equals(wrappedInfo) || width != wrappedWidth) {
			wrappedInfo = shown;
			wrappedWidth = width;
			List<FormattedCharSequence> all = font.split(shown.text(), width - 2 * PAD - indent);
			rows = all.subList(0, Math.min(MAX_LINES, all.size()));
		}
		int height = PAD + rows.size() * LINE_H + PAD - 2;

		float scale = cfg.scale;
		int sw = Math.round(width * scale), sh = Math.round(height * scale);
		int x = HudDraw.cornerX(cfg, g.guiWidth(), sw);
		int y = y(cfg, g.guiHeight(), sh);

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.fill(0, 0, width, height, BG);
		HudDraw.outline(g, 0, 0, width, height, BORDER_ENV);
		if (icon) HudDraw.smallIcon(g, shown.icon(), PAD, PAD);
		int ty = PAD;
		for (FormattedCharSequence row : rows) {
			g.text(font, row, PAD + indent, ty, shown.color(), true);
			ty += LINE_H;
		}
		g.pose().popMatrix();
	}

	/** Under the lowest card in a top corner, above the highest one in a bottom corner, else in the corner itself. */
	private int y(ClientConfig cfg, int guiHeight, int sh) {
		boolean top = HudDraw.topCorner(cfg);
		if (layout.envVisible) return top ? layout.envY + layout.envH + 2 : layout.envY - sh - 2;
		if (layout.taskVisible) return top ? layout.taskY + layout.taskH + 2 : layout.taskY - sh - 2;
		return HudDraw.cornerY(cfg, guiHeight, sh);
	}
}
