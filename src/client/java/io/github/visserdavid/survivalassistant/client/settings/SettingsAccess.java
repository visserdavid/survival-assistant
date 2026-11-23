package io.github.visserdavid.survivalassistant.client.settings;

import com.mojang.brigadier.CommandDispatcher;
import io.github.visserdavid.survivalassistant.state.SettingsPayloads;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** The ways to open the settings screen without Mod Menu (/guidesettings, the hotkey) and the server's answer. */
public final class SettingsAccess {
	private SettingsAccess() {}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommands.literal("guidesettings").executes(c -> {
			Minecraft mc = c.getSource().getClient();
			// after the chat screen has closed, else it would close the settings again
			mc.schedule(() -> open(mc));
			return 1;
		}));
	}

	public static void registerReceiver() {
		ClientPlayNetworking.registerGlobalReceiver(SettingsPayloads.Sync.TYPE, (payload, context) -> {
			if (context.client().gui.screen() instanceof SettingsScreen screen) screen.receive(payload);
		});
	}

	public static void open(Minecraft mc) {
		mc.gui.setScreen(new SettingsScreen(mc.gui.screen()));
	}
}
