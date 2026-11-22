package io.github.visserdavid.survivalassistant.advice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/** Builds a {@link SurvivalContext} from the live game state. */
public final class ContextCollector {
	/** Placed crafting tables, furnaces, chests and beds within this radius count as "has". */
	private static final int BLOCK_SCAN_RADIUS = 8;
	/** Lava within this many blocks (underground) triggers the lava warning. */
	private static final int LAVA_SCAN_RADIUS = 3;

	private ContextCollector() {}

	public static SurvivalContext collect(ServerPlayer player, GuideProgress progress) {
		return collect(player, progress, 0, null);
	}

	/**
	 * @param digDownStreak recent breaks under the player's feet (see {@link DigTracker})
	 * @param stationFound  called with the position of a placed crafting table, chest or bed nearby (may be null)
	 */
	public static SurvivalContext collect(ServerPlayer player, GuideProgress progress, int digDownStreak,
			@Nullable Consumer<BlockPos> stationFound) {
		ServerLevel level = player.level();
		long clock = level.getOverworldClockTime();
		int timeOfDay = (int) Math.floorMod(clock, 24000L);
		int dayIndex = progress.startTick < 0 ? 0 : (int) Math.max(0, clock / 24000L - progress.startTick / 24000L);

		int logs = 0, planks = 0, cobble = 0, foodItems = 0, wool = 0, torches = 0, iron = 0;
		int pickaxe = 0, weapon = 0;
		boolean table = false, bed = false, furnace = false, chest = false, shield = false, waterBucket = false;
		int bread = 0, arrows = 0, diamonds = 0, obsidian = 0, crops = 0;
		boolean bow = false, flintAndSteel = false, enchantingTable = false;
		boolean[] armor = new boolean[4]; // head, chest, legs, feet: iron or better

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

			if (item == Items.BREAD) bread += n;
			else if (item == Items.BOW || item == Items.CROSSBOW) bow = true;
			else if (item == Items.DIAMOND) diamonds += n;
			else if (item == Items.OBSIDIAN) obsidian += n;
			else if (item == Items.FLINT_AND_STEEL) flintAndSteel = true;
			else if (item == Items.ENCHANTING_TABLE) enchantingTable = true;
			if (s.is(ItemTags.ARROWS)) arrows += n;
			markArmor(s, armor);

			if (s.has(DataComponents.FOOD)) foodItems += n;
			if (s.is(ItemTags.PICKAXES)) pickaxe = Math.max(pickaxe, toolTier(item));
			if (s.is(ItemTags.SWORDS) || s.is(ItemTags.AXES)) weapon = Math.max(weapon, toolTier(item));
		}

		boolean ironArmor = false;
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND}) {
			Item worn = player.getItemBySlot(slot).getItem();
			markArmor(player.getItemBySlot(slot), armor);
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
		if (!(table && furnace && chest && bed) || stationFound != null || progress.chapter >= 2) {
			BlockPos origin = player.blockPosition();
			int r = BLOCK_SCAN_RADIUS;
			for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -4, -r), origin.offset(r, 4, r))) {
				BlockState st = level.getBlockState(pos);
				if (st.isAir()) continue;
				boolean station = false;
				if (st.is(Blocks.CRAFTING_TABLE)) { table = true; station = true; }
				else if (st.is(Blocks.FURNACE)) furnace = true;
				else if (st.is(Blocks.CHEST)) { chest = true; station = true; }
				else if (st.is(BlockTags.BEDS)) { bed = true; station = true; }
				else if (st.is(BlockTags.CROPS)) crops++;
				else if (st.is(Blocks.ENCHANTING_TABLE)) enchantingTable = true;
				if (station && stationFound != null) {
					stationFound.accept(pos.immutable());
					stationFound = null; // only the first one
				}
			}
		}

		BlockPos head = BlockPos.containing(player.getEyePosition());
		boolean sheltered = !level.canSeeSky(head);
		int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, head.getX(), head.getZ());
		boolean underground = sheltered && head.getY() < surfaceY - 8;

		boolean lava = false;
		boolean fallingAbove = false;
		if (underground) {
			BlockPos feet = player.blockPosition();
			int r = LAVA_SCAN_RADIUS;
			for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-r, -r, -r), feet.offset(r, r, r))) {
				if (level.getFluidState(pos).is(FluidTags.LAVA)) {
					lava = true;
					break;
				}
			}
			fallingAbove = level.getBlockState(head.above()).getBlock() instanceof FallingBlock;
		}
		int sinceRest = player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
		int bred = player.getStats().getValue(Stats.CUSTOM.get(Stats.ANIMALS_BRED));
		int armorPieces = 0;
		for (boolean b : armor) if (b) armorPieces++;

		AABB around = new AABB(player.blockPosition()).inflate(16);
		int hostiles = level.getEntitiesOfClass(LivingEntity.class, around, e -> e instanceof Enemy && e.isAlive()).size();
		boolean creeper = !level.getEntitiesOfClass(Creeper.class, new AABB(player.blockPosition()).inflate(7), Creeper::isAlive).isEmpty();

		return new SurvivalContext(dayIndex, timeOfDay, player.getHealth(), player.getFoodData().getFoodLevel(),
				logs, planks, table, pickaxe, weapon, cobble, furnace, foodItems, bed, wool, torches, chest, iron, shield, ironArmor,
				waterBucket, sheltered, underground, level.isThundering(), hostiles, creeper, sinceRest, lava, fallingAbove,
				digDownStreak, crops, bread, bred, armorPieces, bow, arrows, diamonds, obsidian, flintAndSteel, enchantingTable);
	}

	/** Marks the armor slot of an iron, diamond or netherite armor piece. */
	private static void markArmor(ItemStack s, boolean[] armor) {
		if (s.isEmpty()) return;
		String path = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
		if (!(path.startsWith("iron_") || path.startsWith("diamond_") || path.startsWith("netherite_"))) return;
		if (s.is(ItemTags.HEAD_ARMOR)) armor[0] = true;
		else if (s.is(ItemTags.CHEST_ARMOR)) armor[1] = true;
		else if (s.is(ItemTags.LEG_ARMOR)) armor[2] = true;
		else if (s.is(ItemTags.FOOT_ARMOR)) armor[3] = true;
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
