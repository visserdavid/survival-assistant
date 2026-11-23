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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * One or two lines under the crosshair about what the player is looking at: a tip for mobs, and for ores which
 * pickaxe is needed (red when the held tool would break the block without dropping anything).
 */
public final class CrosshairInfo implements HudElement {
	private static final int MAX_TEXT_W = 220;
	private static final int ICON = 10;

	/** What was under the crosshair last tick, and for how many ticks in a row. */
	private static Object target;
	private static int dwell;
	/** Ready-made content, rebuilt every tick while the target stays the same. */
	private static Line line;

	private record Line(Component text, int color, ItemStack icon) {}

	/** Called every client tick. */
	public static void tick(Minecraft mc) {
		Object now = currentTarget(mc);
		if (now == null || !now.equals(target)) {
			target = now;
			dwell = 0;
			line = null;
			return;
		}
		dwell++;
		if (dwell >= CrosshairAdvice.DWELL_TICKS) line = build(mc);
	}

	private static Object currentTarget(Minecraft mc) {
		if (mc.player == null || mc.level == null) return null;
		HitResult hit = mc.hitResult;
		if (hit instanceof EntityHitResult eh) return eh.getEntity();
		if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
			return bh.getBlockPos().immutable();
		}
		return null;
	}

	private static Line build(Minecraft mc) {
		if (target instanceof Entity entity) {
			String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
			if (!CrosshairAdvice.hasMobTip(path)) return null;
			MutableComponent text = entity.getType().getDescription().copy().withStyle(ChatFormatting.GOLD)
					.append(Component.literal(": ").withStyle(ChatFormatting.GOLD))
					.append(Component.translatable("survivalassistant.mob." + path));
			return new Line(text, 0xFFFFFFFF, ItemStack.EMPTY);
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
			return new Line(text, works ? 0xFF6BD96B : 0xFFFF6B6B, new ItemStack(pickaxeItem(need)));
		}
		return null;
	}

	private static net.minecraft.world.item.Item pickaxeItem(CrosshairAdvice.Pickaxe p) {
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
		if (l == null || mc.player == null || !HudConfig.get().crosshairInfo) return;
		if (mc.gui.hud.isHidden() || mc.getDebugOverlay().showDebugScreen() || mc.gui.screen() != null) return;
		HudSnapshot snap = mc.player.getAttached(HudState.HUD);
		if (snap == null || !snap.active()) return;

		Font font = mc.font;
		boolean icon = !l.icon().isEmpty();
		List<FormattedCharSequence> rows = font.split(l.text(), MAX_TEXT_W);
		int textW = 0;
		for (FormattedCharSequence r : rows) textW = Math.max(textW, font.width(r));
		int w = textW + (icon ? ICON + 2 : 0) + 6;
		int h = rows.size() * 10 + 3;
		int x = (g.guiWidth() - w) / 2;
		int y = g.guiHeight() / 2 + 12;

		g.fill(x, y, x + w, y + h, 0x90000000);
		int tx = x + 3;
		if (icon) {
			g.pose().pushMatrix();
			g.pose().translate(tx, y + 1);
			g.pose().scale(ICON / 16f, ICON / 16f);
			g.item(l.icon(), 0, 0);
			g.pose().popMatrix();
			tx += ICON + 2;
		}
		int ty = y + 2;
		for (FormattedCharSequence r : rows) {
			g.text(font, r, tx, ty, l.color(), true);
			ty += 10;
		}
	}
}
