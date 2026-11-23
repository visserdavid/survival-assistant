package io.github.visserdavid.survivalassistant.client.hud;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/** Item icon per task / alert id. */
public final class TaskIcons {
	private static final Map<String, ItemStack> ICONS = new HashMap<>();

	static {
		// milestones
		put("wood", Items.OAK_LOG);
		put("crafting_table", Items.CRAFTING_TABLE);
		put("wooden_pickaxe", Items.WOODEN_PICKAXE);
		put("stone_tools", Items.STONE_PICKAXE);
		put("furnace", Items.FURNACE);
		put("food", Items.COOKED_BEEF);
		put("bed", Items.BED.red());
		put("torches", Items.TORCH);
		put("shelter", Items.OAK_DOOR);
		put("chest", Items.CHEST);
		// chapter 2
		put("farm", Items.WHEAT);
		put("bread", Items.BREAD);
		put("breed", Items.WHEAT_SEEDS);
		put("full_iron_armor", Items.IRON_HELMET);
		put("bow", Items.BOW);
		put("diamonds", Items.DIAMOND);
		put("diamond_pickaxe", Items.DIAMOND_PICKAXE);
		put("obsidian", Items.OBSIDIAN);
		put("flint_and_steel", Items.FLINT_AND_STEEL);
		put("enchanting_table", Items.ENCHANTING_TABLE);
		put("iron", Items.RAW_IRON);
		put("shield", Items.SHIELD);
		put("iron_pickaxe", Items.IRON_PICKAXE);
		put("iron_armor", Items.IRON_CHESTPLATE);
		put("water_bucket", Items.WATER_BUCKET);
		// chapter 3: villages and trading
		put("find_village", Items.BELL);
		put("first_trade", Items.EMERALD);
		put("trades", Items.EMERALD);
		put("emeralds", Items.EMERALD_BLOCK);
		put("enchanted_book", Items.ENCHANTED_BOOK);
		put("iron_golem", Items.CARVED_PUMPKIN);
		// chapter 4: the Nether
		put("gold_armor", Items.GOLDEN_HELMET);
		put("enter_nether", Items.OBSIDIAN);
		put("find_fortress", Items.NETHER_BRICKS);
		put("blaze_rods", Items.BLAZE_ROD);
		put("nether_wart", Items.NETHER_WART);
		put("home_safe", Items.GRASS_BLOCK);
		// chapter 5: brewing and enchanting
		put("brewing_stand", Items.BREWING_STAND);
		put("brew_potion", Items.POTION);
		put("bookshelves", Items.BOOKSHELF);
		put("level_30", Items.EXPERIENCE_BOTTLE);
		put("enchant", Items.ENCHANTING_TABLE);
		put("anvil", Items.ANVIL);
		// chapter 6: farming and animals
		put("tame_animal", Items.LEAD);
		put("milk", Items.MILK_BUCKET);
		put("sugar_cane", Items.SUGAR_CANE);
		put("fishing", Items.FISHING_ROD);
		put("honey", Items.HONEY_BOTTLE);
		put("horse", Items.SADDLE);
		// chapter 7: the ocean
		put("boat", Items.OAK_BOAT);
		put("find_wreck", Items.SUSPICIOUS_GRAVEL);
		put("treasure", Items.HEART_OF_THE_SEA);
		put("turtle_helmet", Items.TURTLE_HELMET);
		put("trident", Items.TRIDENT);
		// chapter 8: exploring
		put("map", Items.FILLED_MAP);
		put("spyglass", Items.SPYGLASS);
		put("find_temple", Items.CHISELED_SANDSTONE);
		put("trial_chambers", Items.TRIAL_SPAWNER);
		put("open_vault", Items.TRIAL_KEY);
		// chapter 9: redstone
		put("redstone_dust", Items.REDSTONE);
		put("iron_door", Items.IRON_DOOR);
		put("piston", Items.PISTON);
		put("repeater", Items.REPEATER);
		put("hopper", Items.HOPPER);
		// chapter 10: the End
		put("ender_pearls", Items.ENDER_PEARL);
		put("eyes_of_ender", Items.ENDER_EYE);
		put("find_stronghold", Items.END_PORTAL_FRAME);
		put("enter_end", Items.END_STONE);
		put("dragon", Items.DRAGON_HEAD);
		put("end_city", Items.PURPUR_BLOCK);
		put("elytra", Items.ELYTRA);
		// alerts
		put("creeper", Items.CREEPER_HEAD);
		put("danger", Items.IRON_SWORD);
		put("hungry_eat", Items.COOKED_BEEF);
		put("hungry_hunt", Items.ROTTEN_FLESH);
		put("dusk", Items.CLOCK);
		put("night_outside", Items.CLOCK);
		put("thunder", Items.TRIDENT);
		put("underground", Items.TORCH);
		put("dig_down", Items.LADDER);
		put("lava_near", Items.LAVA_BUCKET);
		put("phantoms", Items.PHANTOM_MEMBRANE);
		put("falling_block", Items.GRAVEL);
		put("ghast", Items.GHAST_TEAR);
		put("piglins", Items.GOLD_INGOT);
		// navigation strip
		put("nav_death", Items.SKELETON_SKULL);
		put("nav_home", Items.COMPASS);
	}

	private TaskIcons() {}

	private static void put(String id, Item item) {
		ICONS.put(id, new ItemStack(item));
	}

	public static ItemStack get(String id) {
		return ICONS.getOrDefault(id, ItemStack.EMPTY);
	}
}
