package io.github.visserdavid.survivalassistant.config;

import io.github.visserdavid.survivalassistant.advice.GuideNames;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModConfigTest {
	@Test
	void defaultsStayAsTheyAre() {
		ModConfig c = new ModConfig();
		c.validate();
		ModConfig d = new ModConfig();
		assertEquals(d.chatEndpoint, c.chatEndpoint);
		assertEquals(d.chatCooldownSeconds, c.chatCooldownSeconds);
		assertEquals(d.museIntervalSeconds, c.museIntervalSeconds);
		assertEquals(d.tipIntervalSeconds, c.tipIntervalSeconds);
		assertEquals(GuideNames.DEFAULT, c.guideName);
	}

	@Test
	void badValuesAreRepaired() {
		ModConfig c = new ModConfig();
		c.tipIntervalSeconds = 0;
		c.chatCooldownSeconds = 1;
		c.museIntervalSeconds = 5;
		c.chatTimeoutSeconds = -3;
		c.chatSightRadius = 1000;
		c.homeDistance = -1;
		c.chatEndpoint = null;
		c.chatModel = " ";
		c.guideName = "!!!";
		c.validate();
		assertEquals(1, c.tipIntervalSeconds);
		assertEquals(10, c.chatCooldownSeconds);
		assertEquals(60, c.museIntervalSeconds);
		assertEquals(1, c.chatTimeoutSeconds);
		assertEquals(64, c.chatSightRadius);
		assertEquals(1, c.homeDistance);
		assertEquals(new ModConfig().chatEndpoint, c.chatEndpoint);
		assertEquals("local", c.chatModel);
		assertEquals(GuideNames.DEFAULT, c.guideName);
	}

	@Test
	void copyIsEqualButSeparate() {
		ModConfig c = new ModConfig();
		c.homeDistance = 100;
		c.chatTemperature = 0.7;
		ModConfig copy = c.copy();
		assertNotSame(c, copy);
		assertEquals(c.toJson(), copy.toJson());
		copy.homeDistance = 20;
		assertEquals(100, c.homeDistance);
	}

	@Test
	void jsonFromTheScreenIsValidated() {
		ModConfig c = ModConfig.fromJson("{\"homeDistance\": 0, \"museIntervalSeconds\": 5}");
		assertEquals(1, c.homeDistance);
		assertEquals(60, c.museIntervalSeconds);
		// fields that are missing keep their default
		assertEquals(new ModConfig().chatEndpoint, c.chatEndpoint);
	}

	@Test
	void brokenJsonIsRefused() {
		assertThrows(com.google.gson.JsonParseException.class, () -> ModConfig.fromJson("{\"homeDistance\": \"far\"}"));
		assertThrows(com.google.gson.JsonParseException.class, () -> ModConfig.fromJson(""));
	}

	@Test
	void chatSettingsAreKeptWithinSafeLimits() {
		ModConfig c = new ModConfig();
		c.chatEndpoint = "file:///etc/passwd";
		c.chatModel = "m".repeat(ModConfig.MAX_MODEL_LENGTH + 1);
		c.chatTemperature = Double.POSITIVE_INFINITY;
		c.chatMaxTokens = 1_000_000;
		c.chatHistoryTurns = 500;
		c.validate();
		assertEquals(new ModConfig().chatEndpoint, c.chatEndpoint);
		assertEquals(new ModConfig().chatModel, c.chatModel);
		assertEquals(-1, c.chatTemperature);
		assertEquals(1024, c.chatMaxTokens);
		assertEquals(20, c.chatHistoryTurns);

		c.chatTemperature = 7;
		c.validate();
		assertEquals(2.0, c.chatTemperature);
		assertTrue(ModConfig.validEndpoint("HTTPS://example.org/v1/chat/completions"));
		assertFalse(ModConfig.validEndpoint("ftp://example.org"));
		assertFalse(ModConfig.validEndpoint("http://" + "a".repeat(ModConfig.MAX_ENDPOINT_LENGTH)));
	}

	/** The defaults as the README documents them: change both together. */
	@Test
	void defaultsMatchTheReadme() {
		ModConfig c = new ModConfig();
		assertTrue(c.moreChapters);
		assertEquals(2, c.tipIntervalSeconds);
		assertEquals(180, c.milestoneCooldownSeconds);
		assertEquals(60, c.urgentCooldownSeconds);
		assertTrue(c.allowRecall);
		assertTrue(c.npcEnabledByDefault);
		assertTrue(c.depthNotices);
		assertTrue(c.navigation);
		assertEquals(64, c.homeDistance);
		assertEquals(GuideNames.DEFAULT, c.guideName);
		assertTrue(c.spawnOnJoin);
		assertTrue(c.pointAtResources);
		assertTrue(c.musings);
		assertEquals(300, c.museIntervalSeconds);
		assertFalse(c.musingsInDangerZones);
		assertFalse(c.chatEnabled);
		assertEquals("http://127.0.0.1:8080/v1/chat/completions", c.chatEndpoint);
		assertEquals("local", c.chatModel);
		assertTrue(c.chatSpontaneous);
		assertEquals(120, c.chatCooldownSeconds);
		assertEquals(16, c.chatHearingDistance);
		assertEquals(16, c.chatSightRadius);
		assertEquals(80, c.chatMaxTokens);
		assertEquals(-1, c.chatTemperature);
		assertEquals(20, c.chatTimeoutSeconds);
		assertEquals(6, c.chatHistoryTurns);
	}
}
