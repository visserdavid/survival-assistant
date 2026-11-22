package io.github.visserdavid.survivalassistant.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import io.github.visserdavid.survivalassistant.advice.BiomeGroup;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.OreTable;
import io.github.visserdavid.survivalassistant.state.HudState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Small card under (or above) the task card: current biome with one pro and one con, ores at this height,
 * a pointer to the best height for the current task, and a depth hazard. Computed entirely on the client.
 */
public final class EnvironmentCard implements HudElement {
	private static final int WIDTH = 150;
	private static final int PAD = 4;
	private static final int LINE_H = 10;
	private static final int HEADER_H = 14;
	private static final int TEXT_W = WIDTH - 2 * PAD;

	private static final int BG = 0xB0101010;
	private static final int BORDER = 0xFF3E5A6B;
	private static final int GLOW = 0xFFFFE066;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFB0B0B0;
	private static final int GREEN = 0xFF7BE07B;
	private static final int RED = 0xFFFF7070;
	private static final int GOLD = 0xFFFFC94D;
	private static final int AQUA = 0xFF6FD3E8;
	private static final int ORANGE = 0xFFFFA040;

	private static final long GLOW_MS = 2000;
	private static final Map<BiomeGroup, ItemStack> ICONS = new EnumMap<>(BiomeGroup.class);

	private String lastBiome = null;
	private long biomeChangedAt = 0;

	/** A line of text with its colour. */
	private record Line(FormattedCharSequence text, int color) {}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		HudConfig cfg = HudConfig.get();
		if (!cfg.showEnvironment || mc.player == null || mc.level == null) return;
		if (mc.gui.hud.isHidden() || mc.getDebugOverlay().showDebugScreen()) return;
		HudSnapshot snap = mc.player.getAttached(HudState.HUD);
		if (snap == null) return; // server without this mod

		Font font = mc.font;
		String biomeId = mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
				.map(k -> k.identifier().toString()).orElse("minecraft:plains");
		BiomeGroup group = BiomeGroup.of(biomeId);
		int y = mc.player.getBlockY();
		long now = System.currentTimeMillis();
		if (!biomeId.equals(lastBiome)) {
			if (lastBiome != null) biomeChangedAt = now;
			lastBiome = biomeId;
		}

		List<Line> lines = buildLines(font, snap, group, y, cfg.envLines == HudConfig.EnvLines.FULL);
		int height = PAD + HEADER_H + lines.size() * LINE_H + PAD - 2;

		float scale = cfg.scale;
		int sw = Math.round(WIDTH * scale), sh = Math.round(height * scale);
		boolean top = cfg.corner == HudConfig.Corner.TOP_LEFT || cfg.corner == HudConfig.Corner.TOP_RIGHT;
		boolean left = cfg.corner == HudConfig.Corner.TOP_LEFT || cfg.corner == HudConfig.Corner.BOTTOM_LEFT;
		int x = left ? cfg.margin : g.guiWidth() - sw - cfg.margin;
		int sy;
		int[] task = TaskHud.visibleBounds;
		if (task != null) {
			sy = top ? task[1] + task[3] + 2 : task[1] - sh - 2;
		} else {
			sy = top ? cfg.margin : g.guiHeight() - sh - cfg.margin - 52;
		}

		g.pose().pushMatrix();
		g.pose().translate(x, sy);
		g.pose().scale(scale, scale);
		draw(g, font, biomeId, group, y, lines, height, now);
		g.pose().popMatrix();
	}

	private List<Line> buildLines(Font font, HudSnapshot snap, BiomeGroup group, int y, boolean full) {
		List<Line> lines = new ArrayList<>();
		if (full) {
			addWrapped(lines, font, Component.literal("+ ").append(Component.translatable("survivalassistant.biome." + group.key() + ".pro")), GREEN);
			addWrapped(lines, font, Component.literal("− ").append(Component.translatable("survivalassistant.biome." + group.key() + ".con")), RED);
		}

		if (group.isOverworld()) {
			List<OreTable.Found> ores = OreTable.oresAt(y, group, 4);
			MutableComponent oreLine = Component.literal("⛏ ").withColor(GRAY)
					.append(Component.translatable("survivalassistant.env.here").withColor(GRAY)).append(" ");
			if (ores.isEmpty()) {
				oreLine.append(Component.translatable("survivalassistant.env.no_ores").withColor(GRAY));
			}
			for (int i = 0; i < ores.size(); i++) {
				OreTable.Found f = ores.get(i);
				if (i > 0) oreLine.append(Component.literal(" · ").withColor(GRAY));
				MutableComponent name = Component.translatable("survivalassistant.ore." + f.id());
				oreLine.append(f.best() ? Component.literal("★").withColor(GOLD).append(name.withColor(GOLD)) : name.withColor(WHITE));
			}
			addWrapped(lines, font, oreLine, WHITE);

			if (full) {
				String task = !snap.active() ? null : snap.next().isEmpty() ? "" : snap.next().get(0).id();
				OreTable.Pointer p = OreTable.pointerFor(task, y, group);
				if (p != null) {
					Component ore = Component.translatable("survivalassistant.ore." + p.oreId());
					Component text = switch (p.direction()) {
						case UP -> Component.translatable("survivalassistant.env.pointer.up", ore, p.targetY());
						case DOWN -> Component.translatable("survivalassistant.env.pointer.down", ore, p.targetY());
						case HERE -> Component.translatable("survivalassistant.env.pointer.here", ore);
						case MOUNTAIN -> Component.translatable("survivalassistant.env.pointer.mountain", ore);
					};
					addWrapped(lines, font, text, AQUA);
				}
				String hazard = OreTable.hazardAt(y, group);
				if (hazard != null) {
					addWrapped(lines, font, Component.literal("⚠ ").append(Component.translatable("survivalassistant.env.hazard." + hazard)), ORANGE);
				}
			}
		}
		return lines;
	}

	private static void addWrapped(List<Line> lines, Font font, Component text, int color) {
		List<FormattedCharSequence> parts = font.split(text, TEXT_W);
		for (int i = 0; i < Math.min(2, parts.size()); i++) lines.add(new Line(parts.get(i), color));
	}

	private void draw(GuiGraphicsExtractor g, Font font, String biomeId, BiomeGroup group, int y, List<Line> lines, int height, long now) {
		g.fill(0, 0, WIDTH, height, BG);
		int border = BORDER;
		long since = now - biomeChangedAt;
		if (biomeChangedAt > 0 && since < GLOW_MS) {
			float t = 1 - since / (float) GLOW_MS;
			border = lerp(BORDER, GLOW, t);
			g.fill(1, 1, WIDTH - 1, HEADER_H + PAD - 1, (Math.round(t * 0x40) << 24) | 0xFFE066);
		}
		g.fill(0, 0, WIDTH, 1, border);
		g.fill(0, height - 1, WIDTH, height, border);
		g.fill(0, 0, 1, height, border);
		g.fill(WIDTH - 1, 0, WIDTH, height, border);

		// header: icon, biome name, Y
		smallIcon(g, icon(group), PAD, PAD + 1);
		Identifier id = Identifier.parse(biomeId);
		Component name = Component.translatable("biome." + id.getNamespace() + "." + id.getPath());
		String yText = "Y " + y;
		int nameMax = WIDTH - 2 * PAD - 12 - font.width(yText) - 4;
		List<FormattedCharSequence> nameParts = font.split(name, nameMax);
		if (!nameParts.isEmpty()) g.text(font, nameParts.get(0), PAD + 12, PAD + 1, WHITE, true);
		g.text(font, yText, WIDTH - PAD - font.width(yText), PAD + 1, y < OreTable.DEEPSLATE_LEVEL ? AQUA : GRAY, true);

		int ly = PAD + HEADER_H;
		for (Line line : lines) {
			g.text(font, line.text(), PAD, ly, line.color(), true);
			ly += LINE_H;
		}
	}

	private static ItemStack icon(BiomeGroup group) {
		return ICONS.computeIfAbsent(group, gr -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(gr.icon()))));
	}

	private static void smallIcon(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
		if (stack.isEmpty()) return;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(0.5f, 0.5f);
		g.item(stack, 0, 0);
		g.pose().popMatrix();
	}

	private static int lerp(int a, int b, float t) {
		int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return 0xFF000000 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
	}
}
