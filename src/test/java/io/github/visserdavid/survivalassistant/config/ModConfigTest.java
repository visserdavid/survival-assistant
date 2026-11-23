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
}
