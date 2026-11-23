package io.github.visserdavid.survivalassistant.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Musing topics of the guide per biome group: a building idea, a scene and a small story or question, plus remarks
 * about animals in sight. Every topic is a language key ({@code survivalassistant.muse.<group>.<kind>} or
 * {@code survivalassistant.muse.mob.<id>}); the English text doubles as the idea a language model gets to work with.
 * Pure logic, no Minecraft types.
 */
public final class BiomeThemes {
	private BiomeThemes() {}

	public enum Kind { BUILD, SCENE, LORE }

	private static final String PREFIX = "survivalassistant.muse.";

	/** Biome groups (lower-case {@code BiomeGroup} names) with a building idea, a scene and a story. */
	public static final List<String> GROUPS = List.of(
			"plains", "forest", "birch_forest", "dark_forest", "pale_garden", "dappled_forest", "taiga", "snowy",
			"mountains", "meadow", "desert", "badlands", "savanna", "jungle", "swamp", "shore", "ocean", "mushroom",
			"lush_caves", "dripstone", "sulfur_caves");

	/** Underground groups where musing is fine (other underground places stay quiet). */
	public static final Set<String> CAVES = Set.of("lush_caves", "dripstone", "sulfur_caves");

	/** Dangerous or unknown places: only a scene and a story, and only when the player opted in. */
	public static final List<String> DANGER_ZONES = List.of("deep_dark", "nether", "end", "other");

	/** Animals with a remark of their own (entity id paths), checked against the game. */
	public static final List<String> MOBS = List.of(
			"cow", "sheep", "pig", "chicken", "horse", "fox", "wolf", "rabbit", "parrot", "bee", "frog", "turtle",
			"camel", "goat", "axolotl", "llama", "panda");

	public static boolean isDangerZone(String group) {
		return DANGER_ZONES.contains(group);
	}

	public static String key(String group, Kind kind) {
		return PREFIX + group + "." + kind.name().toLowerCase(Locale.ROOT);
	}

	public static String mobKey(String mob) {
		return PREFIX + "mob." + mob;
	}

	/** The building idea for a group, or null when it has none (danger zones). */
	public static String buildKey(String group) {
		return GROUPS.contains(group) ? key(group, Kind.BUILD) : null;
	}

	/** Scene and story keys of a group, in the order they are used; empty for unknown groups. */
	public static List<String> otherKeys(String group) {
		if (!GROUPS.contains(group) && !isDangerZone(group)) return List.of();
		return List.of(key(group, Kind.SCENE), key(group, Kind.LORE));
	}

	/** Every musing key (for tests and the language files). */
	public static List<String> allKeys() {
		List<String> out = new ArrayList<>();
		for (String g : GROUPS) {
			for (Kind k : Kind.values()) out.add(key(g, k));
		}
		for (String g : DANGER_ZONES) out.addAll(otherKeys(g));
		for (String m : MOBS) out.add(mobKey(m));
		return out;
	}
}
