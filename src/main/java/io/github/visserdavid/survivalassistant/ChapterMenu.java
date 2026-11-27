package io.github.visserdavid.survivalassistant;

import io.github.visserdavid.survivalassistant.advice.Chapter;
import io.github.visserdavid.survivalassistant.advice.Chapters;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.advice.Route;
import io.github.visserdavid.survivalassistant.advice.Routes;
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

/** The chapter menu with clickable chapters, the route question, and the celebration of a finished chapter. */
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
		progress.askedAtTick = progress.playedTicks;
		progress.reminded = false;
		if (praiseRoute(player, guide, progress)) {
			offerRoute(player, guide, progress, "survivalassistant.route.next_question");
		} else if (Routes.due(progress)) {
			offerRoute(player, guide, progress, "survivalassistant.route.question");
		} else {
			show(player, guide, progress, "survivalassistant.chapters.offer");
		}
	}

	/** The reminder for an open question: the route question while it is due, else the chapter menu. */
	static void remind(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress) {
		if (Routes.due(progress)) offerRoute(player, guide, progress, "survivalassistant.route.reminder");
		else show(player, guide, progress, "survivalassistant.chapters.reminder");
	}

	/**
	 * The player's route is finished and wasn't praised yet: say so with the route's title. Returns whether it did.
	 * Every chapter stays open afterwards.
	 */
	static boolean praiseRoute(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress) {
		Route route = Routes.of(progress).orElse(null);
		if (route == null || !route.done(progress.completed) || !progress.routesDone.add(route.id())) return false;
		say(player, guide, Component.translatable("survivalassistant.route.done", Component.translatable(route.nameKey()),
				Component.translatable("survivalassistant.route." + route.id() + ".title")).withStyle(ChatFormatting.GOLD), false);
		return true;
	}

	/**
	 * The routes as buttons (finished ones in green, the current one in gold), plus "choose freely". While a question
	 * is open it ends with the button to let the guide go, like the chapter menu.
	 */
	static void offerRoute(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress, String headerKey) {
		say(player, guide, Component.translatable(headerKey).withStyle(ChatFormatting.AQUA), true);
		for (Route r : Routes.ALL) {
			Component name = Component.translatable(r.nameKey());
			Component line;
			if (progress.routesDone.contains(r.id()) && r.done(progress.completed)) {
				line = Component.literal("✔ ").append(name).withStyle(ChatFormatting.GREEN);
			} else {
				boolean current = r.id().equals(progress.route);
				MutableComponent text = Component.literal(current ? "▶ [" : "[").append(name).append("]");
				if (current) text.append(" ").append(Component.translatable("survivalassistant.route.current"));
				line = text.withStyle(Style.EMPTY.withColor(current ? ChatFormatting.GOLD : ChatFormatting.AQUA).withBold(true)
						.withClickEvent(new ClickEvent.RunCommand("/guide route " + r.id()))
						.withHoverEvent(new HoverEvent.ShowText(Component.translatable("survivalassistant.route." + r.id() + ".desc"))));
			}
			player.sendSystemMessage(Component.literal(INDENT).append(line));
		}
		player.sendSystemMessage(Component.literal(INDENT).append(button("survivalassistant.route.free",
				"survivalassistant.route.free_hover", "/guide route " + Routes.FREE, ChatFormatting.WHITE)));
		if (progress.chapterOffered) {
			player.sendSystemMessage(Component.literal(INDENT)
					.append(button("survivalassistant.chapters.leave", "survivalassistant.chapters.leave_hover", "/guide dismiss", ChatFormatting.RED)));
		}
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
		Chapter recommended = Routes.recommended(progress).orElse(null);
		return switch (Chapters.status(c, progress.chapter, progress.completed)) {
			case DONE -> Component.literal("✔ " + c.number() + ". ").append(title).withStyle(ChatFormatting.GREEN);
			case CURRENT -> Component.literal("▶ " + c.number() + ". ").append(title)
					.append(" ").append(Component.translatable("survivalassistant.chapters.current")).withStyle(ChatFormatting.GOLD);
			case AVAILABLE -> {
				// the next chapter of the player's route stands out, with the route in the hover text
				boolean next = c == recommended;
				Component desc = Component.translatable("survivalassistant.chapter." + c.id() + ".desc");
				Component hover = next ? Component.translatable("survivalassistant.chapters.recommended",
						Component.translatable(Routes.of(progress).orElseThrow().nameKey())).append("\n").append(desc) : desc;
				yield Component.literal((next ? "★ [" : "[") + c.number() + ". ").append(title).append("]")
						.withStyle(Style.EMPTY.withColor(next ? ChatFormatting.GOLD : ChatFormatting.AQUA).withBold(true)
								.withClickEvent(new ClickEvent.RunCommand("/guide chapter " + c.id()))
								.withHoverEvent(new HoverEvent.ShowText(hover)));
			}
			case LOCKED -> {
				// only what is still missing (the finished requirements are no news)
				MutableComponent needs = Component.empty();
				for (int id : c.requires()) {
					Chapter required = Chapters.get(id);
					if (required.done(progress.completed)) continue;
					if (!needs.getSiblings().isEmpty()) needs.append(", ");
					needs.append(Component.translatable(required.titleKey()));
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
