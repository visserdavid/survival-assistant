package io.github.visserdavid.survivalassistant.advice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Builds a {@link SurvivalContext} from the live game state. */
public final class ContextCollector {
	/** Placed crafting tables, furnaces, chests and beds within this radius count as "has". */
	private static final int BLOCK_SCAN_RADIUS = 8;

	private ContextCollector() {}

	public static SurvivalContext collect(ServerPlayer player, GuideProgress progress) {
		ServerLevel level = player.level();
		long clock = level.getOverworldClockTime();
		int timeOfDay = (int) Math.floorMod(clock, 24000L);
		int dayIndex = progress.startTick < 0 ? 0 : (int) Math.max(0, clock / 24000L - progress.startTick / 24000L);

		int logs = 0, planks = 0, cobble = 0, foodItems = 0, wool = 0, torches = 0, iron = 0;
		int pickaxe = 0, weapon = 0;
		boolean table = false, bed = false, furnace = false, chest = false, shield = false, waterBucket = false;

		var inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.isEmpty()) continue;
			int n = s.getCount();
			Item item = s.getItem();
			if (s.is(ItemTags.LOGS)) logs += n;
			else if (s.is(ItemTags.PLANKS)) planks += n;
			else if (s.is(ItemTags.WOOL)) wool += n;
			else if (s.is(ItemTags.BEDS)) bed = true;
			else if (item == Items.COBBLESTONE || item == Items.COBBLED_DEEPSLATE || item == Items.BLACKSTONE) cobble += n;
			else if (item == Items.CRAFTING_TABLE) table = true;
			else if (item == Items.FURNACE) furnace = true;
			else if (item == Items.CHEST) chest = true;
			else if (item == Items.TORCH) torches += n;
			else if (item == Items.RAW_IRON || item == Items.IRON_INGOT) iron += n;
			else if (item == Items.SHIELD) shield = true;
			else if (item == Items.WATER_BUCKET) waterBucket = true;

			if (s.has(DataComponents.FOOD)) foodItems += n;
			if (s.is(ItemTags.PICKAXES)) pickaxe = Math.max(pickaxe, toolTier(item));
			if (s.is(ItemTags.SWORDS) || s.is(ItemTags.AXES)) weapon = Math.max(weapon, toolTier(item));
		}

		boolean ironArmor = false;
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND}) {
			Item worn = player.getItemBySlot(slot).getItem();
			if (slot == EquipmentSlot.OFFHAND) {
				if (worn == Items.SHIELD) shield = true;
				continue;
			}
			if (worn == Items.IRON_HELMET || worn == Items.IRON_CHESTPLATE || worn == Items.IRON_LEGGINGS || worn == Items.IRON_BOOTS
					|| worn == Items.DIAMOND_HELMET || worn == Items.DIAMOND_CHESTPLATE || worn == Items.DIAMOND_LEGGINGS || worn == Items.DIAMOND_BOOTS
					|| worn == Items.NETHERITE_HELMET || worn == Items.NETHERITE_CHESTPLATE || worn == Items.NETHERITE_LEGGINGS || worn == Items.NETHERITE_BOOTS) {
				ironArmor = true;
			}
		}

		// Placed blocks nearby (players usually place a crafting table/furnace/bed right after crafting it).
		if (!(table && furnace && chest && bed)) {
			BlockPos origin = player.blockPosition();
			int r = BLOCK_SCAN_RADIUS;
			for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -4, -r), origin.offset(r, 4, r))) {
				BlockState st = level.getBlockState(pos);
				if (st.isAir()) continue;
				if (st.is(Blocks.CRAFTING_TABLE)) table = true;
				else if (st.is(Blocks.FURNACE)) furnace = true;
				else if (st.is(Blocks.CHEST)) chest = true;
				else if (st.is(BlockTags.BEDS)) bed = true;
			}
		}

		BlockPos head = BlockPos.containing(player.getEyePosition());
		boolean sheltered = !level.canSeeSky(head);
		int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, head.getX(), head.getZ());
		boolean underground = sheltered && head.getY() < surfaceY - 8;

		AABB around = new AABB(player.blockPosition()).inflate(16);
		int hostiles = level.getEntitiesOfClass(LivingEntity.class, around, e -> e instanceof Enemy && e.isAlive()).size();
		boolean creeper = !level.getEntitiesOfClass(Creeper.class, new AABB(player.blockPosition()).inflate(7), Creeper::isAlive).isEmpty();

		return new SurvivalContext(dayIndex, timeOfDay, player.getHealth(), player.getFoodData().getFoodLevel(),
				logs, planks, table, pickaxe, weapon, cobble, furnace, foodItems, bed, wool, torches, chest, iron, shield, ironArmor,
				waterBucket, sheltered, underground, level.isThundering(), hostiles, creeper);
	}

	private static int toolTier(Item item) {
		if (item == Items.WOODEN_PICKAXE || item == Items.WOODEN_SWORD || item == Items.WOODEN_AXE
				|| item == Items.GOLDEN_PICKAXE || item == Items.GOLDEN_SWORD || item == Items.GOLDEN_AXE) return 1;
		if (item == Items.STONE_PICKAXE || item == Items.STONE_SWORD || item == Items.STONE_AXE
				|| item == Items.COPPER_PICKAXE || item == Items.COPPER_SWORD || item == Items.COPPER_AXE) return 2;
		if (item == Items.IRON_PICKAXE || item == Items.IRON_SWORD || item == Items.IRON_AXE) return 3;
		return 4; // diamond, netherite or modded: treat as top tier
	}
}
