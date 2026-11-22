package io.github.visserdavid.survivalassistant.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.Tip;
import io.github.visserdavid.survivalassistant.advice.TipRegistry;
import io.github.visserdavid.survivalassistant.state.HudState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The "quest card": current task with icon, counter and bar, plus the next two tasks (compact),
 * or all milestones (full). Shows a day/sun clock and turns red when something urgent is going on.
 */
public final class TaskHud implements HudElement {
	private static final int WIDTH = 150;
	private static final int PAD = 4;
	private static final int HEADER_H = 12;
	private static final int CURRENT_H = 20;
	private static final int ROW_H = 10;

	private static final int BG = 0xB0101010;
	private static final int BORDER = 0xFF6B5320;
	private static final int GOLD = 0xFFFFC94D;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFF9A9A9A;
	private static final int DARK_GRAY = 0xFF5A5A5A;
	private static final int GREEN = 0xFF6BD96B;
	private static final int RED = 0xFFFF5050;
	private static final int BAR_BG = 0xFF303030;

	private static final long FLASH_MS = 1500;
	private static final long SLIDE_MS = 250;

	/** Screen rectangle {x, y, w, h} of the card drawn this frame, or null when hidden (used by EnvironmentCard). */
	public static int[] visibleBounds = null;

	// --- completion animation state ---
	private Set<String> knownCompleted = null;
	private String flashId = null;
	private long flashStart = 0;

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		visibleBounds = null;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		HudConfig cfg = HudConfig.get();
		if (cfg.mode == HudConfig.Mode.HIDDEN || mc.gui.hud.isHidden() || mc.getDebugOverlay().showDebugScreen()) return;

		HudSnapshot snap = mc.player.getAttached(HudState.HUD);
		if (snap == null || !snap.active()) {
			knownCompleted = null;
			return;
		}
		long now = System.currentTimeMillis();
		trackCompletion(snap, now);

		boolean full = cfg.mode == HudConfig.Mode.FULL;
		int height = full ? fullHeight() : compactHeight(snap, now);
		float scale = cfg.scale;
		int sw = Math.round(WIDTH * scale), sh = Math.round(height * scale);
		int x = switch (cfg.corner) {
			case TOP_LEFT, BOTTOM_LEFT -> cfg.margin;
			case TOP_RIGHT, BOTTOM_RIGHT -> g.guiWidth() - sw - cfg.margin;
		};
		int y = switch (cfg.corner) {
			case TOP_LEFT, TOP_RIGHT -> cfg.margin;
			// keep clear of the hotbar, health/food bars and the chat line
			case BOTTOM_LEFT, BOTTOM_RIGHT -> g.guiHeight() - sh - cfg.margin - 52;
		};

		visibleBounds = new int[]{x, y, sw, sh};
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		drawCard(g, mc, snap, height, full, now);
		g.pose().popMatrix();
	}

	// ------------------------------------------------------------------ layout

	private int compactHeight(HudSnapshot snap, long now) {
		int rows = Math.max(0, linesForCompact(snap, now).size() - 1);
		return PAD + HEADER_H + CURRENT_H + rows * ROW_H + PAD;
	}

	private static int fullHeight() {
		return PAD + HEADER_H + TipRegistry.MILESTONES.size() * ROW_H + PAD;
	}

	/** Compact lines: optionally the task that was just completed (flashing), then the open tasks. */
	private List<HudSnapshot.Entry> linesForCompact(HudSnapshot snap, long now) {
		List<HudSnapshot.Entry> lines = new ArrayList<>();
		if (flashing(now)) lines.add(new HudSnapshot.Entry(flashId, 1, 0));
		for (HudSnapshot.Entry e : snap.next()) {
			if (lines.size() >= HudSnapshot.SHOWN_TASKS) break;
			lines.add(e);
		}
		if (lines.isEmpty()) lines.add(new HudSnapshot.Entry("", 0, 0)); // "all done"
		return lines;
	}

	private boolean flashing(long now) {
		return flashId != null && now - flashStart < FLASH_MS;
	}

	private void trackCompletion(HudSnapshot snap, long now) {
		Set<String> completed = new HashSet<>(snap.completed());
		if (knownCompleted != null) {
			for (Tip m : TipRegistry.MILESTONES) {
				if (completed.contains(m.id()) && !knownCompleted.contains(m.id())) {
					flashId = m.id();
					flashStart = now;
				}
			}
		}
		knownCompleted = completed;
	}

	// ------------------------------------------------------------------ drawing

	private void drawCard(GuiGraphicsExtractor g, Minecraft mc, HudSnapshot snap, int height, boolean full, long now) {
		Font font = mc.font;
		boolean urgent = !snap.urgentId().isEmpty();

		// background + border (pulsing red when urgent)
		int border = BORDER;
		if (urgent) {
			float pulse = 0.5f + 0.5f * (float) Math.sin(now / 150.0);
			border = lerpColor(0xFF7A1010, RED, pulse);
		}
		g.fill(0, 0, WIDTH, height, BG);
		outline(g, 0, 0, WIDTH, height, border);

		drawHeader(g, mc, font, snap);

		int y = PAD + HEADER_H;
		if (full) {
			drawFullList(g, font, snap, y);
			return;
		}

		List<HudSnapshot.Entry> lines = linesForCompact(snap, now);
		HudSnapshot.Entry first = lines.get(0);
		boolean firstIsFlash = flashing(now) && first.id().equals(flashId);

		// slide: right after the flash ends, the list glides up into place
		int slide = 0;
		if (flashId != null && !flashing(now) && now - flashStart < FLASH_MS + SLIDE_MS) {
			float t = (now - flashStart - FLASH_MS) / (float) SLIDE_MS;
			slide = Math.round((1 - t) * ROW_H);
		}

		if (urgent && !firstIsFlash) {
			drawAlert(g, font, snap.urgentId(), y, now);
		} else if (firstIsFlash) {
			float fade = 1 - (now - flashStart) / (float) FLASH_MS;
			drawCurrent(g, font, first, y, true, fade);
		} else if (first.id().isEmpty()) {
			g.text(font, Component.translatable("survivalassistant.hud.all_done"), PAD + 20, y + 6, GREEN, true);
		} else {
			drawCurrent(g, font, first, y + slide, false, 1f);
		}

		int rowY = y + CURRENT_H + slide;
		for (int i = 1; i < lines.size(); i++) {
			drawSmallRow(g, font, lines.get(i), rowY, GRAY, "·");
			rowY += ROW_H;
		}
	}

	private void drawHeader(GuiGraphicsExtractor g, Minecraft mc, Font font, HudSnapshot snap) {
		int day = Math.min(snap.dayIndex() + 1, Math.max(1, snap.maxDays()));
		g.text(font, Component.translatable("survivalassistant.hud.title", day, snap.maxDays()), PAD, PAD + 1, GOLD, true);

		// sun/moon clock: 6 segments counting down to sunset (day) or sunrise (night)
		long time = Math.floorMod(mc.level.getOverworldClockTime(), 24000L);
		boolean night = time >= 13000 && time < 23000;
		boolean dusk = time >= 11000 && time < 13000;
		int color = night ? RED : dusk ? 0xFFFF8C1A : 0xFFFFD54F;
		long remaining = night ? 23000 - time : time >= 23000 ? 12000 + (24000 - time) : Math.max(0, 12000 - time);
		long span = night ? 10000 : 12000;
		int filled = (int) Math.ceil(Math.min(1.0, remaining / (double) span) * 6);

		int segW = 4, gap = 1, segH = 5;
		int barX = WIDTH - PAD - 6 * (segW + gap) + gap;
		int barY = PAD + 2;
		for (int i = 0; i < 6; i++) {
			int sx = barX + i * (segW + gap);
			g.fill(sx, barY, sx + segW, barY + segH, i < filled ? color : BAR_BG);
		}
		String symbol = night ? "☾" : "☀";
		g.text(font, symbol, barX - font.width(symbol) - 2, PAD + 1, color, true);
	}

	private void drawCurrent(GuiGraphicsExtractor g, Font font, HudSnapshot.Entry e, int y, boolean done, float alpha) {
		int a = Math.max(0x20, Math.round(alpha * 255)) << 24;
		ItemStack icon = TaskIcons.get(e.id());
		if (!icon.isEmpty()) g.item(icon, PAD, y + 1);

		Component title = Component.translatable("survivalassistant.task." + e.id());
		int textColor = done ? (GREEN & 0xFFFFFF) | a : WHITE;
		g.text(font, title, PAD + 20, y + 2, textColor, true);

		String right = done ? "✔" : e.target() > 0 ? e.value() + "/" + e.target() : "";
		if (!right.isEmpty()) {
			g.text(font, right, WIDTH - PAD - font.width(right), y + 2, done ? textColor : GOLD, true);
		}

		// progress bar
		int bx0 = PAD + 20, bx1 = WIDTH - PAD, by = y + 13;
		g.fill(bx0, by, bx1, by + 2, BAR_BG);
		float frac = done ? 1f : e.target() > 0 ? e.value() / (float) e.target() : 0f;
		if (frac > 0) g.fill(bx0, by, bx0 + Math.round((bx1 - bx0) * frac), by + 2, done ? (GREEN & 0xFFFFFF) | a : GOLD);
	}

	private void drawAlert(GuiGraphicsExtractor g, Font font, String alertId, int y, long now) {
		ItemStack icon = TaskIcons.get(alertId);
		if (!icon.isEmpty()) g.item(icon, PAD, y + 1);
		boolean blink = (now / 400) % 2 == 0;
		g.text(font, Component.translatable("survivalassistant.alert." + alertId), PAD + 20, y + 5, blink ? RED : 0xFFFFB0B0, true);
	}

	private void drawSmallRow(GuiGraphicsExtractor g, Font font, HudSnapshot.Entry e, int y, int color, String bullet) {
		smallIcon(g, TaskIcons.get(e.id()), PAD + 6, y);
		g.text(font, bullet, PAD, y + 1, DARK_GRAY, false);
		g.text(font, Component.translatable("survivalassistant.task." + e.id()), PAD + 17, y + 1, color, true);
	}

	private void drawFullList(GuiGraphicsExtractor g, Font font, HudSnapshot snap, int y) {
		Set<String> completed = new HashSet<>(snap.completed());
		String current = snap.next().isEmpty() ? "" : snap.next().get(0).id();
		for (Tip m : TipRegistry.MILESTONES) {
			boolean done = completed.contains(m.id());
			boolean isCurrent = m.id().equals(current);
			int color = done ? GREEN : isCurrent ? GOLD : GRAY;
			smallIcon(g, TaskIcons.get(m.id()), PAD + 8, y);
			g.text(font, done ? "✔" : isCurrent ? "▶" : "·", PAD, y + 1, color, false);
			g.text(font, Component.translatable("survivalassistant.task." + m.id()), PAD + 19, y + 1, color, true);
			if (isCurrent && !snap.next().isEmpty() && snap.next().get(0).target() > 0) {
				HudSnapshot.Entry e = snap.next().get(0);
				String count = e.value() + "/" + e.target();
				g.text(font, count, WIDTH - PAD - font.width(count), y + 1, GOLD, true);
			}
			y += ROW_H;
		}
	}

	// ------------------------------------------------------------------ helpers

	private static void smallIcon(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
		if (stack.isEmpty()) return;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(0.5f, 0.5f);
		g.item(stack, 0, 0);
		g.pose().popMatrix();
	}

	private static void outline(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
		g.fill(x0, y0, x1, y0 + 1, color);
		g.fill(x0, y1 - 1, x1, y1, color);
		g.fill(x0, y0, x0 + 1, y1, color);
		g.fill(x1 - 1, y0, x1, y1, color);
	}

	private static int lerpColor(int a, int b, float t) {
		int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return 0xFF000000 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
	}
}
