package io.github.visserdavid.survivalassistant.advice;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Where monsters could spawn, judged from the blocks and the light (rules in {@link LightScan}). Shared by the
 * light check on the client (key L) and the "well lit" check on the server, so both always agree.
 * A zombie is the measure: the most common 2-block-tall monster.
 */
public final class SpawnSpots {
	private SpawnSpots() {}

	/** A monster could spawn with its feet at {@code feet}: dark enough, a valid floor and room for its body. */
	public static boolean canSpawnAt(Level level, BlockPos feet, int limit) {
		int blockLight = level.getBrightness(LightLayer.BLOCK, feet);
		if (blockLight > limit) return false;
		BlockPos below = feet.below();
		BlockPos head = feet.above();
		BlockState floor = level.getBlockState(below);
		BlockState feetState = level.getBlockState(feet);
		BlockState headState = level.getBlockState(head);
		return LightScan.monsterCanSpawn(blockLight, limit,
				floor.isValidSpawn(level, below, EntityTypes.ZOMBIE),
				freeFor(level, feet, feetState), freeFor(level, head, headState));
	}

	private static boolean freeFor(BlockGetter level, BlockPos pos, BlockState state) {
		return NaturalSpawner.isValidEmptySpawnBlock(level, pos, state, state.getFluidState(), EntityTypes.ZOMBIE);
	}

	/**
	 * Calls {@code found} for every spot within a cylinder of {@code radius} around {@code origin} (and
	 * {@code vertical} blocks up and down) where a monster could spawn; stops early once {@code found} has been
	 * called {@code max} times. Unloaded positions are skipped.
	 *
	 * @return how many spots were found (at most {@code max})
	 */
	public static int scan(Level level, BlockPos origin, int radius, int vertical, int limit, int max,
			Consumer<BlockPos> found) {
		int count = 0;
		BlockPos.MutableBlockPos feet = new BlockPos.MutableBlockPos();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) continue;
				for (int dy = -vertical; dy <= vertical; dy++) {
					feet.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
					if (!level.isLoaded(feet)) continue;
					if (!canSpawnAt(level, feet, limit)) continue;
					found.accept(feet.immutable());
					if (++count >= max) return count;
				}
			}
		}
		return count;
	}
}
