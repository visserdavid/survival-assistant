package io.github.visserdavid.survivalassistant.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;

import java.util.EnumSet;

/** Walks after the owner; teleports next to them when they get too far away. */
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
				guide.getNavigation().moveTo(owner, speed);
			}
		}
	}

	/** Tries a few random spots 2-3 blocks around {@code target}. */
	public static boolean teleportNear(GuideEntity guide, BlockPos target) {
		var random = guide.getRandom();
		for (int attempt = 0; attempt < 15; attempt++) {
			int dx = random.nextIntBetweenInclusive(-3, 3);
			int dz = random.nextIntBetweenInclusive(-3, 3);
			if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
			int dy = random.nextIntBetweenInclusive(-1, 1);
			BlockPos pos = target.offset(dx, dy, dz);
			if (WalkNodeEvaluator.getPathTypeStatic(guide, pos) != PathType.WALKABLE) continue;
			BlockPos delta = pos.subtract(guide.blockPosition());
			if (!guide.level().noCollision(guide, guide.getBoundingBox().move(delta))) continue;
			guide.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, guide.getYRot(), guide.getXRot());
			guide.getNavigation().stop();
			return true;
		}
		return false;
	}
}
