package io.github.visserdavid.survivalassistant.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.Navigation;
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
 *
 * <p>The card is as wide as its content needs (see {@link HudText}); text that still doesn't fit scrolls
 * inside its own slot so it never overlaps the counter or the clock.
 */
public final class TaskHud implements HudElement {
	private static final int PAD = 4;
	private static final int HEADER_H = 12;
	private static final int CURRENT_H = 20;
	private static final int ROW_H = 10;
	private static final int ICON_X = PAD + 20;   // text start next to a full-size icon
	private static final int SMALL_X = PAD + 17;  // text start next to a small icon (compact rows)
	private static final int FULL_X = PAD + 19;   // text start in the full list
	private static final int GAP = 6;             // minimum space between text and right-aligned info
	private static final int NAV_H = 13;          // navigation strip (death spot / home) at the bottom
	private static final int NAV_TEXT_X = PAD + 21;

	// clock: clock item (shows the sun/moon itself) + 6 segments
	private static final int SEG_W = 4, SEG_GAP = 1, SEG_H = 5, SEGMENTS = 6;
	private static final int BAR_W = SEGMENTS * (SEG_W + SEG_GAP) - SEG_GAP;

	private static final int BG = 0xB0101010;
	private static final int BORDER = 0xFF6B5320;
	private static final int GOLD = 0xFFFFC94D;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFF9A9A9A;
	private static final int DARK_GRAY = 0xFF5A5A5A;
	private static final int GREEN = 0xFF6BD96B;
	private static final int RED = 0xFFFF5050;
	private static final int BAR_BG = 0xFF303030;
	private static final int AQUA = 0xFF6FD3E8;
	private static final int DEATH_COLOR = 0xFFFF9A6B;

	private static final long FLASH_MS = 1500;
	private static final long SLIDE_MS = 250;

	/** Screen rectangle {x, y, w, h} of the card drawn this frame, or null when hidden (used by EnvironmentCard). */
	public static int[] visibleBounds = null;
	/** Unscaled card width used this frame (EnvironmentCard uses the same width so both cards line up). */
	public static int cardWidth = HudText.MIN_WIDTH;

	// --- completion animation state ---
	private Set<String> knownCompleted = null;
	private String flashId = null;
	private long flashStart = 0;

	private static ItemStack clockIcon;

	/** Width of the card being drawn this frame. */
	private int width = HudText.MIN_WIDTH;

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
		width = HudText.clampWidth(Math.max(Math.max(wantedWidth(mc.font, snap, full, now), navWanted(mc.font, snap)),
				HudText.environmentWanted));
		cardWidth = width;
		int height = (full ? fullHeight() : compactHeight(snap, now)) + (snap.waypoint().isPresent() ? NAV_H : 0);
		float scale = cfg.scale;
		int sw = Math.round(width * scale), sh = Math.round(height * scale);
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

	/** The width at which nothing on the card would need to scroll. */
	private int wantedWidth(Font font, HudSnapshot snap, boolean full, long now) {
		int wanted = PAD + font.width(title(snap)) + GAP + clockWidth(font) + PAD;
		if (full) {
			for (Tip m : TipRegistry.MILESTONES) {
				wanted = Math.max(wanted, FULL_X + font.width(taskName(m.id())) + GAP + font.width("99/99") + PAD);
			}
			return wanted;
		}
		List<HudSnapshot.Entry> lines = linesForCompact(snap, now);
		for (int i = 0; i < lines.size(); i++) {
			HudSnapshot.Entry e = lines.get(i);
			if (e.id().isEmpty()) continue;
			int text = font.width(taskName(e.id()));
			wanted = Math.max(wanted, i == 0
					? ICON_X + text + GAP + Math.max(PixelIcons.CHECK_W, font.width(counter(e))) + PAD
					: SMALL_X + text + PAD);
		}
		if (!snap.urgentId().isEmpty()) {
			wanted = Math.max(wanted, ICON_X + font.width(alertName(snap.urgentId())) + PAD);
		}
		return wanted;
	}

	private static int navWanted(Font font, HudSnapshot snap) {
		return snap.waypoint().map(w -> NAV_TEXT_X + font.width(navText(w, Minecraft.getInstance())) + PAD).orElse(0);
	}

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

	private static Component title(HudSnapshot snap) {
		int day = Math.min(snap.dayIndex() + 1, Math.max(1, snap.maxDays()));
		return Component.translatable("survivalassistant.hud.title", day, snap.maxDays());
	}

	private static Component taskName(String id) {
		return Component.translatable("survivalassistant.task." + id);
	}

	private static Component alertName(String id) {
		return Component.translatable("survivalassistant.alert." + id);
	}

	private static String counter(HudSnapshot.Entry e) {
		return e.target() > 0 ? e.value() + "/" + e.target() : "";
	}

	private static int clockWidth(Font font) {
		return 8 + 2 + BAR_W;
	}

	private static ItemStack clockIcon() {
		if (clockIcon == null) clockIcon = new ItemStack(Items.CLOCK);
		return clockIcon;
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
		g.fill(0, 0, width, height, BG);
		outline(g, 0, 0, width, height, border);

		drawHeader(g, mc, font, snap, now);
		snap.waypoint().ifPresent(w -> drawNav(g, mc, font, w, height - PAD - NAV_H + 2, now));

		int y = PAD + HEADER_H;
		if (full) {
			drawFullList(g, font, snap, y, now);
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
			drawCurrent(g, font, first, y, true, fade, now);
		} else if (first.id().isEmpty()) {
			HudText.fit(g, font, Component.translatable("survivalassistant.hud.all_done"), ICON_X, y + 6, width - ICON_X - PAD, GREEN, true, now);
		} else {
			drawCurrent(g, font, first, y + slide, false, 1f, now);
		}

		int rowY = y + CURRENT_H + slide;
		for (int i = 1; i < lines.size(); i++) {
			drawSmallRow(g, font, lines.get(i), rowY, GRAY, now);
			rowY += ROW_H;
		}
	}

	private void drawHeader(GuiGraphicsExtractor g, Minecraft mc, Font font, HudSnapshot snap, long now) {
		// sun/moon clock: 6 segments counting down to sunset (day) or sunrise (night)
		long time = Math.floorMod(mc.level.getOverworldClockTime(), 24000L);
		boolean night = time >= 13000 && time < 23000;
		boolean dusk = time >= 11000 && time < 13000;
		int color = night ? RED : dusk ? 0xFFFF8C1A : 0xFFFFD54F;
		long remaining = night ? 23000 - time : time >= 23000 ? 12000 + (24000 - time) : Math.max(0, 12000 - time);
		long span = night ? 10000 : 12000;
		int filled = (int) Math.ceil(Math.min(1.0, remaining / (double) span) * SEGMENTS);

		int barX = width - PAD - BAR_W;
		int barY = PAD + 2;
		for (int i = 0; i < SEGMENTS; i++) {
			int sx = barX + i * (SEG_W + SEG_GAP);
			g.fill(sx, barY, sx + SEG_W, barY + SEG_H, i < filled ? color : BAR_BG);
		}
		int clockX = barX - 8 - 2;
		smallIcon(g, clockIcon(), clockX, PAD);

		HudText.fit(g, font, title(snap), PAD, PAD + 1, clockX - GAP - PAD, GOLD, true, now);
	}

	private void drawCurrent(GuiGraphicsExtractor g, Font font, HudSnapshot.Entry e, int y, boolean done, float alpha, long now) {
		int a = Math.max(0x20, Math.round(alpha * 255)) << 24;
		ItemStack icon = TaskIcons.get(e.id());
		if (!icon.isEmpty()) g.item(icon, PAD, y + 1);

		int textColor = done ? (GREEN & 0xFFFFFF) | a : WHITE;
		int rightX;
		if (done) {
			rightX = width - PAD - PixelIcons.CHECK_W;
			PixelIcons.check(g, rightX, y + 3, textColor);
		} else {
			String right = counter(e);
			rightX = right.isEmpty() ? width - PAD + GAP : width - PAD - font.width(right);
			if (!right.isEmpty()) g.text(font, right, rightX, y + 2, GOLD, true);
		}
		int textMax = rightX - GAP - ICON_X;
		HudText.fit(g, font, taskName(e.id()), ICON_X, y + 2, textMax, textColor, true, now);

		// progress bar
		int bx0 = ICON_X, bx1 = width - PAD, by = y + 13;
		g.fill(bx0, by, bx1, by + 2, BAR_BG);
		float frac = done ? 1f : e.target() > 0 ? e.value() / (float) e.target() : 0f;
		if (frac > 0) g.fill(bx0, by, bx0 + Math.round((bx1 - bx0) * frac), by + 2, done ? (GREEN & 0xFFFFFF) | a : GOLD);
	}

	private void drawAlert(GuiGraphicsExtractor g, Font font, String alertId, int y, long now) {
		ItemStack icon = TaskIcons.get(alertId);
		if (!icon.isEmpty()) g.item(icon, PAD, y + 1);
		boolean blink = (now / 400) % 2 == 0;
		HudText.fit(g, font, alertName(alertId), ICON_X, y + 5, width - ICON_X - PAD, blink ? RED : 0xFFFFB0B0, true, now);
	}

	private void drawSmallRow(GuiGraphicsExtractor g, Font font, HudSnapshot.Entry e, int y, int color, long now) {
		smallIcon(g, TaskIcons.get(e.id()), PAD + 6, y);
		PixelIcons.dot(g, PAD, y + 4, DARK_GRAY);
		HudText.fit(g, font, taskName(e.id()), SMALL_X, y + 1, width - SMALL_X - PAD, color, true, now);
	}

	private void drawFullList(GuiGraphicsExtractor g, Font font, HudSnapshot snap, int y, long now) {
		Set<String> completed = new HashSet<>(snap.completed());
		String current = snap.next().isEmpty() ? "" : snap.next().get(0).id();
		for (Tip m : TipRegistry.MILESTONES) {
			boolean done = completed.contains(m.id());
			boolean isCurrent = m.id().equals(current);
			int color = done ? GREEN : isCurrent ? GOLD : GRAY;
			smallIcon(g, TaskIcons.get(m.id()), PAD + 9, y);
			if (done) PixelIcons.check(g, PAD, y + 2, color);
			else if (isCurrent) PixelIcons.play(g, PAD + 1, y + 1, color);
			else PixelIcons.dot(g, PAD + 2, y + 4, color);

			int textMax = width - FULL_X - PAD;
			if (isCurrent && !snap.next().isEmpty() && snap.next().get(0).target() > 0) {
				String count = counter(snap.next().get(0));
				int countX = width - PAD - font.width(count);
				g.text(font, count, countX, y + 1, GOLD, true);
				textMax = countX - GAP - FULL_X;
			}
			HudText.fit(g, font, taskName(m.id()), FULL_X, y + 1, textMax, color, true, now);
			y += ROW_H;
		}
	}

	// ------------------------------------------------------------------ navigation strip

	/** One line at the bottom of the card: rotating arrow, icon, and where to go (death spot or home). */
	private void drawNav(GuiGraphicsExtractor g, Minecraft mc, Font font, Navigation.Waypoint w, int y, long now) {
		boolean death = w.kind() == Navigation.Kind.DEATH;
		int color = death ? DEATH_COLOR : AQUA;
		g.fill(PAD, y - 2, width - PAD, y - 1, 0x40FFFFFF);

		if (sameDimension(mc, w)) {
			PixelIcons.arrow(g, PAD + 4, y + 4.5f, arrowAngle(mc, w), color);
		} else {
			PixelIcons.dot(g, PAD + 3, y + 4, color);
		}
		smallIcon(g, TaskIcons.get(death ? "nav_death" : "nav_home"), PAD + 11, y);
		HudText.fit(g, font, navText(w, mc), NAV_TEXT_X, y + 1, width - NAV_TEXT_X - PAD, color, true, now);
	}

	private static Component navText(Navigation.Waypoint w, Minecraft mc) {
		boolean same = sameDimension(mc, w);
		if (w.kind() == Navigation.Kind.DEATH) {
			int seconds = Math.max(0, w.ticksLeft() / 20);
			String time = (seconds / 60) + ":" + String.format("%02d", seconds % 60);
			return same
					? Component.translatable("survivalassistant.nav.death", distance(mc, w), time)
					: Component.translatable("survivalassistant.nav.death_other", dimensionName(w), time);
		}
		return Component.translatable("survivalassistant.nav.home", distance(mc, w));
	}

	private static boolean sameDimension(Minecraft mc, Navigation.Waypoint w) {
		return mc.level != null && mc.level.dimension().identifier().toString().equals(w.target().dimension());
	}

	private static int distance(Minecraft mc, Navigation.Waypoint w) {
		double dx = w.target().x() + 0.5 - mc.player.getX(), dz = w.target().z() + 0.5 - mc.player.getZ();
		return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
	}

	private static Component dimensionName(Navigation.Waypoint w) {
		String dim = w.target().dimension();
		String path = dim.substring(dim.indexOf(':') + 1);
		return Component.translatable("survivalassistant.nav.dim." + path);
	}

	/** Angle (radians, clockwise from "straight ahead") from the player's view direction to the target. */
	private static float arrowAngle(Minecraft mc, Navigation.Waypoint w) {
		double dx = w.target().x() + 0.5 - mc.player.getX(), dz = w.target().z() + 0.5 - mc.player.getZ();
		double yaw = Math.toRadians(mc.player.getYRot());
		double lookX = -Math.sin(yaw), lookZ = Math.cos(yaw);
		double forward = dx * lookX + dz * lookZ;
		double right = dx * -lookZ + dz * lookX;
		return (float) Math.atan2(right, forward);
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
