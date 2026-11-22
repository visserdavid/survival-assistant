package io.github.visserdavid.survivalassistant.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.chat.Component;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.client.hud.EnvironmentCard;
import io.github.visserdavid.survivalassistant.client.hud.HudConfig;
import io.github.visserdavid.survivalassistant.client.hud.TaskHud;

import java.util.Locale;

public class SurvivalAssistantClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(SurvivalAssistant.id("guide"));
	private static KeyMapping toggleHud;
	private static KeyMapping toggleEnv;

	@Override
	public void onInitializeClient() {
		EntityRenderers.register(SurvivalAssistant.GUIDE, GuideEntityRenderer::new);

		HudConfig.load();
		HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, SurvivalAssistant.id("task_card"), new TaskHud());
		HudElementRegistry.attachElementAfter(SurvivalAssistant.id("task_card"), SurvivalAssistant.id("environment_card"), new EnvironmentCard());

		toggleHud = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.survivalassistant.toggle_hud", InputConstants.KEY_J, CATEGORY));
		toggleEnv = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.survivalassistant.toggle_env", InputConstants.KEY_K, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			HudConfig cfg = HudConfig.get();
			while (toggleHud.consumeClick()) {
				cfg.mode = cfg.mode.next();
				HudConfig.save();
				if (client.player != null) {
					client.player.sendOverlayMessage(Component.translatable(
							"survivalassistant.hud.mode." + cfg.mode.name().toLowerCase(Locale.ROOT)));
				}
			}
			while (toggleEnv.consumeClick()) {
				cfg.showEnvironment = !cfg.showEnvironment;
				HudConfig.save();
				if (client.player != null) {
					client.player.sendOverlayMessage(Component.translatable(
							cfg.showEnvironment ? "survivalassistant.hud.env_on" : "survivalassistant.hud.env_off"));
				}
			}
		});
	}
}
