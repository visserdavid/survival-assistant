package io.github.visserdavid.survivalassistant.client.hud;

import io.github.visserdavid.survivalassistant.advice.Chapters;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.Navigation;
import io.github.visserdavid.survivalassistant.advice.Recipes;
import io.github.visserdavid.survivalassistant.advice.SafeSignal;
import io.github.visserdavid.survivalassistant.advice.SurvivalContext;
import io.github.visserdavid.survivalassistant.advice.Tip;
import io.github.visserdavid.survivalassistant.state.HudState;
import io.github.visserdavid.survivalassistant.util.Ticks;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.github.visserdavid.survivalassistant.client.hud.HudColors.*;
import static io.github.visserdavid.survivalassistant.client.hud.HudDraw.SMALL_ICON;
import static io.github.visserdavid.survivalassistant.client.hud.HudDraw.smallIcon;

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
	private static final int RECIPE_SLOT = 9;     // half-size grid in the full view
	private static final int RECIPE_H = 3 + 3 * RECIPE_SLOT + 2;
	/** The widest counter in the full list. */
	private static final String WIDEST_COUNTER = "99/99";

	// clock: clock item (shows the sun/moon itself) + 6 segments counting down to sunset or sunrise
	private static final int SEG_W = 4, SEG_GAP = 1, SEG_H = 5, SEGMENTS = 6;
	private static final int BAR_W = SEGMENTS * (SEG_W + SEG_GAP) - SEG_GAP;
	private static final int CLOCK_W = SMALL_ICON + 2 + BAR_W;
	private static final int DAY_SPAN = 12000, NIGHT_SPAN = SurvivalContext.NIGHT_END - SurvivalContext.NIGHT_START;

	private static final long FLASH_MS = 1500;
	private static final long SLIDE_MS = 250;
	/** The urgent border pulses with this period (radians per millisecond: sin(now / 150)). */
	private static final double PULSE_MS = 150.0;
	private static final long BLINK_MS = 400;


	private final HudLayout layout;

	// --- completion animation state ---
	private @Nullable Set<String> knownCompleted;
	/** Chapter and world of {@link #knownCompleted}: a switch starts a fresh comparison (no false flashes). */
	private int knownChapter = -1;
	private @Nullable Object knownLevel;
	private @Nullable String flashId;
	private long flashStart;

	// --- worked out once per snapshot instead of every frame ---
	private @Nullable HudSnapshot cachedSnap;
	private Set<String> completed = Set.of();
	private Optional<Recipes.Recipe> recipe = Optional.empty();
	private Component title = Component.empty();
	private final Map<String, Component> names = new HashMap<>();
	/** Compact lines for {@link #cachedSnap} and the flash shown with them. */
	private List<HudSnapshot.Entry> lines = List.of();
	private @Nullable String linesFlash;

	/** Width of the card being drawn this frame. */
	private int width = HudText.MIN_WIDTH;

	public TaskHud(HudLayout layout) {
		this.layout = layout;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		layout.taskHidden();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		ClientConfig cfg = ClientConfig.get();
		if (cfg.mode == ClientConfig.Mode.HIDDEN || HudDraw.hidden(mc)) return;

		HudSnapshot snap = mc.player.getAttached(HudState.HUD);
		if (snap == null || !snap.active()) {
			knownCompleted = null;
			return;
		}
		long now = System.currentTimeMillis();
		refresh(snap);
		trackCompletion(snap, mc.level, now);
		updateLines(snap, now);

		boolean full = cfg.mode == ClientConfig.Mode.FULL;
		width = HudText.clampWidth(Math.max(Math.max(wantedWidth(mc.font, snap, full), navWanted(mc, snap)),
				layout.environmentWanted));
		int height = (full ? fullHeight(snap) : compactHeight(snap)) + (snap.waypoint().isPresent() ? NAV_H : 0);
		float scale = cfg.scale;
		int sw = Math.round(width * scale), sh = Math.round(height * scale);
		int x = HudDraw.cornerX(cfg, g.guiWidth(), sw);
		int y = HudDraw.cornerY(cfg, g.guiHeight(), sh);

		layout.taskShown(x, y, sw, sh, width);
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		drawCard(g, mc, snap, height, full, now);
		g.pose().popMatrix();
	}

	// ------------------------------------------------------------------ state

	/** Works out what only changes with the snapshot (or the language). */
	private void refresh(HudSnapshot snap) {
		if (snap == cachedSnap) return;
		cachedSnap = snap;
		completed = Set.copyOf(snap.completed());
		recipe = snap.next().isEmpty() ? Optional.empty() : Recipes.forTask(snap.next().getFirst().id());
		title = title(snap);
		linesFlash = null;
		lines = List.of();
	}

	private void trackCompletion(HudSnapshot snap, Object level, long now) {
		if (snap.chapter() != knownChapter || level != knownLevel) {
			knownCompleted = null;
			knownChapter = snap.chapter();
			knownLevel = level;
		}
		if (knownCompleted != null && knownCompleted != completed) {
			for (Tip m : Chapters.get(snap.chapter()).goals()) {
				if (completed.contains(m.id()) && !knownCompleted.contains(m.id())) {
					flashId = m.id();
					flashStart = now;
				}
			}
		}
		knownCompleted = completed;
	}

	private boolean flashing(long now) {
		return flashId != null && now - flashStart < FLASH_MS;
	}

	/** Compact lines: optionally the task that was just completed (flashing), then the open tasks. */
	private void updateLines(HudSnapshot snap, long now) {
		String flash = flashing(now) ? flashId : null;
		if (!lines.isEmpty() && Objects.equals(flash, linesFlash)) return;
		List<HudSnapshot.Entry> out = new ArrayList<>(HudSnapshot.SHOWN_TASKS);
		if (flash != null) out.add(new HudSnapshot.Entry(flash, 1, 0));
		for (HudSnapshot.Entry e : snap.next()) {
			if (out.size() >= HudSnapshot.SHOWN_TASKS) break;
			out.add(e);
		}
		if (out.isEmpty()) out.add(new HudSnapshot.Entry("", 0, 0)); // "all done"
		lines = List.copyOf(out);
		linesFlash = flash;
	}

	// ------------------------------------------------------------------ layout

	/** The width at which nothing on the card would need to scroll. */
	private int wantedWidth(Font font, HudSnapshot snap, boolean full) {
		int wanted = PAD + font.width(title) + GAP + CLOCK_W + PAD;
		if (full) {
			for (Tip m : Chapters.get(snap.chapter()).goals()) {
				wanted = Math.max(wanted, FULL_X + font.width(taskName(m.id())) + GAP + font.width(WIDEST_COUNTER) + PAD);
			}
			if (recipe.isPresent()) {
				wanted = Math.max(wanted, FULL_X + RecipeView.width(RECIPE_SLOT) + RecipeView.countLabelWidth(font, recipe.get(), RECIPE_SLOT) + PAD);
			}
			return wanted;
		}
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
		if (showSafe(snap)) {
			wanted = Math.max(wanted, SMALL_X + font.width(alertName(SafeSignal.ID)) + PAD);
		}
		return wanted;
	}

	private static int navWanted(Minecraft mc, HudSnapshot snap) {
		return snap.waypoint().map(w -> NAV_TEXT_X + mc.font.width(navText(w, mc)) + PAD).orElse(0);
	}

	private int compactHeight(HudSnapshot snap) {
		int rows = Math.max(0, lines.size() - 1) + (showSafe(snap) ? 1 : 0);
		return PAD + HEADER_H + CURRENT_H + rows * ROW_H + PAD;
	}

	private int fullHeight(HudSnapshot snap) {
		return PAD + HEADER_H + Chapters.get(snap.chapter()).goals().size() * ROW_H + PAD + (recipe.isPresent() ? RECIPE_H : 0);
	}

	/** The green "well lit, safe here" row under the tasks (compact card only); a warning always wins. */
	private static boolean showSafe(HudSnapshot snap) {
		return snap.safe() && snap.urgentId().isEmpty();
	}

	private static Component title(HudSnapshot snap) {
		return Component.translatable("survivalassistant.hud.title_chapter", snap.chapter(),
				Component.translatable(Chapters.get(snap.chapter()).titleKey()));
	}

	private Component taskName(String id) {
		return names.computeIfAbsent("survivalassistant.task." + id, Component::translatable);
	}

	private Component alertName(String id) {
		return names.computeIfAbsent("survivalassistant.alert." + id, Component::translatable);
	}

	private static String counter(HudSnapshot.Entry e) {
		return e.target() > 0 ? e.value() + "/" + e.target() : "";
	}

	// ------------------------------------------------------------------ drawing

	private void drawCard(GuiGraphicsExtractor g, Minecraft mc, HudSnapshot snap, int height, boolean full, long now) {
		Font font = mc.font;
		boolean urgent = !snap.urgentId().isEmpty();
		int border = urgent ? lerp(DARK_RED, RED, 0.5f + 0.5f * (float) Math.sin(now / PULSE_MS)) : BORDER_TASK;
		HudDraw.card(g, 0, 0, width, height, BG, border);

		drawHeader(g, mc, font, now);
		snap.waypoint().ifPresent(w -> drawNav(g, mc, font, w, height - PAD - NAV_H + 2, now));

		int y = PAD + HEADER_H;
		if (full) drawFullList(g, font, snap, y, now);
		else drawCompact(g, font, snap, y, urgent, now);
	}

	private void drawCompact(GuiGraphicsExtractor g, Font font, HudSnapshot snap, int y, boolean urgent, long now) {
		HudSnapshot.Entry first = lines.getFirst();
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
			HudText.fit(g, font, names.computeIfAbsent("survivalassistant.hud.all_done", Component::translatable),
					ICON_X, y + 6, width - ICON_X - PAD, GREEN, true, now);
		} else {
			drawCurrent(g, font, first, y + slide, false, 1f, now);
		}

		int rowY = y + CURRENT_H + slide;
		for (int i = 1; i < lines.size(); i++) {
			drawSmallRow(g, font, lines.get(i), rowY, now);
			rowY += ROW_H;
		}
		if (showSafe(snap)) drawSafeRow(g, font, rowY, now);
	}

	private void drawSafeRow(GuiGraphicsExtractor g, Font font, int y, long now) {
		smallIcon(g, TaskIcons.get(SafeSignal.ID), PAD + 6, y);
		PixelIcons.dot(g, PAD, y + 4, GREEN);
		HudText.fit(g, font, alertName(SafeSignal.ID), SMALL_X, y + 1, width - SMALL_X - PAD, GREEN, true, now);
	}

	/** The title and the sun/moon clock: 6 segments counting down to sunset (day) or sunrise (night). */
	private void drawHeader(GuiGraphicsExtractor g, Minecraft mc, Font font, long now) {
		int time = Ticks.timeOfDay(mc.level.getOverworldClockTime());
		boolean night = time >= SurvivalContext.NIGHT_START && time < SurvivalContext.NIGHT_END;
		boolean dusk = time >= SurvivalContext.SUNSET && time < SurvivalContext.NIGHT_START;
		int color = night ? CLOCK_NIGHT : dusk ? CLOCK_DUSK : CLOCK_DAY;
		long remaining = night ? SurvivalContext.NIGHT_END - time
				: time >= SurvivalContext.NIGHT_END ? DAY_SPAN + (Ticks.PER_DAY - time) : Math.max(0, DAY_SPAN - time);
		long span = night ? NIGHT_SPAN : DAY_SPAN;
		int filled = (int) Math.ceil(Math.min(1.0, remaining / (double) span) * SEGMENTS);

		int barX = width - PAD - BAR_W;
		int barY = PAD + 2;
		for (int i = 0; i < SEGMENTS; i++) {
			int sx = barX + i * (SEG_W + SEG_GAP);
			g.fill(sx, barY, sx + SEG_W, barY + SEG_H, i < filled ? color : BAR_BG);
		}
		int clockX = barX - SMALL_ICON - 2;
		smallIcon(g, ItemIcons.of(Items.CLOCK), clockX, PAD);

		HudText.fit(g, font, title, PAD, PAD + 1, clockX - GAP - PAD, GOLD, true, now);
	}

	private void drawCurrent(GuiGraphicsExtractor g, Font font, HudSnapshot.Entry e, int y, boolean done, float alpha, long now) {
		ItemStack icon = TaskIcons.get(e.id());
		if (!icon.isEmpty()) g.item(icon, PAD, y + 1);

		int textColor = done ? withAlpha(GREEN, alpha) : WHITE;
		int rightX;
		if (done) {
			rightX = width - PAD - PixelIcons.CHECK_W;
			PixelIcons.check(g, rightX, y + 3, textColor);
		} else {
			String right = counter(e);
			rightX = right.isEmpty() ? width - PAD + GAP : width - PAD - font.width(right);
			if (!right.isEmpty()) g.text(font, right, rightX, y + 2, GOLD, true);
		}
		HudText.fit(g, font, taskName(e.id()), ICON_X, y + 2, rightX - GAP - ICON_X, textColor, true, now);

		// progress bar
		int bx0 = ICON_X, bx1 = width - PAD, by = y + 13;
		g.fill(bx0, by, bx1, by + 2, BAR_BG);
		float frac = done ? 1f : e.target() > 0 ? e.value() / (float) e.target() : 0f;
		if (frac > 0) g.fill(bx0, by, bx0 + Math.round((bx1 - bx0) * frac), by + 2, done ? textColor : GOLD);
	}

	private void drawAlert(GuiGraphicsExtractor g, Font font, String alertId, int y, long now) {
		ItemStack icon = TaskIcons.get(alertId);
		if (!icon.isEmpty()) g.item(icon, PAD, y + 1);
		boolean blink = (now / BLINK_MS) % 2 == 0;
		HudText.fit(g, font, alertName(alertId), ICON_X, y + 5, width - ICON_X - PAD, blink ? RED : PALE_RED, true, now);
	}

	private void drawSmallRow(GuiGraphicsExtractor g, Font font, HudSnapshot.Entry e, int y, long now) {
		smallIcon(g, TaskIcons.get(e.id()), PAD + 6, y);
		PixelIcons.dot(g, PAD, y + 4, DARK_GRAY);
		HudText.fit(g, font, taskName(e.id()), SMALL_X, y + 1, width - SMALL_X - PAD, GRAY, true, now);
	}

	private void drawFullList(GuiGraphicsExtractor g, Font font, HudSnapshot snap, int y, long now) {
		HudSnapshot.Entry current = snap.next().isEmpty() ? null : snap.next().getFirst();
		for (Tip m : Chapters.get(snap.chapter()).goals()) {
			boolean done = completed.contains(m.id());
			boolean isCurrent = current != null && m.id().equals(current.id());
			int color = done ? GREEN : isCurrent ? GOLD : GRAY;
			smallIcon(g, TaskIcons.get(m.id()), PAD + 9, y);
			if (done) PixelIcons.check(g, PAD, y + 2, color);
			else if (isCurrent) PixelIcons.play(g, PAD + 1, y + 1, color);
			else PixelIcons.dot(g, PAD + 2, y + 4, color);

			int textMax = width - FULL_X - PAD;
			if (isCurrent && current.target() > 0) {
				String count = counter(current);
				int countX = width - PAD - font.width(count);
				g.text(font, count, countX, y + 1, GOLD, true);
				textMax = countX - GAP - FULL_X;
			}
			HudText.fit(g, font, taskName(m.id()), FULL_X, y + 1, textMax, color, true, now);
			y += ROW_H;
		}
		if (recipe.isPresent()) {
			g.fill(PAD, y + 1, width - PAD, y + 2, SEPARATOR);
			PixelIcons.play(g, PAD + 1, y + 3 + RECIPE_SLOT, GOLD);
			RecipeView.draw(g, font, recipe.get(), FULL_X, y + 3, RECIPE_SLOT);
		}
	}

	// ------------------------------------------------------------------ navigation strip

	/** One line at the bottom of the card: rotating arrow, icon, and where to go (death spot or home). */
	private void drawNav(GuiGraphicsExtractor g, Minecraft mc, Font font, Navigation.Waypoint w, int y, long now) {
		boolean death = w.kind() == Navigation.Kind.DEATH;
		int color = death ? SALMON : AQUA;
		g.fill(PAD, y - 2, width - PAD, y - 1, SEPARATOR);

		if (sameDimension(mc, w)) {
			PixelIcons.arrow(g, PAD + 4, y + 4.5f, arrowAngle(mc, w), color);
		} else {
			PixelIcons.dot(g, PAD + 3, y + 4, color);
		}
		smallIcon(g, TaskIcons.get(death ? TaskIcons.NAV_DEATH : TaskIcons.NAV_HOME), PAD + 11, y);
		HudText.fit(g, font, navText(w, mc), NAV_TEXT_X, y + 1, width - NAV_TEXT_X - PAD, color, true, now);
	}

	private static Component navText(Navigation.Waypoint w, Minecraft mc) {
		boolean same = sameDimension(mc, w);
		if (w.kind() == Navigation.Kind.DEATH) {
			int seconds = Math.max(0, w.ticksLeft() / Ticks.PER_SECOND);
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
}
