package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Shape checks; the exact patterns are verified against the game data by tools/recipe_check.py. */
class RecipesTest {
	@Test
	void everyCraftableTaskHasARecipe() {
		Set<String> noCrafting = Set.of("food", "shelter", "iron", "breed", "full_iron_armor", "diamonds", "obsidian",
				"find_village", "first_trade", "trades", "emeralds", "enchanted_book", "enter_nether", "find_fortress", "blaze_rods",
				"nether_wart", "home_safe", "brew_potion", "level_30", "enchant");
		for (Tip m : Chapters.ALL.stream().flatMap(c -> c.goals().stream()).toList()) {
			assertEquals(!noCrafting.contains(m.id()), Recipes.forTask(m.id()).isPresent(), m.id());
		}
	}

	@Test
	void patternsFitTheGridAndUseKnownKeys() {
		Recipes.BY_TASK.forEach((task, r) -> {
			assertTrue(r.pattern().size() <= 3, task);
			for (String row : r.pattern()) {
				assertTrue(row.length() <= 3, task);
				for (char c : row.toCharArray()) {
					if (c != ' ') assertTrue(r.key().containsKey(c), task + " uses undefined key " + c);
				}
			}
		});
	}

	@Test
	void knownAmounts() {
		assertEquals(8, Recipes.forTask("furnace").orElseThrow().ingredientCount());
		assertEquals(8, Recipes.forTask("chest").orElseThrow().ingredientCount());
		assertEquals(7, Recipes.forTask("shield").orElseThrow().ingredientCount(), "6 planks + 1 iron");
		assertEquals(8, Recipes.forTask("iron_armor").orElseThrow().ingredientCount());
		assertEquals(3, Recipes.forTask("water_bucket").orElseThrow().ingredientCount());
		assertEquals(4, Recipes.forTask("torches").orElseThrow().count());
		assertEquals(4, Recipes.forTask("wood").orElseThrow().count());
	}

	@Test
	void tableNeededOnlyForBigRecipes() {
		assertFalse(Recipes.forTask("crafting_table").orElseThrow().needsTable());
		assertFalse(Recipes.forTask("torches").orElseThrow().needsTable());
		assertFalse(Recipes.forTask("wood").orElseThrow().needsTable());
		assertTrue(Recipes.forTask("wooden_pickaxe").orElseThrow().needsTable());
		assertTrue(Recipes.forTask("bed").orElseThrow().needsTable());
	}

	@Test
	void gridLookup() {
		Recipes.Recipe furnace = Recipes.forTask("furnace").orElseThrow();
		assertNull(furnace.at(1, 1), "furnace has a hole in the middle");
		assertEquals("minecraft:cobblestone", furnace.at(0, 0));
		assertNull(Recipes.forTask("torches").orElseThrow().at(0, 1));
		assertNull(Recipes.forTask("torches").orElseThrow().at(2, 0));
	}

	@Test
	void crosshairAdvice() {
		assertTrue(CrosshairAdvice.isInterestingBlock("deepslate_iron_ore"));
		assertTrue(CrosshairAdvice.isInterestingBlock("obsidian"));
		assertFalse(CrosshairAdvice.isInterestingBlock("stone"));
		assertEquals(CrosshairAdvice.Pickaxe.DIAMOND, CrosshairAdvice.requiredPickaxe(true, false, false));
		assertEquals(CrosshairAdvice.Pickaxe.STONE, CrosshairAdvice.requiredPickaxe(false, false, true));
		assertEquals(CrosshairAdvice.Pickaxe.WOODEN, CrosshairAdvice.requiredPickaxe(false, false, false));
		assertTrue(CrosshairAdvice.hasMobTip("creeper"));
		assertFalse(CrosshairAdvice.hasMobTip("villager"));
	}
}
