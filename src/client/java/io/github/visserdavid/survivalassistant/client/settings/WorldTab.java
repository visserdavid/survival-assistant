package io.github.visserdavid.survivalassistant.client.settings;

import io.github.visserdavid.survivalassistant.advice.GuideNames;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.network.chat.Component;

import static io.github.visserdavid.survivalassistant.client.settings.Rows.tip;
import static io.github.visserdavid.survivalassistant.client.settings.SettingsScreen.KEY;

/**
 * World: the server settings (config/survivalassistant.json). Editable without a world, as host of a singleplayer
 * world or as operator; otherwise shown greyed out.
 */
final class WorldTab {
	private final SettingsScreen screen;
	private final ModConfig w;
	private final boolean editable;

	WorldTab(SettingsScreen screen, ModConfig draft, boolean editable) {
		this.screen = screen;
		this.w = draft;
		this.editable = editable;
	}

	void build(OptionsList list) {
		Rows rows = new Rows(screen, list, editable);
		rows.header("world_guide")
				.toggle("more_chapters", w.moreChapters, v -> w.moreChapters = v)
				.toggle("spawn_on_join", w.spawnOnJoin, v -> w.spawnOnJoin = v)
				.toggle("allow_recall", w.allowRecall, v -> w.allowRecall = v)
				.toggle("npc_default", w.npcEnabledByDefault, v -> w.npcEnabledByDefault = v);
		rows.text("default_name", w.guideName, GuideNames.DEFAULT, GuideNames.MAX_LENGTH,
				v -> v.isBlank() || GuideNames.valid(v), v -> w.guideName = v.isBlank() ? GuideNames.DEFAULT : v.strip());

		rows.header("tips")
				.slider("tip_interval", 1, 10, w.tipIntervalSeconds, WorldTab::seconds, v -> w.tipIntervalSeconds = v)
				// pauses in steps of 10 seconds
				.slider("milestone_cooldown", 0, 60, w.milestoneCooldownSeconds / 10, v -> duration(v * 10), v -> w.milestoneCooldownSeconds = v * 10)
				.slider("urgent_cooldown", 0, 30, w.urgentCooldownSeconds / 10, v -> duration(v * 10), v -> w.urgentCooldownSeconds = v * 10)
				.toggle("depth_notices", w.depthNotices, v -> w.depthNotices = v)
				.toggle("point_at_resources", w.pointAtResources, v -> w.pointAtResources = v);

		rows.header("navigation")
				.toggle("navigation", w.navigation, v -> w.navigation = v)
				// 16 to 256 blocks in steps of 8
				.slider("home_distance", 2, 32, w.homeDistance / 8, v -> blocks(v * 8), v -> w.homeDistance = v * 8);

		rows.header("musings")
				.toggle("musings_default", w.musings, v -> w.musings = v)
				.slider("muse_interval", 1, 30, w.museIntervalSeconds / 60, WorldTab::minutes, v -> w.museIntervalSeconds = v * 60)
				.toggle("musings_danger", w.musingsInDangerZones, v -> w.musingsInDangerZones = v);

		boolean open = screen.advancedOpen();
		rows.header("advanced")
				.wideButton(Button.builder(Component.translatable(KEY + (open ? "advanced.hide" : "advanced.show")), b -> screen.toggleAdvanced())
						.width(310).tooltip(tip("advanced")).build());
		if (open) advanced(rows);
		rows.flush();
	}

	/** Small talk through a local language model: only useful with a model server, so folded away by default. */
	private void advanced(Rows rows) {
		rows.toggle("chat_default", w.chatEnabled, v -> w.chatEnabled = v)
				.toggle("chat_spontaneous", w.chatSpontaneous, v -> w.chatSpontaneous = v);
		rows.text("chat_endpoint", w.chatEndpoint, new ModConfig().chatEndpoint, 256,
				v -> v.isBlank() || v.startsWith("http://") || v.startsWith("https://"),
				v -> w.chatEndpoint = v.isBlank() ? new ModConfig().chatEndpoint : v.strip());
		rows.text("chat_model", w.chatModel, new ModConfig().chatModel, 128, v -> true,
				v -> w.chatModel = v.isBlank() ? new ModConfig().chatModel : v.strip());
		rows.slider("chat_cooldown", 1, 60, w.chatCooldownSeconds / 10, v -> duration(v * 10), v -> w.chatCooldownSeconds = v * 10)
				.slider("chat_hearing", 1, 64, w.chatHearingDistance, WorldTab::blocks, v -> w.chatHearingDistance = v)
				.slider("chat_sight", 1, 64, w.chatSightRadius, WorldTab::blocks, v -> w.chatSightRadius = v)
				// 16 to 512 tokens in steps of 16
				.slider("chat_max_tokens", 1, 32, w.chatMaxTokens / 16, v -> Component.literal(Integer.toString(v * 16)), v -> w.chatMaxTokens = v * 16)
				// 0 = the model server's own setting (-1 in the file), else 0.1 to 2.0
				.slider("chat_temperature", 0, 20, w.chatTemperature < 0 ? 0 : (int) Math.round(w.chatTemperature * 10),
						v -> v == 0 ? Component.translatable(KEY + "chat_temperature.server") : Component.literal(String.format("%.1f", v / 10.0)),
						v -> w.chatTemperature = v == 0 ? -1 : v / 10.0)
				.slider("chat_timeout", 1, 120, w.chatTimeoutSeconds, WorldTab::seconds, v -> w.chatTimeoutSeconds = v)
				.slider("chat_history", 0, 20, w.chatHistoryTurns, v -> Component.literal(Integer.toString(v)), v -> w.chatHistoryTurns = v);
	}

	private static Component seconds(int s) {
		return Component.translatable(KEY + "unit.seconds", s);
	}

	private static Component minutes(int m) {
		return Component.translatable(KEY + "unit.minutes", m);
	}

	private static Component blocks(int b) {
		return Component.translatable(KEY + "unit.blocks", b);
	}

	/** 0 = "off", under a minute in seconds, else minutes and seconds ("2 min 30 s"). */
	private static Component duration(int totalSeconds) {
		if (totalSeconds == 0) return Component.translatable(KEY + "unit.none");
		int m = totalSeconds / 60, s = totalSeconds % 60;
		if (m == 0) return seconds(s);
		if (s == 0) return minutes(m);
		return Component.translatable(KEY + "unit.minutes_seconds", m, s);
	}
}
