package io.github.visserdavid.survivalassistant.chat;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ChangeDetectorTest {
	private static final long COOLDOWN = 120 * 20;
	/** First tick at which the guide may speak after it starts watching at tick 0. */
	private static final long FIRST = ChangeDetector.FIRST_REMARK_DELAY;

	private static Surroundings at(String biome, boolean rain, Surroundings.Phase phase, Map<String, Integer> friendly) {
		return new Surroundings("minecraft:" + biome, biome, rain, false, phase, false, friendly, Map.of());
	}

	private static Surroundings calm(String biome) {
		return at(biome, false, Surroundings.Phase.DAY, Map.of());
	}

	/** A detector that has already talked about the forest, so tests start from a quiet state. */
	private static ChangeDetector settledInForest() {
		ChangeDetector d = new ChangeDetector(COOLDOWN);
		d.update(calm("forest"), 0, false);
		assertEquals(ChangeDetector.Kind.NEW_BIOME, d.update(calm("forest"), FIRST, false).orElseThrow().kind());
		return d;
	}

	@Test
	void staysQuietAtFirst() {
		ChangeDetector d = new ChangeDetector(COOLDOWN);
		assertTrue(d.update(calm("forest"), 0, false).isEmpty());
		assertTrue(d.update(calm("forest"), FIRST - 1, false).isEmpty(), "room for the welcome first");
		Optional<ChangeDetector.Change> c = d.update(calm("forest"), FIRST, false);
		assertEquals(new ChangeDetector.Change(ChangeDetector.Kind.NEW_BIOME, "minecraft:forest"), c.orElseThrow());
	}

	@Test
	void respectsCooldown() {
		ChangeDetector d = settledInForest();
		long t = FIRST + 100;
		d.update(at("forest", true, Surroundings.Phase.DAY, Map.of()), t, false);
		assertTrue(d.update(at("forest", true, Surroundings.Phase.DAY, Map.of()), t + 40, false).isEmpty(), "still on cooldown");
	}

	@Test
	void rainIsAMomentThatExpires() {
		ChangeDetector d = settledInForest();
		long start = FIRST + COOLDOWN - 100;
		d.update(at("forest", true, Surroundings.Phase.DAY, Map.of()), start, false); // starts raining, cooldown not over
		Optional<ChangeDetector.Change> c = d.update(at("forest", true, Surroundings.Phase.DAY, Map.of()), FIRST + COOLDOWN, false);
		assertEquals(ChangeDetector.Kind.RAIN_START, c.orElseThrow().kind(), "still fresh after 5 seconds");

		ChangeDetector late = settledInForest();
		late.update(at("forest", true, Surroundings.Phase.DAY, Map.of()), FIRST + 1, false);
		assertTrue(late.update(at("forest", true, Surroundings.Phase.DAY, Map.of()), FIRST + COOLDOWN, false).isEmpty(),
				"rain that started two minutes ago is old news");
	}

	@Test
	void momentDisappearsWhenConditionEnds() {
		ChangeDetector d = settledInForest();
		long t = FIRST + COOLDOWN - 40;
		d.update(at("forest", true, Surroundings.Phase.DAY, Map.of()), t, false);
		d.update(calm("forest"), t + 20, false); // rain stopped again before the guide could speak
		Optional<ChangeDetector.Change> c = d.update(calm("forest"), FIRST + COOLDOWN, false);
		assertNotEquals(ChangeDetector.Kind.RAIN_START, c.map(ChangeDetector.Change::kind).orElse(null));
	}

	@Test
	void blockedMeansSilentButStillTracking() {
		ChangeDetector d = settledInForest();
		long t = FIRST + COOLDOWN;
		d.update(at("forest", false, Surroundings.Phase.DAY, Map.of("cow", 3)), t, true);
		assertTrue(d.update(at("forest", false, Surroundings.Phase.DAY, Map.of("cow", 3)), t + 40, true).isEmpty());
		Optional<ChangeDetector.Change> c = d.update(at("forest", false, Surroundings.Phase.DAY, Map.of("cow", 3)), t + 80, false);
		assertEquals(new ChangeDetector.Change(ChangeDetector.Kind.NEW_MOB, "cow"), c.orElseThrow(), "the cows are still news");
	}

	@Test
	void eachMobTypeOnlyOnce() {
		ChangeDetector d = settledInForest();
		long t = FIRST + COOLDOWN;
		Surroundings cows = at("forest", false, Surroundings.Phase.DAY, Map.of("cow", 2, "sheep", 1));
		assertEquals("cow", d.update(cows, t, false).orElseThrow().subject(), "most numerous first");
		assertEquals("sheep", d.update(cows, t + COOLDOWN, false).orElseThrow().subject());
		assertTrue(d.update(cows, t + 2 * COOLDOWN, false).isEmpty());
	}

	@Test
	void biomeMustSettleAndIsNotRepeatedRightAway() {
		ChangeDetector d = settledInForest();
		long t = FIRST + COOLDOWN;
		d.update(calm("plains"), t, false);
		assertTrue(d.update(calm("plains"), t + ChangeDetector.BIOME_SETTLE - 1, false).isEmpty(), "walking along a border");
		assertEquals("minecraft:plains", d.update(calm("plains"), t + ChangeDetector.BIOME_SETTLE, false).orElseThrow().subject());

		long back = t + 2 * COOLDOWN;
		d.update(calm("forest"), back, false);
		assertTrue(d.update(calm("forest"), back + ChangeDetector.BIOME_SETTLE, false).isEmpty(), "forest was discussed recently");
	}

	@Test
	void sunriseBeatsNewMob() {
		ChangeDetector d = settledInForest();
		long t = FIRST + COOLDOWN;
		d.update(at("forest", false, Surroundings.Phase.NIGHT, Map.of()), t - 40, false);
		Optional<ChangeDetector.Change> c = d.update(at("forest", false, Surroundings.Phase.DAY, Map.of("fox", 1)), t, false);
		assertEquals(ChangeDetector.Kind.SUNRISE, c.orElseThrow().kind());
	}

	@Test
	void conversationResetsCooldown() {
		ChangeDetector d = settledInForest();
		long t = FIRST + COOLDOWN;
		d.conversationAt(t);
		assertTrue(d.update(at("forest", false, Surroundings.Phase.DAY, Map.of("cow", 1)), t + 20, false).isEmpty(),
				"no spontaneous remark right after a conversation");
	}
}
