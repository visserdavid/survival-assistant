package io.github.visserdavid.survivalassistant.client.hud;

import io.github.visserdavid.survivalassistant.advice.CrosshairAdvice;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.state.HudState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * One or two lines under the crosshair about what the player is looking at: a tip for mobs, and for ores which
 * pickaxe is needed (red when the held tool would break the block without dropping anything).
 */
public final class CrosshairInfo implements HudElement {
	private static final int MAX_TEXT_W = 220;
	private static final int ICON = 10;
	private static final int ICON_GAP = 2;
	private static final int LINE_H = 10;
	/** The box starts this far below the middle of the screen. */
	private static final int BELOW_CROSSHAIR = 12;
	private static final int SIDE_PAD = 3;

	/** What was under the crosshair last tick, and for how many ticks in a row. */
	private @Nullable Object target;
	private int dwell;
	/** Ready-made content while the target stays the same, and its wrapped rows. */
	private @Nullable Line line;
	private List<FormattedCharSequence> rows = List.of();
	private int textWidth;
	private boolean rowsStale;

	private record Line(Component text, int color, ItemStack icon) {}

	/** Called every client tick. */
	public void tick(Minecraft mc) {
		Object now = currentTarget(mc);
		if (now == null || !now.equals(target)) {
			target = now;
			dwell = 0;
			line = null;
			return;
		}
		dwell++;
		if (dwell < CrosshairAdvice.DWELL_TICKS) return;
		// rebuilt every tick, so switching tools updates the colour at once
		Line fresh = build(mc, now);
		if (fresh == null ? line != null : !fresh.equals(line)) {
			line = fresh;
			rowsStale = true;
		}
	}

	private static @Nullable Object currentTarget(Minecraft mc) {
		if (mc.player == null || mc.level == null) return null;
		HitResult hit = mc.hitResult;
		if (hit instanceof EntityHitResult eh) return eh.getEntity();
		if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
			return bh.getBlockPos().immutable();
		}
		return null;
	}

	private static @Nullable Line build(Minecraft mc, Object target) {
		if (target instanceof Entity entity) {
			String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
			if (!CrosshairAdvice.hasMobTip(path)) return null;
			MutableComponent text = entity.getType().getDescription().copy().withStyle(ChatFormatting.GOLD)
					.append(Component.literal(": ").withStyle(ChatFormatting.GOLD))
					.append(Component.translatable("survivalassistant.mob." + path));
			return new Line(text, HudColors.WHITE, ItemStack.EMPTY);
		}
		if (target instanceof BlockPos pos) {
			BlockState state = mc.level.getBlockState(pos);
			String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
			if (!CrosshairAdvice.isInterestingBlock(path) || !state.requiresCorrectToolForDrops()) return null;
			CrosshairAdvice.Pickaxe need = CrosshairAdvice.requiredPickaxe(state.is(BlockTags.NEEDS_DIAMOND_TOOL),
					state.is(BlockTags.NEEDS_IRON_TOOL), state.is(BlockTags.NEEDS_STONE_TOOL));
			boolean works = mc.player.getMainHandItem().isCorrectToolForDrops(state);
			Component name = state.getBlock().getName();
			Component text = works
					? Component.translatable("survivalassistant.cross.ok", name)
					: Component.translatable("survivalassistant.cross.needs", name, Component.translatable(need.langKey()));
			return new Line(text, works ? HudColors.GREEN : HudColors.RED, ItemIcons.of(pickaxeItem(need)));
		}
		return null;
	}

	private static Item pickaxeItem(CrosshairAdvice.Pickaxe p) {
		return switch (p) {
			case WOODEN -> Items.WOODEN_PICKAXE;
			case STONE -> Items.STONE_PICKAXE;
			case IRON -> Items.IRON_PICKAXE;
			case DIAMOND -> Items.DIAMOND_PICKAXE;
		};
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		Line l = line;
		if (l == null || mc.player == null || !ClientConfig.get().crosshairInfo) return;
		if (HudDraw.hidden(mc) || mc.gui.screen() != null) return;
		HudSnapshot snap = mc.player.getAttached(HudState.HUD);
		if (snap == null || !snap.active()) return;

		Font font = mc.font;
		if (rowsStale) {
			rows = font.split(l.text(), MAX_TEXT_W);
			textWidth = 0;
			for (FormattedCharSequence r : rows) textWidth = Math.max(textWidth, font.width(r));
			rowsStale = false;
		}
		boolean icon = !l.icon().isEmpty();
		int w = textWidth + (icon ? ICON + ICON_GAP : 0) + 2 * SIDE_PAD;
		int h = rows.size() * LINE_H + 3;
		int x = (g.guiWidth() - w) / 2;
		int y = g.guiHeight() / 2 + BELOW_CROSSHAIR;

		g.fill(x, y, x + w, y + h, HudColors.CROSSHAIR_BG);
		int tx = x + SIDE_PAD;
		if (icon) {
			HudDraw.scaledIcon(g, l.icon(), tx, y + 1, ICON / 16f);
			tx += ICON + ICON_GAP;
		}
		int ty = y + 2;
		for (FormattedCharSequence r : rows) {
			g.text(font, r, tx, ty, l.color(), true);
			ty += LINE_H;
		}
	}
}
