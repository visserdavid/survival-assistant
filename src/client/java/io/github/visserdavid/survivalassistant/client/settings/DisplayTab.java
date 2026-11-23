package io.github.visserdavid.survivalassistant.client.settings;

import com.mojang.blaze3d.Blaze3D;
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
				.slider("margin", 0, 32, c.margin, v -> Component.literal(Integer.toString(v)), v -> c.margin = v);

		rows.header("environment")
				.toggle("show_environment", c.showEnvironment, v -> c.showEnvironment = v)
				.cycle("env_lines", List.of(ClientConfig.EnvLines.values()), c.envLines, m -> name("env_lines", m), v -> c.envLines = v);

		rows.header("helpers")
				.toggle("recipe_card", c.recipeCard, v -> c.recipeCard = v)
				.toggle("crosshair_info", c.crosshairInfo, v -> c.crosshairInfo = v);

		List<String> skins = new ArrayList<>();
		skins.add(SkinNames.DEFAULT);
		skins.addAll(GuideSkins.names());
		if (!skins.contains(c.guideSkin)) skins.add(c.guideSkin);
		rows.header("look")
				.cycle("skin", skins, c.guideSkin,
						s -> s.equals(SkinNames.DEFAULT) ? Component.translatable(KEY + "skin.default") : Component.literal(s),
						v -> c.guideSkin = v)
				.button(Button.builder(label("skin_folder"), b -> Blaze3D.openPath(GuideSkins.folder()))
						.width(150).tooltip(tip("skin_folder")).build());

		rows.header("keys")
				.wideButton(Button.builder(label("keys"),
								b -> Minecraft.getInstance().gui.setScreen(new KeyBindsScreen(screen, Minecraft.getInstance().options)))
						.width(310).tooltip(tip("keys")).build());
		rows.flush();
	}

	/** The name of an enum value: survivalassistant.settings.&lt;id&gt;.&lt;value&gt;. */
	private static Component name(String id, Enum<?> value) {
		return Component.translatable(KEY + id + "." + value.name().toLowerCase(Locale.ROOT));
	}
}
