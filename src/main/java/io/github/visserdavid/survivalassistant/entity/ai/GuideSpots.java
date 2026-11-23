package io.github.visserdavid.survivalassistant.entity.ai;

import io.github.visserdavid.survivalassistant.advice.GuideSpacing;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jspecify.annotations.Nullable;

/** Finds a safe spot for the guide near its owner: beside or behind them, never in front or in the way. */
public final class GuideSpots {
	private GuideSpots() {}

	/** The best free spot around {@code center}, scored against the player's view; null when none is found. */
	public static @Nullable BlockPos find(GuideEntity guide, BlockPos center, float playerYaw) {
		BlockPos best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		int r = (int) Math.ceil(GuideSpacing.MAX_SPOT);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double score = GuideSpacing.score(playerYaw, dx, dz);
				if (score == Double.NEGATIVE_INFINITY || score <= bestScore) continue;
				for (int dy = -1; dy <= 1; dy++) {
					BlockPos pos = center.offset(dx, dy, dz);
					if (isSafe(guide, pos)) {
						best = pos;
						bestScore = score;
						break;
					}
				}
			}
		}
		return best;
	}

	/** A spot for the guide near {@code player}. */
	public static @Nullable BlockPos find(GuideEntity guide, Player player) {
		return find(guide, player.blockPosition(), player.getYRot());
	}

	/** Walkable, room for the guide, and nothing it could trigger or damage there. */
	static boolean isSafe(GuideEntity guide, BlockPos pos) {
		// WALKABLE excludes lava, fire and damaging blocks and the spots right next to them
		if (WalkNodeEvaluator.getPathTypeStatic(guide, pos) != PathType.WALKABLE) return false;
		Level level = guide.level();
		BlockState feet = level.getBlockState(pos);
		if (feet.getBlock() instanceof BasePressurePlateBlock || feet.getBlock() instanceof TripWireBlock) return false;
		BlockState floor = level.getBlockState(pos.below());
		if (floor.getBlock() instanceof FarmlandBlock || floor.is(Blocks.POWDER_SNOW)) return false;
		BlockPos delta = pos.subtract(guide.blockPosition());
		return level.noCollision(guide, guide.getBoundingBox().move(delta));
	}
}
