package io.github.visserdavid.survivalassistant.advice;

import io.github.visserdavid.survivalassistant.config.ModConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class StepsTest {
	private static String current(Tip goal, UnaryOperator<SurvivalContext.Builder> setup) {
		Steps.Step s = Steps.current(goal, setup.apply(SurvivalContext.builder()).build());
		assertNotNull(s, goal.id() + " has steps");
		return s.id();
	}

	@Test
	void goalsWithoutStepsHaveNone() {
		assertNull(Steps.current(TipRegistry.WOOD, SurvivalContext.builder().build()));
	}

	@Test
	void theLastStepIsNeverDoneByItself() {
		for (List<Steps.Step> steps : Steps.BY_GOAL.values()) {
			assertFalse(steps.getLast().done().test(SurvivalContext.builder().build()), steps.getLast().id());
		}
	}

	@Test
	void brewingGoesStepByStep() {
		Tip g = TipRegistry.BREW_POTION;
		assertEquals("brew_potion.bottles", current(g, b -> b));
		assertEquals("brew_potion.bottles", current(g, b -> b.item("glass_bottle", 2)));
		assertEquals("brew_potion.water", current(g, b -> b.item("glass_bottle", 3)));
		assertEquals("brew_potion.water", current(g, b -> b.item("glass_bottle", 1).waterBottles(2)));
		assertEquals("brew_potion.fuel", current(g, b -> b.waterBottles(3)));
		assertEquals("brew_potion.brew", current(g, b -> b.waterBottles(3).item("blaze_powder", 1)));
	}

	@Test
	void bottlesInTheStandStillCount() {
		Tip g = TipRegistry.BREW_POTION;
		assertEquals("brew_potion.fuel", current(g, b -> b.standBottles(3)));
		assertEquals("brew_potion.brew", current(g, b -> b.standBottles(3).standFuel(true)),
				"the powder is used up, but the stand still has fuel");
	}

	@Test
	void netherPortal() {
		Tip g = TipRegistry.ENTER_NETHER;
		assertEquals("enter_nether.obsidian", current(g, b -> b.obsidian(4)));
		assertEquals("enter_nether.light", current(g, b -> b.obsidian(4).block("obsidian", 6)),
				"obsidian in the frame counts while building it");
		assertEquals("enter_nether.step_in", current(g, b -> b.block("nether_portal", 6)), "a lit portal skips ahead");
	}

	@Test
	void fortress() {
		Tip g = TipRegistry.FIND_FORTRESS;
		assertEquals("find_fortress.nether", current(g, b -> b.fortressNear(true)), "first back into the Nether");
		assertEquals("find_fortress.search", current(g, b -> b.dimension(SurvivalContext.NETHER)));
		assertEquals("find_fortress.enter", current(g, b -> b.dimension(SurvivalContext.NETHER).fortressNear(true)));
	}

	@Test
	void blazeRods() {
		Tip g = TipRegistry.BLAZE_RODS;
		assertEquals("blaze_rods.nether", current(g, b -> b.blazeRods(1)));
		assertEquals("blaze_rods.find", current(g, b -> b.dimension(SurvivalContext.NETHER)));
		assertEquals("blaze_rods.fight", current(g, b -> b.dimension(SurvivalContext.NETHER).blazeNearby(true)));
	}

	@Test
	void netherWart() {
		Tip g = TipRegistry.NETHER_WART;
		assertEquals("nether_wart.nether", current(g, b -> b.netherWart(2)));
		assertEquals("nether_wart.find", current(g, b -> b.dimension(SurvivalContext.NETHER)));
		assertEquals("nether_wart.pick", current(g, b -> b.dimension(SurvivalContext.NETHER).netherWart(1)));
		assertEquals("nether_wart.pick", current(g, b -> b.block("nether_wart", 5)), "a wart farm at home counts too");
	}

	@Test
	void respawnAnchor() {
		Tip g = TipRegistry.RESPAWN_ANCHOR;
		assertEquals("respawn_anchor.crying", current(g, b -> b.item("crying_obsidian", 5).item("glowstone", 3)));
		assertEquals("respawn_anchor.glowstone", current(g, b -> b.item("crying_obsidian", 6).item("glowstone_dust", 11)),
				"11 dust is only 2 blocks");
		assertEquals("respawn_anchor.craft", current(g, b -> b.item("crying_obsidian", 6).item("glowstone", 2).item("glowstone_dust", 4)));
		assertEquals("respawn_anchor.place", current(g, b -> b.item("respawn_anchor", 1)), "crafting doesn't go back");
		assertEquals("respawn_anchor.place", current(g, b -> b.block("respawn_anchor", 1)), "placed in the overworld");
		Steps.Step charge = Steps.current(g, SurvivalContext.builder().dimension(SurvivalContext.NETHER)
				.block("respawn_anchor", 1).anchorCharge(2).build());
		assertNotNull(charge);
		assertEquals("respawn_anchor.charge", charge.id());
		assertEquals(Steps.ANCHOR_CHARGES, charge.target());
		assertEquals(2, charge.value(SurvivalContext.builder().anchorCharge(2).build()));
	}

	@Test
	void summonWither() {
		Tip g = TipRegistry.SUMMON_WITHER;
		assertEquals("summon_wither.spot", current(g, b -> b));
		assertEquals("summon_wither.base", current(g, b -> b.underground(true)));
		assertEquals("summon_wither.base", current(g, b -> b.dimension(SurvivalContext.NETHER)));
		assertEquals("summon_wither.skulls", current(g, b -> b.witherBase(true)), "a base built on the surface still counts");
	}

	@Test
	void trident() {
		Tip g = TipRegistry.TRIDENT;
		assertEquals("trident.drowned", current(g, b -> b));
		assertEquals("trident.armed", current(g, b -> b.drownedNearby(true)));
		assertEquals("trident.fight", current(g, b -> b.drownedNearby(true).tridentDrownedNearby(true)));
	}

	@Test
	void recoveryCompass() {
		Tip g = TipRegistry.RECOVERY_COMPASS;
		assertEquals("recovery_compass.shards", current(g, b -> b.echoShards(7).item("compass", 1)));
		assertEquals("recovery_compass.compass", current(g, b -> b.echoShards(8)));
		assertEquals("recovery_compass.craft", current(g, b -> b.echoShards(8).item("compass", 1)));
	}

	@Test
	void mace() {
		Tip g = TipRegistry.MACE;
		assertEquals("mace.rod", current(g, b -> b.hasHeavyCore(true)));
		assertEquals("mace.core", current(g, b -> b.breezeRods(1)));
		assertEquals("mace.craft", current(g, b -> b.breezeRods(1).hasHeavyCore(true)));
	}

	@Test
	void turtleShell() {
		Tip g = TipRegistry.TURTLE_HELMET;
		assertEquals("turtle_helmet.turtles", current(g, b -> b.item("seagrass", 5)));
		assertEquals("turtle_helmet.seagrass", current(g, b -> b.turtleNearby(true).item("seagrass", 1)));
		assertEquals("turtle_helmet.eggs", current(g, b -> b.turtleNearby(true).item("seagrass", 2)));
		assertEquals("turtle_helmet.scutes", current(g, b -> b.block("turtle_egg", 3)), "the seagrass was used up for the eggs");
		assertEquals("turtle_helmet.scutes", current(g, b -> b.turtleNearby(true).babyTurtleNearby(true)),
				"the eggs hatched, the babies are growing up");
		assertEquals("turtle_helmet.craft", current(g, b -> b.item("turtle_scute", 5)));
	}

	@Test
	void driedGhast() {
		Tip g = TipRegistry.DRIED_GHAST;
		assertEquals("dried_ghast.nether", current(g, b -> b.item("ghast_tear", 3)));
		assertEquals("dried_ghast.tears", current(g, b -> b.dimension(SurvivalContext.NETHER).item("ghast_tear", 3)));
		assertEquals("dried_ghast.soul_sand", current(g, b -> b.item("ghast_tear", 8)), "back home with all the tears");
		assertEquals("dried_ghast.craft", current(g, b -> b.item("ghast_tear", 8).soulSand(1)));
	}

	@Test
	void copperBulb() {
		Tip g = TipRegistry.COPPER_BULB;
		assertEquals("copper_bulb.copper", current(g, b -> b.item("copper_block", 2).item("copper_ingot", 8)));
		assertEquals("copper_bulb.blaze_rod", current(g, b -> b.item("copper_block", 2).item("copper_ingot", 9)));
		assertEquals("copper_bulb.redstone", current(g, b -> b.item("copper_block", 3).blazeRods(1)));
		assertEquals("copper_bulb.craft", current(g, b -> b.item("copper_block", 3).blazeRods(1).redstone(1)));
		assertEquals("copper_bulb.place", current(g, b -> b.item("copper_bulb", 4)), "crafting doesn't go back");
		assertEquals("copper_bulb.pulse", current(g, b -> b.block("copper_bulb", 1)));
	}

	@Test
	void upgradeTemplate() {
		Tip g = TipRegistry.UPGRADE_TEMPLATE;
		assertEquals("upgrade_template.nether", current(g, b -> b.bastionNear(true)));
		assertEquals("upgrade_template.bastion", current(g, b -> b.dimension(SurvivalContext.NETHER)));
		assertEquals("upgrade_template.loot", current(g, b -> b.dimension(SurvivalContext.NETHER).bastionNear(true)));
	}

	@Test
	void crafterCraftsACrafter() {
		Tip g = TipRegistry.CRAFTER_CRAFTS;
		assertEquals("crafter_crafts.crafter", current(g, b -> b.item("iron_ingot", 5)));
		assertEquals("crafter_crafts.iron", current(g, b -> b.crafter(true).item("iron_ingot", 4)));
		assertEquals("crafter_crafts.parts", current(g, b -> b.crafter(true).item("iron_ingot", 5).item("dropper", 1).redstone(2)),
				"the crafting table is missing");
		assertEquals("crafter_crafts.fill", current(g, b -> b.crafter(true).item("iron_ingot", 5).item("dropper", 1)
				.item("crafting_table", 1).redstone(2)));
		assertEquals("crafter_crafts.fill", current(g, b -> b.crafter(true).item("iron_ingot", 2).crafterSlots(4)),
				"filling the crafter uses up the carried iron; the step doesn't go back");
		assertEquals("crafter_crafts.pulse", current(g, b -> b.crafter(true).crafterSlots(9)),
				"the materials are in the crafter now");
	}

	@Test
	void farm() {
		Tip g = TipRegistry.FARM;
		assertEquals("farm.hoe", current(g, b -> b.item("wheat_seeds", 6)));
		assertEquals("farm.seeds", current(g, b -> b.item("stone_hoe", 1).item("wheat_seeds", 3).item("carrot", 2)));
		assertEquals("farm.till", current(g, b -> b.item("wooden_hoe", 1).item("wheat_seeds", 4).item("potato", 2)));
		assertEquals("farm.plant", current(g, b -> b.item("wheat_seeds", 6).block("farmland", 2)), "the hoe may stay at home");
		assertEquals("farm.plant", current(g, b -> b.item("wheat_seeds", 2).crops(4).block("farmland", 6)),
				"planted seeds count, so planting doesn't send the player back");
	}

	@Test
	void bow() {
		Tip g = TipRegistry.BOW;
		assertEquals("bow.string", current(g, b -> b.item("string", 2)));
		assertEquals("bow.craft", current(g, b -> b.item("string", 3)));
		assertEquals("bow.flint", current(g, b -> b.hasBow(true).item("flint", 2).arrows(4)), "4 arrows count as one flint");
		assertEquals("bow.feathers", current(g, b -> b.hasBow(true).item("flint", 3).arrows(4)));
		assertEquals("bow.arrows", current(g, b -> b.hasBow(true).item("flint", 4).item("feather", 4)));
		assertEquals("bow.arrows", current(g, b -> b.hasBow(true).item("flint", 1).item("feather", 1).arrows(12)),
				"crafting arrows doesn't send the player back");
	}

	@Test
	void bread() {
		Tip g = TipRegistry.BREAD;
		assertEquals("bread.ripe", current(g, b -> b.crops(6).item("wheat", 2)));
		assertEquals("bread.harvest", current(g, b -> b.ripeWheat(3).item("wheat", 2)));
		assertEquals("bread.bake", current(g, b -> b.item("wheat", 9)));
		assertEquals("bread.bake", current(g, b -> b.item("wheat", 3).bread(2)), "baked bread counts as its wheat");
	}

	@Test
	void tameAnimal() {
		Tip g = TipRegistry.TAME_ANIMAL;
		assertEquals("tame_animal.find", current(g, b -> b.item("bone", 5)));
		assertEquals("tame_animal.food", current(g, b -> b.tameableNearby(true)));
		assertEquals("tame_animal.feed", current(g, b -> b.tameableNearby(true).item("cod", 1)));
	}

	@Test
	void milk() {
		Tip g = TipRegistry.MILK;
		assertEquals("milk.iron", current(g, b -> b.item("iron_ingot", 2).milkableNearby(true)));
		assertEquals("milk.bucket", current(g, b -> b.item("iron_ingot", 3)));
		assertEquals("milk.cow", current(g, b -> b.item("bucket", 1)), "crafting the bucket used up the iron");
		assertEquals("milk.milk", current(g, b -> b.item("bucket", 1).milkableNearby(true)));
	}

	@Test
	void fishing() {
		Tip g = TipRegistry.FISHING;
		assertEquals("fishing.string", current(g, b -> b.item("string", 1).block("water", 20)));
		assertEquals("fishing.rod", current(g, b -> b.item("string", 2)));
		assertEquals("fishing.water", current(g, b -> b.item("fishing_rod", 1)));
		assertEquals("fishing.cast", current(g, b -> b.item("fishing_rod", 1).block("water", 20)));
	}

	@Test
	void fullIronArmor() {
		Tip g = TipRegistry.FULL_IRON_ARMOR;
		assertEquals("full_iron_armor.helmet", current(g, b -> b.item("iron_ingot", 3).armorSlots(2)));
		assertEquals("full_iron_armor.helmet", current(g, b -> b.item("iron_ingot", 20)),
				"enough iron isn't the helmet yet: the step waits until it is crafted");
		assertEquals("full_iron_armor.leggings", current(g, b -> b.armorSlots(1 | 2 | 8)), "pieces the player has are skipped");
		assertEquals("full_iron_armor.boots", current(g, b -> b.armorSlots(1 | 2 | 4)));
		Steps.Step helmet = Steps.BY_GOAL.get("full_iron_armor").getFirst();
		assertEquals(3, helmet.value(SurvivalContext.builder().item("iron_ingot", 3).build()));
		assertEquals(5, helmet.target());
	}

	@Test
	void diamondPickaxe() {
		Tip g = TipRegistry.DIAMOND_PICKAXE;
		assertEquals("diamond_pickaxe.iron_pickaxe", current(g, b -> b.pickaxeTier(2).y(-50)));
		assertEquals("diamond_pickaxe.deep", current(g, b -> b.pickaxeTier(3).y(40)));
		assertEquals("diamond_pickaxe.diamonds", current(g, b -> b.pickaxeTier(3).y(-50).diamonds(1)));
		assertEquals("diamond_pickaxe.craft", current(g, b -> b.pickaxeTier(3).y(70).diamonds(3)), "back home with the diamonds");
	}

	@Test
	void enchant() {
		Tip g = TipRegistry.ENCHANT;
		assertEquals("enchant.table", current(g, b -> b.item("lapis_lazuli", 5).xpLevel(10)));
		assertEquals("enchant.lapis", current(g, b -> b.hasEnchantingTable(true).item("lapis_lazuli", 2)));
		assertEquals("enchant.levels", current(g, b -> b.hasEnchantingTable(true).item("lapis_lazuli", 3).xpLevel(2)));
		assertEquals("enchant.enchant", current(g, b -> b.hasEnchantingTable(true).item("lapis_lazuli", 3).xpLevel(5)));
	}

	@Test
	void elytra() {
		Tip g = TipRegistry.ELYTRA;
		assertEquals("elytra.end", current(g, b -> b.endCityNear(true)));
		assertEquals("elytra.city", current(g, b -> b.dimension(SurvivalContext.END)));
		assertEquals("elytra.ship", current(g, b -> b.dimension(SurvivalContext.END).endCityNear(true)));
		assertEquals("elytra.take", current(g, b -> b.dimension(SurvivalContext.END).elytraFrame(true)),
				"the ship floats beside the city; seeing the frame is enough");
	}

	@Test
	void netheriteGear() {
		Tip g = TipRegistry.NETHERITE_GEAR;
		assertEquals("netherite_gear.template", current(g, b -> b.item("diamond_sword", 1).hasNetheriteIngot(true)));
		assertEquals("netherite_gear.diamond", current(g, b -> b.hasUpgradeTemplate(true)));
		assertEquals("netherite_gear.ingot", current(g, b -> b.hasUpgradeTemplate(true).item("diamond_chestplate", 1)));
		assertEquals("netherite_gear.table", current(g, b -> b.hasUpgradeTemplate(true).item("diamond_pickaxe", 1)
				.hasNetheriteIngot(true)));
		assertEquals("netherite_gear.upgrade", current(g, b -> b.hasUpgradeTemplate(true).item("diamond_pickaxe", 1)
				.hasNetheriteIngot(true).block("smithing_table", 1)));
	}

	@Test
	void winRaid() {
		Tip g = TipRegistry.WIN_RAID;
		assertEquals("win_raid.bottle", current(g, b -> b));
		assertEquals("win_raid.omen", current(g, b -> b.hasOminousBottle(true)));
		assertEquals("win_raid.village", current(g, b -> b.omen(true)), "the bottle is drunk");
		assertEquals("win_raid.fight", current(g, b -> b.raidActive(true)), "the omen turns into the raid");
	}

	@Test
	void decoratedPot() {
		Tip g = TipRegistry.DECORATED_POT;
		assertEquals("decorated_pot.parts", current(g, b -> b.item(Steps.POT_PARTS, 3)));
		assertEquals("decorated_pot.craft", current(g, b -> b.item(Steps.POT_PARTS, 4)));
	}

	@Test
	void armorTrim() {
		Tip g = TipRegistry.ARMOR_TRIM;
		assertEquals("armor_trim.template", current(g, b -> b.item(Steps.TRIM_MATERIALS, 5).item(Steps.TRIMMABLE_ARMOR, 1)));
		assertEquals("armor_trim.material", current(g, b -> b.item(Steps.TRIM_TEMPLATES, 1)));
		assertEquals("armor_trim.armor", current(g, b -> b.item(Steps.TRIM_TEMPLATES, 1).item(Steps.TRIM_MATERIALS, 1)));
		assertEquals("armor_trim.table", current(g, b -> b.item(Steps.TRIM_TEMPLATES, 1).item(Steps.TRIM_MATERIALS, 1)
				.item(Steps.TRIMMABLE_ARMOR, 1)));
		assertEquals("armor_trim.trim", current(g, b -> b.item(Steps.TRIM_TEMPLATES, 1).item(Steps.TRIM_MATERIALS, 1)
				.item(Steps.TRIMMABLE_ARMOR, 1).block("smithing_table", 1)));
	}

	@Test
	void snifferEgg() {
		Tip g = TipRegistry.SNIFFER_EGG;
		assertEquals("sniffer_egg.brush", current(g, b -> b.structure(Structures.OCEAN_RUIN_WARM)));
		assertEquals("sniffer_egg.ruin", current(g, b -> b.hasBrush(true).structure(Structures.OCEAN_RUIN_COLD)),
				"only warm ocean ruins hide sniffer eggs");
		assertEquals("sniffer_egg.dig", current(g, b -> b.hasBrush(true).structure(Structures.OCEAN_RUIN_WARM)));
	}

	@Test
	void hydrateGhast() {
		Tip g = TipRegistry.HYDRATE_GHAST;
		assertEquals("hydrate_ghast.ghast", current(g, b -> b.hasWaterBucket(true)));
		assertEquals("hydrate_ghast.water", current(g, b -> b.hasDriedGhast(true)));
		assertEquals("hydrate_ghast.place", current(g, b -> b.hasDriedGhast(true).hasWaterBucket(true)));
		assertEquals("hydrate_ghast.place", current(g, b -> b.driedGhast(true).block("water", 3)), "placed next to the water");
	}

	@Test
	void ghastling() {
		Tip g = TipRegistry.GHASTLING;
		assertEquals("ghastling.wet", current(g, b -> b.driedGhast(true)), "a dried ghast on land doesn't swell");
		Steps.Step swell = Steps.current(g, SurvivalContext.builder().ghastHydration(2).build());
		assertNotNull(swell);
		assertEquals("ghastling.swell", swell.id());
		assertEquals(2, swell.value(SurvivalContext.builder().ghastHydration(2).build()));
		assertEquals(Steps.GHAST_HYDRATION, swell.target());
	}

	@Test
	void rideHappyGhast() {
		Tip g = TipRegistry.RIDE_HAPPY_GHAST;
		assertEquals("ride_happy_ghast.grown", current(g, b -> b.hasHarness(true)));
		assertEquals("ride_happy_ghast.harness", current(g, b -> b.grownHappyGhast(true)));
		assertEquals("ride_happy_ghast.equip", current(g, b -> b.grownHappyGhast(true).hasHarness(true)));
		assertEquals("ride_happy_ghast.ride", current(g, b -> b.grownHappyGhast(true).harnessedGhast(true)),
				"the harness is on the ghast now, not in the inventory");
	}

	@Test
	void openVault() {
		Tip g = TipRegistry.OPEN_VAULT;
		assertEquals("open_vault.chambers", current(g, b -> b));
		assertEquals("open_vault.key", current(g, b -> b.structure(Structures.TRIAL_CHAMBERS)));
		assertEquals("open_vault.open", current(g, b -> b.item("trial_key", 1)));
	}

	@Test
	void ominousVault() {
		Tip g = TipRegistry.OMINOUS_VAULT;
		assertEquals("ominous_vault.bottle", current(g, b -> b.item("trial_key", 2)), "a plain key doesn't count");
		assertEquals("ominous_vault.omen", current(g, b -> b.hasOminousBottle(true)));
		assertEquals("ominous_vault.chambers", current(g, b -> b.omen(true)));
		assertEquals("ominous_vault.key", current(g, b -> b.trialOmen(true)));
		assertEquals("ominous_vault.open", current(g, b -> b.item("ominous_trial_key", 1)), "the omen may be gone by now");
	}

	@Test
	void ironGolem() {
		Tip g = TipRegistry.IRON_GOLEM;
		assertEquals("iron_golem.iron_blocks", current(g, b -> b.item("iron_block", 3).item("iron_ingot", 8)));
		assertEquals("iron_golem.pumpkin", current(g, b -> b.item("iron_block", 3).item("iron_ingot", 9)), "9 ingots are a block");
		assertEquals("iron_golem.pumpkin", current(g, b -> b.block("iron_block", 4)), "placed blocks count");
		assertEquals("iron_golem.build", current(g, b -> b.block("iron_block", 4).item("carved_pumpkin", 1)));
	}

	@Test
	void bookshelvesCountBooksAsTheirMaterials() {
		Tip g = TipRegistry.BOOKSHELVES;
		assertEquals("bookshelves.paper", current(g, b -> b.item("paper", 8)));
		assertEquals("bookshelves.leather", current(g, b -> b.item("paper", 9)));
		assertEquals("bookshelves.books", current(g, b -> b.item("paper", 9).item("leather", 3)));
		// after crafting one book: 6 paper, 2 leather, 1 book is still enough for 3 books
		assertEquals("bookshelves.books", current(g, b -> b.item("paper", 6).item("leather", 2).item("book", 1)));
		assertEquals("bookshelves.craft", current(g, b -> b.item("book", 3)));
		assertEquals("bookshelves.place", current(g, b -> b.item("bookshelf", 1)));
	}

	@Test
	void eyesAndStronghold() {
		assertEquals("eyes_of_ender.powder", current(TipRegistry.EYES_OF_ENDER, b -> b.item("blaze_powder", 6).eyesOfEnder(5)));
		assertEquals("eyes_of_ender.craft", current(TipRegistry.EYES_OF_ENDER, b -> b.item("blaze_powder", 7).eyesOfEnder(5)));
		assertEquals("find_stronghold.throw", current(TipRegistry.FIND_STRONGHOLD, b -> b));
		assertEquals("find_stronghold.follow", current(TipRegistry.FIND_STRONGHOLD, b -> b.eyesThrown(1)));
	}

	@Test
	void dragonCrystalsOnlyCountInTheEnd() {
		Tip g = TipRegistry.DRAGON;
		assertEquals("dragon.crystals", current(g, b -> b));
		assertEquals("dragon.crystals", current(g, b -> b.dimension(SurvivalContext.END).endCrystals(3)));
		assertEquals("dragon.fight", current(g, b -> b.dimension(SurvivalContext.END)));
		Steps.Step crystals = Steps.BY_GOAL.get("dragon").getFirst();
		assertEquals(7, crystals.value(SurvivalContext.builder().dimension(SurvivalContext.END).endCrystals(3).build()));
		assertEquals(0, crystals.value(SurvivalContext.builder().build()), "no false progress outside the End");
	}

	@Test
	void netheriteBeaconAndSmelter() {
		assertEquals("netherite_ingot.smelt", current(TipRegistry.NETHERITE_INGOT, b -> b.item("ancient_debris", 2).item("netherite_scrap", 2)));
		assertEquals("netherite_ingot.gold", current(TipRegistry.NETHERITE_INGOT, b -> b.item("netherite_scrap", 4)));
		assertEquals("netherite_ingot.craft", current(TipRegistry.NETHERITE_INGOT, b -> b.item("netherite_scrap", 4).item("gold_ingot", 4)));

		assertEquals("beacon.obsidian", current(TipRegistry.BEACON, b -> b.item("glass", 5)));
		assertEquals("beacon.base", current(TipRegistry.BEACON, b -> b.block("beacon", 1)), "a placed beacon skips the crafting");

		assertEquals("full_beacon.level2", current(TipRegistry.FULL_BEACON, b -> b.beaconLevel(1)));
		assertEquals("full_beacon.level4", current(TipRegistry.FULL_BEACON, b -> b.beaconLevel(3)));

		assertEquals("auto_smelter.top", current(TipRegistry.AUTO_SMELTER, b -> b.item("hopper", 1).block("hopper", 1)));
		assertEquals("auto_smelter.bottom", current(TipRegistry.AUTO_SMELTER, b -> b.block("hopper", 2).hopperOnFurnace(true)));
	}

	@Test
	void enchantingTable() {
		Tip g = TipRegistry.ENCHANTING_TABLE;
		assertEquals("enchanting_table.diamonds", current(g, b -> b.diamonds(1).obsidian(10)), "the pickaxe used the diamonds");
		assertEquals("enchanting_table.obsidian", current(g, b -> b.diamonds(2).obsidian(3)));
		assertEquals("enchanting_table.paper", current(g, b -> b.diamonds(2).obsidian(4).item("paper", 2)));
		assertEquals("enchanting_table.leather", current(g, b -> b.diamonds(2).obsidian(4).item("paper", 3)));
		assertEquals("enchanting_table.book", current(g, b -> b.diamonds(2).obsidian(4).item("paper", 3).item("leather", 1)));
		assertEquals("enchanting_table.craft", current(g, b -> b.diamonds(2).obsidian(4).item("book", 1)),
				"a book counts as its paper and leather");
	}

	@Test
	void treasure() {
		Tip g = TipRegistry.TREASURE;
		assertEquals("treasure.map", current(g, b -> b));
		assertEquals("treasure.follow", current(g, b -> b.treasureDistance(300)));
		assertEquals("treasure.dig", current(g, b -> b.treasureDistance(Steps.TREASURE_NEAR)));
		assertEquals("treasure.dig", current(g, b -> b.treasureDistance(0)), "standing on the X");
	}

	@Test
	void harness() {
		Tip g = TipRegistry.HARNESS;
		assertEquals("harness.leather", current(g, b -> b.item("leather", 2).item("glass", 2).wool(1)));
		assertEquals("harness.glass", current(g, b -> b.item("leather", 3).item("glass", 1)));
		assertEquals("harness.wool", current(g, b -> b.item("leather", 3).item("glass", 2)));
		assertEquals("harness.craft", current(g, b -> b.item("leather", 3).item("glass", 2).wool(1)));
	}

	@Test
	void honey() {
		Tip g = TipRegistry.HONEY;
		assertEquals("honey.bottle", current(g, b -> b.hive(true)));
		assertEquals("honey.hive", current(g, b -> b.item("glass_bottle", 1)));
		assertEquals("honey.campfire", current(g, b -> b.item("glass_bottle", 1).hive(true).hiveFull(true)),
				"a full hive without smoke: the bees would attack");
		assertEquals("honey.wait", current(g, b -> b.item("glass_bottle", 1).hive(true).hiveSmoked(true)));
		assertEquals("honey.collect", current(g, b -> b.item("glass_bottle", 1).hive(true).hiveSmoked(true).hiveFull(true)));
	}

	@Test
	void horse() {
		Tip g = TipRegistry.HORSE;
		assertEquals("horse.find", current(g, b -> b.item("saddle", 1)));
		assertEquals("horse.tame", current(g, b -> b.horseNearby(true)));
		assertEquals("horse.saddle", current(g, b -> b.horseNearby(true).tamedHorseNearby(true)));
		assertEquals("horse.ride", current(g, b -> b.horseNearby(true).tamedHorseNearby(true).item("saddle", 1)));
		assertEquals("horse.ride", current(g, b -> b.horseNearby(true).tamedHorseNearby(true).saddledHorseNearby(true)),
				"the saddle is already on the horse");
	}

	@Test
	void countersAreClamped() {
		Steps.Step bottles = Steps.BY_GOAL.get("brew_potion").getFirst();
		assertEquals(3, bottles.target());
		assertEquals(3, bottles.value(SurvivalContext.builder().item("glass_bottle", 9).build()));
		assertEquals(0, Steps.BY_GOAL.get("brew_potion").get(2).value(SurvivalContext.builder().build()), "no counter");
	}

	@Test
	void advancedOnlyCountsMovingForwardInTheSameGoal() {
		List<Steps.Step> brew = Steps.BY_GOAL.get("brew_potion");
		assertTrue(Steps.advanced("brew_potion.bottles", brew.get(1)));
		assertFalse(Steps.advanced("brew_potion.water", brew.get(0)), "going back (bottles used up) is no news");
		assertFalse(Steps.advanced("brew_potion.water", brew.get(1)));
		assertFalse(Steps.advanced("", brew.get(1)), "the first look is no news");
		assertFalse(Steps.advanced("bookshelves.paper", brew.get(1)), "another goal");
		assertFalse(Steps.advanced("brew_potion.bottles", null));
	}

	// ---------------------------------------------------------------- the engine and the card

	private static GuideProgress inChapter(Chapter chapter, Tip upTo) {
		GuideProgress p = new GuideProgress();
		p.chapter = chapter.number();
		for (Tip t : chapter.goals()) {
			if (t == upTo) break;
			p.completed.add(t.id());
		}
		return p;
	}

	private static List<AdviceEngine.Message> evaluate(SurvivalContext c, GuideProgress p, long now) {
		return AdviceEngine.evaluate(c, p, now, AdviceEngine.Settings.from(new ModConfig()), false);
	}

	@Test
	void theGuideSaysTheStepAndMovesOnRightAway() {
		GuideProgress p = inChapter(Chapters.MAGIC, TipRegistry.BREW_POTION);
		List<AdviceEngine.Message> first = evaluate(SurvivalContext.builder().build(), p, 0);
		assertEquals(1, first.size());
		assertEquals(TipRegistry.BREW_POTION, first.getFirst().tip());
		assertEquals("brew_potion.bottles", first.getFirst().step().id());

		assertTrue(evaluate(SurvivalContext.builder().build(), p, 20).isEmpty(), "same step: wait for the cooldown");

		List<AdviceEngine.Message> next = evaluate(SurvivalContext.builder().item("glass_bottle", 3).build(), p, 40);
		assertEquals(1, next.size(), "the next step is said at once");
		assertEquals("brew_potion.water", next.getFirst().step().id());

		assertTrue(evaluate(SurvivalContext.builder().item("glass_bottle", 1).build(), p, 60).isEmpty(),
				"going back is not repeated before the cooldown");
	}

	@Test
	void theCardShowsTheStepOfTheCurrentTaskOnly() {
		GuideProgress p = inChapter(Chapters.MAGIC, TipRegistry.BREW_POTION);
		HudSnapshot s = HudSnapshot.of(SurvivalContext.builder().item("glass_bottle", 3).waterBottles(1).build(), p);
		assertEquals("brew_potion", s.next().getFirst().id());
		assertEquals(new HudSnapshot.Entry("brew_potion.water", 1, 3), s.step());

		GuideProgress ch1 = new GuideProgress();
		assertEquals(HudSnapshot.NO_STEP, HudSnapshot.of(SurvivalContext.builder().build(), ch1).step(), "wood has no steps");
	}
}
