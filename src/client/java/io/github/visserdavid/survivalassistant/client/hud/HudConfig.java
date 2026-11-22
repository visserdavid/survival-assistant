package io.github.visserdavid.survivalassistant.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Client-only settings for the task card: config/survivalassistant-client.json. */
public final class HudConfig {
	public enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

	public enum Mode {
		COMPACT, FULL, HIDDEN;

		public Mode next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static HudConfig instance = new HudConfig();

	/** Screen corner of the task card. */
	public Corner corner = Corner.TOP_LEFT;
	/** Remembered display mode (toggled with the hotkey, default J). */
	public Mode mode = Mode.COMPACT;
	/** Extra scale on top of the GUI scale (0.5 - 2.0). */
	public float scale = 1.0f;
	/** Distance from the screen edge in GUI pixels. */
	public int margin = 4;
	/** Show the environment card (biome + height advice); toggled with K. */
	public boolean showEnvironment = true;
	/** FULL = biome pros/cons, ores, pointer and hazard; COMPACT = header and ores only. */
	public EnvLines envLines = EnvLines.FULL;
	/** Recipe card for the current task next to the inventory / crafting table. */
	public boolean recipeCard = true;
	/** Tip under the crosshair when looking at a mob or an ore. */
	public boolean crosshairInfo = true;

	public enum EnvLines { FULL, COMPACT }

	public static HudConfig get() {
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(SurvivalAssistant.MOD_ID + "-client.json");
	}

	public static void load() {
		Path path = path();
		try {
			if (Files.exists(path)) {
				try (Reader r = Files.newBufferedReader(path)) {
					HudConfig loaded = GSON.fromJson(r, HudConfig.class);
					if (loaded != null) instance = loaded;
				}
			}
		} catch (IOException | RuntimeException e) {
			SurvivalAssistant.LOGGER.error("Could not read {}, using defaults", path, e);
			instance = new HudConfig();
		}
		if (instance.corner == null) instance.corner = Corner.TOP_LEFT;
		if (instance.mode == null) instance.mode = Mode.COMPACT;
		if (instance.envLines == null) instance.envLines = EnvLines.FULL;
		instance.scale = Math.max(0.5f, Math.min(2.0f, instance.scale));
		save();
	}

	public static void save() {
		try (Writer w = Files.newBufferedWriter(path())) {
			GSON.toJson(instance, w);
		} catch (IOException e) {
			SurvivalAssistant.LOGGER.error("Could not write {}", path(), e);
		}
	}
}
