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

	// --- transient (not saved) ---
	/** Game tick until which unsolicited tips are muted. */
	public long muteUntil = 0;
	public final Map<String, Long> lastShown = new HashMap<>();
	public boolean comfortPending = false;

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
