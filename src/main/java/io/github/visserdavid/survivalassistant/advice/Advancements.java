package io.github.visserdavid.survivalassistant.advice;

import java.util.Set;

/** Vanilla advancements used as goals (ids checked in the 26.3 data: data/minecraft/advancement). */
public final class Advancements {
	private Advancements() {}

	public static final String TRADE = "adventure/trade";
	public static final String IRON_GOLEM = "adventure/summon_iron_golem";
	public static final String ENTER_NETHER = "story/enter_the_nether";
	public static final String FORTRESS = "nether/find_fortress";
	public static final String BREW = "nether/brew_potion";
	public static final String ENCHANT = "story/enchant_item";
	public static final String TAME = "husbandry/tame_an_animal";
	public static final String FISH = "husbandry/fishy_business";
	public static final String HONEY = "husbandry/safely_harvest_honey";
	public static final String TRIDENT = "adventure/throw_trident";
	public static final String TRIAL_CHAMBERS = "adventure/minecraft_trials_edition";
	public static final String VAULT = "adventure/under_lock_and_key";
	public static final String STRONGHOLD = "story/follow_ender_eye";
	public static final String ENTER_END = "story/enter_the_end";
	public static final String DRAGON = "end/kill_dragon";
	public static final String END_CITY = "end/find_end_city";
	public static final String ELYTRA = "end/elytra";
	public static final String DEBRIS = "nether/obtain_ancient_debris";
	public static final String BASTION = "nether/find_bastion";
	public static final String ANCHOR = "nether/charge_respawn_anchor";
	public static final String SUMMON_WITHER = "nether/summon_wither";
	public static final String BEACON = "nether/create_beacon";
	public static final String FULL_BEACON = "nether/create_full_beacon";
	public static final String SNEAK = "adventure/avoid_vibration";
	public static final String RAID_CAPTAIN = "adventure/voluntary_exile";
	public static final String RAID = "adventure/hero_of_the_village";
	public static final String TOTEM = "adventure/totem_of_undying";
	public static final String SHERD = "adventure/salvage_sherd";
	public static final String TRIM = "adventure/trim_with_any_armor_pattern";
	public static final String SNIFFER_EGG = "husbandry/obtain_sniffer_egg";
	public static final String OMINOUS_VAULT = "adventure/revaulting";
	public static final String DRIED_GHAST = "husbandry/place_dried_ghast_in_water";
	public static final String CRAFTER = "adventure/crafters_crafting_crafters";

	/** Every advancement above: the ones the collector checks for each player. */
	public static final Set<String> ALL = Set.of(
			TRADE, IRON_GOLEM, ENTER_NETHER, FORTRESS, BREW, ENCHANT, TAME, FISH, HONEY, TRIDENT, TRIAL_CHAMBERS,
			VAULT, STRONGHOLD, ENTER_END, DRAGON, END_CITY, ELYTRA, DEBRIS, BASTION, ANCHOR,
			SUMMON_WITHER, BEACON, FULL_BEACON, SNEAK, RAID_CAPTAIN, RAID, TOTEM, SHERD, TRIM, SNIFFER_EGG,
			OMINOUS_VAULT, DRIED_GHAST, CRAFTER);
}
