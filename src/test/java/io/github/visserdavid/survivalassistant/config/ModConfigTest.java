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
		assertEquals(d.maxDays, c.maxDays);
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
		c.extendDays = -1;
		c.chatEndpoint = null;
		c.chatModel = " ";
		c.guideName = "!!!";
		c.validate();
		assertEquals(1, c.tipIntervalSeconds);
		assertEquals(10, c.chatCooldownSeconds);
		assertEquals(60, c.museIntervalSeconds);
		assertEquals(1, c.chatTimeoutSeconds);
		assertEquals(64, c.chatSightRadius);
		assertEquals(0, c.extendDays);
		assertEquals(new ModConfig().chatEndpoint, c.chatEndpoint);
		assertEquals("local", c.chatModel);
		assertEquals(GuideNames.DEFAULT, c.guideName);
	}
}
