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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** The server side of the settings screen: sends the current settings and applies the changes. */
final class GuideSettings {
	private GuideSettings() {}

	/** A player can send at most one change per this many server ticks (1 second); more is ignored. */
	private static final int MIN_TICKS_BETWEEN_UPDATES = 20;
	/** Server tick of each player's last accepted change. */
	private static final Map<UUID, Integer> LAST_UPDATE = new HashMap<>();

	static void register() {
		SettingsPayloads.register();
		ServerPlayNetworking.registerGlobalReceiver(SettingsPayloads.Request.TYPE, (payload, context) -> sync(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(SettingsPayloads.ChoicesUpdate.TYPE, (payload, context) -> {
			if (allowUpdate(context.player())) applyChoices(context.player(), payload.choices());
		});
		ServerPlayNetworking.registerGlobalReceiver(SettingsPayloads.ConfigUpdate.TYPE, (payload, context) -> {
			if (allowUpdate(context.player())) applyConfig(context.player(), payload.configJson());
		});
	}

	static void forget(UUID player) {
		LAST_UPDATE.remove(player);
	}

	static void clear() {
		LAST_UPDATE.clear();
	}

	/** At most one change per second per player, so a modified client can't flood the server. */
	private static boolean allowUpdate(ServerPlayer player) {
		int now = player.level().getServer().getTickCount();
		Integer last = LAST_UPDATE.get(player.getUUID());
		if (last != null && now - last < MIN_TICKS_BETWEEN_UPDATES) return false;
		LAST_UPDATE.put(player.getUUID(), now);
		return true;
	}

	/**
	 * The host of a singleplayer world or an admin operator may change the server settings. The settings include the
	 * address the server sends player chat to, so the lower operator levels may not.
	 */
	static boolean canEditConfig(ServerPlayer player) {
		return player.level().getServer().isSingleplayerOwner(player.nameAndId())
				|| player.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
	}

	/** Players who may not change the server settings don't get to see them either (they include the chat address). */
	private static void sync(ServerPlayer player) {
		ModConfig cfg = ModConfig.get();
		GuideChoices choices = GuideChoices.of(GuideState.of(player), cfg.npcEnabledByDefault, cfg.chatEnabled);
		boolean canEdit = canEditConfig(player);
		ServerPlayNetworking.send(player, new SettingsPayloads.Sync(choices, cfg.guideName, canEdit ? cfg.toJson() : "",
				canEdit, cfg.chatEnabled));
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
		if (!Objects.equals(now.name(), wanted.name())) GuideActions.rename(player, wanted.storedName());
		if (!Objects.equals(now.musings(), wanted.musings())) progress.musings = wanted.storedMusings();
		// small talk can only be switched on when the server allows it
		if (now.chat() != wanted.chat() && (cfg.chatEnabled || !wanted.chat())) progress.chatEnabled = wanted.chat();
		if (now.character() != wanted.character()) GuideActions.setNpcEnabled(player, wanted.character());
		// a new route works like /guide route (a finished chapter goes on with it); without a guide it is only stored
		if (!now.route().equals(wanted.route()) && !GuideActions.chooseRoute(player, wanted.route())) {
			progress.route = wanted.route();
		}
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
