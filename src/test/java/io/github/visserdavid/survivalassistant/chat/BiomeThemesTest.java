package io.github.visserdavid.survivalassistant.chat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.visserdavid.survivalassistant.advice.BiomeGroup;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class BiomeThemesTest {
	@Test
	void everyMusingHasAnEnglishText() throws Exception {
		JsonObject en;
		try (Reader r = new InputStreamReader(
				BiomeThemesTest.class.getResourceAsStream("/assets/survivalassistant/lang/en_us.json"), StandardCharsets.UTF_8)) {
			en = JsonParser.parseReader(r).getAsJsonObject();
		}
		for (String key : BiomeThemes.allKeys()) assertTrue(en.has(key), "missing text for " + key);
	}

	@Test
	void everyBiomeGroupIsCovered() {
		for (BiomeGroup g : BiomeGroup.values()) {
			String name = g.name().toLowerCase(Locale.ROOT);
			assertTrue(BiomeThemes.GROUPS.contains(name) || BiomeThemes.isDangerZone(name), name + " has no musings");
		}
	}

	@Test
	void dangerZonesHaveNoBuildingIdea() {
		for (String g : BiomeThemes.DANGER_ZONES) assertNull(BiomeThemes.buildKey(g), g);
		assertNotNull(BiomeThemes.buildKey("plains"));
	}
}
