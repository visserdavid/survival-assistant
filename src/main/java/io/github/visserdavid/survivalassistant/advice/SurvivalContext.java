package io.github.visserdavid.survivalassistant.advice;

/**
 * Immutable snapshot of everything the advice engine needs to know about a player.
 * Deliberately free of Minecraft types so it can be unit tested in plain JUnit.
 *
 * <p>"has" flags for placeable blocks (crafting table, furnace, chest, bed) are true when the item is in the
 * inventory <em>or</em> the block stands within a few blocks of the player.
 *
 * @param dayIndex       days since the guide arrived (0 = first day)
 * @param timeOfDay      0..23999, 0 = sunrise, 12000 = sunset, 13000 = night
 * @param pickaxeTier    0 none, 1 wood/gold, 2 stone/copper, 3 iron, 4 diamond+
 * @param swordTier      same scale as pickaxeTier (axes count as weapons too)
 * @param iron           raw iron + iron ingots
 * @param sheltered      the player has a roof above them
 * @param underground    no sky above and well below the surface
 * @param hostilesNearby number of hostile mobs within ~16 blocks
 * @param creeperNearby  a creeper within ~7 blocks
 */
public record SurvivalContext(
		int dayIndex,
		int timeOfDay,
		float health,
		int food,
		int logs,
		int planks,
		boolean hasCraftingTable,
		int pickaxeTier,
		int swordTier,
		int cobblestone,
		boolean hasFurnace,
		int foodItems,
		boolean hasBed,
		int wool,
		int torches,
		boolean hasChest,
		int iron,
		boolean hasShield,
		boolean hasIronArmor,
		boolean hasWaterBucket,
		boolean sheltered,
		boolean underground,
		boolean thundering,
		int hostilesNearby,
		boolean creeperNearby
) {
	public static final int SUNSET = 11000;
	public static final int NIGHT_START = 13000;
	public static final int NIGHT_END = 23000;

	public boolean isNight() {
		return timeOfDay >= NIGHT_START && timeOfDay < NIGHT_END;
	}

	public boolean isDusk() {
		return timeOfDay >= SUNSET && timeOfDay < NIGHT_START;
	}

	public static Builder builder() {
		return new Builder();
	}

	/** Convenience builder, mainly for tests. Defaults: day 0, morning, full health and food, empty inventory, outside. */
	public static final class Builder {
		private int dayIndex, timeOfDay = 1000, food = 20, logs, planks, pickaxeTier, swordTier, cobblestone, foodItems, wool, torches, iron, hostilesNearby;
		private float health = 20f;
		private boolean hasCraftingTable, hasFurnace, hasBed, hasChest, hasShield, hasIronArmor, hasWaterBucket,
				sheltered, underground, thundering, creeperNearby;

		public Builder day(int v) { dayIndex = v; return this; }
		public Builder time(int v) { timeOfDay = v; return this; }
		public Builder health(float v) { health = v; return this; }
		public Builder food(int v) { food = v; return this; }
		public Builder logs(int v) { logs = v; return this; }
		public Builder planks(int v) { planks = v; return this; }
		public Builder craftingTable(boolean v) { hasCraftingTable = v; return this; }
		public Builder pickaxe(int v) { pickaxeTier = v; return this; }
		public Builder sword(int v) { swordTier = v; return this; }
		public Builder cobblestone(int v) { cobblestone = v; return this; }
		public Builder furnace(boolean v) { hasFurnace = v; return this; }
		public Builder foodItems(int v) { foodItems = v; return this; }
		public Builder bed(boolean v) { hasBed = v; return this; }
		public Builder wool(int v) { wool = v; return this; }
		public Builder torches(int v) { torches = v; return this; }
		public Builder chest(boolean v) { hasChest = v; return this; }
		public Builder iron(int v) { iron = v; return this; }
		public Builder shield(boolean v) { hasShield = v; return this; }
		public Builder ironArmor(boolean v) { hasIronArmor = v; return this; }
		public Builder waterBucket(boolean v) { hasWaterBucket = v; return this; }
		public Builder sheltered(boolean v) { sheltered = v; return this; }
		public Builder underground(boolean v) { underground = v; return this; }
		public Builder thundering(boolean v) { thundering = v; return this; }
		public Builder hostiles(int v) { hostilesNearby = v; return this; }
		public Builder creeper(boolean v) { creeperNearby = v; return this; }

		public SurvivalContext build() {
			return new SurvivalContext(dayIndex, timeOfDay, health, food, logs, planks, hasCraftingTable, pickaxeTier, swordTier,
					cobblestone, hasFurnace, foodItems, hasBed, wool, torches, hasChest, iron, hasShield, hasIronArmor, hasWaterBucket,
					sheltered, underground, thundering, hostilesNearby, creeperNearby);
		}
	}
}
