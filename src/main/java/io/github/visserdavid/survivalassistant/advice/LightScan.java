package io.github.visserdavid.survivalassistant.advice;

/**
 * Rules for the light check (key L): which spots could spawn monsters. Pure logic; the client gathers the
 * block facts. Verified against 26.3: the overworld's dimension type allows monster spawns only at block
 * light 0 ({@code monster_spawn_block_light_limit}); the Nether uses 15, so light doesn't help there.
 */
public final class LightScan {
	private LightScan() {}

	/** Horizontal scan radius in blocks. */
	public static final int RADIUS = 16;
	/** Blocks scanned above and below the player's feet. */
	public static final int VERTICAL = 6;
	/** Most markers shown at once (nearest first), to keep particles cheap. */
	public static final int MAX_MARKERS = 250;
	/** Rescan / redraw interval in ticks (0.5 s). */
	public static final int INTERVAL_TICKS = 10;
	/** "Well lit" check on the server: no spawnable spot within this radius (and {@link #LIT_VERTICAL} up/down). */
	public static final int LIT_RADIUS = 6;
	public static final int LIT_VERTICAL = 2;
	/** Block light at or above this value never allows spawns in any dimension. */
	public static final int MAX_LIGHT = 15;

	/** Light only prevents spawns in dimensions whose limit is below 15 (overworld 0, Nether 15). */
	public static boolean lightMatters(int dimensionBlockLightLimit) {
		return dimensionBlockLightLimit < MAX_LIGHT;
	}

	/**
	 * @param blockLight light from torches etc. at the feet position
	 * @param limit      the dimension's monster spawn block light limit
	 * @param floorOk    the block below is a valid spawn floor (solid top, not glass/leaves/slab top etc.)
	 * @param feetFree   feet position has no collision and no fluid
	 * @param headFree   the block above the feet is free too (zombies and skeletons are 2 blocks tall)
	 */
	public static boolean monsterCanSpawn(int blockLight, int limit, boolean floorOk, boolean feetFree, boolean headFree) {
		return floorOk && feetFree && headFree && blockLight <= limit;
	}
}
