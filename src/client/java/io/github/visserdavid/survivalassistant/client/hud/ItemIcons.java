package io.github.visserdavid.survivalassistant.client.hud;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * One shared ItemStack per item for drawing icons, so the cards don't create stacks every frame. The stacks are
 * only drawn, never changed.
 */
public final class ItemIcons {
	private static final Map<Item, ItemStack> BY_ITEM = new HashMap<>();
	private static final Map<String, ItemStack> BY_ID = new HashMap<>();

	private ItemIcons() {}

	public static ItemStack of(Item item) {
		return BY_ITEM.computeIfAbsent(item, ItemStack::new);
	}

	/** The icon for an item id such as {@code minecraft:torch}; an unknown id gives the air item (nothing drawn). */
	public static ItemStack of(String id) {
		return BY_ID.computeIfAbsent(id, k -> of(BuiltInRegistries.ITEM.getValue(Identifier.parse(k))));
	}
}
