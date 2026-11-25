package io.github.visserdavid.survivalassistant.client.settings;

import net.minecraft.util.Util;
import io.github.visserdavid.survivalassistant.client.hud.ClientConfig;
import io.github.visserdavid.survivalassistant.client.skin.GuideSkins;
import io.github.visserdavid.survivalassistant.skin.SkinNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.github.visserdavid.survivalassistant.client.settings.Rows.label;
import static io.github.visserdavid.survivalassistant.client.settings.Rows.tip;
import static io.github.visserdavid.survivalassistant.client.settings.SettingsScreen.KEY;

/** Display: the cards on screen and the guide's look. Stored on this computer only. */
final class DisplayTab {
	private final SettingsScreen screen;
	private final ClientConfig c;

	DisplayTab(SettingsScreen screen, ClientConfig draft) {
		this.screen = screen;
		this.c = draft;
	}

	void build(OptionsList list) {
		Rows rows = new Rows(screen, list, true);
		rows.header("task_card")
				.cycle("mode", List.of(ClientConfig.Mode.values()), c.mode, m -> name("mode", m), v -> c.mode = v)
				.cycle("corner", List.of(ClientConfig.Corner.values()), c.corner, m -> name("corner", m), v -> c.corner = v)
				// 50 to 200 % in steps of 5
				.slider("scale", 10, 40, Math.round(c.scale * 20), v -> Component.literal(v * 5 + "%"), v -> c.scale = v / 20f)
				.slider("margin", 0, 32, c.margin, v -> Component.literal(Integer.toString(v)), v -> c.margin = v)
				.toggle("show_steps", c.showSteps, v -> c.showSteps = v);

		rows.header("environment")
				.toggle("show_environment", c.showEnvironment, v -> c.showEnvironment = v)
				.cycle("env_lines", List.of(ClientConfig.EnvLines.values()), c.envLines, m -> name("env_lines", m), v -> c.envLines = v);

		rows.header("helpers")
				.toggle("recipe_card", c.recipeCard, v -> c.recipeCard = v)
				.toggle("crosshair_info", c.crosshairInfo, v -> c.crosshairInfo = v)
				.cycle("crosshair_place", List.of(ClientConfig.Place.values()), c.crosshairPlace, m -> name("crosshair_place", m),
						v -> c.crosshairPlace = v)
				.slider("crosshair_range", 3, 32, c.crosshairRange, Rows::blocks, v -> c.crosshairRange = v);

		List<String> skins = new ArrayList<>();
		skins.add(SkinNames.DEFAULT);
		skins.addAll(GuideSkins.names());
		if (!skins.contains(c.guideSkin)) skins.add(c.guideSkin);
		rows.header("look")
				.cycle("skin", skins, c.guideSkin,
						s -> s.equals(SkinNames.DEFAULT) ? Component.translatable(KEY + "skin.default") : Component.literal(shortName(s)),
						v -> c.guideSkin = v)
				.button(Button.builder(label("skin_folder"), b -> Util.getPlatform().openPath(GuideSkins.folder()))
						.width(Rows.WIDGET_WIDTH).tooltip(tip("skin_folder")).build());

		rows.header("keys")
				.wideButton(Button.builder(label("keys"),
								b -> Minecraft.getInstance().gui.setScreen(new KeyBindsScreen(screen, Minecraft.getInstance().options)))
						.width(Rows.WIDE).tooltip(tip("keys")).build());
		rows.flush();
	}

	/** The name of an enum value: survivalassistant.settings.&lt;id&gt;.&lt;value&gt;. */
	private static Component name(String id, Enum<?> value) {
		return Component.translatable(KEY + id + "." + value.name().toLowerCase(Locale.ROOT));
	}

	/** Skin file names can be long ("cool_explorer_on_some_website_com"): keep the button readable. */
	private static String shortName(String name) {
		return name.length() <= MAX_SKIN_NAME ? name : name.substring(0, MAX_SKIN_NAME - 1) + "…";
	}

	private static final int MAX_SKIN_NAME = 16;
}
