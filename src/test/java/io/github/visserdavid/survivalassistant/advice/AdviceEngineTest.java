package io.github.visserdavid.survivalassistant.advice;

import io.github.visserdavid.survivalassistant.config.ModConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdviceEngineTest {
	private static final AdviceEngine.Settings S = AdviceEngine.Settings.from(new ModConfig());

	private static List<AdviceEngine.Message> eval(SurvivalContext c, GuideProgress p, long now) {
		return AdviceEngine.evaluate(c, p, now, S, false);
	}

	private static Tip only(List<AdviceEngine.Message> msgs) {
		List<AdviceEngine.Message> tips = msgs.stream().filter(m -> !m.praise()).toList();
		assertEquals(1, tips.size(), "expected exactly one tip, got " + msgs);
		return tips.get(0).tip();
	}

	@Test
	void freshStartGivesWoodTip() {
		assertEquals(TipRegistry.WOOD, only(eval(SurvivalContext.builder().build(), new GuideProgress(), 0)));
	}

	@Test
	void woodDoneMovesOnWithPraise() {
		GuideProgress p = new GuideProgress();
		List<AdviceEngine.Message> msgs = eval(SurvivalContext.builder().logs(12).build(), p, 0);
		assertTrue(msgs.stream().anyMatch(m -> m.praise() && m.tip() == TipRegistry.WOOD));
		assertEquals(TipRegistry.CRAFTING_TABLE, only(msgs));
		assertTrue(p.completed.contains("wood"));
	}

	@Test
	void skippingAheadCompletesEarlierMilestones() {
		GuideProgress p = new GuideProgress();
		// Stone pickaxe + stone sword implies wood, table and wooden pickaxe were done.
		Tip next = only(eval(SurvivalContext.builder().pickaxeTier(2).swordTier(2).build(), p, 0));
		assertEquals(TipRegistry.FURNACE, next);
		assertTrue(p.completed.containsAll(List.of("wood", "crafting_table", "wooden_pickaxe", "stone_tools")));
	}

	@Test
	void completedMilestonesStayCompletedWhenItemsAreUsedUp() {
		GuideProgress p = new GuideProgress();
		eval(SurvivalContext.builder().logs(12).build(), p, 0);
		// Logs turned into planks/table: wood must not come back.
		Tip next = only(eval(SurvivalContext.builder().build(), p, 10_000));
		assertEquals(TipRegistry.CRAFTING_TABLE, next);
	}

	@Test
	void duskWithoutShelterIsUrgent() {
		List<AdviceEngine.Message> msgs = eval(SurvivalContext.builder().timeOfDay(11500).build(), new GuideProgress(), 0);
		assertEquals(TipRegistry.DUSK, only(msgs));
		assertTrue(msgs.get(msgs.size() - 1).urgent());
	}

	@Test
	void duskButShelteredIsNotUrgent() {
		assertEquals(TipRegistry.WOOD, only(eval(SurvivalContext.builder().timeOfDay(11500).sheltered(true).build(), new GuideProgress(), 0)));
	}

	@Test
	void creeperBeatsEverything() {
		SurvivalContext c = SurvivalContext.builder().timeOfDay(14000).food(2).health(4).hostiles(3).creeper(true).build();
		assertEquals(TipRegistry.CREEPER, only(eval(c, new GuideProgress(), 0)));
	}

	@Test
	void hungerTipDependsOnHavingFood() {
		assertEquals(TipRegistry.HUNGRY_EAT, only(eval(SurvivalContext.builder().food(5).foodItems(3).build(), new GuideProgress(), 0)));
		assertEquals(TipRegistry.HUNGRY_HUNT, only(eval(SurvivalContext.builder().food(5).build(), new GuideProgress(), 0)));
	}

	@Test
	void urgentCooldownPreventsSpamAndHoldsBackMilestones() {
		GuideProgress p = new GuideProgress();
		SurvivalContext night = SurvivalContext.builder().timeOfDay(15000).build();
		assertEquals(TipRegistry.NIGHT_OUTSIDE, only(eval(night, p, 0)));
		assertTrue(eval(night, p, 100).isEmpty(), "on cooldown: nothing at all");
		assertEquals(TipRegistry.NIGHT_OUTSIDE, only(eval(night, p, S.urgentCooldownTicks())));
	}

	@Test
	void warningOnCooldownDoesNotHoldBackMilestones() {
		GuideProgress p = new GuideProgress();
		SurvivalContext cave = SurvivalContext.builder().sheltered(true).underground(true).pickaxeTier(2).swordTier(2).build();
		assertEquals(TipRegistry.UNDERGROUND, only(eval(cave, p, 0)));
		assertEquals(TipRegistry.FURNACE, only(eval(cave, p, 100)));
	}

	@Test
	void milestoneCooldown() {
		GuideProgress p = new GuideProgress();
		SurvivalContext c = SurvivalContext.builder().build();
		assertEquals(TipRegistry.WOOD, only(eval(c, p, 0)));
		assertTrue(eval(c, p, S.milestoneCooldownTicks() - 1).isEmpty());
		assertEquals(TipRegistry.WOOD, only(eval(c, p, S.milestoneCooldownTicks())));
	}

	@Test
	void muteSilencesUnsolicitedButNotForcedTips() {
		GuideProgress p = new GuideProgress();
		p.muteUntil = 1000;
		SurvivalContext c = SurvivalContext.builder().timeOfDay(15000).build();
		assertTrue(eval(c, p, 0).isEmpty());
		assertEquals(TipRegistry.NIGHT_OUTSIDE, only(AdviceEngine.evaluate(c, p, 0, S, true)));
	}

	@Test
	void forcedIgnoresCooldown() {
		GuideProgress p = new GuideProgress();
		SurvivalContext c = SurvivalContext.builder().build();
		eval(c, p, 0);
		assertEquals(TipRegistry.WOOD, only(AdviceEngine.evaluate(c, p, 1, S, true)));
	}

	@Test
	void shelterCompletesAfterFirstNight() {
		GuideProgress p = new GuideProgress();
		SurvivalContext day1 = SurvivalContext.builder().dayIndex(1).pickaxeTier(2).swordTier(2).hasFurnace(true).foodItems(6).hasBed(true).torches(10).build();
		assertEquals(TipRegistry.CHEST, only(eval(day1, p, 0)));
		assertTrue(p.completed.contains("shelter"));
	}

	@Test
	void withoutCharacterOnlyUrgentGoesToChat() {
		AdviceEngine.Message milestone = new AdviceEngine.Message(TipRegistry.WOOD, false, false);
		AdviceEngine.Message praise = new AdviceEngine.Message(TipRegistry.WOOD, false, true);
		AdviceEngine.Message urgent = new AdviceEngine.Message(TipRegistry.CREEPER, true, false);
		AdviceEngine.Message warning = new AdviceEngine.Message(TipRegistry.UNDERGROUND, true, false);

		assertEquals(AdviceEngine.Delivery.NONE, AdviceEngine.delivery(milestone, false, false));
		assertEquals(AdviceEngine.Delivery.ACTIONBAR, AdviceEngine.delivery(praise, false, false));
		assertEquals(AdviceEngine.Delivery.CHAT, AdviceEngine.delivery(urgent, false, false));
		assertEquals(AdviceEngine.Delivery.CHAT, AdviceEngine.delivery(warning, false, false));
		assertEquals(AdviceEngine.Delivery.CHAT, AdviceEngine.delivery(milestone, false, true), "asked for: always chat");
		assertEquals(AdviceEngine.Delivery.CHAT, AdviceEngine.delivery(milestone, true, false), "with character: as before");
		assertEquals(AdviceEngine.Delivery.CHAT, AdviceEngine.delivery(praise, true, false));
	}

	@Test
	void npcChoiceFallsBackToServerDefault() {
		GuideProgress p = new GuideProgress();
		assertTrue(p.npcEnabled(true));
		assertFalse(p.npcEnabled(false));
		p.npcEnabled = false;
		assertFalse(p.npcEnabled(true), "player's own choice wins");
	}

	@Test
	void everythingDoneMeansSilence() {
		GuideProgress p = new GuideProgress();
		SurvivalContext all = SurvivalContext.builder().dayIndex(2).pickaxeTier(3).swordTier(3).hasFurnace(true).foodItems(10).hasBed(true)
				.torches(20).hasChest(true).iron(5).hasShield(true).hasIronArmor(true).hasWaterBucket(true).build();
		List<AdviceEngine.Message> msgs = eval(all, p, 0);
		assertTrue(msgs.stream().allMatch(AdviceEngine.Message::praise));
		assertEquals(TipRegistry.CHAPTER_FIRST_DAYS.size(), p.completed.size());
		assertNull(AdviceEngine.nextMilestone(p));
		assertTrue(AdviceEngine.evaluate(all, p, 1, S, true).isEmpty());
	}
}
