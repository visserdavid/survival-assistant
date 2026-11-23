package io.github.visserdavid.survivalassistant.advice;

import org.jspecify.annotations.Nullable;

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
		/** The crafting grid is 3x3. */
		public static final int GRID = 3;

		public Recipe {
			pattern = List.copyOf(pattern);
			key = Map.copyOf(key);
			if (count < 1) throw new IllegalArgumentException(resultId + ": count must be at least 1");
			if (pattern.isEmpty() || pattern.size() > GRID) throw new IllegalArgumentException(resultId + ": 1 to 3 rows");
			for (String row : pattern) {
				if (row.isEmpty() || row.length() > GRID) throw new IllegalArgumentException(resultId + ": 1 to 3 slots per row");
				for (char c : row.toCharArray()) {
					if (c != ' ' && !key.containsKey(c)) throw new IllegalArgumentException(resultId + ": '" + c + "' is not in the key");
				}
			}
		}

		/** 2x2 recipes also work in the inventory; larger ones need a crafting table. */
		public boolean needsTable() {
			if (pattern.size() > 2) return true;
			for (String row : pattern) if (row.length() > 2) return true;
			return false;
		}

		/** Item id in grid slot (row, col), or null for an empty slot. */
		public @Nullable String at(int row, int col) {
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
	private static final String STRING = "minecraft:string";
	private static final String REDSTONE = "minecraft:redstone";

	/** A bucket: for water (chapter 1) and for milk (chapter 6). */
	private static final Recipe BUCKET = new Recipe("minecraft:bucket", 1, List.of("# #", " # "), Map.of('#', IRON));

	/** Every pickaxe has the same shape: three of the material over two sticks. */
	private static Recipe pickaxe(String resultId, String material) {
		return new Recipe(resultId, 1, List.of("XXX", " # ", " # "), Map.of('X', material, '#', STICK));
	}

	/** Task id to recipe. Tasks without a crafting step (food, shelter, iron, breeding, mining) have none. */
	public static final Map<String, Recipe> BY_TASK = Map.ofEntries(
			Map.entry("wood", new Recipe("minecraft:oak_planks", 4, List.of("L"), Map.of('L', "minecraft:oak_log"))),
			Map.entry("crafting_table", new Recipe("minecraft:crafting_table", 1, List.of("##", "##"), Map.of('#', PLANKS))),
			Map.entry("wooden_pickaxe", pickaxe("minecraft:wooden_pickaxe", PLANKS)),
			Map.entry("stone_tools", pickaxe("minecraft:stone_pickaxe", COBBLE)),
			Map.entry("furnace", new Recipe("minecraft:furnace", 1, List.of("###", "# #", "###"), Map.of('#', COBBLE))),
			Map.entry("bed", new Recipe("minecraft:white_bed", 1, List.of("###", "XXX"),
					Map.of('#', "minecraft:white_wool", 'X', PLANKS))),
			Map.entry("torches", new Recipe("minecraft:torch", 4, List.of("X", "#"), Map.of('X', "minecraft:coal", '#', STICK))),
			Map.entry("chest", new Recipe("minecraft:chest", 1, List.of("###", "# #", "###"), Map.of('#', PLANKS))),
			Map.entry("shield", new Recipe("minecraft:shield", 1, List.of("WoW", "WWW", " W "), Map.of('W', PLANKS, 'o', IRON))),
			Map.entry("iron_pickaxe", pickaxe("minecraft:iron_pickaxe", IRON)),
			Map.entry("iron_armor", new Recipe("minecraft:iron_chestplate", 1, List.of("X X", "XXX", "XXX"), Map.of('X', IRON))),
			Map.entry("water_bucket", BUCKET),
			// chapter 2
			Map.entry("farm", new Recipe("minecraft:wooden_hoe", 1, List.of("XX", " #", " #"), Map.of('X', PLANKS, '#', STICK))),
			Map.entry("bread", new Recipe("minecraft:bread", 1, List.of("###"), Map.of('#', "minecraft:wheat"))),
			Map.entry("bow", new Recipe("minecraft:bow", 1, List.of(" #X", "# X", " #X"), Map.of('#', STICK, 'X', STRING))),
			Map.entry("diamond_pickaxe", pickaxe("minecraft:diamond_pickaxe", "minecraft:diamond")),
			Map.entry("flint_and_steel", new Recipe("minecraft:flint_and_steel", 1, List.of("IF"),
					Map.of('I', IRON, 'F', "minecraft:flint"))),
			Map.entry("enchanting_table", new Recipe("minecraft:enchanting_table", 1, List.of(" B ", "D#D", "###"),
					Map.of('B', "minecraft:book", 'D', "minecraft:diamond", '#', "minecraft:obsidian"))),
			// chapter 3: an iron block for the golem (4 of them in a T, carved pumpkin on top)
			Map.entry("iron_golem", new Recipe("minecraft:iron_block", 1, List.of("###", "###", "###"), Map.of('#', IRON))),
			// chapter 4: a golden helmet keeps piglins calm
			Map.entry("gold_armor", new Recipe("minecraft:golden_helmet", 1, List.of("XXX", "X X"), Map.of('X', "minecraft:gold_ingot"))),
			// chapter 5
			Map.entry("brewing_stand", new Recipe("minecraft:brewing_stand", 1, List.of(" B ", "###"),
					Map.of('B', "minecraft:blaze_rod", '#', COBBLE))),
			Map.entry("bookshelves", new Recipe("minecraft:bookshelf", 1, List.of("###", "XXX", "###"),
					Map.of('#', PLANKS, 'X', "minecraft:book"))),
			Map.entry("anvil", new Recipe("minecraft:anvil", 1, List.of("III", " i ", "iii"),
					Map.of('I', "minecraft:iron_block", 'i', IRON))),
			// chapter 6: a bucket for the milk, a fishing rod, a campfire under the hive, a saddle
			Map.entry("milk", BUCKET),
			Map.entry("fishing", new Recipe("minecraft:fishing_rod", 1, List.of("  #", " #X", "# X"),
					Map.of('#', STICK, 'X', STRING))),
			Map.entry("honey", new Recipe("minecraft:campfire", 1, List.of(" S ", "SCS", "LLL"),
					Map.of('S', STICK, 'C', "minecraft:coal", 'L', "minecraft:oak_log"))),
			Map.entry("horse", new Recipe("minecraft:saddle", 1, List.of(" X ", "X#X"),
					Map.of('X', "minecraft:leather", '#', IRON))),
			// chapter 7
			Map.entry("boat", new Recipe("minecraft:oak_boat", 1, List.of("# #", "###"), Map.of('#', PLANKS))),
			Map.entry("turtle_helmet", new Recipe("minecraft:turtle_helmet", 1, List.of("XXX", "X X"),
					Map.of('X', "minecraft:turtle_scute"))),
			// chapter 8
			Map.entry("map", new Recipe("minecraft:map", 1, List.of("###", "#X#", "###"),
					Map.of('#', "minecraft:paper", 'X', "minecraft:compass"))),
			Map.entry("spyglass", new Recipe("minecraft:spyglass", 1, List.of(" # ", " X ", " X "),
					Map.of('#', "minecraft:amethyst_shard", 'X', "minecraft:copper_ingot"))),
			// chapter 9: redstone
			Map.entry("iron_door", new Recipe("minecraft:iron_door", 3, List.of("##", "##", "##"), Map.of('#', IRON))),
			Map.entry("piston", new Recipe("minecraft:piston", 1, List.of("TTT", "#X#", "#R#"),
					Map.of('T', PLANKS, '#', COBBLE, 'X', IRON, 'R', REDSTONE))),
			Map.entry("repeater", new Recipe("minecraft:repeater", 1, List.of("#X#", "III"),
					Map.of('#', "minecraft:redstone_torch", 'X', REDSTONE, 'I', "minecraft:stone"))),
			Map.entry("hopper", new Recipe("minecraft:hopper", 1, List.of("I I", "ICI", " I "),
					Map.of('I', IRON, 'C', "minecraft:chest"))),
			// chapter 10: an eye of ender is a pearl with blaze powder (shapeless)
			Map.entry("eyes_of_ender", new Recipe("minecraft:ender_eye", 1, List.of("PB"),
					Map.of('P', "minecraft:ender_pearl", 'B', "minecraft:blaze_powder"))),
			// chapter 11: a netherite ingot is 4 scrap and 4 gold (shapeless); the respawn anchor
			Map.entry("netherite_ingot", new Recipe("minecraft:netherite_ingot", 1, List.of("SSS", "SGG", "GG"),
					Map.of('S', "minecraft:netherite_scrap", 'G', "minecraft:gold_ingot"))),
			Map.entry("respawn_anchor", new Recipe("minecraft:respawn_anchor", 1, List.of("OOO", "GGG", "OOO"),
					Map.of('O', "minecraft:crying_obsidian", 'G', "minecraft:glowstone"))),
			// chapter 12
			Map.entry("beacon", new Recipe("minecraft:beacon", 1, List.of("GGG", "GSG", "OOO"),
					Map.of('G', "minecraft:glass", 'S', "minecraft:nether_star", 'O', "minecraft:obsidian"))),
			// chapter 13
			Map.entry("recovery_compass", new Recipe("minecraft:recovery_compass", 1, List.of("SSS", "SCS", "SSS"),
					Map.of('S', "minecraft:echo_shard", 'C', "minecraft:compass"))),
			// chapter 15
			Map.entry("brush", new Recipe("minecraft:brush", 1, List.of("X", "#", "I"),
					Map.of('X', "minecraft:feather", '#', "minecraft:copper_ingot", 'I', STICK))),
			// chapter 16: a breeze rod gives 4 wind charges (shapeless); the mace
			Map.entry("wind_charge", new Recipe("minecraft:wind_charge", 4, List.of("B"), Map.of('B', "minecraft:breeze_rod"))),
			Map.entry("mace", new Recipe("minecraft:mace", 1, List.of(" # ", " I "),
					Map.of('#', "minecraft:heavy_core", 'I', "minecraft:breeze_rod"))),
			// chapter 17
			Map.entry("dried_ghast", new Recipe("minecraft:dried_ghast", 1, List.of("###", "#X#", "###"),
					Map.of('#', "minecraft:ghast_tear", 'X', "minecraft:soul_sand"))),
			Map.entry("harness", new Recipe("minecraft:white_harness", 1, List.of("LLL", "G#G"),
					Map.of('L', "minecraft:leather", 'G', "minecraft:glass", '#', "minecraft:white_wool"))),
			// chapter 18
			Map.entry("observer", new Recipe("minecraft:observer", 1, List.of("###", "RRQ", "###"),
					Map.of('#', COBBLE, 'R', REDSTONE, 'Q', "minecraft:quartz"))),
			Map.entry("copper_bulb", new Recipe("minecraft:copper_bulb", 4, List.of(" C ", "CBC", " R "),
					Map.of('C', "minecraft:copper_block", 'B', "minecraft:blaze_rod", 'R', REDSTONE))),
			Map.entry("crafter", new Recipe("minecraft:crafter", 1, List.of("###", "#C#", "RDR"),
					Map.of('#', IRON, 'C', "minecraft:crafting_table", 'R', REDSTONE, 'D', "minecraft:dropper")))
	);

	public static Optional<Recipe> forTask(String taskId) {
		return Optional.ofNullable(BY_TASK.get(taskId));
	}
}
