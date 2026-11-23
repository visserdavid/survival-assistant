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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static io.github.visserdavid.survivalassistant.client.hud.HudColors.*;
import static io.github.visserdavid.survivalassistant.client.hud.HudDraw.smallIcon;

/**
 * Small card under (or above) the task card: current biome with one pro and one con, ores at this height,
 * a pointer to the best height for the current task, and a depth hazard. Computed entirely on the client.
 *
 * <p>It shares its width with the task card so both line up. Long lines wrap (up to 3 lines); a long biome
 * name scrolls inside its slot instead of overlapping the Y-level. The lines are only rebuilt when something
 * on the card changes (biome, height, task, width or language), not every frame.
 */
public final class EnvironmentCard implements HudElement {
	private static final int PAD = 4;
	private static final int LINE_H = 10;
	private static final int HEADER_H = 14;
	private static final int NAME_X = PAD + 12;
	private static final int GAP = 6;
	private static final int MAX_WRAP_LINES = 3;
	/** Ores listed on the "here" line. */
	private static final int MAX_ORES = 4;
	/** Text indent after a small item icon at the start of a line. */
	private static final int ICON_INDENT = 10;
	/** The widest height label, for the wanted width. */
	private static final String WIDEST_Y = "Y -64";
	/** Strongest alpha of the header glow after entering a new biome. */
	private static final int GLOW_ALPHA = 0x40;
	private static final long GLOW_MS = 2000;
	private static final String FALLBACK_BIOME = "minecraft:plains";

	private final HudLayout layout;

	private @Nullable String lastBiome;
	private long biomeChangedAt;
	private int width = HudText.MIN_WIDTH;

	/** What the content depends on; when it is unchanged, the cached entries and lines are drawn again. */
	private record Key(String biomeId, int y, boolean full, @Nullable String task, String language) {}

	private @Nullable Key key;
	private Component name = Component.empty();
	private String yText = "";
	private List<Entry> entries = List.of();
	private int wanted;
	private int wrappedWidth = -1;
	private List<Line> lines = List.of();

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

	public EnvironmentCard(HudLayout layout) {
		this.layout = layout;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		ClientConfig cfg = ClientConfig.get();
		HudSnapshot snap = mc.player == null ? null : mc.player.getAttached(HudState.HUD);
		// snap == null: a server without this mod
		if (!cfg.showEnvironment || mc.player == null || mc.level == null || snap == null || HudDraw.hidden(mc)) {
			layout.environmentWanted = 0;
			layout.envHidden();
			return;
		}

		String biomeId = mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
				.map(k -> k.identifier().toString()).orElse(FALLBACK_BIOME);
		BiomeGroup group = BiomeGroup.of(biomeId);
		int y = mc.player.getBlockY();
		long now = System.currentTimeMillis();
		if (!biomeId.equals(lastBiome)) {
			if (lastBiome != null) biomeChangedAt = now;
			lastBiome = biomeId;
		}

		Font font = mc.font;
		update(font, new Key(biomeId, y, cfg.envLines == ClientConfig.EnvLines.FULL, task(snap),
				mc.getLanguageManager().getSelected()), group);
		// The width at which nothing wraps or scrolls; the task card takes this into account next frame.
		layout.environmentWanted = wanted;
		width = layout.taskVisible ? layout.taskCardWidth : HudText.clampWidth(wanted);
		wrap(font);
		int height = PAD + HEADER_H + lines.size() * LINE_H + PAD - 2;

		float scale = cfg.scale;
		int sw = Math.round(width * scale), sh = Math.round(height * scale);
		int x = HudDraw.cornerX(cfg, g.guiWidth(), sw);
		boolean top = HudDraw.topCorner(cfg);
		int sy = !layout.taskVisible ? HudDraw.cornerY(cfg, g.guiHeight(), sh)
				: top ? layout.taskY + layout.taskH + 2 : layout.taskY - sh - 2;

		layout.envShown(x, sy, sw, sh, width);

		g.pose().pushMatrix();
		g.pose().translate(x, sy);
		g.pose().scale(scale, scale);
		draw(g, font, group, y, height, now);
		g.pose().popMatrix();
	}

	/** The current task id, "" when every goal is done, or null when the guide is gone. */
	private static @Nullable String task(HudSnapshot snap) {
		return !snap.active() ? null : snap.next().isEmpty() ? "" : snap.next().getFirst().id();
	}

	/** Rebuilds the header and entries when the key changed. */
	private void update(Font font, Key now, BiomeGroup group) {
		if (now.equals(key)) return;
		key = now;
		name = biomeName(now.biomeId());
		yText = "Y " + now.y();
		entries = buildEntries(now, group);
		wanted = NAME_X + font.width(name) + GAP + font.width(WIDEST_Y) + PAD;
		for (Entry e : entries) wanted = Math.max(wanted, PAD + e.indent() + font.width(e.text()) + PAD);
		wrappedWidth = -1;
	}

	private static Component biomeName(String biomeId) {
		Identifier id = Identifier.parse(biomeId);
		return Component.translatable("biome." + id.getNamespace() + "." + id.getPath());
	}

	private static List<Entry> buildEntries(Key key, BiomeGroup group) {
		List<Entry> entries = new ArrayList<>();
		if (key.full()) {
			entries.add(new Entry(Component.literal("+ ").append(Component.translatable("survivalassistant.biome." + group.key() + ".pro")), GREEN));
			entries.add(new Entry(Component.literal("- ").append(Component.translatable("survivalassistant.biome." + group.key() + ".con")), RED));
		}
		if (!group.isOverworld()) return entries;

		entries.add(new Entry(oreLine(key.y(), group), WHITE, ItemIcons.of(Items.IRON_PICKAXE)));
		if (key.full()) {
			Entry pointer = pointer(key.task(), key.y(), group);
			if (pointer != null) entries.add(pointer);
			String hazard = OreTable.hazardAt(key.y(), group);
			if (hazard != null) {
				entries.add(new Entry(Component.translatable("survivalassistant.env.hazard." + hazard), ORANGE,
						ItemIcons.of(hazardItem(hazard))));
			}
		}
		return entries;
	}

	/** "Here: iron · coal · ...", with the ores at their best height in bold gold. */
	private static Component oreLine(int y, BiomeGroup group) {
		List<OreTable.Found> ores = OreTable.oresAt(y, group, MAX_ORES);
		MutableComponent line = Component.translatable("survivalassistant.env.here").withColor(GRAY).append(" ");
		if (ores.isEmpty()) line.append(Component.translatable("survivalassistant.env.no_ores").withColor(GRAY));
		for (int i = 0; i < ores.size(); i++) {
			OreTable.Found f = ores.get(i);
			if (i > 0) line.append(Component.literal(" · ").withColor(GRAY));
			MutableComponent oreName = Component.translatable("survivalassistant.ore." + f.id());
			// best height: gold and bold (no special glyphs, they may be missing from the font)
			line.append(f.best() ? oreName.withColor(GOLD).withStyle(s -> s.withBold(true)) : oreName.withColor(WHITE));
		}
		return line;
	}

	/** Where to dig for the ore the current task needs, or null when the task needs none. */
	private static @Nullable Entry pointer(@Nullable String task, int y, BiomeGroup group) {
		OreTable.Pointer p = OreTable.pointerFor(task, y, group);
		if (p == null) return null;
		Component ore = Component.translatable("survivalassistant.ore." + p.oreId());
		Component text = switch (p.direction()) {
			case UP -> Component.translatable("survivalassistant.env.pointer.up", ore, p.targetY());
			case DOWN -> Component.translatable("survivalassistant.env.pointer.down", ore, p.targetY());
			case HERE -> Component.translatable("survivalassistant.env.pointer.here", ore);
			case MOUNTAIN -> Component.translatable("survivalassistant.env.pointer.mountain", ore);
		};
		return new Entry(text, AQUA, ItemIcons.of(oreItem(p.oreId())));
	}

	/** Wraps the entries to the card width (only when the width or the entries changed). */
	private void wrap(Font font) {
		if (width == wrappedWidth) return;
		wrappedWidth = width;
		List<Line> out = new ArrayList<>();
		for (Entry e : entries) {
			List<FormattedCharSequence> parts = font.split(e.text(), width - 2 * PAD - e.indent());
			for (int i = 0; i < Math.min(MAX_WRAP_LINES, parts.size()); i++) {
				out.add(new Line(parts.get(i), e.color(), i == 0 ? e.icon() : ItemStack.EMPTY, e.indent()));
			}
		}
		lines = List.copyOf(out);
	}

	private void draw(GuiGraphicsExtractor g, Font font, BiomeGroup group, int y, int height, long now) {
		g.fill(0, 0, width, height, BG);
		int border = BORDER_ENV;
		long since = now - biomeChangedAt;
		if (biomeChangedAt > 0 && since < GLOW_MS) {
			float t = 1 - since / (float) GLOW_MS;
			border = lerp(BORDER_ENV, GLOW, t);
			g.fill(1, 1, width - 1, HEADER_H + PAD - 1, (Math.round(t * GLOW_ALPHA) << 24) | (GLOW & 0xFFFFFF));
		}
		HudDraw.outline(g, 0, 0, width, height, border);

		// header: icon, biome name (scrolls if too long), Y
		smallIcon(g, ItemIcons.of(group.icon()), PAD, PAD + 1);
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
}
