package io.github.visserdavid.survivalassistant.client.hud;

import io.github.visserdavid.survivalassistant.advice.CrosshairAdvice;
import io.github.visserdavid.survivalassistant.advice.CrosshairHold;
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
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Info about what the player is looking at: a tip for mobs, and for ores which pickaxe is needed (red when the held
 * tool would break the block without dropping anything). Shown in one or two lines under the crosshair, or handed to
 * the {@link TargetCard}, depending on the settings.
 *
 * <p>Mobs are found up to the distance in the settings (a creeper should be read about before it hisses); blocks
 * stop that look, so a mob behind a wall gives no tip. Ores keep the normal reach: that info is about mining.
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
	/** The info for the current target, and the info on screen (it may stay a while in the card). */
	private @Nullable Info line;
	private @Nullable Info shown;
	private final CrosshairHold hold = new CrosshairHold();
	/** Wrapped rows of {@link #shown} for under the crosshair. */
	private List<FormattedCharSequence> rows = List.of();
	private int textWidth;
	private boolean rowsStale;

	/** What to show: the text, its colour and an optional icon (the pickaxe for ores). */
	record Info(Component text, int color, ItemStack icon) {}

	/** The info to show now, or null. */
	@Nullable Info shown() {
		return shown;
	}

	/** Called every client tick. */
	public void tick(Minecraft mc) {
		ClientConfig cfg = ClientConfig.get();
		// no work without the guide's cards (a server without the mod, or the guide gone)
		HudSnapshot snap = mc.player == null ? null : mc.player.getAttached(HudState.HUD);
		boolean active = snap != null && snap.active();
		Object now = cfg.crosshairInfo && active ? currentTarget(mc, cfg.crosshairRange) : null;
		if (now == null || !now.equals(target)) {
			target = now;
			dwell = 0;
			line = null;
		} else if (++dwell >= CrosshairAdvice.DWELL_TICKS) {
			// rebuilt every tick, so switching tools updates the colour at once
			line = build(mc, now);
		}
		boolean keep = hold.tick(line != null, cfg.crosshairPlace == ClientConfig.Place.CARD);
		Info show = line != null ? line : keep ? shown : null;
		if (!Objects.equals(show, shown)) {
			shown = show;
			rowsStale = true;
		}
	}

	/** A mob with a tip within the range, else whatever is within normal reach (a block, or a mob up close). */
	private static @Nullable Object currentTarget(Minecraft mc, int range) {
		Entity camera = mc.getCameraEntity();
		if (mc.player == null || mc.level == null || camera == null) return null;
		HitResult hit = mc.hitResult;
		if (hit instanceof EntityHitResult eh && eligible(eh.getEntity(), camera)) return eh.getEntity();
		Entity far = mobInSight(camera, range);
		if (far != null) return far;
		if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
			return bh.getBlockPos().immutable();
		}
		return null;
	}

	/**
	 * The nearest mob with a tip along the view line, up to {@code range} blocks; blocks stop the look. Seen from the
	 * camera (which is the player, unless they spectate through a mob's eyes).
	 */
	private static @Nullable Entity mobInSight(Entity camera, int range) {
		Vec3 eye = camera.getEyePosition(1f);
		Vec3 end = eye.add(camera.getViewVector(1f).scale(range));
		BlockHitResult wall = camera.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, camera));
		if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
		AABB area = camera.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(camera, eye, end, area, e -> eligible(e, camera), eye.distanceToSqr(end));
		return hit == null ? null : hit.getEntity();
	}

	/** A living, visible vanilla mob with a tip (an invisible mob isn't given away, a modded look-alike gets no tip). */
	private static boolean eligible(Entity e, Entity camera) {
		if (e == camera || !e.isAlive() || e.isSpectator() || e.isInvisible()) return false;
		var id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
		return Identifier.DEFAULT_NAMESPACE.equals(id.getNamespace()) && CrosshairAdvice.hasMobTip(id.getPath());
	}

	private static @Nullable Info build(Minecraft mc, Object target) {
		if (target instanceof Entity entity) {
			String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
			if (!CrosshairAdvice.hasMobTip(path)) return null;
			// "Creeper: ..." with the word order and punctuation of the player's language
			MutableComponent text = Component.translatable("survivalassistant.cross.mob",
					entity.getType().getDescription().copy().withStyle(ChatFormatting.GOLD),
					Component.translatable("survivalassistant.mob." + path));
			return new Info(text, HudColors.WHITE, ItemStack.EMPTY);
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
			return new Info(text, works ? HudColors.GREEN : HudColors.RED, ItemIcons.of(pickaxeItem(need)));
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
		Info l = shown;
		ClientConfig cfg = ClientConfig.get();
		if (l == null || mc.player == null || !cfg.crosshairInfo || cfg.crosshairPlace != ClientConfig.Place.CROSSHAIR) return;
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
