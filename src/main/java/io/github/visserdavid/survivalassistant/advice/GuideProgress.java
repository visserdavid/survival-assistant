package io.github.visserdavid.survivalassistant.advice;

import io.github.visserdavid.survivalassistant.util.Ticks;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-player guide state. Persisted (except cooldowns) via a player attachment.
 * Plain Java so the engine tests don't need Minecraft.
 */
public final class GuideProgress {
	/** Overworld clock tick at which the guide first arrived; -1 = never met. Only used as "has met" ({@link #met()}). */
	public long startTick = -1;
	/**
	 * Time played with an active guide, in ticks. Chapter days and the chapter-menu question count in this time, so
	 * they keep working with a frozen daylight cycle, and sleeping or /time set skip nothing.
	 */
	public long playedTicks = 0;
	/** {@link #playedTicks} when the current chapter started (or the guide was last called back); -1 = not yet. */
	public long chapterStartedAt = -1;
	public boolean retired = false;
	public final Set<String> completed = new LinkedHashSet<>();
	/** Player's own choice for the guide character; null = use the server default. */
	public Boolean npcEnabled = null;
	/** Player's own choice for small talk through a local language model; null = on (when the server allows it). */
	public Boolean chatEnabled = null;
	/** Player's choice for the guide's musings: null = on (server default), "off", or "all" (also in danger zones). */
	public @Nullable String musings = null;
	/** Player's own name for the guide; null = the server default (see {@link GuideNames}). */
	public @Nullable String guideName = null;
	/** Next "about me" line the guide tells (/guide about and the occasional musing). */
	public int aboutIndex = 0;
	/** One-time depth/biome notices already shown (e.g. "deepslate", "lava"). */
	public final Set<String> depthNotices = new LinkedHashSet<>();
	/** Fallback home when the player has no bed/respawn point: the first workstation they placed. */
	public Navigation.@Nullable Mark homeFallback = null;
	/** {@link #playedTicks} when the guide offered the next chapter; -1 = no open question. */
	public long askedAtTick = -1;
	/** Current chapter of goals (see {@link Chapters}; 1 = the first days). */
	public int chapter = 1;
	/** The next chapter was offered and the question is open. */
	public boolean chapterOffered = false;
	/** The final congratulation happened; when called back the guide only helps, without new goals. */
	public boolean finished = false;
	/** The chosen route (see {@link Routes}): "" = not asked yet, {@link Routes#FREE}, or a route id. */
	public String route = "";
	/** Routes the player finished (the title was given once for each). */
	public final Set<String> routesDone = new LinkedHashSet<>();

	/** Game tick until which unsolicited tips are muted (saved, so logging out and in doesn't end /guide mute). */
	public long muteUntil = 0;
	/** The reminder for the open question was already given (saved, so it isn't repeated after a reconnect). */
	public boolean reminded = false;

	// --- transient (not saved) ---
	public final Map<String, Long> lastShown = new HashMap<>();
	public boolean comfortPending = false;
	/** Id of the goal step at the last evaluation ("" = none), to notice when the player moves on to the next step. */
	public String lastStep = "";

	/** The guide has met this player. */
	public boolean met() {
		return startTick >= 0;
	}

	/** The first meeting (does nothing when they have met before). */
	public void meet(long overworldClock) {
		if (startTick < 0) startTick = overworldClock;
		if (chapterStartedAt < 0) chapterStartedAt = playedTicks;
	}

	/** Starts the guide again (called back, or a new chapter): the chapter's days count from now. */
	public void restart() {
		this.chapterStartedAt = playedTicks;
		this.retired = false;
		this.askedAtTick = -1;
		this.reminded = false;
		this.chapterOffered = false;
	}

	/** Whole in-game days of played time since the chapter started (0 on the first day). */
	public int daysInChapter() {
		return chapterStartedAt < 0 ? 0 : (int) Math.max(0, (playedTicks - chapterStartedAt) / Ticks.PER_DAY);
	}

	public boolean npcEnabled(boolean serverDefault) {
		return npcEnabled != null ? npcEnabled : serverDefault;
	}

	/** Small talk is on when the server allows it and the player hasn't switched it off. */
	public boolean chatEnabled(boolean serverAllows) {
		return serverAllows && !Boolean.FALSE.equals(chatEnabled);
	}
}
