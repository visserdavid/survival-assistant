package io.github.visserdavid.survivalassistant.advice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Where valuable ores generate in the overworld, verified against the 26.3 worldgen data
 * ({@code worldgen/placed_feature/ore_*.json}, see docs/biomes-and-heights.md). Pure logic, unit tested.
 */
public final class OreTable {
	private OreTable() {}

	/** Within this distance of {@code bestY} the ore is marked as "best height" (★). */
	public static final int BEST_RANGE = 12;
	/** Lava aquifers fill caves below this level (NoiseBasedChunkGenerator). */
	public static final int LAVA_LEVEL = -54;
	/** Deepslate replaces stone from about here downward. */
	public static final int DEEPSLATE_LEVEL = 0;
	/** Above this height the huge "iron upper" feature (all overworld biomes) kicks in; in practice: mountains. */
	public static final int MOUNTAIN_IRON_LEVEL = 80;

	/** @param rank lower = more valuable (sort order on the card) */
	public record Ore(String id, int minY, int maxY, int bestY, int rank) {
		public boolean at(int y) {
			return y >= minY && y <= maxY;
		}

		public boolean best(int y) {
			return Math.abs(y - bestY) <= BEST_RANGE;
		}
	}

	/** An ore present at a height; {@code best} = near its peak. */
	public record Found(String id, boolean best) {}

	// Ranges are clamped to the world (-64..320). Peaks of trapezoid distributions are the midpoints.
	public static final Ore DIAMOND = new Ore("diamond", -64, 16, -59, 0);   // trapezoid -144..16 (bottom±80)
	public static final Ore EMERALD = new Ore("emerald", -16, 320, 232, 1);  // trapezoid -16..480, mountains only
	public static final Ore GOLD = new Ore("gold", -64, 32, -16, 2);         // trapezoid -64..32
	public static final Ore BADLANDS_GOLD = new Ore("gold", 32, 256, 144, 2); // ore_gold_extra, badlands only
	public static final Ore REDSTONE = new Ore("redstone", -64, 0, -59, 3);  // uniform -64..-49 + trapezoid -96..0
	public static final Ore LAPIS = new Ore("lapis", -64, 32, 0, 4);         // trapezoid -32..32 + buried uniform -64..0
	public static final Ore IRON = new Ore("iron", -64, 72, 16, 5);          // trapezoid -24..56 + uniform -64..72
	public static final Ore HIGH_IRON = new Ore("iron", 80, 320, 232, 5);    // trapezoid 80..384, count 90
	public static final Ore COPPER = new Ore("copper", -16, 112, 48, 6);     // trapezoid -16..112
	public static final Ore COAL = new Ore("coal", 0, 320, 96, 7);           // trapezoid 0..192 + uniform 136..320

	private static List<Ore> oresFor(BiomeGroup g) {
		if (!g.isOverworld()) return List.of(); // nether/end have their own ores; not covered here
		List<Ore> list = new ArrayList<>(List.of(DIAMOND, GOLD, REDSTONE, LAPIS, IRON, HIGH_IRON, COPPER, COAL));
		if (g.hasEmerald()) list.add(EMERALD);
		if (g.hasExtraGold()) list.add(BADLANDS_GOLD);
		return list;
	}

	/** Up to {@code max} ores that generate at {@code y}, most valuable first, without duplicates. */
	public static List<Found> oresAt(int y, BiomeGroup g, int max) {
		List<Found> out = new ArrayList<>();
		oresFor(g).stream()
				.filter(o -> o.at(y))
				.sorted(Comparator.comparingInt(Ore::rank).thenComparing(o -> !o.best(y)))
				.forEach(o -> {
					if (out.size() < max && out.stream().noneMatch(f -> f.id().equals(o.id()))) {
						out.add(new Found(o.id(), o.best(y)));
					}
				});
		return out;
	}

	/** Direction hint towards the best height for the ore the current task needs. */
	public record Pointer(String oreId, Direction direction, int targetY) {}

	public enum Direction { UP, DOWN, HERE, MOUNTAIN }

	/** Which ore a task needs, or null if the task doesn't involve mining. */
	public static String oreForTask(String taskId) {
		if (taskId == null) return null;
		return switch (taskId) {
			case "torches" -> "coal";
			case "iron", "shield", "iron_pickaxe", "iron_armor", "water_bucket", "full_iron_armor", "flint_and_steel" -> "iron";
			case "diamonds", "diamond_pickaxe", "enchanting_table" -> "diamond";
			case "" -> "diamond"; // all milestones done: diamonds are the natural next goal
			default -> null;
		};
	}

	/** @param taskId current task id, "" when all milestones are done, null when unknown */
	public static Pointer pointerFor(String taskId, int y, BiomeGroup g) {
		String ore = oreForTask(taskId);
		if (ore == null || !g.isOverworld()) return null;
		if (ore.equals("iron") && y >= MOUNTAIN_IRON_LEVEL) {
			return new Pointer(ore, Direction.MOUNTAIN, y);
		}
		Ore o = switch (ore) {
			case "coal" -> COAL;
			case "diamond" -> DIAMOND;
			default -> IRON;
		};
		if (o.best(y) || (o == COAL && o.at(y) && y >= o.bestY())) return new Pointer(ore, Direction.HERE, o.bestY());
		return new Pointer(ore, y > o.bestY() ? Direction.DOWN : Direction.UP, o.bestY());
	}

	/**
	 * The first one-time notice that applies here and hasn't been shown yet, or null.
	 * Ids: deep_dark, lava, deepslate, high_iron.
	 */
	public static String newNotice(int y, BiomeGroup g, java.util.Set<String> shown) {
		if (!g.isOverworld()) return null;
		List<String> candidates = new ArrayList<>();
		if (g == BiomeGroup.DEEP_DARK) candidates.add("deep_dark");
		if (y < LAVA_LEVEL) candidates.add("lava");
		if (y < DEEPSLATE_LEVEL) candidates.add("deepslate");
		if (y >= MOUNTAIN_IRON_LEVEL && g.isMountain()) candidates.add("high_iron");
		return candidates.stream().filter(c -> !shown.contains(c)).findFirst().orElse(null);
	}

	/** One depth/biome hazard id worth showing, or null. Highest priority first. */
	public static String hazardAt(int y, BiomeGroup g) {
		if (g == BiomeGroup.DEEP_DARK) return "deep_dark";
		if (y < LAVA_LEVEL) return "lava";
		if (y < DEEPSLATE_LEVEL) return "deepslate";
		return null;
	}
}
