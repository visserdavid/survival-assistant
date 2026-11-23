package io.github.visserdavid.survivalassistant;

import com.google.gson.JsonParseException;
import io.github.visserdavid.survivalassistant.advice.GuideChoices;
import io.github.visserdavid.survivalassistant.advice.GuideNames;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import io.github.visserdavid.survivalassistant.state.GuideState;
import io.github.visserdavid.survivalassistant.state.SettingsPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Objects;

/** The server side of the settings screen: sends the current settings and applies the changes. */
final class GuideSettings {
	private GuideSettings() {}

	static void register() {
		SettingsPayloads.register();
		ServerPlayNetworking.registerGlobalReceiver(SettingsPayloads.Request.TYPE, (payload, context) -> sync(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(SettingsPayloads.ChoicesUpdate.TYPE,
				(payload, context) -> applyChoices(context.player(), payload.choices()));
		ServerPlayNetworking.registerGlobalReceiver(SettingsPayloads.ConfigUpdate.TYPE,
				(payload, context) -> applyConfig(context.player(), payload.configJson()));
	}

	/** The host of a singleplayer world or an operator (the level of /gamemode) may change the server settings. */
	static boolean canEditConfig(ServerPlayer player) {
		return player.level().getServer().isSingleplayerOwner(player.nameAndId())
				|| player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	private static void sync(ServerPlayer player) {
		ModConfig cfg = ModConfig.get();
		GuideChoices choices = GuideChoices.of(GuideState.of(player), cfg.npcEnabledByDefault, cfg.chatEnabled);
		ServerPlayNetworking.send(player, new SettingsPayloads.Sync(choices, cfg.guideName, cfg.toJson(), canEditConfig(player)));
	}

	/** Applies only what differs from the current choices, with the same effects as the /guide commands. */
	private static void applyChoices(ServerPlayer player, GuideChoices wanted) {
		if (!wanted.nameValid()) {
			player.sendSystemMessage(Component.translatable("survivalassistant.cmd.name_invalid", GuideNames.MAX_LENGTH)
					.withStyle(ChatFormatting.RED));
			return;
		}
		ModConfig cfg = ModConfig.get();
		GuideProgress progress = GuideState.of(player);
		GuideChoices now = GuideChoices.of(progress, cfg.npcEnabledByDefault, cfg.chatEnabled);
		if (!Objects.equals(now.name(), wanted.name())) GuideManager.rename(player, wanted.storedName());
		if (!Objects.equals(now.musings(), wanted.musings())) progress.musings = wanted.storedMusings();
		if (now.chat() != wanted.chat()) progress.chatEnabled = wanted.chat();
		if (now.character() != wanted.character()) GuideManager.setNpcEnabled(player, wanted.character());
	}

	private static void applyConfig(ServerPlayer player, String json) {
		if (!canEditConfig(player)) {
			player.sendSystemMessage(Component.translatable("survivalassistant.settings.denied").withStyle(ChatFormatting.RED));
			return;
		}
		try {
			ModConfig.update(ModConfig.fromJson(json));
			SurvivalAssistant.LOGGER.info("{} changed the Survival Assistant settings", player.getPlainTextName());
		} catch (JsonParseException e) {
			SurvivalAssistant.LOGGER.warn("Ignored invalid settings from {}", player.getPlainTextName(), e);
		}
	}
}
