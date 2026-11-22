package io.github.visserdavid.survivalassistant.advice;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-player guide state. Persisted (except cooldowns) via a player attachment.
 * Plain Java so the engine tests don't need Minecraft.
 */
public final class GuideProgress {
	/** Overworld clock tick at which the guide first arrived; -1 = never. */
	public long startTick = -1;
	public boolean retired = false;
	public final Set<String> completed = new LinkedHashSet<>();
	/** Player's own choice for the guide character; null = use the server default. */
	public Boolean npcEnabled = null;
	/** One-time depth/biome notices already shown (e.g. "deepslate", "lava"). */
	public final Set<String> depthNotices = new LinkedHashSet<>();
	/** Fallback home when the player has no bed/respawn point: the first workstation they placed. */
	public Navigation.Mark homeFallback = null;
	/** Days added because the player said they still needed the guide. */
	public int extraDays = 0;
	/** Overworld clock tick when the guide asked "do you still need me?"; -1 = no open question. */
	public long askedAtTick = -1;
	/** Current chapter of goals (1 = first days, 2 = settling in). */
	public int chapter = 1;
	/** The next chapter was offered and the question is open. */
	public boolean chapterOffered = false;
	/** The final congratulation happened; when called back the guide only helps, without new goals. */
	public boolean finished = false;

	// --- transient (not saved) ---
	/** Game tick until which unsolicited tips are muted. */
	public long muteUntil = 0;
	public final Map<String, Long> lastShown = new HashMap<>();
	public boolean comfortPending = false;
	/** The reminder for the open question was already given. */
	public boolean reminded = false;

	/** Resets the guide period (used when the guide is called back). */
	public void restart(long startTick) {
		this.startTick = startTick;
		this.retired = false;
		this.extraDays = 0;
		this.askedAtTick = -1;
		this.reminded = false;
		this.chapterOffered = false;
	}

	public GuideProgress() {}

	public GuideProgress(long startTick, boolean retired, Set<String> completed) {
		this.startTick = startTick;
		this.retired = retired;
		this.completed.addAll(completed);
	}

	public GuideProgress(long startTick, boolean retired, Set<String> completed, Boolean npcEnabled, Set<String> depthNotices) {
		this(startTick, retired, completed);
		this.npcEnabled = npcEnabled;
		this.depthNotices.addAll(depthNotices);
	}

	public boolean npcEnabled(boolean serverDefault) {
		return npcEnabled != null ? npcEnabled : serverDefault;
	}
}
