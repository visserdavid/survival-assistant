package io.github.visserdavid.survivalassistant.client.settings;

import io.github.visserdavid.survivalassistant.advice.GuideChoices;
import io.github.visserdavid.survivalassistant.advice.GuideNames;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import static io.github.visserdavid.survivalassistant.client.settings.SettingsScreen.KEY;

/** My guide: the player's own choices, stored with the player on the server (also works on multiplayer servers). */
final class GuideTab {
	private final SettingsScreen screen;
	private final SettingsScreen.ChoicesDraft d;
	private final String serverName;
	private final boolean chatAllowed;

	GuideTab(SettingsScreen screen, SettingsScreen.ChoicesDraft draft, String serverName, boolean chatAllowed) {
		this.screen = screen;
		this.d = draft;
		this.serverName = serverName;
		this.chatAllowed = chatAllowed;
	}

	void build(OptionsList list) {
		Rows rows = new Rows(screen, list, true);
		rows.header("my_guide")
				.toggle("character", d.character, v -> d.character = v)
				.cycle("musings", GuideChoices.MUSINGS, d.musings, GuideTab::musingsName, v -> d.musings = v);
		rows.text("name", d.name, serverName, GuideNames.MAX_LENGTH,
				v -> v.isBlank() || GuideNames.valid(v), v -> d.name = v.strip());

		rows.header("small_talk")
				.toggle("chat", d.chat, v -> d.chat = v);
		// only the server can allow small talk; then each player can switch it off for themselves
		if (!chatAllowed) rows.lockLast(Component.translatable(KEY + "chat.not_allowed"));
		rows.flush();
	}

	/** On and off look like every other switch; "everywhere" is the third choice. */
	private static Component musingsName(String value) {
		return switch (value) {
			case GuideChoices.MUSINGS_ON -> CommonComponents.OPTION_ON;
			case GuideChoices.MUSINGS_OFF -> CommonComponents.OPTION_OFF;
			default -> Component.translatable(KEY + "musings.all");
		};
	}
}
