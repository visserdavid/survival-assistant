package io.github.visserdavid.survivalassistant;

import io.github.visserdavid.survivalassistant.advice.DigTracker;
import io.github.visserdavid.survivalassistant.advice.Navigation;
import io.github.visserdavid.survivalassistant.advice.SafeSignal;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** What the guide remembers about each player while the server runs (not saved with the player). */
final class GuideSessions {
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();

	private GuideSessions() {}

	/** Where and when a player died. */
	record Death(Navigation.Mark mark, long tick) {}

	static final class Session {
		/** Game tick of the last evaluation; null = evaluate on the next pass. */
		@Nullable Long lastEval;
		/** Breaks under the player's feet ("digging straight down"). */
		final DigTracker dig = new DigTracker();
		/** The last death (kept here, not on the player, so it survives the respawn copy and a reconnect). */
		@Nullable Death death;
		/** The spot was well lit at the last evaluation. */
		boolean wasSafe;
		/** Game tick when the guide last said a spot is well lit. */
		long lastSafeNotice = SafeSignal.NEVER;

		/** Evaluate on the next pass instead of waiting for the interval. */
		void evaluateSoon() {
			lastEval = null;
		}
	}

	static Session of(UUID playerId) {
		return SESSIONS.computeIfAbsent(playerId, id -> new Session());
	}

	/** The player left: forget everything except the last death, so the way back still shows after a reconnect. */
	static void disconnect(UUID playerId) {
		Session old = SESSIONS.remove(playerId);
		if (old != null && old.death != null) of(playerId).death = old.death;
	}

	static void clear() {
		SESSIONS.clear();
	}
}
