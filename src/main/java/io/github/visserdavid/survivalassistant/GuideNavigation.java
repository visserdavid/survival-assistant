package io.github.visserdavid.survivalassistant;

import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.advice.Navigation;
import io.github.visserdavid.survivalassistant.advice.SurvivalContext;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/** The navigation strip on the task card: the way back to your items after dying, and the way home. */
final class GuideNavigation {
	private GuideNavigation() {}

	/** Where the strip points now (see {@link Navigation#pick}); forgets a death spot once reached or expired. */
	static Optional<Navigation.Waypoint> waypoint(ServerPlayer player, GuideProgress progress, SurvivalContext ctx, long now) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.navigation) return Optional.empty();
		Navigation.Mark here = mark(player, player.blockPosition());

		GuideSessions.Session session = GuideSessions.of(player.getUUID());
		GuideSessions.Death death = session.death;
		if (death != null && (Navigation.reached(death.mark(), here) || now - death.tick() >= Navigation.ITEM_LIFETIME_TICKS)) {
			session.death = null;
			death = null;
		}
		boolean duskOrNight = ctx.world().isDuskOrNight();
		return Navigation.pick(death == null ? null : death.mark(), death == null ? 0 : death.tick(),
				home(player, progress), here, now, duskOrNight, cfg.homeDistance);
	}

	/** Remembers where the player died, so the task card can lead them back to their items (not with keepInventory). */
	static void rememberDeath(ServerPlayer player) {
		if (player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) return;
		GuideSessions.of(player.getUUID()).death =
				new GuideSessions.Death(mark(player, player.blockPosition()), player.level().getGameTime());
	}

	/** The blocks that can be a home when the player has no bed spawn: a crafting table, a chest or a bed. */
	static boolean isHomeBlock(BlockState state) {
		return state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.CHEST) || state.is(BlockTags.BEDS);
	}

	/**
	 * The fallback home is the first crafting table, chest or bed the player placed in the overworld themselves (a
	 * village's or a shipwreck's doesn't count). It is forgotten once that block is gone.
	 */
	static void updateHome(ServerPlayer player, GuideProgress progress, GuideSessions.Session session) {
		ServerLevel level = player.level();
		if (level.dimension() != Level.OVERWORLD) return;
		BlockPos placed = session.placedHome;
		session.placedHome = null;
		if (progress.homeFallback == null && placed != null && isHomeBlock(level.getBlockState(placed))) {
			progress.homeFallback = mark(player, placed);
		}
		Navigation.Mark home = progress.homeFallback;
		if (home == null || !home.dimension().equals(mark(player, BlockPos.ZERO).dimension())) return;
		BlockPos pos = new BlockPos(home.x(), home.y(), home.z());
		if (level.isLoaded(pos) && !isHomeBlock(level.getBlockState(pos))) progress.homeFallback = null;
	}

	/** A position in the player's current dimension. */
	static Navigation.Mark mark(ServerPlayer player, BlockPos pos) {
		return new Navigation.Mark(player.level().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
	}

	/** The bed/respawn point if the player has one, otherwise the crafting table, chest or bed they placed. */
	private static Navigation.@Nullable Mark home(ServerPlayer player, GuideProgress progress) {
		ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
		if (respawn != null) {
			BlockPos pos = respawn.respawnData().pos();
			return new Navigation.Mark(respawn.respawnData().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
		}
		return progress.homeFallback;
	}
}
