package io.github.visserdavid.survivalassistant.advice;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A player's own guide settings as the settings screen shows them: the values that apply now, with the server
 * defaults filled in. Plain Java so it can be tested without Minecraft.
 *
 * @param character the guide walks along as a character
 * @param name      the player's own name for the guide; empty = the server's name
 * @param musings   {@link #MUSINGS_ON}, {@link #MUSINGS_OFF} or {@link #MUSINGS_ALL}
 * @param chat      small talk through a local language model
 */
public record GuideChoices(boolean character, String name, String musings, boolean chat) {
	public static final String MUSINGS_ON = "on";
	public static final String MUSINGS_OFF = "off";
	public static final String MUSINGS_ALL = "all";
	public static final List<String> MUSINGS = List.of(MUSINGS_OFF, MUSINGS_ON, MUSINGS_ALL);

	public GuideChoices {
		name = name == null ? "" : name.strip();
		if (!MUSINGS.contains(musings)) musings = MUSINGS_ON;
	}

	/** The choices that apply to this player now. */
	public static GuideChoices of(GuideProgress p, boolean npcDefault, boolean chatDefault) {
		String musings = p.musings == null ? MUSINGS_ON : p.musings;
		return new GuideChoices(p.npcEnabled(npcDefault), p.guideName == null ? "" : p.guideName, musings,
				p.chatEnabled(chatDefault));
	}

	/** The value for {@link GuideProgress#musings}: null means "on" (the server default). */
	public @Nullable String storedMusings() {
		return MUSINGS_ON.equals(musings) ? null : musings;
	}

	/** The name to store: null = back to the server's name. */
	public @Nullable String storedName() {
		return name.isEmpty() ? null : name;
	}

	/** The name is empty (= the server's name) or a valid guide name. */
	public boolean nameValid() {
		return name.isEmpty() || GuideNames.valid(name);
	}
}
