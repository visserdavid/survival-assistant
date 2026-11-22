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
		put("iron", Items.RAW_IRON);
		put("shield", Items.SHIELD);
		put("iron_pickaxe", Items.IRON_PICKAXE);
		put("iron_armor", Items.IRON_CHESTPLATE);
		put("water_bucket", Items.WATER_BUCKET);
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
