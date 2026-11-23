package io.github.visserdavid.survivalassistant.advice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What the client-side task card shows. Computed on the server (single source of truth) and synced to the player.
 * Plain Java so it can be unit tested.
 *
 * @param active    false when the guide is gone (retired/dismissed): the card hides
 * @param chapter   current chapter of goals (see {@link Chapters})
 * @param completed ids of the completed goals of the current chapter (for the full list and the completion animation)
 * @param next      the current task followed by up to 2 upcoming tasks
 * @param urgentId  id of the urgent tip/warning that applies right now, or "" when all is calm
 * @param waypoint  where the navigation strip points (death spot or home), if anywhere
 * @param safe      the spot is well lit and calm: the card shows the green "safe" line (see {@link SafeSignal})
 */
public record HudSnapshot(boolean active, int dayIndex, int maxDays, int chapter, List<String> completed, List<Entry> next, String urgentId,
		Optional<Navigation.Waypoint> waypoint, boolean safe) {
	public static final int SHOWN_TASKS = 3;
	/** Upper bound for {@link #completed} on the wire: no chapter has more goals than this. */
	public static final int MAX_COMPLETED = 64;
	public static final HudSnapshot INACTIVE = new HudSnapshot(false, 0, 0, 1, List.of(), List.of(), "", Optional.empty(), false);

	/** One task line. {@code target == 0} means the task has no counter. */
	public record Entry(String id, int value, int target) {}

	public static HudSnapshot of(SurvivalContext ctx, GuideProgress progress, int maxDays) {
		return of(ctx, progress, maxDays, Optional.empty());
	}

	public static HudSnapshot of(SurvivalContext ctx, GuideProgress progress, int maxDays, Optional<Navigation.Waypoint> waypoint) {
		List<Entry> next = new ArrayList<>(SHOWN_TASKS);
		List<String> completed = new ArrayList<>();
		for (Tip m : Chapters.get(progress.chapter).goals()) {
			if (progress.completed.contains(m.id())) {
				completed.add(m.id());
				continue;
			}
			if (next.size() >= SHOWN_TASKS) continue;
			int value = m.hasCounter() ? Math.min(m.target(), Math.max(0, m.counter().applyAsInt(ctx))) : 0;
			next.add(new Entry(m.id(), value, m.hasCounter() ? m.target() : 0));
		}
		Tip urgent = AdviceEngine.activeUrgent(ctx);
		return new HudSnapshot(true, ctx.world().dayIndex(), maxDays, progress.chapter, List.copyOf(completed), List.copyOf(next),
				urgent == null ? "" : urgent.id(), waypoint, SafeSignal.show(ctx));
	}
}
