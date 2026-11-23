package io.github.visserdavid.survivalassistant;

import io.github.visserdavid.survivalassistant.advice.Chapter;
import io.github.visserdavid.survivalassistant.advice.Chapters;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

import static io.github.visserdavid.survivalassistant.GuideSpeech.button;
import static io.github.visserdavid.survivalassistant.GuideSpeech.say;

/** The chapter menu with clickable chapters, and the celebration of a finished chapter. */
final class ChapterMenu {
	/** Indent of the menu lines under the guide's question. */
	private static final String INDENT = "   ";

	private ChapterMenu() {}

	/** All goals of a chapter reached: celebrate, then show the chapter menu. The guide stays until the player picks. */
	static void offerNextChapter(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress) {
		Chapter done = Chapters.get(progress.chapter);
		say(player, guide, (done.number() == 1 ? Component.translatable("survivalassistant.congrats.1")
				: Component.translatable("survivalassistant.chapter.done", Component.translatable(done.titleKey())))
				.withStyle(ChatFormatting.GREEN), false);
		celebrate(player, guide, progress);
		progress.chapterOffered = true;
		progress.askedAtTick = player.level().getOverworldClockTime();
		progress.reminded = false;
		show(player, guide, progress, "survivalassistant.chapters.offer");
	}

	/**
	 * The chapter menu in chat: finished chapters in green, the ones you can start as buttons, locked ones in grey
	 * with what they need. After a finished chapter it ends with a button to let the guide go.
	 */
	static void show(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress, String headerKey) {
		say(player, guide, Component.translatable(headerKey).withStyle(ChatFormatting.AQUA), true);
		for (Chapter c : Chapters.ALL) {
			player.sendSystemMessage(Component.literal(INDENT).append(line(c, progress)));
		}
		if (progress.chapterOffered) {
			player.sendSystemMessage(Component.literal(INDENT)
					.append(button("survivalassistant.chapters.leave", "survivalassistant.chapters.leave_hover", "/guide dismiss", ChatFormatting.RED)));
		}
	}

	/** One chapter in the menu. */
	private static Component line(Chapter c, GuideProgress progress) {
		Component title = Component.translatable(c.titleKey());
		return switch (Chapters.status(c, progress.chapter, progress.completed)) {
			case DONE -> Component.literal("✔ " + c.number() + ". ").append(title).withStyle(ChatFormatting.GREEN);
			case CURRENT -> Component.literal("▶ " + c.number() + ". ").append(title)
					.append(" ").append(Component.translatable("survivalassistant.chapters.current")).withStyle(ChatFormatting.GOLD);
			case AVAILABLE -> Component.literal("[" + c.number() + ". ").append(title).append("]")
					.withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withBold(true)
							.withClickEvent(new ClickEvent.RunCommand("/guide chapter " + c.id()))
							.withHoverEvent(new HoverEvent.ShowText(Component.translatable("survivalassistant.chapter." + c.id() + ".desc"))));
			case LOCKED -> {
				MutableComponent needs = Component.empty();
				for (int i = 0; i < c.requires().size(); i++) {
					if (i > 0) needs.append(", ");
					needs.append(Component.translatable(Chapters.get(c.requires().get(i)).titleKey()));
				}
				yield Component.literal(c.number() + ". ").append(title).append(" ")
						.append(Component.translatable("survivalassistant.chapters.locked", needs)).withStyle(ChatFormatting.GRAY);
			}
		};
	}

	/** Title, sound and particles for a finished chapter. */
	static void celebrate(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress) {
		int total = Chapters.get(progress.chapter).goals().size();
		player.connection.send(new ClientboundSetTitleTextPacket(
				Component.translatable("survivalassistant.congrats.title").withStyle(ChatFormatting.GOLD)));
		player.connection.send(new ClientboundSetSubtitleTextPacket(
				Component.translatable("survivalassistant.congrats.subtitle", total)));
		GuideSpeech.playTo(player, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE),
				SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f);
		player.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(),
				60, 0.6, 0.8, 0.6, 0.3);
		if (guide != null && !guide.isRemoved()) {
			player.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, guide.getX(), guide.getY() + 1, guide.getZ(),
					40, 0.4, 0.8, 0.4, 0.3);
		}
	}
}
