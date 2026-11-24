package io.github.visserdavid.survivalassistant.advice;

import io.github.visserdavid.survivalassistant.util.Ticks;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Builds a {@link SurvivalContext} from the live game state. */
public final class ContextCollector {
	/** Placed crafting tables, furnaces, chests, beds and the like within this radius count as "has". */
	private static final int BLOCK_SCAN_RADIUS = 8;
	/** Height of the block scan above and below the feet. */
	private static final int BLOCK_SCAN_HEIGHT = 4;
	/** Lava within this many blocks (underground) triggers the lava warning. */
	private static final int LAVA_SCAN_RADIUS = 3;
	/** "Underground" means at least this many blocks below the surface (with a roof above). */
	private static final int UNDERGROUND_DEPTH = 8;

	/** Hostile mobs, piglins and ghastlings within this radius count as "nearby". */
	private static final int MOB_RADIUS = 16;
	private static final int CREEPER_RADIUS = 7;
	private static final int WARDEN_RADIUS = 24;
	/** Ghasts are big and shoot from afar. Also the size of the one entity query. */
	private static final int GHAST_RADIUS = 32;
	/** The obsidian pillars of the End stand within this distance of 0,0 (their crystals are what the dragon step counts). */
	private static final int END_PILLAR_RADIUS = 64;
	private static final int END_PILLAR_TOP = 256;
	/** Bee nests hang in trees: hives are also looked for this many blocks above the normal block scan. */
	private static final int HIVE_EXTRA_HEIGHT = 4;
	/** Brewing stand slots: 0 - 2 hold the bottles, 4 the blaze powder. */
	private static final int STAND_BOTTLE_SLOTS = 3, STAND_FUEL_SLOT = 4;

	/** The armor slots plus the off hand (shield, totem, a held trident or mace). */
	private static final EquipmentSlot[] WORN_SLOTS =
			{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND};
	/** Head, chest, legs, feet (see {@link #armorSlot}). */
	private static final int ARMOR_SLOTS = 4;

	private ContextCollector() {}

	public static SurvivalContext collect(ServerPlayer player, GuideProgress progress) {
		return collect(player, progress, 0);
	}

	/** @param digDownStreak recent breaks under the player's feet (see {@link DigTracker}) */
	public static SurvivalContext collect(ServerPlayer player, GuideProgress progress, int digDownStreak) {
		SurvivalContext.Builder b = SurvivalContext.builder();
		Environment env = environment(player, progress, b);
		scanPlayer(player, digDownStreak, b);
		scanInventory(player, b);
		scanBlocks(player.level(), player.blockPosition(), b);
		scanDanger(player, env, b);
		scanCreatures(player, env.overworld(), b);
		if (player.level().dimension() == Level.END) b.endCrystals(endCrystals(player.level()));
		return b.build();
	}

	/** What {@link #environment} found that the other scans need. */
	private record Environment(BlockPos head, boolean overworld, boolean underground, boolean sheltered, boolean duskOrNight,
			boolean thundering) {}

	/** Day, time, dimension, structure, weather, shelter. */
	private static Environment environment(ServerPlayer player, GuideProgress progress, SurvivalContext.Builder b) {
		ServerLevel level = player.level();
		long clock = level.getOverworldClockTime();
		int timeOfDay = Ticks.timeOfDay(clock);
		int dayIndex = progress.daysInChapter();

		BlockPos head = BlockPos.containing(player.getEyePosition());
		boolean sheltered = !level.canSeeSky(head);
		int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, head.getX(), head.getZ());
		String dimension = dimensionName(level);
		boolean overworld = SurvivalContext.OVERWORLD.equals(dimension);
		// The Nether's roof makes everything look "underground"; only the overworld has real caves for these tips.
		boolean underground = overworld && sheltered && head.getY() < surfaceY - UNDERGROUND_DEPTH;
		boolean knownDimension = overworld || SurvivalContext.END.equals(dimension) || SurvivalContext.NETHER.equals(dimension);
		boolean thundering = level.isThundering();

		b.dayIndex(dayIndex).timeOfDay(timeOfDay).dimension(dimension)
				.structure(knownDimension ? structureAt(level, player.blockPosition()) : "")
				.thundering(thundering).sheltered(sheltered).underground(underground);
		boolean duskOrNight = SurvivalContext.duskOrNight(timeOfDay);
		return new Environment(head, overworld, underground, sheltered, duskOrNight, thundering);
	}

	/** Health, food, experience, statistics and advancements. */
	private static void scanPlayer(ServerPlayer player, int digDownStreak, SurvivalContext.Builder b) {
		var stats = player.getStats();
		b.health(player.getHealth()).food(player.getFoodData().getFoodLevel()).xpLevel(player.experienceLevel)
				.ticksSinceRest(stats.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)))
				.animalsBred(stats.getValue(Stats.CUSTOM.get(Stats.ANIMALS_BRED)))
				.trades(stats.getValue(Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER)))
				.eyesThrown(stats.getValue(Stats.ITEM_USED.get(Items.ENDER_EYE)))
				.digDownStreak(digDownStreak)
				.advancements(advancements(player));
	}

	/** Everything carried, then what is worn or held in the off hand. */
	private static void scanInventory(ServerPlayer player, SurvivalContext.Builder b) {
		int logs = 0, planks = 0, cobble = 0, foodItems = 0, wool = 0, torches = 0, iron = 0, bread = 0, arrows = 0;
		int diamonds = 0, obsidian = 0, emeralds = 0, blazeRods = 0, netherWart = 0, sugarCane = 0, redstone = 0;
		int pearls = 0, eyes = 0, skulls = 0, soulSand = 0, echoShards = 0, breezeRods = 0, pickaxe = 0, weapon = 0;
		int waterBottles = 0, treasureDistance = -1;
		Map<String, Integer> tracked = new HashMap<>();
		boolean[] armor = new boolean[ARMOR_SLOTS];

		var inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.isEmpty()) continue;
			int n = s.getCount();
			Item item = s.getItem();
			if (s.is(ItemTags.LOGS)) logs += n;
			else if (s.is(ItemTags.PLANKS)) planks += n;
			else if (s.is(ItemTags.WOOL)) wool += n;
			else if (s.is(ItemTags.BEDS)) b.hasBed(true).bedInInventory(true);
			else if (item == Items.COBBLESTONE || item == Items.COBBLED_DEEPSLATE || item == Items.BLACKSTONE) cobble += n;
			else if (item == Items.CRAFTING_TABLE) b.hasCraftingTable(true);
			else if (item == Items.FURNACE) b.hasFurnace(true);
			else if (item == Items.CHEST) b.hasChest(true);
			else if (item == Items.TORCH) torches += n;
			else if (item == Items.RAW_IRON || item == Items.IRON_INGOT) iron += n;
			else if (item == Items.SHIELD) b.hasShield(true);
			else if (item == Items.WATER_BUCKET) b.hasWaterBucket(true);

			if (item == Items.BREAD) bread += n;
			else if (item == Items.BOW || item == Items.CROSSBOW) b.hasBow(true);
			else if (item == Items.DIAMOND) diamonds += n;
			else if (item == Items.OBSIDIAN) obsidian += n;
			else if (item == Items.FLINT_AND_STEEL) b.hasFlintAndSteel(true);
			else if (item == Items.ENCHANTING_TABLE) b.hasEnchantingTable(true);
			else if (item == Items.EMERALD) emeralds += n;
			else if (item == Items.ENCHANTED_BOOK) b.hasEnchantedBook(true);
			else if (item == Items.BLAZE_ROD) blazeRods += n;
			else if (item == Items.NETHER_WART) netherWart += n;
			else if (item == Items.BREWING_STAND) b.hasBrewingStand(true);
			else if (s.is(ItemTags.ANVIL)) b.hasAnvil(true);
			else if (s.is(ItemTags.BOATS) || s.is(ItemTags.CHEST_BOATS)) b.hasBoat(true);
			else if (item == Items.MILK_BUCKET) b.hasMilk(true);
			else if (item == Items.SUGAR_CANE) sugarCane += n;
			else if (item == Items.HEART_OF_THE_SEA) b.hasHeartOfTheSea(true);
			else if (item == Items.FILLED_MAP) b.hasFilledMap(true);
			else if (item == Items.SPYGLASS) b.hasSpyglass(true);
			else if (item == Items.REDSTONE) redstone += n;
			else if (item == Items.ENDER_PEARL) pearls += n;
			else if (item == Items.ENDER_EYE) { eyes += n; pearls += n; }
			else if (item == Items.NETHERITE_INGOT) b.hasNetheriteIngot(true);
			else if (item == Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE) b.hasUpgradeTemplate(true);
			else if (item == Items.WITHER_SKELETON_SKULL) skulls += n;
			else if (item == Items.SOUL_SAND) soulSand += n;
			else if (item == Items.NETHER_STAR) b.hasNetherStar(true);
			else if (item == Items.ECHO_SHARD) echoShards += n;
			else if (item == Items.RECOVERY_COMPASS) b.hasRecoveryCompass(true);
			else if (item == Items.OMINOUS_BOTTLE) b.hasOminousBottle(true);
			else if (item == Items.BRUSH) b.hasBrush(true);
			else if (item == Items.DECORATED_POT) b.hasDecoratedPot(true);
			else if (item == Items.BREEZE_ROD) breezeRods += n;
			else if (item == Items.WIND_CHARGE) b.hasWindCharge(true);
			else if (item == Items.HEAVY_CORE) b.hasHeavyCore(true);
			else if (item == Items.DRIED_GHAST) b.hasDriedGhast(true);
			else if (s.is(ItemTags.HARNESSES)) b.hasHarness(true);
			markGear(item, b);
			if (s.is(ItemTags.ARROWS)) arrows += n;
			if (item == Items.POTION && s.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER)) {
				waterBottles += n;
			}
			int toX = treasureDistance(s, player);
			if (toX >= 0 && (treasureDistance < 0 || toX < treasureDistance)) treasureDistance = toX;
			String id = path(item);
			if (Steps.TRACKED_ITEMS.contains(id)) tracked.merge(id, n, Integer::sum);
			markArmor(s, armor);

			if (s.has(DataComponents.FOOD)) foodItems += n;
			if (s.is(ItemTags.PICKAXES)) pickaxe = Math.max(pickaxe, Gear.toolTier(path(item)));
			if (s.is(ItemTags.SWORDS) || s.is(ItemTags.AXES)) weapon = Math.max(weapon, Gear.toolTier(path(item)));
		}

		for (EquipmentSlot slot : WORN_SLOTS) {
			ItemStack worn = player.getItemBySlot(slot);
			markArmor(worn, armor);
			markGear(worn.getItem(), b);
			if (slot == EquipmentSlot.OFFHAND) {
				if (worn.is(Items.SHIELD)) b.hasShield(true);
				continue;
			}
			// held in the off hand doesn't count as worn
			if (worn.is(ItemTags.PIGLIN_SAFE_ARMOR)) b.goldArmorWorn(true);
			if (armorSlot(worn) >= 0) b.hasIronArmor(true);
		}
		if (player.getVehicle() instanceof AbstractBoat) b.hasBoat(true);

		int armorPieces = 0;
		for (boolean covered : armor) if (covered) armorPieces++;

		b.logs(logs).planks(planks).cobblestone(cobble).foodItems(foodItems).wool(wool).torches(torches).iron(iron)
				.bread(bread).arrows(arrows).diamonds(diamonds).obsidian(obsidian).emeralds(emeralds).blazeRods(blazeRods)
				.netherWart(netherWart).sugarCane(sugarCane).redstone(redstone).enderPearls(pearls).eyesOfEnder(eyes)
				.witherSkulls(skulls).soulSand(soulSand).echoShards(echoShards).breezeRods(breezeRods)
				.pickaxeTier(pickaxe).swordTier(weapon).armorPieces(armorPieces).waterBottles(waterBottles)
				.treasureDistance(treasureDistance);
		tracked.forEach(b::item);
	}

	/** Blocks (horizontally) from the player to the red X on this map, or -1 when it isn't a treasure map. */
	private static int treasureDistance(ItemStack s, ServerPlayer player) {
		MapDecorations decorations = s.get(DataComponents.MAP_DECORATIONS);
		if (decorations == null) return -1;
		int best = -1;
		for (MapDecorations.Entry e : decorations.decorations().values()) {
			if (e.type().value() != MapDecorationTypes.RED_X.value()) continue;
			int d = (int) Math.round(Math.hypot(e.x() - player.getX(), e.z() - player.getZ()));
			if (best < 0 || d < best) best = d;
		}
		return best;
	}

	/** Items that count whether they are carried, worn or held in the off hand. */
	private static void markGear(Item item, SurvivalContext.Builder b) {
		if (item == Items.TURTLE_HELMET) b.hasTurtleHelmet(true);
		else if (item == Items.TRIDENT) b.hasTrident(true);
		else if (item == Items.ELYTRA) b.hasElytra(true);
		else if (item == Items.TOTEM_OF_UNDYING) b.hasTotem(true);
		else if (item == Items.MACE) b.hasMace(true);
		if (Gear.isNetheriteGear(path(item))) b.hasNetheriteGear(true);
	}

	/** Placed blocks nearby (players usually place a crafting table, furnace or bed right after crafting it). */
	private static void scanBlocks(ServerLevel level, BlockPos origin, SurvivalContext.Builder b) {
		int r = BLOCK_SCAN_RADIUS, h = BLOCK_SCAN_HEIGHT;
		int crops = 0, bookshelves = 0;
		boolean autoSmelter = false, hopperOnFurnace = false, standFuel = false;
		int standBottles = 0, beaconLevel = 0;
		Map<String, Integer> tracked = new HashMap<>();
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -h, -r), origin.offset(r, h, r))) {
			BlockState st = level.getBlockState(pos);
			if (st.isAir()) continue;
			String id = BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath();
			if (Steps.TRACKED_BLOCKS.contains(id)) tracked.merge(id, 1, Integer::sum);
			if (st.is(Blocks.CRAFTING_TABLE)) b.hasCraftingTable(true);
			else if (st.is(Blocks.FURNACE)) b.hasFurnace(true);
			else if (st.is(Blocks.CHEST)) b.hasChest(true);
			else if (st.is(BlockTags.BEDS)) b.hasBed(true);
			else if (st.is(BlockTags.CROPS)) crops++;
			else if (st.is(Blocks.ENCHANTING_TABLE)) {
				b.hasEnchantingTable(true);
				bookshelves = Math.max(bookshelves, bookshelvesAround(level, pos));
			}
			else if (st.is(Blocks.BREWING_STAND)) {
				b.hasBrewingStand(true);
				if (level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand) {
					standBottles = Math.max(standBottles, standBottles(stand));
					if (stand.fuel > 0 || stand.getItem(STAND_FUEL_SLOT).is(Items.BLAZE_POWDER)) standFuel = true;
				}
			}
			else if (st.is(Blocks.BEACON)) beaconLevel = Math.max(beaconLevel, beaconLevel(level, pos));
			else if (st.is(BlockTags.ANVIL)) b.hasAnvil(true);
			else if (st.is(Blocks.IRON_DOOR)) { if (st.getValue(BlockStateProperties.OPEN)) b.ironDoorOpen(true); }
			else if (st.is(Blocks.PISTON) || st.is(Blocks.STICKY_PISTON)) {
				if (st.getValue(BlockStateProperties.EXTENDED)) b.pistonExtended(true);
			}
			else if (st.is(Blocks.REPEATER)) b.repeater(true);
			else if (st.is(Blocks.HOPPER)) b.hopper(true);
			else if (st.is(Blocks.DECORATED_POT)) b.hasDecoratedPot(true);
			else if (st.is(Blocks.OBSERVER)) b.observer(true);
			else if (st.is(Blocks.CRAFTER)) b.crafter(true);
			else if (st.getBlock() instanceof CopperBulbBlock) { if (st.getValue(BlockStateProperties.LIT)) b.copperBulbLit(true); }
			if (st.getBlock() instanceof AbstractFurnaceBlock) {
				if (!hopperOnFurnace) hopperOnFurnace = hopperAbove(level, pos);
				if (!autoSmelter) autoSmelter = fedByHoppers(level, pos);
			}
		}
		scanHives(level, origin, b);
		b.crops(crops).bookshelves(bookshelves).autoSmelter(autoSmelter).hopperOnFurnace(hopperOnFurnace)
				.standBottles(standBottles).standFuel(standFuel).beaconLevel(beaconLevel);
		tracked.forEach(b::block);
	}

	/** Beehives and bee nests from the feet up to the treetops: whether one is there, smoked and full of honey. */
	private static void scanHives(ServerLevel level, BlockPos origin, SurvivalContext.Builder b) {
		int r = BLOCK_SCAN_RADIUS, h = BLOCK_SCAN_HEIGHT;
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -h, -r), origin.offset(r, h + HIVE_EXTRA_HEIGHT, r))) {
			BlockState st = level.getBlockState(pos);
			if (!st.is(BlockTags.BEEHIVES)) continue;
			b.hive(true);
			if (CampfireBlock.isSmokeyPos(level, pos)) b.hiveSmoked(true);
			if (st.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS) b.hiveFull(true);
		}
	}

	/** Bottles (water or potions) in the bottom slots of a brewing stand. */
	private static int standBottles(BrewingStandBlockEntity stand) {
		int n = 0;
		for (int slot = 0; slot < STAND_BOTTLE_SLOTS; slot++) if (!stand.getItem(slot).isEmpty()) n++;
		return n;
	}

	/** Pyramid levels under a beacon, counted the way the game does: every level a full square of base blocks. */
	private static int beaconLevel(ServerLevel level, BlockPos beacon) {
		int levels = 0;
		for (int i = 1; i <= Steps.MAX_BEACON_LEVEL; i++) {
			int y = beacon.getY() - i;
			if (y < level.getMinY()) break;
			for (int x = -i; x <= i; x++) {
				for (int z = -i; z <= i; z++) {
					if (!level.getBlockState(new BlockPos(beacon.getX() + x, y, beacon.getZ() + z)).is(BlockTags.BEACON_BASE_BLOCKS)) {
						return levels;
					}
				}
			}
			levels = i;
		}
		return levels;
	}

	/** End crystals still standing on the central island (only loaded ones count: the player is usually close). */
	private static int endCrystals(ServerLevel level) {
		int r = END_PILLAR_RADIUS;
		return level.getEntitiesOfClass(EndCrystal.class, new AABB(-r, 0, -r, r, END_PILLAR_TOP, r), Entity::isAlive).size();
	}

	/** A hopper above that feeds into the furnace and a hopper below that takes the result out. */
	private static boolean fedByHoppers(ServerLevel level, BlockPos furnace) {
		return hopperAbove(level, furnace) && level.getBlockState(furnace.below()).is(Blocks.HOPPER);
	}

	/** A hopper on top of the furnace that points down into it. */
	private static boolean hopperAbove(ServerLevel level, BlockPos furnace) {
		BlockState above = level.getBlockState(furnace.above());
		return above.is(Blocks.HOPPER) && above.getValue(BlockStateProperties.FACING_HOPPER) == Direction.DOWN;
	}

	/** Lava and falling blocks (underground or in the Nether) and whether the spot is well lit. */
	private static void scanDanger(ServerPlayer player, Environment env, SurvivalContext.Builder b) {
		ServerLevel level = player.level();
		BlockPos feet = player.blockPosition();
		if (env.underground() || level.dimension() == Level.NETHER) {
			int r = LAVA_SCAN_RADIUS;
			for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-r, -r, -r), feet.offset(r, r, r))) {
				if (level.getFluidState(pos).is(FluidTags.LAVA)) {
					b.lava(true);
					break;
				}
			}
			// only underground is the falling-block tip used (the Nether's roof is too high to matter)
			if (env.underground()) b.fallingBlockAbove(level.getBlockState(env.head().above()).getBlock() instanceof FallingBlock);
		}
		boolean darkMatters = SurvivalContext.darkMatters(env.underground(), env.overworld(), env.sheltered(),
				env.duskOrNight(), env.thundering());
		b.litArea(darkMatters && litAround(level, feet));
	}

	/** Mobs around the player (one entity query, sorted out in memory) and what the player rides. */
	private static void scanCreatures(ServerPlayer player, boolean overworld, SurvivalContext.Builder b) {
		AABB near = around(player, MOB_RADIUS), creeperZone = around(player, CREEPER_RADIUS), wardenZone = around(player, WARDEN_RADIUS);
		int hostiles = 0;
		for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, around(player, GHAST_RADIUS), e -> true)) {
			AABB box = e.getBoundingBox();
			boolean alive = e.isAlive(), close = box.intersects(near);
			if (alive && close && e instanceof Enemy) hostiles++;
			if (alive && e instanceof Creeper && box.intersects(creeperZone)) b.creeper(true);
			if (alive && e instanceof Warden && box.intersects(wardenZone)) b.warden(true);
			if (close && e instanceof HappyGhast ghast && ghast.isBaby()) b.ghastling(true);
			if (!overworld && alive && e instanceof Ghast) b.ghast(true);
			if (!overworld && alive && close && e instanceof Piglin) b.piglin(true);
			if (alive && close && e instanceof AbstractHorse horse && !(e instanceof Llama) && !(e instanceof Camel)) {
				b.horseNearby(true);
				if (horse.isTamed()) b.tamedHorseNearby(true);
				if (horse.isSaddled()) b.saddledHorseNearby(true);
			}
		}
		b.hostiles(hostiles)
				.ridingTamedHorse(player.getVehicle() instanceof AbstractHorse horse && horse.isTamed())
				.ridingHappyGhast(player.getVehicle() instanceof HappyGhast);
	}

	/** The box {@code radius} blocks around the player's block, as entity queries use it. */
	private static AABB around(ServerPlayer player, int radius) {
		return new AABB(player.blockPosition()).inflate(radius);
	}

	/** No monster can spawn close by: light matters in this dimension and every spot nearby is lit. */
	private static boolean litAround(ServerLevel level, BlockPos feet) {
		int limit = level.dimensionType().monsterSpawnBlockLightLimit();
		if (!LightScan.lightMatters(limit)) return false;
		return SpawnSpots.scan(level, feet, LightScan.LIT_RADIUS, LightScan.LIT_VERTICAL, limit, 1, pos -> {}) == 0;
	}

	/** The notable structure ({@link Structures#ALL}) the position is in, or "". */
	private static String structureAt(ServerLevel level, BlockPos pos) {
		StructureStart start = level.structureManager().getStructureWithPieceAt(pos,
				holder -> holder.is(key -> Identifier.DEFAULT_NAMESPACE.equals(key.identifier().getNamespace())
						&& Structures.ALL.contains(key.identifier().getPath())));
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
		if (!Gear.ironOrBetter(path(s.getItem()))) return -1;
		if (s.is(ItemTags.HEAD_ARMOR)) return 0;
		if (s.is(ItemTags.CHEST_ARMOR)) return 1;
		if (s.is(ItemTags.LEG_ARMOR)) return 2;
		if (s.is(ItemTags.FOOT_ARMOR)) return 3;
		return -1;
	}

	private static String path(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).getPath();
	}
}
