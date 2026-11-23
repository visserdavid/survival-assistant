package io.github.visserdavid.survivalassistant;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import io.github.visserdavid.survivalassistant.advice.AdviceEngine;
import io.github.visserdavid.survivalassistant.advice.BiomeGroup;
import io.github.visserdavid.survivalassistant.advice.Chapter;
import io.github.visserdavid.survivalassistant.advice.Chapters;
import io.github.visserdavid.survivalassistant.advice.PlaceNotices;
import io.github.visserdavid.survivalassistant.advice.OreTable;
import io.github.visserdavid.survivalassistant.advice.ContextCollector;
import io.github.visserdavid.survivalassistant.advice.DigTracker;
import io.github.visserdavid.survivalassistant.advice.GuideLifecycle;
import io.github.visserdavid.survivalassistant.advice.GuideNames;
import io.github.visserdavid.survivalassistant.advice.Navigation;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.SurvivalContext;
import io.github.visserdavid.survivalassistant.advice.Tip;
import io.github.visserdavid.survivalassistant.advice.TipRegistry;
import io.github.visserdavid.survivalassistant.chat.GuideChat;
import io.github.visserdavid.survivalassistant.chat.GuideMusings;
import io.github.visserdavid.survivalassistant.chat.Persona;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import io.github.visserdavid.survivalassistant.entity.ai.FollowOwnerGoal;
import io.github.visserdavid.survivalassistant.state.GuideState;
import io.github.visserdavid.survivalassistant.state.HudState;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Server-side brain: keeps one guide next to every player who still has an active guide,
 * runs the advice engine periodically and handles the guide's lifecycle.
 */
public final class GuideManager {
	private static final Map<UUID, GuideEntity> GUIDES = new HashMap<>();
	private static final Map<UUID, Long> LAST_EVAL = new HashMap<>();
	/** Where and when each player last died (kept here, not on the player, so it survives the respawn copy). */
	private static final Map<UUID, DeathRecord> DEATHS = new HashMap<>();
	private static final Map<UUID, DigTracker> DIGS = new HashMap<>();

	private record DeathRecord(Navigation.Mark mark, long tick) {}
	private static final double RESPAWN_DIST_SQR = 48 * 48;

	private GuideManager() {}

	// ---------------------------------------------------------------- ticking

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
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
				removeGuide(player.getUUID(), false);
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
			retire(player, progress, progress.chapterOffered ? "chapters.no_answer"
					: progress.askedAtTick >= 0 ? "extend.no_answer" : "farewell");
			return;
		}

		boolean npc = progress.npcEnabled(cfg.npcEnabledByDefault);
		GuideEntity guide = null;
		if (npc) {
			guide = ensureGuide(player);
			if (guide == null) return;
		} else {
			removeGuide(player.getUUID(), true);
		}

		if (firstMeeting) {
			say(player, guide, Component.translatable("survivalassistant.welcome.1", player.getName(), guideName(player)), false);
			say(player, guide, Component.translatable(npc ? "survivalassistant.welcome.2" : "survivalassistant.welcome_no_npc"), false);
			LAST_EVAL.put(player.getUUID(), 0L); // give the first tip on the next pass
			return;
		}

		if (step == GuideLifecycle.Step.ASK) {
			progress.askedAtTick = player.level().getOverworldClockTime();
			askToExtend(player, guide, "survivalassistant.extend.ask");
		} else if (step == GuideLifecycle.Step.OFFER_CHAPTER) {
			offerNextChapter(player, guide, progress);
		} else if (step == GuideLifecycle.Step.REMIND) {
			progress.reminded = true;
			if (progress.chapterOffered) showChapters(player, guide, progress, "survivalassistant.chapters.reminder");
			else askToExtend(player, guide, "survivalassistant.extend.reminder");
		}

		if (progress.comfortPending) {
			progress.comfortPending = false;
			say(player, guide, randomVariant("survivalassistant.comfort", 2, player.getRandom()), true);
		}

		checkDepthNotices(player, progress, guide);

		long now = player.level().getGameTime();
		long interval = cfg.tipIntervalSeconds * 20L;
		Long last = LAST_EVAL.get(player.getUUID());
		if (last != null && now - last < interval) return;
		LAST_EVAL.put(player.getUUID(), now);

		evaluateAndSay(player, guide, progress, false);
	}

	/** Evaluates the situation, says whatever is due and refreshes the task card. */
	private static void evaluateAndSay(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress, boolean forced) {
		long now = player.level().getGameTime();
		DigTracker dig = DIGS.get(player.getUUID());
		SurvivalContext ctx = ContextCollector.collect(player, progress, dig == null ? 0 : dig.current(now),
				progress.homeFallback == null ? pos -> progress.homeFallback = mark(player, pos) : null);
		respond(player, guide, ctx, progress, forced);
		checkPlaceNotices(player, guide, ctx, progress);
		GuideChat.observe(player, guide, ctx, progress);
		GuideMusings.observe(player, guide, ctx, progress);
		HudState.update(player, HudSnapshot.of(ctx, progress, dayLimit(progress), waypoint(player, progress, ctx, now)));
	}

	// ---------------------------------------------------------------- navigation (death spot, home)

	private static Optional<Navigation.Waypoint> waypoint(ServerPlayer player, GuideProgress progress, SurvivalContext ctx, long now) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.navigation) return Optional.empty();
		Navigation.Mark here = mark(player, player.blockPosition());

		DeathRecord death = DEATHS.get(player.getUUID());
		if (death != null && (Navigation.reached(death.mark(), here) || now - death.tick() >= Navigation.ITEM_LIFETIME_TICKS)) {
			DEATHS.remove(player.getUUID());
			death = null;
		}
		boolean duskOrNight = ctx.isDusk() || ctx.isNight();
		return Navigation.pick(death == null ? null : death.mark(), death == null ? 0 : death.tick(),
				home(player, progress), here, now, duskOrNight, cfg.homeDistance);
	}

	/** The bed/respawn point if the player has one, otherwise the first workstation they placed. */
	private static Navigation.@Nullable Mark home(ServerPlayer player, GuideProgress progress) {
		ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
		if (respawn != null) {
			BlockPos pos = respawn.respawnData().pos();
			return new Navigation.Mark(respawn.respawnData().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
		}
		return progress.homeFallback;
	}

	private static Navigation.Mark mark(ServerPlayer player, BlockPos pos) {
		return new Navigation.Mark(player.level().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
	}

	/** Called when a player dies: remember the spot so the task card can lead them back to their items. */
	public static void onDeath(ServerPlayer player) {
		DEATHS.put(player.getUUID(), new DeathRecord(mark(player, player.blockPosition()), player.level().getGameTime()));
	}

	/** Called after a player breaks a block: detects digging straight down (the block right under the feet). */
	public static void onBlockBroken(ServerPlayer player, BlockPos pos) {
		if (pos.getX() != player.getBlockX() || pos.getZ() != player.getBlockZ() || pos.getY() != player.getBlockY() - 1) return;
		int streak = DIGS.computeIfAbsent(player.getUUID(), id -> new DigTracker()).record(player.level().getGameTime());
		if (streak >= 2) LAST_EVAL.remove(player.getUUID()); // react on the next pass instead of waiting for the interval
	}

	private static void respond(ServerPlayer player, @Nullable GuideEntity guide, SurvivalContext ctx, GuideProgress progress, boolean forced) {
		boolean npc = progress.npcEnabled(ModConfig.get().npcEnabledByDefault);
		List<AdviceEngine.Message> messages = AdviceEngine.evaluate(ctx, progress, player.level().getGameTime(), settings(), forced);
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
				sayTip(player, guide, m.tip(), m.urgent());
			}
		}
		if (forced && messages.isEmpty()) {
			say(player, guide, Component.translatable("survivalassistant.all_done"), false);
		}
	}

	private static AdviceEngine.Settings settings() {
		ModConfig cfg = ModConfig.get();
		return new AdviceEngine.Settings(cfg.urgentCooldownSeconds * 20L, cfg.milestoneCooldownSeconds * 20L);
	}

	/** Configured period of the current chapter (without extensions); 0 = no limit. */
	private static int baseDays(GuideProgress progress) {
		return Chapters.get(progress.chapter).limited() ? ModConfig.get().maxDays : 0;
	}

	/** The guide period including extensions; 0 = no limit (the guide stays while the chapter runs). */
	public static int dayLimit(GuideProgress progress) {
		int base = baseDays(progress);
		return base <= 0 ? 0 : base + progress.extraDays;
	}

	/** Another chapter can be started after the current one. */
	private static boolean nextChapterAvailable(GuideProgress progress) {
		return ModConfig.get().moreChapters && !Chapters.available(progress.chapter, progress.completed).isEmpty();
	}

	private static int doneInChapter(GuideProgress progress) {
		int n = 0;
		for (Tip m : TipRegistry.milestones(progress.chapter)) if (progress.completed.contains(m.id())) n++;
		return n;
	}

	private static GuideLifecycle.Step lifecycle(ServerPlayer player, GuideProgress progress) {
		ModConfig cfg = ModConfig.get();
		return GuideLifecycle.decide(new GuideLifecycle.State(dayIndex(player, progress), baseDays(progress), progress.extraDays,
				cfg.extendDays, TipRegistry.chapterDone(progress.chapter, progress.completed), nextChapterAvailable(progress),
				progress.chapterOffered, progress.finished, progress.askedAtTick, progress.reminded,
				player.level().getOverworldClockTime()));
	}

	public static int dayIndex(ServerPlayer player, GuideProgress progress) {
		long clock = player.level().getOverworldClockTime();
		return (int) Math.max(0, clock / 24000L - progress.startTick / 24000L);
	}

	// ---------------------------------------------------------------- guide entity

	private static @Nullable GuideEntity ensureGuide(ServerPlayer player) {
		GuideEntity guide = GUIDES.get(player.getUUID());
		boolean usable = guide != null && !guide.isRemoved() && guide.level() == player.level()
				&& guide.distanceToSqr(player) < RESPAWN_DIST_SQR;
		if (usable) return guide;

		if (guide != null && !guide.isRemoved()) guide.discard();
		ServerLevel level = player.level();
		GuideEntity fresh = SurvivalAssistant.GUIDE.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (fresh == null) return null;
		fresh.setOwner(player);
		fresh.setCustomName(Component.literal(guideName(player)));
		fresh.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0f);
		FollowOwnerGoal.teleportNear(fresh, player.blockPosition());
		level.addFreshEntity(fresh);
		GUIDES.put(player.getUUID(), fresh);
		return fresh;
	}

	private static void removeGuide(UUID playerId, boolean poof) {
		GuideEntity guide = GUIDES.remove(playerId);
		if (guide != null && !guide.isRemoved()) {
			if (poof && guide.level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.POOF, guide.getX(), guide.getY() + 1, guide.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
			}
			guide.discard();
		}
	}

	private static void retire(ServerPlayer player, GuideProgress progress, String messageKey) {
		GuideEntity guide = GUIDES.get(player.getUUID());
		int done = doneInChapter(progress);
		int total = TipRegistry.milestones(progress.chapter).size();
		say(player, guide, Component.translatable("survivalassistant." + messageKey + ".1"), false);
		say(player, guide, Component.translatable("survivalassistant.farewell.summary", done, total), false);
		if (ModConfig.get().allowRecall) {
			say(player, guide, Component.translatable("survivalassistant.farewell.recall"), false);
		}
		progress.retired = true;
		removeGuide(player.getUUID(), true);
		HudState.update(player, HudSnapshot.INACTIVE);
	}

	/** All goals of the last chapter reached: a proper celebration, then the guide leaves. */
	private static void congratulate(ServerPlayer player, GuideProgress progress) {
		GuideEntity guide = GUIDES.get(player.getUUID());
		String key = progress.chapter <= 1 ? "survivalassistant.congrats." : "survivalassistant.congrats.all.";
		say(player, guide, Component.translatable(key + "1").withStyle(ChatFormatting.GREEN), false);
		say(player, guide, Component.translatable(key + "2"), false);
		if (ModConfig.get().allowRecall) {
			say(player, guide, Component.translatable("survivalassistant.farewell.recall"), false);
		}
		celebrate(player, guide, progress);
		progress.finished = true;
		progress.retired = true;
		progress.askedAtTick = -1;
		removeGuide(player.getUUID(), true);
		HudState.update(player, HudSnapshot.INACTIVE);
	}

	/** All goals of a chapter reached: celebrate, then show the chapter menu. The guide stays until the player picks. */
	private static void offerNextChapter(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress) {
		Chapter done = Chapters.get(progress.chapter);
		say(player, guide, (done.number() == 1 ? Component.translatable("survivalassistant.congrats.1")
				: Component.translatable("survivalassistant.chapter.done", Component.translatable(done.titleKey())))
				.withStyle(ChatFormatting.GREEN), false);
		celebrate(player, guide, progress);
		progress.chapterOffered = true;
		progress.askedAtTick = player.level().getOverworldClockTime();
		progress.reminded = false;
		showChapters(player, guide, progress, "survivalassistant.chapters.offer");
	}

	/**
	 * The chapter menu in chat: finished chapters in green, the ones you can start as buttons, locked ones in grey
	 * with what they need. After a finished chapter it ends with a button to let the guide go.
	 */
	private static void showChapters(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress, String headerKey) {
		say(player, guide, Component.translatable(headerKey).withStyle(ChatFormatting.AQUA), true);
		for (Chapter c : Chapters.ALL) {
			Component title = Component.translatable(c.titleKey());
			MutableComponent line = Component.literal("   ");
			switch (Chapters.status(c, progress.chapter, progress.completed)) {
				case DONE -> line.append(Component.literal("\u2714 " + c.number() + ". ").append(title).withStyle(ChatFormatting.GREEN));
				case CURRENT -> line.append(Component.literal("\u25B6 " + c.number() + ". ").append(title)
						.append(" ").append(Component.translatable("survivalassistant.chapters.current")).withStyle(ChatFormatting.GOLD));
				case AVAILABLE -> line.append(Component.literal("[" + c.number() + ". ").append(title).append("]")
						.withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withBold(true)
								.withClickEvent(new ClickEvent.RunCommand("/guide chapter " + c.id()))
								.withHoverEvent(new HoverEvent.ShowText(Component.translatable("survivalassistant.chapter." + c.id() + ".desc")))));
				case LOCKED -> {
					MutableComponent needs = Component.empty();
					for (int i = 0; i < c.requires().size(); i++) {
						if (i > 0) needs.append(", ");
						needs.append(Component.translatable(Chapters.get(c.requires().get(i)).titleKey()));
					}
					line.append(Component.literal(c.number() + ". ").append(title).append(" ")
							.append(Component.translatable("survivalassistant.chapters.locked", needs)).withStyle(ChatFormatting.GRAY));
				}
			}
			player.sendSystemMessage(line);
		}
		if (progress.chapterOffered) {
			player.sendSystemMessage(Component.literal("   ")
					.append(button("survivalassistant.chapters.leave", "survivalassistant.chapters.leave_hover", "/guide dismiss", ChatFormatting.RED)));
		}
	}

	/** One-time notes about special places (the Nether, a village, a night in a village). */
	private static void checkPlaceNotices(ServerPlayer player, @Nullable GuideEntity guide, SurvivalContext ctx, GuideProgress progress) {
		if (!ModConfig.get().depthNotices) return;
		String id = PlaceNotices.next(ctx, progress.depthNotices);
		if (id == null) return;
		progress.depthNotices.add(id);
		say(player, guide, Component.translatable("survivalassistant.place." + id).withStyle(ChatFormatting.AQUA), true);
	}

	private static MutableComponent button(String labelKey, String hoverKey, String command, ChatFormatting color, Object... args) {
		return Component.literal("[").append(Component.translatable(labelKey, args)).append("]")
				.withStyle(Style.EMPTY.withColor(color).withBold(true)
						.withClickEvent(new ClickEvent.RunCommand(command))
						.withHoverEvent(new HoverEvent.ShowText(Component.translatable(hoverKey, args))));
	}

	/** Title, sound and particles. */
	private static void celebrate(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress) {
		int total = TipRegistry.milestones(progress.chapter).size();
		player.connection.send(new ClientboundSetTitleTextPacket(
				Component.translatable("survivalassistant.congrats.title").withStyle(ChatFormatting.GOLD)));
		player.connection.send(new ClientboundSetSubtitleTextPacket(
				Component.translatable("survivalassistant.congrats.subtitle", total)));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
		player.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(),
				60, 0.6, 0.8, 0.6, 0.3);
		if (guide != null && !guide.isRemoved()) {
			player.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, guide.getX(), guide.getY() + 1, guide.getZ(),
					40, 0.4, 0.8, 0.4, 0.3);
		}
	}

	/** "Do you still need me?" with clickable yes/no buttons. */
	private static void askToExtend(ServerPlayer player, @Nullable GuideEntity guide, String questionKey) {
		int days = ModConfig.get().extendDays;
		say(player, guide, Component.translatable(questionKey).withStyle(ChatFormatting.AQUA), true);
		player.sendSystemMessage(Component.literal("   ")
				.append(button("survivalassistant.extend.yes", "survivalassistant.extend.yes_hover", "/guide extend", ChatFormatting.GREEN, days))
				.append("  ")
				.append(button("survivalassistant.extend.no", "survivalassistant.extend.no_hover", "/guide dismiss", ChatFormatting.RED)));
	}

	// ---------------------------------------------------------------- talking

	private static void sayTip(ServerPlayer player, @Nullable GuideEntity guide, Tip tip, boolean urgent) {
		MutableComponent text = randomVariant("survivalassistant.tip." + tip.id(), tip.variants(), player.getRandom());
		say(player, guide, urgent ? text.withStyle(ChatFormatting.YELLOW) : text, urgent);
		if (urgent) player.sendOverlayMessage(text.copy().withStyle(ChatFormatting.GOLD));
		if (guide != null && ModConfig.get().pointAtResources) {
			if (tip == TipRegistry.WOOD) pointAtNearest(guide, s -> s.is(BlockTags.LOGS));
			else if (tip == TipRegistry.STONE_TOOLS) pointAtNearest(guide, s -> s.is(Blocks.STONE) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.DEEPSLATE));
		}
	}

	private static MutableComponent randomVariant(String baseKey, int variants, RandomSource random) {
		int v = 1 + random.nextInt(Math.max(1, variants));
		return Component.translatable(baseKey + "." + v);
	}

	/** The player's guide character, or null when it isn't around (character off, retired, not spawned yet). */
	public static @Nullable GuideEntity guideOf(ServerPlayer player) {
		GuideEntity guide = GUIDES.get(player.getUUID());
		return guide == null || guide.isRemoved() ? null : guide;
	}

	/** A chat line from the guide (used by the small talk), with the usual name tag and a soft sound. */
	public static void speak(ServerPlayer player, @Nullable GuideEntity guide, Component text) {
		say(player, guide, text, false);
	}

	private static void say(ServerPlayer player, @Nullable GuideEntity guide, Component text, boolean important) {
		MutableComponent line = Component.literal("[").withStyle(ChatFormatting.GOLD)
				.append(Component.literal(guideName(player)).withStyle(ChatFormatting.GOLD))
				.append(Component.literal("] ").withStyle(ChatFormatting.GOLD))
				.append(text);
		player.sendSystemMessage(line);
		GuideMusings.noteRemark(player);
		if (guide != null) {
			guide.level().playSound(null, guide.getX(), guide.getY(), guide.getZ(),
					important ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL,
					important ? 0.9f : 0.5f, 1.15f);
		} else if (important) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
		}
	}

	private static void checkDepthNotices(ServerPlayer player, GuideProgress progress, @Nullable GuideEntity guide) {
		if (!ModConfig.get().depthNotices) return;
		String id = OreTable.newNotice(player.getBlockY(), BiomeGroup.of(biomeId(player)), progress.depthNotices);
		if (id == null) return;
		progress.depthNotices.add(id);
		say(player, guide, Component.translatable("survivalassistant.depth." + id).withStyle(ChatFormatting.AQUA), true);
	}

	public static String biomeId(ServerPlayer player) {
		return player.level().getBiome(player.blockPosition()).unwrapKey()
				.map(k -> k.identifier().toString()).orElse("");
	}

	private static void pointAtNearest(GuideEntity guide, Predicate<BlockState> match) {
		BlockPos origin = guide.blockPosition();
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-12, -6, -12), origin.offset(12, 6, 12))) {
			if (!match.test(guide.level().getBlockState(pos))) continue;
			double d = pos.distSqr(origin);
			if (d < bestDist) {
				bestDist = d;
				best = pos.immutable();
			}
		}
		if (best != null) guide.pointAt(best);
	}

	// ---------------------------------------------------------------- events & commands

	public static void onGuideClicked(ServerPlayer player, GuideEntity guide) {
		GuideProgress progress = GuideState.of(player);
		evaluateAndSay(player, guide, progress, true);
		int done = doneInChapter(progress), total = TipRegistry.milestones(progress.chapter).size();
		// Later chapters have no day limit, so they show the chapter instead of the days left.
		Component line = dayLimit(progress) <= 0
				? Component.translatable("survivalassistant.progress_chapter", done, total,
						Component.translatable(Chapters.get(progress.chapter).titleKey()))
				: Component.translatable("survivalassistant.progress", done, total,
						Math.max(0, dayLimit(progress) - dayIndex(player, progress)));
		say(player, null, line.copy().withStyle(ChatFormatting.GRAY), false);
	}

	public static void onRespawn(ServerPlayer newPlayer) {
		GuideProgress progress = GuideState.of(newPlayer);
		if (!progress.retired && progress.startTick >= 0) progress.comfortPending = true;
	}

	public static void onDisconnect(ServerPlayer player) {
		removeGuide(player.getUUID(), false);
		LAST_EVAL.remove(player.getUUID());
		DIGS.remove(player.getUUID());
		GuideChat.forget(player.getUUID());
		GuideMusings.forget(player.getUUID());
	}

	public static void onServerStopping() {
		GUIDES.values().forEach(g -> { if (!g.isRemoved()) g.discard(); });
		GUIDES.clear();
		LAST_EVAL.clear();
		DEATHS.clear();
		DIGS.clear();
		GuideChat.clear();
		GuideMusings.clear();
	}

	/** /guide tip */
	public static boolean tipNow(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired) return false;
		GuideEntity guide = null;
		if (npcEnabled(player)) {
			guide = ensureGuide(player);
			if (guide == null) return false;
		}
		evaluateAndSay(player, guide, progress, true);
		return true;
	}

	/** The guide's name for this player: their own choice, else the server's (see {@link GuideNames}). */
	public static String guideName(ServerPlayer player) {
		return GuideNames.effective(GuideState.of(player).guideName, ModConfig.get().guideName);
	}

	/** /guide name: a new name (null = back to the default); the name tag changes at once. */
	public static void rename(ServerPlayer player, @Nullable String name) {
		GuideState.of(player).guideName = name == null ? null : name.strip();
		GuideEntity guide = GUIDES.get(player.getUUID());
		if (guide != null && !guide.isRemoved()) guide.setCustomName(Component.literal(guideName(player)));
	}

	/** /guide about: the guide tells the next thing about itself. */
	public static boolean tellAbout(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired || progress.startTick < 0) return false;
		GuideEntity guide = npcEnabled(player) ? ensureGuide(player) : null;
		say(player, guide, Component.translatable(nextAbout(progress)), false);
		return true;
	}

	/** The next "about me" line; every call moves on, so the guide doesn't keep telling the same story. */
	public static String nextAbout(GuideProgress progress) {
		String key = Persona.aboutKey(progress.aboutIndex);
		progress.aboutIndex = (progress.aboutIndex + 1) % Persona.ABOUT_COUNT;
		return key;
	}

	public static boolean npcEnabled(ServerPlayer player) {
		return GuideState.of(player).npcEnabled(ModConfig.get().npcEnabledByDefault);
	}

	/** /guide character on|off: stored on the player, takes effect immediately. */
	public static void setNpcEnabled(ServerPlayer player, boolean enabled) {
		GuideProgress progress = GuideState.of(player);
		progress.npcEnabled = enabled;
		if (!enabled) {
			removeGuide(player.getUUID(), true);
		} else if (!progress.retired && progress.startTick >= 0) {
			GuideEntity guide = ensureGuide(player);
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
		GuideEntity guide = ensureGuide(player);
		if (guide == null) return false;
		FollowOwnerGoal.teleportNear(guide, player.blockPosition());
		say(player, guide, Component.translatable("survivalassistant.recall"), false);
		HudState.update(player, HudSnapshot.of(ContextCollector.collect(player, progress), progress, dayLimit(progress)));
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

	/** /guide extend: the guide stays a few more days. */
	public static boolean extend(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		int days = ModConfig.get().extendDays;
		if (progress.retired || progress.startTick < 0 || days <= 0 || progress.chapterOffered) return false;
		progress.extraDays += days;
		progress.askedAtTick = -1;
		progress.reminded = false;
		GuideEntity guide = GUIDES.get(player.getUUID());
		int left = Math.max(0, dayLimit(progress) - dayIndex(player, progress));
		say(player, guide, Component.translatable("survivalassistant.extend.confirmed", left).withStyle(ChatFormatting.GREEN), false);
		LAST_EVAL.remove(player.getUUID()); // refresh the card on the next pass
		return true;
	}

	/** /guide continue: start the first chapter that is open (after the current one is finished). */
	public static boolean continueChapter(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (!TipRegistry.chapterDone(progress.chapter, progress.completed)) return false;
		java.util.List<Chapter> open = Chapters.available(progress.chapter, progress.completed);
		return !open.isEmpty() && startChapter(player, open.get(0));
	}

	/** /guide chapter <id>: start or switch to a chapter that is open. Progress in other chapters is kept. */
	public static boolean startChapter(ServerPlayer player, Chapter chapter) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired || progress.startTick < 0 || !ModConfig.get().moreChapters) return false;
		if (Chapters.status(chapter, progress.chapter, progress.completed) != Chapters.Status.AVAILABLE) return false;
		progress.chapter = chapter.number();
		progress.restart(player.level().getOverworldClockTime());
		GuideEntity guide = GUIDES.get(player.getUUID());
		say(player, guide, Component.translatable("survivalassistant.chapter." + chapter.id() + ".start").withStyle(ChatFormatting.GREEN), true);
		LAST_EVAL.put(player.getUUID(), 0L); // first tip of the chapter on the next pass
		return true;
	}

	/** /guide chapters: show the chapter menu. */
	public static boolean chapters(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired || progress.startTick < 0) return false;
		showChapters(player, GUIDES.get(player.getUUID()), progress, "survivalassistant.chapters.header");
		return true;
	}

	/** /guide mute <minutes> */
	public static void mute(ServerPlayer player, int minutes) {
		GuideState.of(player).muteUntil = player.level().getGameTime() + minutes * 60L * 20L;
	}
}
