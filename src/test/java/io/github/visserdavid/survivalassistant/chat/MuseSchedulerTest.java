package io.github.visserdavid.survivalassistant.chat;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MuseSchedulerTest {
	private static final long INTERVAL = 300 * 20;
	private static final long Q = MuseScheduler.QUIET_TICKS;

	private static MuseScheduler.Input at(String group, Set<String> mobs) {
		return new MuseScheduler.Input(group, true, false, mobs, false, false);
	}

	private static MuseScheduler.Input plains() {
		return at("plains", Set.of());
	}

	/** A scheduler that started at tick 0 and is past its first delay. */
	private static MuseScheduler started() {
		MuseScheduler s = new MuseScheduler(INTERVAL);
		assertTrue(s.update(plains(), 0).isEmpty(), "never on the first call");
		return s;
	}

	@Test
	void buildIdeaComesFirstInANewBiome() {
		MuseScheduler s = started();
		assertTrue(s.update(plains(), MuseScheduler.FIRST_DELAY - 1).isEmpty(), "room for the welcome first");
		assertEquals(BiomeThemes.buildKey("plains"), s.update(plains(), MuseScheduler.FIRST_DELAY).orElseThrow());
	}

	@Test
	void buildIdeaWaitsUntilTheBiomeSettles() {
		MuseScheduler s = started();
		long t = MuseScheduler.FIRST_DELAY + 10;
		assertTrue(s.update(at("desert", Set.of()), t).isEmpty(), "just crossed the border");
		assertEquals(BiomeThemes.buildKey("desert"), s.update(at("desert", Set.of()), t + MuseScheduler.SETTLE_TICKS).orElseThrow());
	}

	@Test
	void quietAfterOtherRemarksAndBetweenMusings() {
		MuseScheduler s = started();
		s.noteRemark(MuseScheduler.FIRST_DELAY);
		assertTrue(s.update(plains(), MuseScheduler.FIRST_DELAY + Q - 1).isEmpty(), "a tip was just given");
		long t = MuseScheduler.FIRST_DELAY + Q;
		assertTrue(s.update(plains(), t).isPresent(), "the build idea");
		assertTrue(s.update(plains(), t + Q).isEmpty(), "scene/story waits for the full interval");
		assertTrue(s.update(plains(), t + INTERVAL).isPresent());
	}

	@Test
	void animalInSightBeforeTheScene() {
		MuseScheduler s = started();
		long t = MuseScheduler.FIRST_DELAY;
		s.update(plains(), t); // build idea
		Optional<String> next = s.update(at("plains", Set.of("cow", "zombie_horse_unknown")), t + INTERVAL);
		assertEquals(BiomeThemes.mobKey("cow"), next.orElseThrow());
		assertEquals(BiomeThemes.key("plains", BiomeThemes.Kind.SCENE),
				s.update(at("plains", Set.of("cow")), t + 2 * INTERVAL).orElseThrow(), "the cows were already discussed");
	}

	@Test
	void eachTopicOnlyOnce() {
		MuseScheduler s = started();
		long t = MuseScheduler.FIRST_DELAY;
		for (int i = 0; i < 3; i++) assertTrue(s.update(plains(), t + i * INTERVAL).isPresent());
		assertTrue(s.update(plains(), t + 3 * INTERVAL).isEmpty(), "build, scene and story are used up");
	}

	@Test
	void notAtNightNotUndergroundNotWhenBlocked() {
		MuseScheduler s = started();
		long t = MuseScheduler.FIRST_DELAY;
		assertTrue(s.update(new MuseScheduler.Input("plains", false, false, Set.of(), false, false), t).isEmpty(), "night");
		assertTrue(s.update(new MuseScheduler.Input("plains", true, true, Set.of(), false, false), t).isEmpty(), "underground");
		assertTrue(s.update(new MuseScheduler.Input("plains", true, false, Set.of(), true, false), t).isEmpty(), "blocked");
		s.update(new MuseScheduler.Input("lush_caves", false, true, Set.of(), false, false), t + 200);
		assertEquals(BiomeThemes.buildKey("lush_caves"),
				s.update(new MuseScheduler.Input("lush_caves", false, true, Set.of(), false, false), t + 200 + MuseScheduler.SETTLE_TICKS)
						.orElse(null), "friendly caves are fine, day or night");
	}

	@Test
	void dangerZonesOnlyWhenAllowed() {
		MuseScheduler s = started();
		long t = MuseScheduler.FIRST_DELAY + INTERVAL;
		assertTrue(s.update(new MuseScheduler.Input("nether", true, false, Set.of(), false, false), t).isEmpty());
		assertEquals(BiomeThemes.key("nether", BiomeThemes.Kind.SCENE),
				s.update(new MuseScheduler.Input("nether", true, false, Set.of(), false, true), t + 1).orElseThrow(),
				"no building idea in the Nether, a scene instead");
	}

	@Test
	void everyGroupHasItsKeys() {
		assertEquals(21 * 3 + 4 * 2 + BiomeThemes.MOBS.size(), BiomeThemes.allKeys().size());
		assertEquals(BiomeThemes.allKeys().size(), Set.copyOf(BiomeThemes.allKeys()).size(), "no duplicates");
	}
}
