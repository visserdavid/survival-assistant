package io.github.visserdavid.survivalassistant.chat;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Notices changes in the player's surroundings and decides when the guide may make a spontaneous remark.
 * One instance per player. Pure logic: no Minecraft, no randomness, no I/O.
 *
 * <p>Two kinds of changes:
 * <ul>
 *   <li><b>Moments</b> (rain starts, sunrise, entering a cave): only worth a remark shortly after they happen.
 *       They stay pending for {@link #MOMENT_TTL} ticks and only while the condition still holds.</li>
 *   <li><b>Discoveries</b> (a biome or mob type the guide hasn't talked about yet): stay pending until remarked.
 *       Each mob type gets one remark per session; a biome gets a new remark once the player has seen a few others.</li>
 * </ul>
 * Between two remarks there is always at least the cooldown, and nothing is said while {@code blocked}
 * (urgent situation, muted, a request already running).
 */
public final class ChangeDetector {
	public enum Kind {
		// in order of priority
		THUNDER_START, SUNRISE, RAIN_START, NEW_BIOME, NEW_MOB, NIGHTFALL, ENTER_CAVE, LEAVE_CAVE, RAIN_STOP
	}

	/** @param subject biome id for NEW_BIOME, entity id path for NEW_MOB, otherwise empty */
	public record Change(Kind kind, String subject) {}

	/** A moment is worth a remark for 30 seconds. */
	public static final long MOMENT_TTL = 30 * 20;
	/** A new biome must hold for 5 seconds (no remarks while walking along a border). */
	public static final long BIOME_SETTLE = 5 * 20;
	/** First remark comes this long after the guide starts watching (leaves room for the welcome). */
	public static final long FIRST_REMARK_DELAY = 30 * 20;
	/** Biomes remembered as "already talked about". */
	private static final int RECENT_BIOMES = 4;

	private final long cooldownTicks;

	private Surroundings previous;
	private long lastRemark;
	private final Map<Kind, Long> moments = new EnumMap<>(Kind.class);
	private final Set<String> remarkedMobs = new HashSet<>();
	private final Deque<String> remarkedBiomes = new ArrayDeque<>();
	private String settlingBiome = "";
	private long settlingSince;

	public ChangeDetector(long cooldownTicks) {
		this.cooldownTicks = Math.max(0, cooldownTicks);
	}

	/**
	 * Feed the current surroundings (call regularly, for example every 2 seconds).
	 *
	 * @param blocked the guide may not talk right now; changes are still tracked
	 * @return the change to remark on now, if any. A returned change counts as remarked.
	 */
	public Optional<Change> update(Surroundings now, long tick, boolean blocked) {
		if (previous == null) {
			previous = now;
			lastRemark = tick - cooldownTicks + FIRST_REMARK_DELAY;
			settlingBiome = now.biomeId();
			settlingSince = tick;
			return Optional.empty();
		}

		detectMoments(previous, now, tick);
		if (!now.biomeId().equals(settlingBiome)) {
			settlingBiome = now.biomeId();
			settlingSince = tick;
		}
		previous = now;

		moments.entrySet().removeIf(e -> tick - e.getValue() > MOMENT_TTL || !holds(e.getKey(), now));

		if (blocked || tick - lastRemark < cooldownTicks) return Optional.empty();

		Optional<Change> change = pick(now, tick);
		change.ifPresent(c -> remarked(c, tick));
		return change;
	}

	/** The player talked to the guide: that counts as a remark, so no spontaneous one follows right away. */
	public void conversationAt(long tick) {
		lastRemark = Math.max(lastRemark, tick);
	}

	private void detectMoments(Surroundings before, Surroundings now, long tick) {
		if (now.thundering() && !before.thundering()) moments.put(Kind.THUNDER_START, tick);
		if (now.raining() && !before.raining()) moments.put(Kind.RAIN_START, tick);
		if (!now.raining() && before.raining() && !now.thundering()) moments.put(Kind.RAIN_STOP, tick);
		if (now.phase() == Surroundings.Phase.DAY && before.phase() == Surroundings.Phase.NIGHT) moments.put(Kind.SUNRISE, tick);
		if (now.phase() == Surroundings.Phase.NIGHT && before.phase() != Surroundings.Phase.NIGHT) moments.put(Kind.NIGHTFALL, tick);
		if (now.underground() && !before.underground()) moments.put(Kind.ENTER_CAVE, tick);
		if (!now.underground() && before.underground()) moments.put(Kind.LEAVE_CAVE, tick);
	}

	private static boolean holds(Kind kind, Surroundings now) {
		return switch (kind) {
			case THUNDER_START -> now.thundering();
			case RAIN_START -> now.raining();
			case RAIN_STOP -> !now.raining();
			case SUNRISE -> now.phase() == Surroundings.Phase.DAY;
			case NIGHTFALL -> now.phase() == Surroundings.Phase.NIGHT;
			case ENTER_CAVE -> now.underground();
			case LEAVE_CAVE -> !now.underground();
			default -> true;
		};
	}

	private Optional<Change> pick(Surroundings now, long tick) {
		Optional<Change> mob = newMob(now);
		boolean biomeReady = tick - settlingSince >= BIOME_SETTLE && !remarkedBiomes.contains(now.biomeId());
		for (Kind kind : Kind.values()) {
			if (kind == Kind.NEW_BIOME) {
				if (biomeReady) return Optional.of(new Change(kind, now.biomeId()));
			} else if (kind == Kind.NEW_MOB) {
				if (mob.isPresent()) return mob;
			} else if (moments.containsKey(kind)) {
				return Optional.of(new Change(kind, ""));
			}
		}
		return Optional.empty();
	}

	/** The most numerous mob type the guide hasn't talked about yet (alphabetical on a tie, for predictability). */
	private Optional<Change> newMob(Surroundings now) {
		Comparator<Map.Entry<String, Integer>> order = Map.Entry.<String, Integer>comparingByValue().reversed()
				.thenComparing(Map.Entry.comparingByKey());
		return java.util.stream.Stream.concat(now.hostile().entrySet().stream(), now.friendly().entrySet().stream())
				.filter(e -> !remarkedMobs.contains(e.getKey()))
				.sorted(order)
				.findFirst()
				.map(e -> new Change(Kind.NEW_MOB, e.getKey()));
	}

	private void remarked(Change change, long tick) {
		lastRemark = tick;
		moments.remove(change.kind());
		if (change.kind() == Kind.NEW_MOB) remarkedMobs.add(change.subject());
		if (change.kind() == Kind.NEW_BIOME) {
			remarkedBiomes.remove(change.subject());
			remarkedBiomes.addFirst(change.subject());
			while (remarkedBiomes.size() > RECENT_BIOMES) remarkedBiomes.removeLast();
		}
	}
}
