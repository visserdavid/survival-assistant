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
	/** Player data as 0.20.0 and earlier saved it: one flat map. */
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
		assertEquals(3, p.extraDays);
		assertEquals(51000, p.askedAtTick);
		assertEquals(2, p.chapter);
		assertTrue(p.chapterOffered);
		assertFalse(p.finished);
		assertEquals(Boolean.TRUE, p.chatEnabled);
		assertEquals("all", p.musings);
		assertEquals("Pim", p.guideName);
		assertEquals(4, p.aboutIndex);
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
	}

	@Test
	void writesTheSameFlatKeys() {
		JsonElement written = GuideProgressCodec.CODEC.encodeStart(JsonOps.INSTANCE, decode(OLD_FORMAT)).getOrThrow();
		JsonObject expected = JsonParser.parseString(OLD_FORMAT).getAsJsonObject();
		// values equal to their default are left out, as before
		expected.remove("retired");
		expected.remove("finished");
		assertEquals(expected, written);
	}
}
