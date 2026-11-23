package io.github.visserdavid.survivalassistant.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config at config/survivalassistant.json. Missing fields fall back to defaults. */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Number of in-game days the guide stays. */
	public int maxDays = 3;
	/** When the period ends the guide asks whether it is still needed; "yes" adds this many days. 0 = never ask. */
	public int extendDays = 3;
	/** After all chapter-1 goals, offer chapter 2 (farm, full armor, diamonds, Nether preparation). */
	public boolean chapter2 = true;
	/** In-game days the guide stays for chapter 2 (can be extended like chapter 1). */
	public int chapter2Days = 4;
	/** How often the guide checks the situation (cooldowns below prevent spam). */
	public int tipIntervalSeconds = 2;
	public int milestoneCooldownSeconds = 180;
	public int urgentCooldownSeconds = 60;
	/** Allow /guide call to bring a retired guide back. */
	public boolean allowRecall = true;
	/** Default for the guide character (players can override with /guide character on|off). */
	public boolean npcEnabledByDefault = true;
	/** Show one-time chat notes when reaching new depths (deepslate, lava level, deep dark, high mountains). */
	public boolean depthNotices = true;
	/** Show the way back to your items after dying, and the way home when far away or at night. */
	public boolean navigation = true;
	/** Show the way home from this distance (blocks); at dusk/night already from 24 blocks. */
	public int homeDistance = 64;
	/** Spawn a guide automatically for new players. */
	public boolean spawnOnJoin = true;
	/** Let the guide look at nearby trees/stone when giving those tips. */
	public boolean pointAtResources = true;

	// --- small talk through a local language model (llama.cpp llama-server or any OpenAI-compatible server)
	/**
	 * Default for small talk through a local language model (players can override it with /guide chat on|off).
	 * Off by default: it needs a running model server. Without one the guide simply stays quiet.
	 */
	public boolean chatEnabled = false;
	/** Chat completions endpoint. llama-server listens on port 8080 by default. */
	public String chatEndpoint = "http://127.0.0.1:8080/v1/chat/completions";
	/** Model name sent along; llama-server ignores it, other servers may need it. */
	public String chatModel = "local";
	/** Let the guide remark on changes by itself (weather, time of day, a new biome, a mob it hasn't seen yet). */
	public boolean chatSpontaneous = true;
	/** Minimum time between two spontaneous remarks. */
	public int chatCooldownSeconds = 120;
	/** The guide reacts to chat from players within this distance (blocks). */
	public int chatHearingDistance = 16;
	/** Mobs within this distance (and in sight) count as "nearby" for the conversation. */
	public int chatSightRadius = 16;
	/** Maximum length of a reply in tokens. */
	public int chatMaxTokens = 80;
	/** Higher is more playful, lower is more predictable. */
	public double chatTemperature = 0.8;
	/** Give up on an answer after this many seconds. */
	public int chatTimeoutSeconds = 20;
	/** How many exchanges the guide remembers within a session. */
	public int chatHistoryTurns = 6;

	private static ModConfig instance = new ModConfig();

	public static ModConfig get() {
		return instance;
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(SurvivalAssistant.MOD_ID + ".json");
		try {
			if (Files.exists(path)) {
				try (Reader r = Files.newBufferedReader(path)) {
					ModConfig loaded = GSON.fromJson(r, ModConfig.class);
					if (loaded != null) instance = loaded;
				}
			}
			// Always write back so new options appear in the file.
			try (Writer w = Files.newBufferedWriter(path)) {
				GSON.toJson(instance, w);
			}
		} catch (IOException | RuntimeException e) {
			SurvivalAssistant.LOGGER.error("Could not load config {}, using defaults", path, e);
			instance = new ModConfig();
		}
	}
}
