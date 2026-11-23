package io.github.visserdavid.survivalassistant.advice;

/**
 * Where the guide may stand: never right next to the player and never in front of them, so it can't block an
 * escape route, a tunnel or the view. Pure geometry on the horizontal plane (x/z), no Minecraft types.
 *
 * <p>Minecraft yaw: 0 = looking towards +z (south), 90 = towards -x (west); the look vector is (-sin, cos).
 */
public final class GuideSpacing {
	private GuideSpacing() {}

	/** Closer than this the guide steps away. */
	public static final double PERSONAL_SPACE = 2.5;
	/** Teleport/stand spots must be at least this far away... */
	public static final double MIN_SPOT = 2.5;
	/** ...and at most this far. */
	public static final double MAX_SPOT = 5.0;
	/** Preferred distance from the player. */
	public static final double PREFERRED = 3.5;
	/** Half the opening angle of the view cone the guide keeps out of. */
	public static final double VIEW_HALF_ANGLE_DEG = 30;
	/** The view cone matters up to this distance. */
	public static final double VIEW_RANGE = 6.0;

	/** dx/dz from the player to the spot, normalised against the player's look direction: 1 = straight ahead, -1 = behind. */
	public static double facing(double yawDeg, double dx, double dz) {
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len < 1e-6) return 1; // on top of the player: treat as "in the way"
		double yaw = Math.toRadians(yawDeg);
		return (dx * -Math.sin(yaw) + dz * Math.cos(yaw)) / len;
	}

	/** The spot (dx, dz from the player) is in front of the player, within the view cone and range. */
	public static boolean inView(double yawDeg, double dx, double dz) {
		double dist = Math.sqrt(dx * dx + dz * dz);
		return dist <= VIEW_RANGE && facing(yawDeg, dx, dz) >= Math.cos(Math.toRadians(VIEW_HALF_ANGLE_DEG));
	}

	/** The guide at (dx, dz) from the player is in the way: too close, or in front of the player. */
	public static boolean inTheWay(double yawDeg, double dx, double dz) {
		return Math.sqrt(dx * dx + dz * dz) < PERSONAL_SPACE || inView(yawDeg, dx, dz);
	}

	/**
	 * Score of a spot (dx, dz from the player) where the guide could stand; higher is better,
	 * {@link Double#NEGATIVE_INFINITY} when the spot is not allowed (too close, too far or in view).
	 * Behind the player is best, beside is fine; around {@link #PREFERRED} blocks away.
	 */
	public static double score(double yawDeg, double dx, double dz) {
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist < MIN_SPOT || dist > MAX_SPOT || inView(yawDeg, dx, dz)) return Double.NEGATIVE_INFINITY;
		return -2 * facing(yawDeg, dx, dz) - Math.abs(dist - PREFERRED);
	}
}
