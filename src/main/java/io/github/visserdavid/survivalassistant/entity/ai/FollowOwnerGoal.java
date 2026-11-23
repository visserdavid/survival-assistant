package io.github.visserdavid.survivalassistant.entity.ai;

import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.PathType;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Walks after the owner, to a spot beside or behind them (never right up to them); teleports to such a spot when
 * they get too far away.
 */
public class FollowOwnerGoal extends Goal {
	private static final double TELEPORT_DIST_SQR = 20 * 20;

	private final GuideEntity guide;
	private final double speed;
	private final float startDistance;
	private final float stopDistance;
	private ServerPlayer owner;
	private int recalcPath;
	private float oldWaterCost;

	public FollowOwnerGoal(GuideEntity guide, double speed, float startDistance, float stopDistance) {
		this.guide = guide;
		this.speed = speed;
		this.startDistance = startDistance;
		this.stopDistance = stopDistance;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		ServerPlayer p = guide.getOwner();
		if (p == null || p.isSpectator()) return false;
		if (guide.distanceToSqr(p) < startDistance * startDistance) return false;
		owner = p;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (owner == null || owner.isRemoved() || guide.getNavigation().isDone()) return false;
		return guide.distanceToSqr(owner) > stopDistance * stopDistance;
	}

	@Override
	public void start() {
		recalcPath = 0;
		oldWaterCost = guide.getPathfindingMalus(PathType.WATER);
		guide.setPathfindingMalus(PathType.WATER, 0f);
	}

	@Override
	public void stop() {
		owner = null;
		guide.getNavigation().stop();
		guide.setPathfindingMalus(PathType.WATER, oldWaterCost);
	}

	@Override
	public void tick() {
		boolean far = guide.distanceToSqr(owner) >= TELEPORT_DIST_SQR;
		if (!far && !guide.isPointing()) {
			guide.getLookControl().setLookAt(owner, 10f, guide.getMaxHeadXRot());
		}
		if (--recalcPath <= 0) {
			recalcPath = adjustedTickDelay(10);
			if (far) {
				teleportNear(guide, owner.blockPosition());
			} else {
				// walk to a spot beside/behind the player, not into them
				BlockPos spot = GuideSpots.find(guide, owner);
				if (spot != null) {
					guide.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, speed);
				} else {
					guide.getNavigation().moveTo(owner, speed);
				}
			}
		}
	}

	/** Teleports the guide to a safe spot beside or behind its owner (or around {@code target} without an owner). */
	public static boolean teleportNear(GuideEntity guide, BlockPos target) {
		ServerPlayer owner = guide.getOwner();
		BlockPos spot = GuideSpots.find(guide, target, owner != null ? owner.getYRot() : guide.getYRot());
		return spot != null && teleportTo(guide, spot);
	}

	static boolean teleportTo(GuideEntity guide, @Nullable BlockPos spot) {
		if (spot == null) return false;
		guide.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, guide.getYRot(), guide.getXRot());
		guide.getNavigation().stop();
		return true;
	}
}
