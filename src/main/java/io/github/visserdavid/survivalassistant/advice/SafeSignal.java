package io.github.visserdavid.survivalassistant.advice;

import io.github.visserdavid.survivalassistant.util.Ticks;
/**
 * The "well lit, you're safe here" signal: in a lit cave or a lit spot outside at night the guide says it's safe
 * instead of warning about the dark. Pure logic.
 */
public final class SafeSignal {
	private SafeSignal() {}

	/** Id of the signal on the task card ({@code survivalassistant.alert.lit_safe}) and in {@code TaskIcons}. */
	public static final String ID = "lit_safe";
	/** The guide says it at most once per 5 minutes (game ticks), so walking in and out of the light isn't spammy. */
	public static final long NOTICE_COOLDOWN_TICKS = Ticks.minutes(5);
	/** {@code lastNotice} value for "never said". */
	public static final long NEVER = -1;

	/**
	 * Show the signal: the dark would matter here, but every spot nearby is lit, no monster is around and nothing
	 * urgent is going on.
	 */
	public static boolean show(SurvivalContext c) {
		return c.nearby().litArea() && c.darkMatters() && c.creatures().hostiles() == 0 && AdviceEngine.activeUrgent(c) == null;
	}

	/** Say it in chat: only when the player has just come into the light, and not again within the cooldown. */
	public static boolean notice(boolean wasSafe, boolean nowSafe, long now, long lastNotice) {
		if (!nowSafe || wasSafe) return false;
		return lastNotice == NEVER || now - lastNotice >= NOTICE_COOLDOWN_TICKS;
	}
}
