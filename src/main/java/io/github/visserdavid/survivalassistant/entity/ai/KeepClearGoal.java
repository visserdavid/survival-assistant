package io.github.visserdavid.survivalassistant.entity.ai;

import io.github.visserdavid.survivalassistant.advice.GuideSpacing;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Steps out of the way: when the guide is very close to its owner or stands in front of them, it walks to a spot
 * beside or behind them. If it can't get there within two seconds (a narrow tunnel, for example), it teleports.
 */
public class KeepClearGoal extends Goal {
	private static final int GIVE_UP_TICKS = 40;

	private final GuideEntity guide;
	private final double speed;
	private ServerPlayer owner;
	private BlockPos spot;
	private int ticks;
	/** After a search that found no free spot (a narrow tunnel), wait before searching again: it is costly. */
	private int retryAt;
	private static final int RETRY_TICKS = 20;

	public KeepClearGoal(GuideEntity guide, double speed) {
		this.guide = guide;
		this.speed = speed;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	private static boolean inTheWay(GuideEntity guide, ServerPlayer owner) {
		return GuideSpacing.inTheWay(owner.getYRot(), guide.getX() - owner.getX(), guide.getZ() - owner.getZ())
				&& Math.abs(guide.getY() - owner.getY()) < 3;
	}

	@Override
	public boolean canUse() {
		ServerPlayer p = guide.getOwner();
		if (p == null || p.isSpectator() || p.level() != guide.level() || !inTheWay(guide, p)) return false;
		if (guide.tickCount < retryAt) return false;
		BlockPos found = GuideSpots.find(guide, p);
		if (found == null) {
			retryAt = guide.tickCount + RETRY_TICKS;
			return false;
		}
		owner = p;
		spot = found;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return owner != null && !owner.isRemoved() && ticks < GIVE_UP_TICKS && inTheWay(guide, owner);
	}

	@Override
	public void start() {
		ticks = 0;
		guide.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, speed);
	}

	@Override
	public void tick() {
		ticks++;
		if (ticks % 10 == 0) {
			BlockPos fresh = GuideSpots.find(guide, owner); // the player keeps moving and turning
			if (fresh != null) {
				spot = fresh;
				guide.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, speed);
			}
		}
		if (ticks >= GIVE_UP_TICKS && inTheWay(guide, owner)) {
			FollowOwnerGoal.teleportTo(guide, GuideSpots.find(guide, owner));
		}
	}

	@Override
	public void stop() {
		owner = null;
		spot = null;
		guide.getNavigation().stop();
	}
}
