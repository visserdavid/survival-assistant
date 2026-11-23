package io.github.visserdavid.survivalassistant;

import io.github.visserdavid.survivalassistant.advice.GuideNames;
import io.github.visserdavid.survivalassistant.advice.Tip;
import io.github.visserdavid.survivalassistant.advice.TipRegistry;
import io.github.visserdavid.survivalassistant.chat.GuideMusings;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import io.github.visserdavid.survivalassistant.state.GuideState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/** How the guide talks: chat lines with its name tag, sounds, tips with variants, clickable buttons. */
final class GuideSpeech {
	/** The guide looks this far around itself for the trees or stone it points at. */
	private static final int POINT_RADIUS = 12;
	private static final int POINT_HEIGHT = 6;

	private GuideSpeech() {}

	/** The guide's name for this player: their own choice, else the server's (see {@link GuideNames}). */
	static String guideName(ServerPlayer player) {
		return GuideNames.effective(GuideState.of(player).guideName, ModConfig.get().guideName);
	}

	/** A chat line from the guide with its name tag and a sound; {@code important} lines sound a bit louder. */
	static void say(ServerPlayer player, @Nullable GuideEntity guide, Component text, boolean important) {
		MutableComponent line = Component.literal("[").withStyle(ChatFormatting.GOLD)
				.append(Component.literal(guideName(player)).withStyle(ChatFormatting.GOLD))
				.append(Component.literal("] ").withStyle(ChatFormatting.GOLD))
				.append(text);
		player.sendSystemMessage(line);
		GuideMusings.noteRemark(player);
		if (guide != null) {
			playTo(player, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(important ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_AMBIENT),
					SoundSource.NEUTRAL, guide.getX(), guide.getY(), guide.getZ(), important ? 0.9f : 0.5f, 1.15f);
		} else if (important) {
			playTo(player, SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 0.6f, 1.2f);
		}
	}

	/** A sound only this player hears (the guide is theirs; other players nearby shouldn't hear its tips). */
	static void playTo(ServerPlayer player, Holder<SoundEvent> sound, SoundSource source, double x, double y, double z,
			float volume, float pitch) {
		player.connection.send(new ClientboundSoundPacket(sound, source, x, y, z, volume, pitch, player.getRandom().nextLong()));
	}

	/** A tip in one of its text variants; urgent tips are yellow and also show above the hotbar. */
	static void sayTip(ServerPlayer player, @Nullable GuideEntity guide, Tip tip, boolean urgent) {
		MutableComponent text = randomVariant(tip.baseKey(), tip.variants(), player.getRandom());
		say(player, guide, urgent ? text.withStyle(ChatFormatting.YELLOW) : text, urgent);
		if (urgent) player.sendOverlayMessage(text.copy().withStyle(ChatFormatting.GOLD));
		if (guide != null && ModConfig.get().pointAtResources) {
			if (tip == TipRegistry.WOOD) pointAtNearest(guide, s -> s.is(BlockTags.LOGS));
			else if (tip == TipRegistry.STONE_TOOLS) pointAtNearest(guide, s -> s.is(Blocks.STONE) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.DEEPSLATE));
		}
	}

	/** {@code baseKey.1} .. {@code baseKey.<variants>}, picked at random. */
	static MutableComponent randomVariant(String baseKey, int variants, RandomSource random) {
		int v = 1 + random.nextInt(Math.max(1, variants));
		return Component.translatable(baseKey + "." + v);
	}

	/** A clickable "[label]" that runs a command, with a hover text. */
	static MutableComponent button(String labelKey, String hoverKey, String command, ChatFormatting color, Object... args) {
		return Component.literal("[").append(Component.translatable(labelKey, args)).append("]")
				.withStyle(Style.EMPTY.withColor(color).withBold(true)
						.withClickEvent(new ClickEvent.RunCommand(command))
						.withHoverEvent(new HoverEvent.ShowText(Component.translatable(hoverKey, args))));
	}

	/** Makes the guide look at the nearest block that matches. */
	private static void pointAtNearest(GuideEntity guide, Predicate<BlockState> match) {
		BlockPos origin = guide.blockPosition();
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		int r = POINT_RADIUS, h = POINT_HEIGHT;
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -h, -r), origin.offset(r, h, r))) {
			if (!match.test(guide.level().getBlockState(pos))) continue;
			double d = pos.distSqr(origin);
			if (d < bestDist) {
				bestDist = d;
				best = pos.immutable();
			}
		}
		if (best != null) guide.pointAt(best);
	}
}
