package io.github.visserdavid.survivalassistant;

import io.github.visserdavid.survivalassistant.advice.AdviceEngine;
import io.github.visserdavid.survivalassistant.advice.BiomeGroup;
import io.github.visserdavid.survivalassistant.advice.Chapter;
import io.github.visserdavid.survivalassistant.advice.Chapters;
import io.github.visserdavid.survivalassistant.advice.ContextCollector;
import io.github.visserdavid.survivalassistant.advice.GuideLifecycle;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.OreTable;
import io.github.visserdavid.survivalassistant.advice.PlaceNotices;
import io.github.visserdavid.survivalassistant.advice.SafeSignal;
import io.github.visserdavid.survivalassistant.advice.SurvivalContext;
import io.github.visserdavid.survivalassistant.chat.GuideChat;
import io.github.visserdavid.survivalassistant.chat.GuideMusings;
import io.github.visserdavid.survivalassistant.chat.Persona;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import io.github.visserdavid.survivalassistant.entity.ai.FollowOwnerGoal;
import io.github.visserdavid.survivalassistant.state.GuideState;
import io.github.visserdavid.survivalassistant.state.HudState;
import io.github.visserdavid.survivalassistant.util.Ticks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static io.github.visserdavid.survivalassistant.GuideSpeech.randomVariant;
import static io.github.visserdavid.survivalassistant.GuideSpeech.say;

/**
 * Server-side brain: keeps one guide next to every player who still has an active guide, runs the advice engine
 * periodically, handles the guide's lifecycle and is the entry point for events and commands.
 *
 * <p>The work is shared with {@link GuideSessions} (per-player memory), {@link GuideEntities} (the characters),
 * {@link GuideSpeech} (talking), {@link ChapterMenu} (questions with buttons) and {@link GuideNavigation} (the way
 * back and home).
 */
public final class GuideManager {
	private GuideManager() {}

	// ---------------------------------------------------------------- ticking

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % Ticks.PER_SECOND != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			try {
				process(player);
			} catch (RuntimeException e) {
				SurvivalAssistant.LOGGER.error("Guide update failed for {}", player.getName().getString(), e);
			}
		}
	}

	private static void process(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		ModConfig cfg = ModConfig.get();

		if (progress.retired || player.isSpectator() || !player.isAlive()) {
			if (progress.retired) {
				GuideEntities.remove(player.getUUID(), false);
				HudState.update(player, HudSnapshot.INACTIVE);
			}
			return;
		}

		boolean firstMeeting = false;
		if (progress.startTick < 0) {
			if (!cfg.spawnOnJoin) return;
			progress.startTick = player.level().getOverworldClockTime();
			firstMeeting = true;
		}

		GuideLifecycle.Step step = lifecycle(player, progress);
		if (step == GuideLifecycle.Step.CONGRATULATE) {
			congratulate(player, progress);
			return;
		}
		if (step == GuideLifecycle.Step.FAREWELL) {
			retire(player, progress, "chapters.no_answer");
			return;
		}

		boolean npc = progress.npcEnabled(cfg.npcEnabledByDefault);
		GuideEntity guide = null;
		if (npc) {
			guide = GuideEntities.ensure(player);
			if (guide == null) return;
		} else {
			GuideEntities.remove(player.getUUID(), true);
		}

		GuideSessions.Session session = GuideSessions.of(player.getUUID());
		if (firstMeeting) {
			say(player, guide, Component.translatable("survivalassistant.welcome.1", player.getName(), guideName(player)), false);
			say(player, guide, Component.translatable(npc ? "survivalassistant.welcome.2" : "survivalassistant.welcome_no_npc"), false);
			session.evaluateSoon(); // the first tip on the next pass
			return;
		}

		if (step == GuideLifecycle.Step.OFFER_CHAPTER) {
			ChapterMenu.offerNextChapter(player, guide, progress);
		} else if (step == GuideLifecycle.Step.REMIND) {
			progress.reminded = true;
			ChapterMenu.show(player, guide, progress, "survivalassistant.chapters.reminder");
		}

		if (progress.comfortPending) {
			progress.comfortPending = false;
			say(player, guide, randomVariant("survivalassistant.comfort", 2, player.getRandom()), true);
		}

		checkDepthNotices(player, progress, guide);

		long now = player.level().getGameTime();
		if (session.lastEval != null && now - session.lastEval < Ticks.seconds(cfg.tipIntervalSeconds)) return;
		session.lastEval = now;

		evaluateAndSay(player, guide, progress, false);
	}

	/** Evaluates the situation, says whatever is due and refreshes the task card. */
	private static void evaluateAndSay(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress, boolean forced) {
		long now = player.level().getGameTime();
		GuideSessions.Session session = GuideSessions.of(player.getUUID());
		SurvivalContext ctx = ContextCollector.collect(player, progress, session.dig.current(now),
				progress.homeFallback == null ? pos -> progress.homeFallback = GuideNavigation.mark(player, pos) : null);
		respond(player, guide, ctx, progress, forced);
		checkLight(player, guide, ctx, progress, session, now);
		checkPlaceNotices(player, guide, ctx, progress);
		GuideChat.observe(player, guide, ctx, progress);
		GuideMusings.observe(player, guide, ctx, progress);
		HudState.update(player, HudSnapshot.of(ctx, progress, GuideNavigation.waypoint(player, progress, ctx, now)));
	}

	private static void respond(ServerPlayer player, @Nullable GuideEntity guide, SurvivalContext ctx, GuideProgress progress, boolean forced) {
		boolean npc = progress.npcEnabled(ModConfig.get().npcEnabledByDefault);
		List<AdviceEngine.Message> messages = AdviceEngine.evaluate(ctx, progress, player.level().getGameTime(),
				AdviceEngine.Settings.from(ModConfig.get()), forced);
		for (AdviceEngine.Message m : messages) {
			AdviceEngine.Delivery delivery = AdviceEngine.delivery(m, npc, forced);
			if (delivery == AdviceEngine.Delivery.NONE) continue;
			if (m.praise()) {
				MutableComponent praise = Component.translatable("survivalassistant.praise." + m.tip().id()).withStyle(ChatFormatting.GREEN);
				if (delivery == AdviceEngine.Delivery.ACTIONBAR) {
					player.sendOverlayMessage(praise);
				} else {
					say(player, guide, praise, false);
				}
				if (guide != null) {
					player.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, guide.getX(), guide.getY() + 2.1, guide.getZ(), 6, 0.3, 0.2, 0.3, 0);
				}
			} else {
				GuideSpeech.sayTip(player, guide, m.tip(), m.urgent());
			}
		}
		if (forced && messages.isEmpty()) {
			say(player, guide, Component.translatable("survivalassistant.all_done"), false);
		}
	}

	/** Coming from the dark into a well-lit spot: the guide says it's safe here (once, with a cooldown; not while muted). */
	private static void checkLight(ServerPlayer player, @Nullable GuideEntity guide, SurvivalContext ctx, GuideProgress progress,
			GuideSessions.Session session, long now) {
		boolean safe = SafeSignal.show(ctx);
		if (SafeSignal.notice(session.wasSafe, safe, now, session.lastSafeNotice) && now >= progress.muteUntil) {
			say(player, guide, randomVariant("survivalassistant.safe.lit", 2, player.getRandom()).withStyle(ChatFormatting.GREEN), false);
			session.lastSafeNotice = now;
		}
		session.wasSafe = safe;
	}

	/** One-time notes about special places (the Nether, a village, a temple, ...). */
	private static void checkPlaceNotices(ServerPlayer player, @Nullable GuideEntity guide, SurvivalContext ctx, GuideProgress progress) {
		if (!ModConfig.get().depthNotices) return;
		String id = PlaceNotices.next(ctx, progress.depthNotices);
		if (id == null) return;
		progress.depthNotices.add(id);
		say(player, guide, Component.translatable("survivalassistant.place." + id).withStyle(ChatFormatting.AQUA), true);
	}

	/** One-time notes about depths (deepslate, the lava level, the deep dark, high mountains). */
	private static void checkDepthNotices(ServerPlayer player, GuideProgress progress, @Nullable GuideEntity guide) {
		if (!ModConfig.get().depthNotices) return;
		String id = OreTable.newNotice(player.getBlockY(), BiomeGroup.of(biomeId(player)), progress.depthNotices);
		if (id == null) return;
		progress.depthNotices.add(id);
		say(player, guide, Component.translatable("survivalassistant.depth." + id).withStyle(ChatFormatting.AQUA), true);
	}

	// ---------------------------------------------------------------- lifecycle

	/** Another chapter can be started after the current one. */
	private static boolean nextChapterAvailable(GuideProgress progress) {
		return ModConfig.get().moreChapters && !Chapters.available(progress.chapter, progress.completed).isEmpty();
	}

	private static GuideLifecycle.Step lifecycle(ServerPlayer player, GuideProgress progress) {
		return GuideLifecycle.decide(new GuideLifecycle.State(Chapters.get(progress.chapter).done(progress.completed),
				nextChapterAvailable(progress), progress.chapterOffered, progress.finished, progress.askedAtTick,
				progress.reminded, player.level().getOverworldClockTime()));
	}

	private static void retire(ServerPlayer player, GuideProgress progress, String messageKey) {
		GuideEntity guide = GuideEntities.get(player);
		Chapter chapter = Chapters.get(progress.chapter);
		say(player, guide, Component.translatable("survivalassistant." + messageKey + ".1"), false);
		say(player, guide, Component.translatable("survivalassistant.farewell.summary",
				chapter.doneCount(progress.completed), chapter.goals().size()), false);
		if (ModConfig.get().allowRecall) {
			say(player, guide, Component.translatable("survivalassistant.farewell.recall"), false);
		}
		progress.retired = true;
		GuideEntities.remove(player.getUUID(), true);
		HudState.update(player, HudSnapshot.INACTIVE);
	}

	/** All goals of the last chapter reached: a proper celebration, then the guide leaves. */
	private static void congratulate(ServerPlayer player, GuideProgress progress) {
		GuideEntity guide = GuideEntities.get(player);
		String key = progress.chapter <= 1 ? "survivalassistant.congrats." : "survivalassistant.congrats.all.";
		say(player, guide, Component.translatable(key + "1").withStyle(ChatFormatting.GREEN), false);
		say(player, guide, Component.translatable(key + "2"), false);
		if (ModConfig.get().allowRecall) {
			say(player, guide, Component.translatable("survivalassistant.farewell.recall"), false);
		}
		ChapterMenu.celebrate(player, guide, progress);
		progress.finished = true;
		progress.retired = true;
		progress.askedAtTick = -1;
		GuideEntities.remove(player.getUUID(), true);
		HudState.update(player, HudSnapshot.INACTIVE);
	}

	// ---------------------------------------------------------------- used by the chat and musings

	/** The player's guide character, or null when it isn't around (character off, retired, not spawned yet). */
	public static @Nullable GuideEntity guideOf(ServerPlayer player) {
		return GuideEntities.get(player);
	}

	/** A chat line from the guide (used by the small talk), with the usual name tag and a soft sound. */
	public static void speak(ServerPlayer player, @Nullable GuideEntity guide, Component text) {
		say(player, guide, text, false);
	}

	/** The guide's name for this player: their own choice, else the server's. */
	public static String guideName(ServerPlayer player) {
		return GuideSpeech.guideName(player);
	}

	/** The next "about me" line; every call moves on, so the guide doesn't keep telling the same story. */
	public static String nextAbout(GuideProgress progress) {
		String key = Persona.aboutKey(progress.aboutIndex);
		progress.aboutIndex = (progress.aboutIndex + 1) % Persona.ABOUT_COUNT;
		return key;
	}

	public static String biomeId(ServerPlayer player) {
		return player.level().getBiome(player.blockPosition()).unwrapKey()
				.map(k -> k.identifier().toString()).orElse("");
	}

	// ---------------------------------------------------------------- events

	/** Called when a player dies: remember the spot so the task card can lead them back to their items. */
	public static void onDeath(ServerPlayer player) {
		GuideNavigation.rememberDeath(player);
	}

	/** Called after a player breaks a block: detects digging straight down (the block right under the feet). */
	public static void onBlockBroken(ServerPlayer player, BlockPos pos) {
		if (pos.getX() != player.getBlockX() || pos.getZ() != player.getBlockZ() || pos.getY() != player.getBlockY() - 1) return;
		GuideSessions.Session session = GuideSessions.of(player.getUUID());
		if (session.dig.record(player.level().getGameTime()) >= 2) session.evaluateSoon(); // react now, not after the interval
	}

	public static void onGuideClicked(ServerPlayer player, GuideEntity guide) {
		GuideProgress progress = GuideState.of(player);
		evaluateAndSay(player, guide, progress, true);
		Chapter chapter = Chapters.get(progress.chapter);
		int done = chapter.doneCount(progress.completed), total = chapter.goals().size();
		Component line = Component.translatable("survivalassistant.progress_chapter", done, total,
				Component.translatable(chapter.titleKey()));
		say(player, null, line.copy().withStyle(ChatFormatting.GRAY), false);
	}

	public static void onRespawn(ServerPlayer newPlayer) {
		GuideProgress progress = GuideState.of(newPlayer);
		if (!progress.retired && progress.startTick >= 0) progress.comfortPending = true;
	}

	public static void onDisconnect(ServerPlayer player) {
		GuideEntities.remove(player.getUUID(), false);
		GuideSessions.disconnect(player.getUUID());
		GuideChat.forget(player.getUUID());
		GuideMusings.forget(player.getUUID());
	}

	public static void onServerStopping() {
		GuideEntities.clear();
		GuideSessions.clear();
		GuideChat.clear();
		GuideMusings.clear();
	}

	// ---------------------------------------------------------------- commands

	/** /guide tip */
	public static boolean tipNow(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired) return false;
		GuideEntity guide = null;
		if (npcEnabled(player)) {
			guide = GuideEntities.ensure(player);
			if (guide == null) return false;
		}
		evaluateAndSay(player, guide, progress, true);
		return true;
	}

	/** /guide name: a new name (null = back to the default); the name tag changes at once. */
	public static void rename(ServerPlayer player, @Nullable String name) {
		GuideState.of(player).guideName = name == null ? null : name.strip();
		GuideEntities.refreshName(player);
	}

	/** /guide about: the guide tells the next thing about itself. */
	public static boolean tellAbout(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired || progress.startTick < 0) return false;
		GuideEntity guide = npcEnabled(player) ? GuideEntities.ensure(player) : null;
		say(player, guide, Component.translatable(nextAbout(progress)), false);
		return true;
	}

	public static boolean npcEnabled(ServerPlayer player) {
		return GuideState.of(player).npcEnabled(ModConfig.get().npcEnabledByDefault);
	}

	/** /guide character on|off: stored on the player, takes effect immediately. */
	public static void setNpcEnabled(ServerPlayer player, boolean enabled) {
		GuideProgress progress = GuideState.of(player);
		progress.npcEnabled = enabled;
		if (!enabled) {
			GuideEntities.remove(player.getUUID(), true);
		} else if (!progress.retired && progress.startTick >= 0) {
			GuideEntity guide = GuideEntities.ensure(player);
			if (guide != null) say(player, guide, Component.translatable("survivalassistant.recall"), false);
		}
	}

	/** /guide call */
	public static boolean recall(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired) {
			if (!ModConfig.get().allowRecall) return false;
			progress.restart(player.level().getOverworldClockTime());
		} else if (progress.startTick < 0) {
			progress.restart(player.level().getOverworldClockTime());
		}
		// Finished everything before new chapters existed (or switched them on)? Then the menu comes up again.
		if (progress.finished && nextChapterAvailable(progress)) progress.finished = false;
		GuideEntity guide = GuideEntities.ensure(player);
		if (guide == null) return false;
		FollowOwnerGoal.teleportNear(guide, player.blockPosition());
		say(player, guide, Component.translatable("survivalassistant.recall"), false);
		HudState.update(player, HudSnapshot.of(ContextCollector.collect(player, progress), progress));
		return true;
	}

	/** /guide dismiss */
	public static boolean dismiss(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired) return false;
		if (progress.startTick < 0) progress.startTick = player.level().getOverworldClockTime();
		retire(player, progress, "dismissed");
		return true;
	}

	/** /guide continue: start the first chapter that is open (after the current one is finished). */
	public static boolean continueChapter(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (!Chapters.get(progress.chapter).done(progress.completed)) return false;
		List<Chapter> open = Chapters.available(progress.chapter, progress.completed);
		return !open.isEmpty() && startChapter(player, open.getFirst());
	}

	/** /guide chapter &lt;id&gt;: start or switch to a chapter that is open. Progress in other chapters is kept. */
	public static boolean startChapter(ServerPlayer player, Chapter chapter) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired || progress.startTick < 0 || !ModConfig.get().moreChapters) return false;
		if (Chapters.status(chapter, progress.chapter, progress.completed) != Chapters.Status.AVAILABLE) return false;
		progress.chapter = chapter.number();
		progress.restart(player.level().getOverworldClockTime());
		say(player, GuideEntities.get(player), Component.translatable("survivalassistant.chapter." + chapter.id() + ".start")
				.withStyle(ChatFormatting.GREEN), true);
		GuideSessions.of(player.getUUID()).evaluateSoon(); // the first tip of the chapter on the next pass
		return true;
	}

	/** /guide chapters: show the chapter menu. */
	public static boolean chapters(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired || progress.startTick < 0) return false;
		ChapterMenu.show(player, GuideEntities.get(player), progress, "survivalassistant.chapters.header");
		return true;
	}

	/** /guide mute &lt;minutes&gt; */
	public static void mute(ServerPlayer player, int minutes) {
		GuideState.of(player).muteUntil = player.level().getGameTime() + Ticks.minutes(minutes);
	}
}
