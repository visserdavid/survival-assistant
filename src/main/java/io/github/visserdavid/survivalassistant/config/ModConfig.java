package io.github.visserdavid.survivalassistant.config;

import com.google.gson.annotations.SerializedName;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.advice.GuideNames;
import net.fabricmc.loader.api.FabricLoader;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

/** Simple JSON config at config/survivalassistant.json. Missing fields fall back to defaults. */
public final class ModConfig {

	/** After chapter 1, offer the next chapters (settling in, then villages, the Nether, brewing and enchanting). */
	@SerializedName(value = "moreChapters", alternate = {"chapter2"})
	public boolean moreChapters = true;
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
	/** The guide's name (players can choose their own with /guide name). */
	public String guideName = GuideNames.DEFAULT;
	/** Spawn a guide automatically for new players. */
	public boolean spawnOnJoin = true;
	/** Let the guide look at nearby trees/stone when giving those tips. */
	public boolean pointAtResources = true;

	// --- the guide's musings (building ideas, animals, scenes; work with or without a language model)
	/** Let the guide muse about the biome and animals around you (players can switch it with /guide musings). */
	public boolean musings = true;
	/** Minimum time between two musings (a building idea for a new biome only waits for the 2-minute quiet time). */
	public int museIntervalSeconds = 300;
	/** Also muse in the Nether, the End, the Deep Dark and unknown places (players can opt in with /guide musings all). */
	public boolean musingsInDangerZones = false;

	// --- small talk through a local language model (llama.cpp llama-server or any OpenAI-compatible server)
	/**
	 * Allow small talk through a local language model on this server. Off by default: it needs a running model server.
	 * Only the server decides; players can switch it off for themselves with /guide chat off.
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
	/**
	 * Higher is more playful, lower is more predictable. -1 (default) uses the server's setting, so the model's
	 * recommended sampling (set when starting llama-server) applies.
	 */
	public double chatTemperature = -1;
	/** Give up on an answer after this many seconds. */
	public int chatTimeoutSeconds = 20;
	/** How many exchanges the guide remembers within a session. */
	public int chatHistoryTurns = 6;

	private static volatile ModConfig instance = new ModConfig();

	public static ModConfig get() {
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(SurvivalAssistant.MOD_ID + ".json");
	}

	/**
	 * Loads the config and writes it back so new options appear in the file. A file that can't be read is left
	 * untouched (the defaults apply until it is fixed), so a typo never wipes someone's settings.
	 */
	public static void load() {
		Path path = path();
		ModConfig loaded;
		try {
			loaded = JsonFiles.read(path, ModConfig.class);
		} catch (JsonFiles.ReadFailure e) {
			SurvivalAssistant.LOGGER.error("Could not read config {}, using defaults until it is fixed", path, e.getCause());
			instance = new ModConfig();
			return;
		}
		update(loaded == null ? new ModConfig() : loaded);
	}

	/** Makes {@code cfg} (validated) the active config and writes it to the file; used by the settings screen. */
	public static void update(ModConfig cfg) {
		cfg.validate();
		instance = cfg;
		try {
			JsonFiles.write(path(), cfg);
		} catch (IOException | RuntimeException e) {
			SurvivalAssistant.LOGGER.error("Could not write config {}", path(), e);
		}
	}

	/** A separate copy to edit (the settings screen changes this, never the active config). */
	public ModConfig copy() {
		return fromJson(toJson());
	}

	public String toJson() {
		return JsonFiles.toJson(this);
	}

	/**
	 * A config from JSON text (sent by the settings screen), validated.
	 *
	 * @throws com.google.gson.JsonParseException the text is not a valid config
	 */
	public static ModConfig fromJson(String json) {
		ModConfig cfg = JsonFiles.fromJson(json, ModConfig.class);
		cfg.validate();
		return cfg;
	}

	/** Brings every value into its valid range; empty or missing texts get their default. */
	void validate() {
		ModConfig d = new ModConfig();
		tipIntervalSeconds = Math.max(1, tipIntervalSeconds);
		milestoneCooldownSeconds = Math.max(0, milestoneCooldownSeconds);
		urgentCooldownSeconds = Math.max(0, urgentCooldownSeconds);
		homeDistance = Math.max(1, homeDistance);
		museIntervalSeconds = Math.max(60, museIntervalSeconds);
		chatCooldownSeconds = Math.clamp(chatCooldownSeconds, 10, 3600);
		chatHearingDistance = Math.clamp(chatHearingDistance, 1, 64);
		chatSightRadius = Math.clamp(chatSightRadius, 1, 64);
		chatMaxTokens = Math.clamp(chatMaxTokens, 1, 1024);
		chatTimeoutSeconds = Math.clamp(chatTimeoutSeconds, 1, 120);
		chatHistoryTurns = Math.clamp(chatHistoryTurns, 0, 20);
		// -1 = the model server's own setting; anything else must be a real number from 0 to 2
		if (!Double.isFinite(chatTemperature) || chatTemperature < 0) chatTemperature = -1;
		else chatTemperature = Math.min(chatTemperature, 2.0);
		if (!GuideNames.valid(guideName)) guideName = d.guideName;
		if (!validEndpoint(chatEndpoint)) chatEndpoint = d.chatEndpoint;
		if (chatModel == null || chatModel.isBlank() || chatModel.length() > MAX_MODEL_LENGTH) chatModel = d.chatModel;
	}

	public static final int MAX_ENDPOINT_LENGTH = 256;
	public static final int MAX_MODEL_LENGTH = 128;

	/** An http(s) address of reasonable length (the server sends player chat there, so nothing else is accepted). */
	public static boolean validEndpoint(@Nullable String url) {
		if (url == null || url.isBlank() || url.length() > MAX_ENDPOINT_LENGTH) return false;
		String lower = url.strip().toLowerCase(java.util.Locale.ROOT);
		return lower.startsWith("http://") || lower.startsWith("https://");
	}
}
