package io.github.visserdavid.survivalassistant.chat;

import io.github.visserdavid.survivalassistant.util.Ticks;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Decides when the guide muses and about what. One instance per player. Pure logic: no Minecraft, no randomness.
 *
 * <ul>
 *   <li>Only when it is safe: nothing urgent, not muted, the guide character is around, daytime and outside
 *       (or in a friendly cave), and at least {@link #QUIET_TICKS} after anything else the guide said.</li>
 *   <li>A new biome gets its building idea once the player has been there for {@link #SETTLE_TICKS}.</li>
 *   <li>Otherwise at most one musing per interval: an animal in sight first, then a scene or story of the biome.</li>
 *   <li>Every topic is used at most once per session.</li>
 *   <li>When nothing about the place is left, the guide tells something about itself, once per session.</li>
 * </ul>
 */
public final class MuseScheduler {
	/** Minimum silence after any other remark of the guide (2 minutes). */
	public static final long QUIET_TICKS = Ticks.minutes(2);
	/** A biome must hold this long before its building idea (no remarks while walking along a border). */
	public static final long SETTLE_TICKS = Ticks.seconds(5);
	/** The first musing comes no earlier than this after the guide starts (leaves room for the welcome and first tips). */
	public static final long FIRST_DELAY = Ticks.minutes(1);

	/**
	 * @param group       lower-case biome group, for example {@code plains}
	 * @param daytime     day or dusk (not night)
	 * @param underground no sky above and well below the surface
	 * @param mobsInSight friendly mob ids (entity id paths) the player can see
	 * @param blocked     the guide may not talk now (urgent, muted, no character, waiting for the model)
	 * @param dangerZones musings in the Nether, the End, the Deep Dark and unknown places are allowed
	 * @param selfKey     the "about me" line the guide would tell next, or null
	 */
	public record Input(String group, boolean daytime, boolean underground, Set<String> mobsInSight, boolean blocked,
			boolean dangerZones, @Nullable String selfKey) {
		public Input(String group, boolean daytime, boolean underground, Set<String> mobsInSight, boolean blocked,
				boolean dangerZones) {
			this(group, daytime, underground, mobsInSight, blocked, dangerZones, null);
		}
	}

	private final long intervalTicks;
	private boolean started;
	private long lastMuse;
	private long lastRemark = Long.MIN_VALUE / 4;
	private String settlingGroup = "";
	private long settlingSince;
	private final Set<String> used = new HashSet<>();
	private boolean toldAboutSelf;

	public MuseScheduler(long intervalTicks) {
		this.intervalTicks = Math.max(QUIET_TICKS, intervalTicks);
	}

	/** Something else was said by the guide (a tip, praise, a reply): keep quiet for a while. */
	public void noteRemark(long tick) {
		lastRemark = Math.max(lastRemark, tick);
	}

	/** @return the language key to muse about now, if any; a returned key counts as used */
	public Optional<String> update(Input in, long tick) {
		if (!started) {
			started = true;
			lastMuse = tick - intervalTicks + FIRST_DELAY;
			lastRemark = Math.max(lastRemark, tick - QUIET_TICKS + FIRST_DELAY);
			settlingGroup = in.group();
			settlingSince = tick;
			return Optional.empty();
		}
		if (!in.group().equals(settlingGroup)) {
			settlingGroup = in.group();
			settlingSince = tick;
		}
		if (in.blocked() || !allowedHere(in)) return Optional.empty();
		if (tick - lastRemark < QUIET_TICKS) return Optional.empty();

		String build = BiomeThemes.buildKey(in.group());
		if (build != null && !used.contains(build)) {
			// a new biome: its building idea comes first, once the player has settled in
			return tick - settlingSince >= SETTLE_TICKS ? pick(build, tick) : Optional.empty();
		}
		if (tick - lastMuse < intervalTicks) return Optional.empty();

		for (String mob : new TreeSet<>(in.mobsInSight())) {
			if (!BiomeThemes.MOBS.contains(mob)) continue;
			String key = BiomeThemes.mobKey(mob);
			if (!used.contains(key)) return pick(key, tick);
		}
		for (String key : BiomeThemes.otherKeys(in.group())) {
			if (!used.contains(key)) return pick(key, tick);
		}
		if (in.selfKey() != null && !toldAboutSelf) {
			toldAboutSelf = true;
			return pick(in.selfKey(), tick);
		}
		return Optional.empty();
	}

	private static boolean allowedHere(Input in) {
		boolean danger = BiomeThemes.isDangerZone(in.group());
		if (danger) return in.dangerZones();
		if (in.underground()) return BiomeThemes.CAVES.contains(in.group());
		return in.daytime();
	}

	private Optional<String> pick(String key, long tick) {
		used.add(key);
		lastMuse = tick;
		lastRemark = tick;
		return Optional.of(key);
	}
}
