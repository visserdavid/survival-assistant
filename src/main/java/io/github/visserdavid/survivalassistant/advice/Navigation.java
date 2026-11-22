package io.github.visserdavid.survivalassistant.advice;

import java.util.Optional;

/**
 * Decides which place the task card should point to: the spot where the player died (while the dropped items
 * still exist) or home (when far away or when night is coming). Pure logic, unit tested.
 */
public final class Navigation {
	private Navigation() {}

	/** Dropped items despawn after 6000 ticks (ItemEntity.LIFETIME in 26.3), counted only while the chunk is loaded. */
	public static final int ITEM_LIFETIME_TICKS = 6000;
	/** Standing within this many blocks of the death spot counts as "items picked up". */
	public static final double REACHED_DISTANCE = 3.0;
	/** At dusk/night, home is shown from this distance on. */
	public static final int HOME_DISTANCE_AT_NIGHT = 24;

	/** A block position in a dimension, e.g. {@code minecraft:overworld}. */
	public record Mark(String dimension, int x, int y, int z) {
		public double distanceTo(Mark other) {
			double dx = x - other.x, dy = y - other.y, dz = z - other.z;
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		}

		public double horizontalDistanceTo(Mark other) {
			double dx = x - other.x, dz = z - other.z;
			return Math.sqrt(dx * dx + dz * dz);
		}
	}

	/** Where to point. {@code ticksLeft} is only used for {@link Kind#DEATH} (-1 otherwise). */
	public record Waypoint(Kind kind, Mark target, int ticksLeft) {}

	public enum Kind { DEATH, HOME }

	/**
	 * @param death       where the player last died, or null
	 * @param deathTick   game time of that death
	 * @param home        the player's home (bed/respawn point or first workstation), or null
	 * @param player      current player position
	 * @param duskOrNight true from sunset until morning
	 */
	public static Optional<Waypoint> pick(Mark death, long deathTick, Mark home, Mark player, long now,
			boolean duskOrNight, int homeDistance) {
		if (death != null) {
			long left = ITEM_LIFETIME_TICKS - (now - deathTick);
			if (left > 0) return Optional.of(new Waypoint(Kind.DEATH, death, (int) left));
		}
		if (home != null && home.dimension().equals(player.dimension())) {
			double d = home.horizontalDistanceTo(player);
			if (d > homeDistance || (duskOrNight && d > HOME_DISTANCE_AT_NIGHT)) {
				return Optional.of(new Waypoint(Kind.HOME, home, -1));
			}
		}
		return Optional.empty();
	}

	/** True when the player stands at (or next to) the target in the same dimension. */
	public static boolean reached(Mark target, Mark player) {
		return target.dimension().equals(player.dimension()) && target.distanceTo(player) <= REACHED_DISTANCE;
	}
}
