package io.github.visserdavid.survivalassistant.advice;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * The specializations the guide offers after chapter 2. {@link GuideProgress#route} holds the choice:
 * {@code ""} = not chosen yet (the guide asks), {@link #FREE} = the player picks chapters themselves, or a route id.
 * Pure logic, no Minecraft types.
 */
public final class Routes {
	private Routes() {}

	/** The player chose to pick chapters themselves. */
	public static final String FREE = "free";

	public static final Route FIGHTER = new Route("fighter", List.of(4, 3, 14, 5, 12, 8, 16, 11, 10));
	public static final Route FARMER = new Route("farmer", List.of(6, 3, 4, 5, 17, 9, 18));
	public static final Route EXPLORER = new Route("explorer", List.of(8, 7, 15, 13, 4, 5, 10, 17));
	public static final Route RESEARCHER = new Route("researcher", List.of(9, 4, 5, 18, 8, 15, 13));

	public static final List<Route> ALL = List.of(FIGHTER, FARMER, EXPLORER, RESEARCHER);

	/** Values for the settings screen: {@link #FREE} first, then every route id. */
	public static final List<String> CHOICES = List.of(FREE, FIGHTER.id(), FARMER.id(), EXPLORER.id(), RESEARCHER.id());

	public static @Nullable Route byId(@Nullable String id) {
		for (Route r : ALL) if (r.id().equals(id)) return r;
		return null;
	}

	/** The player's route, if they follow one. */
	public static Optional<Route> of(GuideProgress p) {
		return Optional.ofNullable(byId(p.route));
	}

	/** The chapter the guide recommends next on the player's route, if any. */
	public static Optional<Chapter> recommended(GuideProgress p) {
		return of(p).flatMap(r -> r.next(p.completed));
	}

	/** The player hasn't answered the route question yet (it is asked once chapter 2 is finished). */
	public static boolean undecided(GuideProgress p) {
		return p.route.isEmpty();
	}

	/** The guide asks the route question: none chosen yet and the basics (chapters 1 and 2) are done. */
	public static boolean due(GuideProgress p) {
		return undecided(p) && Chapters.SETTLING_IN.done(p.completed);
	}

	/** A valid stored value: {@link #FREE} or a route id. */
	public static boolean valid(@Nullable String value) {
		return FREE.equals(value) || byId(value) != null;
	}
}
