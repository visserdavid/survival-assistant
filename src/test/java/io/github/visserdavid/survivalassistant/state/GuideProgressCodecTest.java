package io.github.visserdavid.survivalassistant.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.advice.Navigation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideProgressCodecTest {
	/** Player data as 0.20.0 saved it: one flat map (extra_days belonged to the old day limit and is ignored now). */
	private static final String OLD_FORMAT = """
			{
			  "start_tick": 48000, "retired": false, "completed": ["wood", "crafting_table", "farm"],
			  "npc_enabled": false, "depth_notices": ["deepslate", "nether"],
			  "home_fallback": {"dimension": "minecraft:overworld", "x": 10, "y": 64, "z": -5},
			  "extra_days": 3, "asked_at": 51000, "chapter": 2, "chapter_offered": true, "finished": false,
			  "chat_enabled": true, "musings": "all", "guide_name": "Pim", "about_index": 4
			}""";

	private static GuideProgress decode(String json) {
		return GuideProgressCodec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
	}

	@Test
	void readsTheOldFlatFormat() {
		GuideProgress p = decode(OLD_FORMAT);
		assertEquals(48000, p.startTick);
		assertFalse(p.retired);
		assertEquals(List.of("wood", "crafting_table", "farm"), List.copyOf(p.completed));
		assertEquals(Boolean.FALSE, p.npcEnabled);
		assertEquals(Set.of("deepslate", "nether"), p.depthNotices);
		assertEquals(new Navigation.Mark("minecraft:overworld", 10, 64, -5), p.homeFallback);
		// before played time was saved: the open question and the chapter's days start counting now
		assertEquals(0, p.playedTicks);
		assertEquals(0, p.askedAtTick);
		assertEquals(0, p.chapterStartedAt);
		assertEquals(2, p.chapter);
		assertTrue(p.chapterOffered);
		assertFalse(p.finished);
		assertEquals(Boolean.TRUE, p.chatEnabled);
		assertEquals("all", p.musings);
		assertEquals("Pim", p.guideName);
		assertEquals(4, p.aboutIndex);
	}

	@Test
	void theRouteAndFinishedRoutesRoundTrip() {
		GuideProgress p = decode("{\"route\": \"farmer\", \"routes_done\": [\"explorer\"]}");
		assertEquals("farmer", p.route);
		assertEquals(Set.of("explorer"), p.routesDone);
		GuideProgress again = decode(GuideProgressCodec.CODEC.encodeStart(JsonOps.INSTANCE, p).getOrThrow().toString());
		assertEquals("farmer", again.route);
		assertEquals(Set.of("explorer"), again.routesDone);
		assertEquals("free", decode("{\"route\": \"free\"}").route);
		// a route this version doesn't know: the guide asks again
		assertEquals("", decode("{\"route\": \"wizard\"}").route);
	}

	@Test
	void missingKeysGetTheirDefaults() {
		GuideProgress p = decode("{}");
		assertEquals(-1, p.startTick);
		assertEquals(1, p.chapter);
		assertTrue(p.completed.isEmpty());
		assertNull(p.npcEnabled);
		assertNull(p.homeFallback);
		assertEquals(-1, p.askedAtTick);
		assertEquals(-1, p.chapterStartedAt);
		assertEquals(0, p.playedTicks);
		assertEquals("", p.route);
		assertTrue(p.routesDone.isEmpty());
	}

	@Test
	void writesTheSameFlatKeysPlusPlayedTime() {
		JsonElement written = GuideProgressCodec.CODEC.encodeStart(JsonOps.INSTANCE, decode(OLD_FORMAT)).getOrThrow();
		JsonObject expected = JsonParser.parseString(OLD_FORMAT).getAsJsonObject();
		// values equal to their default are left out, as before
		expected.remove("retired");
		expected.remove("finished");
		expected.remove("extra_days"); // no longer saved
		expected.remove("asked_at"); // replaced by asked_played (played time)
		expected.addProperty("chapter_started", 0);
		expected.addProperty("asked_played", 0);
		assertEquals(expected, written);
	}

	@Test
	void playedTimeSurvivesTheTrip() {
		GuideProgress p = new GuideProgress();
		p.meet(1000);
		p.playedTicks = 50_000;
		p.restart();
		p.askedAtTick = 60_000;
		p.playedTicks = 61_000;
		p.muteUntil = 123_456;
		p.reminded = true;
		JsonElement json = GuideProgressCodec.CODEC.encodeStart(JsonOps.INSTANCE, p).getOrThrow();
		GuideProgress back = GuideProgressCodec.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		assertEquals(61_000, back.playedTicks);
		assertEquals(50_000, back.chapterStartedAt);
		assertEquals(60_000, back.askedAtTick);
		assertEquals(123_456, back.muteUntil);
		assertTrue(back.reminded);
	}

	@Test
	void aBrokenValueDoesNotWipeTheProgress() {
		GuideProgress p = decode("{\"chapter\": \"x\", \"start_tick\": 5, \"completed\": [\"wood\"]}");
		assertEquals(1, p.chapter, "the broken value falls back to its default");
		assertEquals(5, p.startTick);
		assertEquals(List.of("wood"), List.copyOf(p.completed));
	}
}
