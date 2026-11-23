package io.github.visserdavid.survivalassistant.advice;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
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
		int emeralds = 0, blazeRods = 0, netherWart = 0, bookshelves = 0;
		boolean enchantedBook = false, bedInInventory = false, brewingStand = false, anvil = false, goldWorn = false;
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
			else if (s.is(ItemTags.BEDS)) { bed = true; bedInInventory = true; }
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
			else if (item == Items.EMERALD) emeralds += n;
			else if (item == Items.ENCHANTED_BOOK) enchantedBook = true;
			else if (item == Items.BLAZE_ROD) blazeRods += n;
			else if (item == Items.NETHER_WART) netherWart += n;
			else if (item == Items.BREWING_STAND) brewingStand = true;
			else if (s.is(ItemTags.ANVIL)) anvil = true;
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
			if (player.getItemBySlot(slot).is(ItemTags.PIGLIN_SAFE_ARMOR)) goldWorn = true;
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
				else if (st.is(Blocks.ENCHANTING_TABLE)) {
					enchantingTable = true;
					bookshelves = Math.max(bookshelves, bookshelvesAround(level, pos));
				}
				else if (st.is(Blocks.BREWING_STAND)) brewingStand = true;
				else if (st.is(BlockTags.ANVIL)) anvil = true;
				if (station && stationFound != null) {
					stationFound.accept(pos.immutable());
					stationFound = null; // only the first one
				}
			}
		}

		BlockPos head = BlockPos.containing(player.getEyePosition());
		boolean sheltered = !level.canSeeSky(head);
		int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, head.getX(), head.getZ());
		String dimension = dimensionName(level);
		boolean overworld = SurvivalContext.OVERWORLD.equals(dimension);
		// The Nether's roof makes everything look "underground"; only the overworld has real caves for these tips.
		boolean underground = overworld && sheltered && head.getY() < surfaceY - 8;

		boolean lava = false;
		boolean fallingAbove = false;
		if (underground || SurvivalContext.NETHER.equals(dimension)) {
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
		AABB far = new AABB(player.blockPosition()).inflate(32);
		boolean ghast = !overworld && !level.getEntitiesOfClass(Ghast.class, far, Ghast::isAlive).isEmpty();
		boolean piglin = !overworld && !level.getEntitiesOfClass(Piglin.class, around, Piglin::isAlive).isEmpty();
		boolean inVillage = overworld && level.structureManager()
				.getStructureWithPieceAt(player.blockPosition(), StructureTags.VILLAGE).isValid();
		int trades = player.getStats().getValue(Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER));
		boolean creeper = !level.getEntitiesOfClass(Creeper.class, new AABB(player.blockPosition()).inflate(7), Creeper::isAlive).isEmpty();

		return new SurvivalContext(dayIndex, timeOfDay, player.getHealth(), player.getFoodData().getFoodLevel(),
				logs, planks, table, pickaxe, weapon, cobble, furnace, foodItems, bed, wool, torches, chest, iron, shield, ironArmor,
				waterBucket, sheltered, underground, level.isThundering(), hostiles, creeper, sinceRest, lava, fallingAbove,
				digDownStreak, crops, bread, bred, armorPieces, bow, arrows, diamonds, obsidian, flintAndSteel, enchantingTable,
				dimension, inVillage, emeralds, trades, enchantedBook, goldWorn, blazeRods, netherWart, bedInInventory, ghast, piglin,
				brewingStand, bookshelves, anvil, player.experienceLevel, advancements(player));
	}

	/** "overworld", "nether", "end", or the path of another dimension's id. */
	private static String dimensionName(ServerLevel level) {
		if (level.dimension() == Level.OVERWORLD) return SurvivalContext.OVERWORLD;
		if (level.dimension() == Level.NETHER) return SurvivalContext.NETHER;
		if (level.dimension() == Level.END) return SurvivalContext.END;
		return level.dimension().identifier().getPath();
	}

	/** Bookshelves that power this enchanting table, counted the way the game does (at most 15 are useful). */
	private static int bookshelvesAround(ServerLevel level, BlockPos table) {
		int n = 0;
		for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
			if (EnchantingTableBlock.isValidBookShelf(level, table, offset)) n++;
		}
		return n;
	}

	/** The vanilla advancements the chapters track that this player has completed. */
	private static Set<String> advancements(ServerPlayer player) {
		Set<String> done = new HashSet<>();
		var manager = player.level().getServer().getAdvancements();
		for (String id : Chapters.trackedAdvancements()) {
			AdvancementHolder holder = manager.get(Identifier.withDefaultNamespace(id));
			if (holder != null && player.getAdvancements().getOrStartProgress(holder).isDone()) done.add(id);
		}
		return done;
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
