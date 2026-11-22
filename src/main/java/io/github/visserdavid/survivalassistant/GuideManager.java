package io.github.visserdavid.survivalassistant;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import io.github.visserdavid.survivalassistant.advice.OreTable;
import io.github.visserdavid.survivalassistant.advice.ContextCollector;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.SurvivalContext;
import io.github.visserdavid.survivalassistant.advice.Tip;
import io.github.visserdavid.survivalassistant.advice.TipRegistry;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import io.github.visserdavid.survivalassistant.entity.ai.FollowOwnerGoal;
import io.github.visserdavid.survivalassistant.state.GuideState;
import io.github.visserdavid.survivalassistant.state.HudState;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Server-side brain: keeps one guide next to every player who still has an active guide,
 * runs the advice engine periodically and handles the guide's lifecycle.
 */
public final class GuideManager {
	private static final Map<UUID, GuideEntity> GUIDES = new HashMap<>();
	private static final Map<UUID, Long> LAST_EVAL = new HashMap<>();
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

		if (dayIndex(player, progress) >= cfg.maxDays) {
			retire(player, progress, "farewell");
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
			say(player, guide, Component.translatable("survivalassistant.welcome.1", player.getName()), false);
			say(player, guide, Component.translatable(npc ? "survivalassistant.welcome.2" : "survivalassistant.welcome_no_npc"), false);
			LAST_EVAL.put(player.getUUID(), 0L); // give the first tip on the next pass
			return;
		}

		if (progress.comfortPending) {
			progress.comfortPending = false;
			say(player, guide, randomVariant("survivalassistant.comfort", 2, player.getRandom()), true);
		}

		checkDepthNotices(player, progress, guide);

		long now = player.level().getGameTime();
		long interval = Math.max(1, cfg.tipIntervalSeconds) * 20L;
		Long last = LAST_EVAL.get(player.getUUID());
		if (last != null && now - last < interval) return;
		LAST_EVAL.put(player.getUUID(), now);

		evaluateAndSay(player, guide, progress, false);
	}

	/** Evaluates the situation, says whatever is due and refreshes the task card. */
	private static void evaluateAndSay(ServerPlayer player, @Nullable GuideEntity guide, GuideProgress progress, boolean forced) {
		SurvivalContext ctx = ContextCollector.collect(player, progress);
		respond(player, guide, ctx, progress, forced);
		HudState.update(player, HudSnapshot.of(ctx, progress, ModConfig.get().maxDays));
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
		fresh.setCustomName(Component.translatable("entity.survivalassistant.guide"));
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
		int done = progress.completed.size();
		int total = TipRegistry.MILESTONES.size();
		say(player, guide, Component.translatable("survivalassistant." + messageKey + ".1"), false);
		say(player, guide, Component.translatable("survivalassistant.farewell.summary", done, total), false);
		if (ModConfig.get().allowRecall) {
			say(player, guide, Component.translatable("survivalassistant.farewell.recall"), false);
		}
		progress.retired = true;
		removeGuide(player.getUUID(), true);
		HudState.update(player, HudSnapshot.INACTIVE);
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

	private static void say(ServerPlayer player, @Nullable GuideEntity guide, Component text, boolean important) {
		MutableComponent line = Component.literal("[").withStyle(ChatFormatting.GOLD)
				.append(Component.translatable("entity.survivalassistant.guide").withStyle(ChatFormatting.GOLD))
				.append(Component.literal("] ").withStyle(ChatFormatting.GOLD))
				.append(text);
		player.sendSystemMessage(line);
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
		say(player, null, Component.translatable("survivalassistant.progress",
				progress.completed.size(), TipRegistry.MILESTONES.size(),
				Math.max(0, ModConfig.get().maxDays - dayIndex(player, progress))).withStyle(ChatFormatting.GRAY), false);
	}

	public static void onRespawn(ServerPlayer newPlayer) {
		GuideProgress progress = GuideState.of(newPlayer);
		if (!progress.retired && progress.startTick >= 0) progress.comfortPending = true;
	}

	public static void onDisconnect(ServerPlayer player) {
		removeGuide(player.getUUID(), false);
		LAST_EVAL.remove(player.getUUID());
	}

	public static void onServerStopping() {
		GUIDES.values().forEach(g -> { if (!g.isRemoved()) g.discard(); });
		GUIDES.clear();
		LAST_EVAL.clear();
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
			progress.retired = false;
			progress.startTick = player.level().getOverworldClockTime();
		} else if (progress.startTick < 0) {
			progress.startTick = player.level().getOverworldClockTime();
		}
		GuideEntity guide = ensureGuide(player);
		if (guide == null) return false;
		FollowOwnerGoal.teleportNear(guide, player.blockPosition());
		say(player, guide, Component.translatable("survivalassistant.recall"), false);
		HudState.update(player, HudSnapshot.of(ContextCollector.collect(player, progress), progress, ModConfig.get().maxDays));
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

	/** /guide mute <minutes> */
	public static void mute(ServerPlayer player, int minutes) {
		GuideState.of(player).muteUntil = player.level().getGameTime() + minutes * 60L * 20L;
	}
}
