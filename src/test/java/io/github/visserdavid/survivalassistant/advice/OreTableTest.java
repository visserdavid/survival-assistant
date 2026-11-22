package io.github.visserdavid.survivalassistant.advice;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OreTableTest {
	private static List<String> ids(List<OreTable.Found> found) {
		return found.stream().map(OreTable.Found::id).toList();
	}

	private static boolean best(List<OreTable.Found> found, String id) {
		return found.stream().anyMatch(f -> f.id().equals(id) && f.best());
	}

	@Test
	void deepGivesDiamondAndRedstoneAsBest() {
		List<OreTable.Found> f = OreTable.oresAt(-59, BiomeGroup.PLAINS, 4);
		assertEquals("diamond", f.get(0).id(), "most valuable first");
		assertTrue(best(f, "diamond"));
		assertTrue(best(f, "redstone"));
		assertTrue(f.size() <= 4);
	}

	@Test
	void y16IsBestForIron() {
		List<OreTable.Found> f = OreTable.oresAt(16, BiomeGroup.FOREST, 8);
		assertTrue(best(f, "iron"));
		assertTrue(ids(f).contains("diamond"), "diamond reaches up to Y 16");
		assertFalse(ids(f).contains("redstone"), "redstone stops at Y 0");
	}

	@Test
	void emeraldOnlyInMountainBiomes() {
		assertTrue(ids(OreTable.oresAt(100, BiomeGroup.MOUNTAINS, 8)).contains("emerald"));
		assertTrue(ids(OreTable.oresAt(100, BiomeGroup.MEADOW, 8)).contains("emerald"));
		assertFalse(ids(OreTable.oresAt(100, BiomeGroup.PLAINS, 8)).contains("emerald"));
	}

	@Test
	void goldAboveGroundOnlyInBadlands() {
		assertTrue(ids(OreTable.oresAt(100, BiomeGroup.BADLANDS, 8)).contains("gold"));
		assertFalse(ids(OreTable.oresAt(100, BiomeGroup.PLAINS, 8)).contains("gold"));
		assertTrue(ids(OreTable.oresAt(-16, BiomeGroup.PLAINS, 8)).contains("gold"));
	}

	@Test
	void noDuplicateIronAtHighAltitude() {
		List<String> f = ids(OreTable.oresAt(100, BiomeGroup.MOUNTAINS, 8));
		assertEquals(1, f.stream().filter("iron"::equals).count());
	}

	@Test
	void noOverworldOresInNetherOrEnd() {
		assertTrue(OreTable.oresAt(30, BiomeGroup.NETHER, 4).isEmpty());
		assertTrue(OreTable.oresAt(60, BiomeGroup.END, 4).isEmpty());
		assertNull(OreTable.pointerFor("iron", 30, BiomeGroup.NETHER));
	}

	@Test
	void pointerForIronTask() {
		OreTable.Pointer down = OreTable.pointerFor("iron", 64, BiomeGroup.PLAINS);
		assertEquals(OreTable.Direction.DOWN, down.direction());
		assertEquals(16, down.targetY());
		assertEquals(OreTable.Direction.UP, OreTable.pointerFor("shield", -40, BiomeGroup.PLAINS).direction());
		assertEquals(OreTable.Direction.HERE, OreTable.pointerFor("iron_armor", 20, BiomeGroup.PLAINS).direction());
		assertEquals(OreTable.Direction.MOUNTAIN, OreTable.pointerFor("iron", 120, BiomeGroup.MOUNTAINS).direction());
	}

	@Test
	void pointerForOtherTasks() {
		assertNull(OreTable.pointerFor("wood", 64, BiomeGroup.PLAINS), "no mining needed");
		assertNull(OreTable.pointerFor(null, 64, BiomeGroup.PLAINS), "no task known");
		assertEquals("coal", OreTable.pointerFor("torches", 64, BiomeGroup.PLAINS).oreId());
		assertEquals(OreTable.Direction.HERE, OreTable.pointerFor("torches", 150, BiomeGroup.PLAINS).direction());
		OreTable.Pointer afterAll = OreTable.pointerFor("", 64, BiomeGroup.PLAINS);
		assertEquals("diamond", afterAll.oreId());
		assertEquals(-59, afterAll.targetY());
	}

	@Test
	void hazards() {
		assertEquals("lava", OreTable.hazardAt(-58, BiomeGroup.PLAINS));
		assertEquals("deepslate", OreTable.hazardAt(-20, BiomeGroup.PLAINS));
		assertNull(OreTable.hazardAt(40, BiomeGroup.PLAINS));
		assertEquals("deep_dark", OreTable.hazardAt(-58, BiomeGroup.DEEP_DARK), "deep dark wins");
	}

	@Test
	void depthNoticesShowOnceInPriorityOrder() {
		Set<String> shown = new java.util.HashSet<>();
		assertNull(OreTable.newNotice(64, BiomeGroup.PLAINS, shown));
		assertEquals("lava", OreTable.newNotice(-58, BiomeGroup.PLAINS, shown));
		shown.add("lava");
		assertEquals("deepslate", OreTable.newNotice(-58, BiomeGroup.PLAINS, shown));
		shown.add("deepslate");
		assertNull(OreTable.newNotice(-58, BiomeGroup.PLAINS, shown));
		assertEquals("high_iron", OreTable.newNotice(120, BiomeGroup.MOUNTAINS, shown));
		assertNull(OreTable.newNotice(120, BiomeGroup.PLAINS, Set.of()), "only mountains get the iron notice");
		assertNull(OreTable.newNotice(20, BiomeGroup.NETHER, Set.of()));
	}
}
