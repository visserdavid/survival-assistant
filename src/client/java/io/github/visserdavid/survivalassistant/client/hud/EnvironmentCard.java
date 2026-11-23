package io.github.visserdavid.survivalassistant.client.hud;

import io.github.visserdavid.survivalassistant.advice.BiomeGroup;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.OreTable;
import io.github.visserdavid.survivalassistant.state.HudState;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Small card under (or above) the task card: current biome with one pro and one con, ores at this height,
 * a pointer to the best height for the current task, and a depth hazard. Computed entirely on the client.
 *
 * <p>It shares its width with the task card so both line up. Long lines wrap (up to 3 lines); a long biome
 * name scrolls inside its slot instead of overlapping the Y-level.
 */
public final class EnvironmentCard implements HudElement {
	private static final int PAD = 4;
	private static final int LINE_H = 10;
	private static final int HEADER_H = 14;
	private static final int NAME_X = PAD + 12;
	private static final int GAP = 6;
	private static final int MAX_WRAP_LINES = 3;
	/** Text indent after a small item icon at the start of a line. */
	private static final int ICON_INDENT = 10;

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
	private static final Map<Item, ItemStack> STACKS = new HashMap<>();

	private String lastBiome = null;
	private long biomeChangedAt = 0;
	private int width = HudText.MIN_WIDTH;

	/** A piece of content before wrapping (optionally led by an item icon), and a wrapped line ready to draw. */
	private record Entry(Component text, int color, ItemStack icon) {
		Entry(Component text, int color) {
			this(text, color, ItemStack.EMPTY);
		}

		int indent() {
			return icon.isEmpty() ? 0 : ICON_INDENT;
		}
	}

	private record Line(FormattedCharSequence text, int color, ItemStack icon, int indent) {}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		HudConfig cfg = HudConfig.get();
		HudSnapshot snap = mc.player == null ? null : mc.player.getAttached(HudState.HUD);
		if (!cfg.showEnvironment || mc.player == null || mc.level == null || snap == null // snap == null: server without this mod
				|| mc.gui.hud.isHidden() || mc.getDebugOverlay().showDebugScreen()) {
			HudText.environmentWanted = 0;
			return;
		}

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

		Component name = biomeName(biomeId);
		String yText = "Y " + y;
		List<Entry> entries = buildEntries(snap, group, y, cfg.envLines == HudConfig.EnvLines.FULL);

		// The width at which nothing wraps or scrolls; the task card takes this into account next frame.
		int wanted = NAME_X + font.width(name) + GAP + font.width("Y -64") + PAD;
		for (Entry e : entries) wanted = Math.max(wanted, PAD + e.indent() + font.width(e.text()) + PAD);
		HudText.environmentWanted = wanted;
		int[] task = TaskHud.visibleBounds;
		width = task != null ? TaskHud.cardWidth : HudText.clampWidth(wanted);

		List<Line> lines = wrap(font, entries);
		int height = PAD + HEADER_H + lines.size() * LINE_H + PAD - 2;

		float scale = cfg.scale;
		int sw = Math.round(width * scale), sh = Math.round(height * scale);
		boolean top = cfg.corner == HudConfig.Corner.TOP_LEFT || cfg.corner == HudConfig.Corner.TOP_RIGHT;
		boolean left = cfg.corner == HudConfig.Corner.TOP_LEFT || cfg.corner == HudConfig.Corner.BOTTOM_LEFT;
		int x = left ? cfg.margin : g.guiWidth() - sw - cfg.margin;
		int sy;
		if (task != null) {
			sy = top ? task[1] + task[3] + 2 : task[1] - sh - 2;
		} else {
			sy = top ? cfg.margin : g.guiHeight() - sh - cfg.margin - 52;
		}

		g.pose().pushMatrix();
		g.pose().translate(x, sy);
		g.pose().scale(scale, scale);
		draw(g, font, name, yText, group, y, lines, height, now);
		g.pose().popMatrix();
	}

	private static Component biomeName(String biomeId) {
		Identifier id = Identifier.parse(biomeId);
		return Component.translatable("biome." + id.getNamespace() + "." + id.getPath());
	}

	private List<Entry> buildEntries(HudSnapshot snap, BiomeGroup group, int y, boolean full) {
		List<Entry> entries = new ArrayList<>();
		if (full) {
			entries.add(new Entry(Component.literal("+ ").append(Component.translatable("survivalassistant.biome." + group.key() + ".pro")), GREEN));
			entries.add(new Entry(Component.literal("- ").append(Component.translatable("survivalassistant.biome." + group.key() + ".con")), RED));
		}

		if (group.isOverworld()) {
			List<OreTable.Found> ores = OreTable.oresAt(y, group, 4);
			MutableComponent oreLine = Component.translatable("survivalassistant.env.here").withColor(GRAY).append(" ");
			if (ores.isEmpty()) {
				oreLine.append(Component.translatable("survivalassistant.env.no_ores").withColor(GRAY));
			}
			for (int i = 0; i < ores.size(); i++) {
				OreTable.Found f = ores.get(i);
				if (i > 0) oreLine.append(Component.literal(" · ").withColor(GRAY));
				MutableComponent oreName = Component.translatable("survivalassistant.ore." + f.id());
				// best height: gold and bold (no special glyphs, they may be missing from the font)
				oreLine.append(f.best() ? oreName.withColor(GOLD).withStyle(s -> s.withBold(true)) : oreName.withColor(WHITE));
			}
			entries.add(new Entry(oreLine, WHITE, stack(Items.IRON_PICKAXE)));

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
					entries.add(new Entry(text, AQUA, stack(oreItem(p.oreId()))));
				}
				String hazard = OreTable.hazardAt(y, group);
				if (hazard != null) {
					entries.add(new Entry(Component.translatable("survivalassistant.env.hazard." + hazard), ORANGE, stack(hazardItem(hazard))));
				}
			}
		}
		return entries;
	}

	private List<Line> wrap(Font font, List<Entry> entries) {
		List<Line> lines = new ArrayList<>();
		for (Entry e : entries) {
			List<FormattedCharSequence> parts = font.split(e.text(), width - 2 * PAD - e.indent());
			for (int i = 0; i < Math.min(MAX_WRAP_LINES, parts.size()); i++) {
				lines.add(new Line(parts.get(i), e.color(), i == 0 ? e.icon() : ItemStack.EMPTY, e.indent()));
			}
		}
		return lines;
	}

	private void draw(GuiGraphicsExtractor g, Font font, Component name, String yText, BiomeGroup group, int y,
			List<Line> lines, int height, long now) {
		g.fill(0, 0, width, height, BG);
		int border = BORDER;
		long since = now - biomeChangedAt;
		if (biomeChangedAt > 0 && since < GLOW_MS) {
			float t = 1 - since / (float) GLOW_MS;
			border = lerp(BORDER, GLOW, t);
			g.fill(1, 1, width - 1, HEADER_H + PAD - 1, (Math.round(t * 0x40) << 24) | 0xFFE066);
		}
		g.fill(0, 0, width, 1, border);
		g.fill(0, height - 1, width, height, border);
		g.fill(0, 0, 1, height, border);
		g.fill(width - 1, 0, width, height, border);

		// header: icon, biome name (scrolls if too long), Y
		smallIcon(g, icon(group), PAD, PAD + 1);
		int yX = width - PAD - font.width(yText);
		g.text(font, yText, yX, PAD + 1, y < OreTable.DEEPSLATE_LEVEL ? AQUA : GRAY, true);
		HudText.fit(g, font, name, NAME_X, PAD + 1, yX - GAP - NAME_X, WHITE, true, now);

		int ly = PAD + HEADER_H;
		for (Line line : lines) {
			smallIcon(g, line.icon(), PAD, ly);
			g.text(font, line.text(), PAD + line.indent(), ly, line.color(), true);
			ly += LINE_H;
		}
	}

	private static Item oreItem(String oreId) {
		return switch (oreId) {
			case "coal" -> Items.COAL;
			case "diamond" -> Items.DIAMOND;
			default -> Items.RAW_IRON;
		};
	}

	private static Item hazardItem(String hazard) {
		return switch (hazard) {
			case "deep_dark" -> Items.SCULK_SHRIEKER;
			case "lava" -> Items.LAVA_BUCKET;
			default -> Items.DEEPSLATE;
		};
	}

	private static ItemStack stack(Item item) {
		return STACKS.computeIfAbsent(item, ItemStack::new);
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
