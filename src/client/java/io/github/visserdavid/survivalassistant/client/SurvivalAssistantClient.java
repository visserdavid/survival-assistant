package io.github.visserdavid.survivalassistant.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.chat.Component;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import io.github.visserdavid.survivalassistant.client.hud.CrosshairInfo;
import io.github.visserdavid.survivalassistant.client.hud.EnvironmentCard;
import io.github.visserdavid.survivalassistant.client.hud.HudConfig;
import io.github.visserdavid.survivalassistant.client.hud.LightCheck;
import io.github.visserdavid.survivalassistant.client.hud.RecipeOverlay;
import io.github.visserdavid.survivalassistant.client.hud.TaskHud;
import io.github.visserdavid.survivalassistant.client.skin.GuideSkins;
import io.github.visserdavid.survivalassistant.client.skin.SkinCommand;

import java.util.Locale;

public class SurvivalAssistantClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(SurvivalAssistant.id("guide"));
	private static KeyMapping toggleHud;
	private static KeyMapping toggleEnv;
	private static KeyMapping toggleLight;

	@Override
	public void onInitializeClient() {
		EntityRenderers.register(SurvivalAssistant.GUIDE, GuideEntityRenderer::new);

		HudConfig.load();
		// the guide can only be clicked while sneaking, so normal clicks and hits pass through it
		GuideEntity.clientPickable = guide -> {
			var player = net.minecraft.client.Minecraft.getInstance().player;
			return player != null && player.isShiftKeyDown();
		};
		GuideSkins.init();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> SkinCommand.register(dispatcher));
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
			if (client.player != null && GuideSkins.chosenMissing()) {
				client.player.sendSystemMessage(Component.translatable("survivalassistant.skin.fallback", HudConfig.get().guideSkin)
						.withStyle(ChatFormatting.YELLOW));
			}
		}));
		HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, SurvivalAssistant.id("task_card"), new TaskHud());
		HudElementRegistry.attachElementAfter(SurvivalAssistant.id("task_card"), SurvivalAssistant.id("environment_card"), new EnvironmentCard());
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, SurvivalAssistant.id("crosshair_info"), new CrosshairInfo());
		RecipeOverlay.register();

		toggleHud = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.survivalassistant.toggle_hud", InputConstants.KEY_J, CATEGORY));
		toggleEnv = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.survivalassistant.toggle_env", InputConstants.KEY_K, CATEGORY));
		toggleLight = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.survivalassistant.light_check", InputConstants.KEY_L, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			CrosshairInfo.tick(client);
			while (toggleLight.consumeClick()) LightCheck.toggle(client);
			LightCheck.tick(client);
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
