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
import io.github.visserdavid.survivalassistant.chat.Surroundings;
import io.github.visserdavid.survivalassistant.chat.SurroundingsCollector;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import java.util.List;

import static io.github.visserdavid.survivalassistant.GuideSpeech.randomVariant;
import static io.github.visserdavid.survivalassistant.GuideSpeech.say;

/**
 * Server-side brain: keeps one guide next to every player who still has an active guide, runs the advice engine
 * periodically, handles the guide's lifecycle and is the entry point for events.
 *
 * <p>The work is shared with {@link GuideActions} (what the player asks: commands and settings), {@link GuideSessions}
 * (per-player memory), {@link GuideEntities} (the characters), {@link GuideSpeech} (talking), {@link ChapterMenu}
 * (questions with buttons) and {@link GuideNavigation} (the way back and home).
 */
public final class GuideManager {
	private GuideManager() {}

	// ---------------------------------------------------------------- ticking

	/** Every player once a second, spread over the ticks so the world scans of many players don't all land together. */
	public static void tick(MinecraftServer server) {
		int tick = server.getTickCount();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (Math.floorMod(tick + player.getId(), Ticks.PER_SECOND) != 0) continue;
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
			if (progress.retired) HudState.update(player, HudSnapshot.INACTIVE);
			// no guide while retired or spectating; while dead it waits (it comes back after the respawn)
			if (progress.retired || player.isSpectator()) GuideEntities.remove(player.getUUID(), false);
			return;
		}

		boolean firstMeeting = false;
		if (!progress.met()) {
			if (!cfg.spawnOnJoin) return;
			progress.meet(player.level().getOverworldClockTime());
			firstMeeting = true;
		}
		progress.playedTicks += Ticks.PER_SECOND; // this runs once a second while the guide is active

		GuideLifecycle.State lifecycle = lifecycle(progress);
		GuideLifecycle.Step step = GuideLifecycle.decide(lifecycle);
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
			guide.setRoute(progress.route); // the guide dresses for the route (client side, see GuideSkins)
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
		} else if (step == GuideLifecycle.Step.START_NEXT) {
			GuideActions.continueChapter(player);
		} else if (step == GuideLifecycle.Step.WAITING) {
			long left = GuideLifecycle.autoStartSecondsLeft(lifecycle);
			if (left > 0) {
				player.sendOverlayMessage(Component.translatable("survivalassistant.chapters.auto_countdown", left)
						.withStyle(ChatFormatting.GOLD));
			}
		} else if (step == GuideLifecycle.Step.REMIND) {
			progress.reminded = true;
			ChapterMenu.remind(player, guide, progress);
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
	static void evaluateAndSay(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress, boolean forced) {
		long now = player.level().getGameTime();
		GuideSessions.Session session = GuideSessions.of(player.getUUID());
		GuideNavigation.updateHome(player, progress, session);
		SurvivalContext ctx = ContextCollector.collect(player, progress, session.dig.current(now));
		respond(player, guide, ctx, progress, forced);
		checkLight(player, guide, ctx, progress, session, now);
		checkPlaceNotices(player, guide, ctx, progress);
		// the look around (mobs in sight) is shared by small talk and musings, and only made when one of them needs it
		Surroundings[] seen = new Surroundings[1];
		Supplier<Surroundings> surroundings = () -> seen[0] != null ? seen[0]
				: (seen[0] = SurroundingsCollector.collect(player, ctx, ModConfig.get().chatSightRadius));
		GuideChat.observe(player, guide, ctx, progress, surroundings);
		GuideMusings.observe(player, guide, ctx, progress, surroundings);
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
				GuideSpeech.sayTip(player, guide, m.tip(), m.urgent(), m.step());
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
	static boolean nextChapterAvailable(GuideProgress progress) {
		return ModConfig.get().moreChapters && !Chapters.available(progress.chapter, progress.completed).isEmpty();
	}

	private static GuideLifecycle.State lifecycle(GuideProgress progress) {
		return new GuideLifecycle.State(Chapters.get(progress.chapter).done(progress.completed),
				nextChapterAvailable(progress), progress.chapterOffered, progress.finished, progress.askedAtTick,
				progress.reminded, progress.playedTicks, progress.chapter == Chapters.FIRST_DAYS.number());
	}

	static void retire(ServerPlayer player, GuideProgress progress, String messageKey) {
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
		ChapterMenu.praiseRoute(player, guide, progress);
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

	/** A click on the guide while sneaking: the most important tip and the chapter's progress (at most once a second). */
	public static void onGuideClicked(ServerPlayer player, GuideEntity guide) {
		GuideSessions.Session session = GuideSessions.of(player.getUUID());
		long now = player.level().getGameTime();
		if (now - session.lastClick < Ticks.PER_SECOND) return;
		session.lastClick = now;
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
		if (!progress.retired && progress.met()) progress.comfortPending = true;
	}

	/** About to place a crafting table, chest or bed: remember the spot (the next pass checks it was really placed). */
	public static void onUseBlock(ServerPlayer player, InteractionHand hand, BlockHitResult hit) {
		if (player.getItemInHand(hand).getItem() instanceof BlockItem item
				&& GuideNavigation.isHomeBlock(item.getBlock().defaultBlockState())) {
			BlockPos clicked = hit.getBlockPos();
			BlockPos pos = player.level().getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
			GuideSessions.of(player.getUUID()).placedHome = pos.immutable();
		}
	}

	public static void onDisconnect(ServerPlayer player) {
		GuideEntities.remove(player.getUUID(), false);
		GuideSessions.disconnect(player.getUUID());
		GuideChat.forget(player.getUUID());
		GuideMusings.forget(player.getUUID());
		GuideSettings.forget(player.getUUID());
	}

	public static void onServerStopping() {
		GuideEntities.clear();
		GuideSessions.clear();
		GuideChat.clear();
		GuideMusings.clear();
		GuideSettings.clear();
	}
}
