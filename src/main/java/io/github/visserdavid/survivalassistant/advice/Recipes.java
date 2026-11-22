package io.github.visserdavid.survivalassistant.advice;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The crafting recipe that belongs to each task, as a 3x3 grid. Checked against the Minecraft 26.3 recipe data
 * with {@code tools/recipe_check.py}. Where the game accepts a group of items (any planks, any cobblestone-like
 * stone, coal or charcoal) the grid shows the most common one.
 */
public final class Recipes {
	private Recipes() {}

	/**
	 * @param resultId item id of the result
	 * @param count    how many items one craft gives
	 * @param pattern  up to 3 rows of up to 3 characters; a space is an empty slot
	 * @param key      character to item id
	 */
	public record Recipe(String resultId, int count, List<String> pattern, Map<Character, String> key) {
		/** 2x2 recipes also work in the inventory; larger ones need a crafting table. */
		public boolean needsTable() {
			if (pattern.size() > 2) return true;
			for (String row : pattern) if (row.length() > 2) return true;
			return false;
		}

		/** Item id in grid slot (row, col), or null for an empty slot. */
		public String at(int row, int col) {
			if (row >= pattern.size()) return null;
			String line = pattern.get(row);
			if (col >= line.length()) return null;
			char c = line.charAt(col);
			return c == ' ' ? null : key.get(c);
		}

		/** Total number of items needed for one craft. */
		public int ingredientCount() {
			int n = 0;
			for (String row : pattern) for (char c : row.toCharArray()) if (c != ' ') n++;
			return n;
		}
	}

	private static final String PLANKS = "minecraft:oak_planks";
	private static final String STICK = "minecraft:stick";
	private static final String COBBLE = "minecraft:cobblestone";
	private static final String IRON = "minecraft:iron_ingot";

	/** Task id to recipe. Tasks without a crafting step (food, shelter, iron) have none. */
	public static final Map<String, Recipe> BY_TASK = Map.ofEntries(
			Map.entry("wood", new Recipe("minecraft:oak_planks", 4, List.of("L"), Map.of('L', "minecraft:oak_log"))),
			Map.entry("crafting_table", new Recipe("minecraft:crafting_table", 1, List.of("##", "##"), Map.of('#', PLANKS))),
			Map.entry("wooden_pickaxe", new Recipe("minecraft:wooden_pickaxe", 1, List.of("XXX", " # ", " # "),
					Map.of('X', PLANKS, '#', STICK))),
			Map.entry("stone_tools", new Recipe("minecraft:stone_pickaxe", 1, List.of("XXX", " # ", " # "),
					Map.of('X', COBBLE, '#', STICK))),
			Map.entry("furnace", new Recipe("minecraft:furnace", 1, List.of("###", "# #", "###"), Map.of('#', COBBLE))),
			Map.entry("bed", new Recipe("minecraft:white_bed", 1, List.of("###", "XXX"),
					Map.of('#', "minecraft:white_wool", 'X', PLANKS))),
			Map.entry("torches", new Recipe("minecraft:torch", 4, List.of("X", "#"), Map.of('X', "minecraft:coal", '#', STICK))),
			Map.entry("chest", new Recipe("minecraft:chest", 1, List.of("###", "# #", "###"), Map.of('#', PLANKS))),
			Map.entry("shield", new Recipe("minecraft:shield", 1, List.of("WoW", "WWW", " W "), Map.of('W', PLANKS, 'o', IRON))),
			Map.entry("iron_pickaxe", new Recipe("minecraft:iron_pickaxe", 1, List.of("XXX", " # ", " # "),
					Map.of('X', IRON, '#', STICK))),
			Map.entry("iron_armor", new Recipe("minecraft:iron_chestplate", 1, List.of("X X", "XXX", "XXX"), Map.of('X', IRON))),
			Map.entry("water_bucket", new Recipe("minecraft:bucket", 1, List.of("# #", " # "), Map.of('#', IRON)))
	);

	public static Optional<Recipe> forTask(String taskId) {
		return Optional.ofNullable(BY_TASK.get(taskId));
	}
}
