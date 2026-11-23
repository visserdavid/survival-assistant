package io.github.visserdavid.survivalassistant.client.hud;

import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.config.JsonFiles;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
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
	/** Guide skin from config/survivalassistant/skins (file name without .png), or "default". */
	public String guideSkin = "default";

	public enum EnvLines { FULL, COMPACT }

	public static HudConfig get() {
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(SurvivalAssistant.MOD_ID + "-client.json");
	}

	/**
	 * Loads the settings. A file that can't be read is left alone (defaults apply); it is only replaced once the
	 * player changes a setting in the game.
	 */
	public static void load() {
		Path path = path();
		HudConfig loaded;
		boolean broken = false;
		try {
			loaded = JsonFiles.read(path, HudConfig.class);
		} catch (JsonFiles.ReadFailure e) {
			SurvivalAssistant.LOGGER.error("Could not read {}, using defaults", path, e.getCause());
			loaded = null;
			broken = true;
		}
		HudConfig cfg = loaded == null ? new HudConfig() : loaded;
		cfg.validate();
		instance = cfg;
		if (!broken) save();
	}

	private void validate() {
		if (corner == null) corner = Corner.TOP_LEFT;
		if (mode == null) mode = Mode.COMPACT;
		if (envLines == null) envLines = EnvLines.FULL;
		if (guideSkin == null || guideSkin.isBlank()) guideSkin = "default";
		scale = Math.clamp(scale, 0.5f, 2.0f);
	}

	public static void save() {
		try {
			JsonFiles.write(path(), instance);
		} catch (IOException e) {
			SurvivalAssistant.LOGGER.error("Could not write {}", path(), e);
		}
	}
}
