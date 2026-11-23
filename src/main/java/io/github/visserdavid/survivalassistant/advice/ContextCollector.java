package io.github.visserdavid.survivalassistant.advice;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
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

	/** The armor slots plus the off hand (shield, totem, a held trident or mace). */
	private static final EquipmentSlot[] WORN_SLOTS =
			{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND};

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
		boolean boat = false, milk = false, heart = false, turtleHelmet = false, trident = false, filledMap = false, spyglass = false;
		int sugarCane = 0, redstone = 0, pearls = 0, eyes = 0;
		boolean doorOpen = false, pistonOut = false, repeater = false, hopper = false, elytra = false;
		boolean netheriteIngot = false, template = false, netheriteGear = false, netherStar = false, recoveryCompass = false,
				ominousBottle = false, totem = false;
		int skulls = 0, soulSand = 0, echoShards = 0, breezeRods = 0;
		boolean brush = false, pot = false, windCharge = false, heavyCore = false, mace = false, driedGhast = false,
				harness = false, observer = false, crafter = false, autoSmelter = false, bulbLit = false;
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
			else if (s.is(ItemTags.BOATS) || s.is(ItemTags.CHEST_BOATS)) boat = true;
			else if (item == Items.MILK_BUCKET) milk = true;
			else if (item == Items.SUGAR_CANE) sugarCane += n;
			else if (item == Items.HEART_OF_THE_SEA) heart = true;
			else if (item == Items.TURTLE_HELMET) turtleHelmet = true;
			else if (item == Items.TRIDENT) trident = true;
			else if (item == Items.FILLED_MAP) filledMap = true;
			else if (item == Items.SPYGLASS) spyglass = true;
			else if (item == Items.REDSTONE) redstone += n;
			else if (item == Items.ENDER_PEARL) pearls += n;
			else if (item == Items.ENDER_EYE) { eyes += n; pearls += n; }
			else if (item == Items.ELYTRA) elytra = true;
			else if (item == Items.NETHERITE_INGOT) netheriteIngot = true;
			else if (item == Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE) template = true;
			else if (item == Items.WITHER_SKELETON_SKULL) skulls += n;
			else if (item == Items.SOUL_SAND) soulSand += n;
			else if (item == Items.NETHER_STAR) netherStar = true;
			else if (item == Items.ECHO_SHARD) echoShards += n;
			else if (item == Items.RECOVERY_COMPASS) recoveryCompass = true;
			else if (item == Items.OMINOUS_BOTTLE) ominousBottle = true;
			else if (item == Items.TOTEM_OF_UNDYING) totem = true;
			else if (item == Items.BRUSH) brush = true;
			else if (item == Items.DECORATED_POT) pot = true;
			else if (item == Items.BREEZE_ROD) breezeRods += n;
			else if (item == Items.WIND_CHARGE) windCharge = true;
			else if (item == Items.HEAVY_CORE) heavyCore = true;
			else if (item == Items.MACE) mace = true;
			else if (item == Items.DRIED_GHAST) driedGhast = true;
			else if (s.is(ItemTags.HARNESSES)) harness = true;
			if (isNetheriteGear(item)) netheriteGear = true;
			if (s.is(ItemTags.ARROWS)) arrows += n;
			markArmor(s, armor);

			if (s.has(DataComponents.FOOD)) foodItems += n;
			if (s.is(ItemTags.PICKAXES)) pickaxe = Math.max(pickaxe, toolTier(item));
			if (s.is(ItemTags.SWORDS) || s.is(ItemTags.AXES)) weapon = Math.max(weapon, toolTier(item));
		}

		boolean ironArmor = false;
		for (EquipmentSlot slot : WORN_SLOTS) {
			ItemStack wornStack = player.getItemBySlot(slot);
			Item worn = wornStack.getItem();
			markArmor(wornStack, armor);
			if (worn == Items.TURTLE_HELMET) turtleHelmet = true;
			if (worn == Items.TRIDENT) trident = true;
			if (worn == Items.ELYTRA) elytra = true;
			if (worn == Items.TOTEM_OF_UNDYING) totem = true;
			if (worn == Items.MACE) mace = true;
			if (isNetheriteGear(worn)) netheriteGear = true;
			if (slot == EquipmentSlot.OFFHAND) {
				if (worn == Items.SHIELD) shield = true;
				continue;
			}
			// held in the off hand doesn't count as worn
			if (wornStack.is(ItemTags.PIGLIN_SAFE_ARMOR)) goldWorn = true;
			if (armorSlot(wornStack) >= 0) ironArmor = true;
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
				else if (st.is(Blocks.IRON_DOOR)) doorOpen |= st.getValue(BlockStateProperties.OPEN);
				else if (st.is(Blocks.PISTON) || st.is(Blocks.STICKY_PISTON)) pistonOut |= st.getValue(BlockStateProperties.EXTENDED);
				else if (st.is(Blocks.REPEATER)) repeater = true;
				else if (st.is(Blocks.HOPPER)) hopper = true;
				else if (st.is(Blocks.DECORATED_POT)) pot = true;
				else if (st.is(Blocks.OBSERVER)) observer = true;
				else if (st.is(Blocks.CRAFTER)) crafter = true;
				else if (st.getBlock() instanceof CopperBulbBlock) bulbLit |= st.getValue(BlockStateProperties.LIT);
				if (st.getBlock() instanceof AbstractFurnaceBlock && !autoSmelter) {
					BlockState above = level.getBlockState(pos.above()), below = level.getBlockState(pos.below());
					autoSmelter = above.is(Blocks.HOPPER) && above.getValue(BlockStateProperties.FACING_HOPPER) == Direction.DOWN
							&& below.is(Blocks.HOPPER);
				}
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
		boolean warden = !level.getEntitiesOfClass(Warden.class, new AABB(player.blockPosition()).inflate(24), Warden::isAlive).isEmpty();
		boolean ghastling = !level.getEntitiesOfClass(HappyGhast.class, around, HappyGhast::isBaby).isEmpty();
		boolean ridingHappyGhast = player.getVehicle() instanceof HappyGhast;
		boolean ghast = !overworld && !level.getEntitiesOfClass(Ghast.class, far, Ghast::isAlive).isEmpty();
		boolean piglin = !overworld && !level.getEntitiesOfClass(Piglin.class, around, Piglin::isAlive).isEmpty();
		String structure = overworld || SurvivalContext.END.equals(dimension) || SurvivalContext.NETHER.equals(dimension)
				? structureAt(level, player.blockPosition()) : "";
		boolean inVillage = structure.startsWith("village_");
		boolean ridingHorse = player.getVehicle() instanceof AbstractHorse horse && horse.isTamed();
		if (player.getVehicle() instanceof AbstractBoat) boat = true;
		int trades = player.getStats().getValue(Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER));
		boolean creeper = !level.getEntitiesOfClass(Creeper.class, new AABB(player.blockPosition()).inflate(7), Creeper::isAlive).isEmpty();

		return new SurvivalContext(dayIndex, timeOfDay, player.getHealth(), player.getFoodData().getFoodLevel(),
				logs, planks, table, pickaxe, weapon, cobble, furnace, foodItems, bed, wool, torches, chest, iron, shield, ironArmor,
				waterBucket, sheltered, underground, level.isThundering(), hostiles, creeper, sinceRest, lava, fallingAbove,
				digDownStreak, crops, bread, bred, armorPieces, bow, arrows, diamonds, obsidian, flintAndSteel, enchantingTable,
				dimension, inVillage, emeralds, trades, enchantedBook, goldWorn, blazeRods, netherWart, bedInInventory, ghast, piglin,
				brewingStand, bookshelves, anvil, player.experienceLevel, advancements(player),
				structure, boat, ridingHorse, milk, sugarCane, heart, turtleHelmet, trident, filledMap, spyglass,
				redstone, doorOpen, pistonOut, repeater, hopper, pearls, eyes, elytra,
				netheriteIngot, template, netheriteGear, skulls, soulSand, netherStar, echoShards, recoveryCompass,
				ominousBottle, totem, warden, brush, pot, breezeRods, windCharge, heavyCore, mace, driedGhast, ghastling, harness,
				ridingHappyGhast, observer, crafter, autoSmelter, bulbLit);
	}

	/** A netherite tool, weapon or armor piece (made in a smithing table, so not the ingot, scrap or template). */
	private static boolean isNetheriteGear(Item item) {
		String path = BuiltInRegistries.ITEM.getKey(item).getPath();
		return path.startsWith("netherite_") && !path.equals("netherite_ingot") && !path.equals("netherite_scrap")
				&& !path.equals("netherite_block") && !path.endsWith("smithing_template");
	}

	/** The notable structure ({@link SurvivalContext#STRUCTURES}) the position is in, or "". */
	private static String structureAt(ServerLevel level, BlockPos pos) {
		StructureStart start = level.structureManager().getStructureWithPieceAt(pos,
				holder -> holder.is(key -> SurvivalContext.STRUCTURES.contains(key.identifier().getPath())));
		if (!start.isValid()) return "";
		Identifier id = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
		return id == null ? "" : id.getPath();
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
		int slot = armorSlot(s);
		if (slot >= 0) armor[slot] = true;
	}

	/** 0 head, 1 chest, 2 legs, 3 feet for an iron, diamond or netherite armor piece; -1 for anything else. */
	private static int armorSlot(ItemStack s) {
		if (s.isEmpty()) return -1;
		String path = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
		if (!(path.startsWith("iron_") || path.startsWith("diamond_") || path.startsWith("netherite_"))) return -1;
		if (s.is(ItemTags.HEAD_ARMOR)) return 0;
		if (s.is(ItemTags.CHEST_ARMOR)) return 1;
		if (s.is(ItemTags.LEG_ARMOR)) return 2;
		if (s.is(ItemTags.FOOT_ARMOR)) return 3;
		return -1;
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
