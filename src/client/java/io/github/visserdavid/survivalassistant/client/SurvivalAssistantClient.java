package io.github.visserdavid.survivalassistant.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.client.hud.ClientConfig;
import io.github.visserdavid.survivalassistant.client.hud.CrosshairInfo;
import io.github.visserdavid.survivalassistant.client.hud.EnvironmentCard;
import io.github.visserdavid.survivalassistant.client.hud.HudLayout;
import io.github.visserdavid.survivalassistant.client.hud.LightCheck;
import io.github.visserdavid.survivalassistant.client.hud.RecipeOverlay;
import io.github.visserdavid.survivalassistant.client.hud.TaskHud;
import io.github.visserdavid.survivalassistant.client.settings.SettingsAccess;
import io.github.visserdavid.survivalassistant.client.skin.GuideSkins;
import io.github.visserdavid.survivalassistant.client.skin.SkinCommand;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Function;

/** Client entry point: the guide's look, the cards, the recipe card, the light check and the keys. */
public class SurvivalAssistantClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(SurvivalAssistant.id("guide"));

	@Override
	public void onInitializeClient() {
		ClientConfig.load();
		registerGuide();
		CrosshairInfo crosshair = registerHud();
		registerKeys(crosshair);
	}

	/** The guide's renderer and skins, and the rule that it can only be clicked while sneaking. */
	private static void registerGuide() {
		EntityRenderers.register(SurvivalAssistant.GUIDE, GuideEntityRenderer::new);
		// the guide can only be clicked while sneaking, so normal clicks and hits pass through it
		GuideEntity.setClientPickable(guide -> {
			var player = Minecraft.getInstance().player;
			return player != null && player.isShiftKeyDown();
		});
		GuideSkins.init();
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> GuideSkins.reload());
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
			SkinCommand.register(dispatcher);
			SettingsAccess.register(dispatcher);
		});
		SettingsAccess.registerReceiver();
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
			if (client.player != null && GuideSkins.chosenMissing()) {
				client.player.sendSystemMessage(Component.translatable("survivalassistant.skin.fallback", ClientConfig.get().guideSkin)
						.withStyle(ChatFormatting.YELLOW));
			}
		}));
	}

	/** The task card, the environment card under it, the crosshair info and the recipe card. */
	private static CrosshairInfo registerHud() {
		HudLayout layout = new HudLayout();
		CrosshairInfo crosshair = new CrosshairInfo();
		HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, SurvivalAssistant.id("task_card"), new TaskHud(layout));
		HudElementRegistry.attachElementAfter(SurvivalAssistant.id("task_card"), SurvivalAssistant.id("environment_card"),
				new EnvironmentCard(layout));
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, SurvivalAssistant.id("crosshair_info"), crosshair);
		RecipeOverlay.register();
		return crosshair;
	}

	/** J: task card mode, K: environment card, L: light check; the settings key has no default (bind it yourself). */
	private static void registerKeys(CrosshairInfo crosshair) {
		KeyMapping toggleHud = key("toggle_hud", InputConstants.KEY_J);
		KeyMapping toggleEnv = key("toggle_env", InputConstants.KEY_K);
		KeyMapping toggleLight = key("light_check", InputConstants.KEY_L);
		KeyMapping settings = key("settings", InputConstants.UNKNOWN.getValue());

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			crosshair.tick(client);
			while (toggleLight.consumeClick()) LightCheck.toggle(client);
			LightCheck.tick(client);
			while (settings.consumeClick()) SettingsAccess.open(client);
			onPress(toggleHud, client, cfg -> {
				cfg.mode = cfg.mode.next();
				return "survivalassistant.hud.mode." + cfg.mode.name().toLowerCase(Locale.ROOT);
			});
			onPress(toggleEnv, client, cfg -> {
				cfg.showEnvironment = !cfg.showEnvironment;
				return cfg.showEnvironment ? "survivalassistant.hud.env_on" : "survivalassistant.hud.env_off";
			});
		});
	}

	private static KeyMapping key(String name, int defaultKey) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.survivalassistant." + name, defaultKey, CATEGORY));
	}

	/** A toggle key: changes a setting, saves it and shows the new state above the hotbar. */
	private static void onPress(KeyMapping key, Minecraft client, Function<ClientConfig, String> toggle) {
		while (key.consumeClick()) {
			String message = toggle.apply(ClientConfig.get());
			ClientConfig.save();
			if (client.player != null) client.player.sendOverlayMessage(Component.translatable(message));
		}
	}
}
