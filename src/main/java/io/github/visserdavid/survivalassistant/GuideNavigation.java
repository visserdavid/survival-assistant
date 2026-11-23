package io.github.visserdavid.survivalassistant;

import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.advice.Navigation;
import io.github.visserdavid.survivalassistant.advice.SurvivalContext;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
		boolean duskOrNight = ctx.world().isDusk() || ctx.world().isNight();
		return Navigation.pick(death == null ? null : death.mark(), death == null ? 0 : death.tick(),
				home(player, progress), here, now, duskOrNight, cfg.homeDistance);
	}

	/** Remembers where the player died, so the task card can lead them back to their items. */
	static void rememberDeath(ServerPlayer player) {
		GuideSessions.of(player.getUUID()).death =
				new GuideSessions.Death(mark(player, player.blockPosition()), player.level().getGameTime());
	}

	/** A position in the player's current dimension. */
	static Navigation.Mark mark(ServerPlayer player, BlockPos pos) {
		return new Navigation.Mark(player.level().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
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
}
