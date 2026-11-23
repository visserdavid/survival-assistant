package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GearTest {
	@Test
	void toolTiersFollowTheMaterial() {
		assertEquals(1, Gear.toolTier("wooden_pickaxe"));
		assertEquals(1, Gear.toolTier("golden_sword"));
		assertEquals(2, Gear.toolTier("stone_axe"));
		assertEquals(2, Gear.toolTier("copper_pickaxe"));
		assertEquals(3, Gear.toolTier("iron_sword"));
		assertEquals(4, Gear.toolTier("diamond_pickaxe"));
		assertEquals(4, Gear.toolTier("netherite_axe"));
		assertEquals(4, Gear.toolTier("mace"), "anything else counts as top tier");
	}

	@Test
	void ironOrBetterArmor() {
		assertTrue(Gear.ironOrBetter("iron_helmet"));
		assertTrue(Gear.ironOrBetter("diamond_chestplate"));
		assertTrue(Gear.ironOrBetter("netherite_boots"));
		assertFalse(Gear.ironOrBetter("golden_leggings"));
		assertFalse(Gear.ironOrBetter("chainmail_helmet"));
		assertFalse(Gear.ironOrBetter("leather_boots"));
		assertFalse(Gear.ironOrBetter("turtle_helmet"));
	}

	@Test
	void netheriteGearIsMadeInASmithingTable() {
		assertTrue(Gear.isNetheriteGear("netherite_sword"));
		assertTrue(Gear.isNetheriteGear("netherite_chestplate"));
		assertFalse(Gear.isNetheriteGear("netherite_ingot"));
		assertFalse(Gear.isNetheriteGear("netherite_scrap"));
		assertFalse(Gear.isNetheriteGear("netherite_block"));
		assertFalse(Gear.isNetheriteGear("netherite_upgrade_smithing_template"));
		assertFalse(Gear.isNetheriteGear("diamond_sword"));
	}
}
