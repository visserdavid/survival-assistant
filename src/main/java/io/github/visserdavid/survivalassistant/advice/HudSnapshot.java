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
 * @param step      the current step of the current task (see {@link Steps}); id "" when it has none
 * @param urgentId  id of the urgent tip/warning that applies right now, or "" when all is calm
 * @param waypoint  where the navigation strip points (death spot or home), if anywhere
 * @param safe      the spot is well lit and calm: the card shows the green "safe" line (see {@link SafeSignal})
 */
public record HudSnapshot(boolean active, int chapter, List<String> completed, List<Entry> next, Entry step, String urgentId,
		Optional<Navigation.Waypoint> waypoint, boolean safe) {
	public static final int SHOWN_TASKS = 3;
	/** Upper bound for {@link #completed} on the wire: no chapter has more goals than this. */
	public static final int MAX_COMPLETED = 64;
	/** No step. */
	public static final Entry NO_STEP = new Entry("", 0, 0);
	public static final HudSnapshot INACTIVE = new HudSnapshot(false, 1, List.of(), List.of(), NO_STEP, "", Optional.empty(), false);

	/** One task (or step) line. {@code target == 0} means it has no counter. */
	public record Entry(String id, int value, int target) {}

	public static HudSnapshot of(SurvivalContext ctx, GuideProgress progress) {
		return of(ctx, progress, Optional.empty());
	}

	public static HudSnapshot of(SurvivalContext ctx, GuideProgress progress, Optional<Navigation.Waypoint> waypoint) {
		List<Entry> next = new ArrayList<>(SHOWN_TASKS);
		List<String> completed = new ArrayList<>();
		Entry step = NO_STEP;
		for (Tip m : Chapters.get(progress.chapter).goals()) {
			if (progress.completed.contains(m.id())) {
				completed.add(m.id());
				continue;
			}
			if (next.size() >= SHOWN_TASKS) continue;
			int value = m.hasCounter() ? Math.min(m.target(), Math.max(0, m.counter().applyAsInt(ctx))) : 0;
			if (next.isEmpty()) {
				Steps.Step s = Steps.current(m, ctx);
				if (s != null) step = new Entry(s.id(), s.value(ctx), s.hasCounter() ? s.target() : 0);
			}
			next.add(new Entry(m.id(), value, m.hasCounter() ? m.target() : 0));
		}
		Tip urgent = AdviceEngine.activeUrgent(ctx);
		return new HudSnapshot(true, progress.chapter, List.copyOf(completed), List.copyOf(next), step,
				urgent == null ? "" : urgent.id(), waypoint, SafeSignal.show(ctx));
	}
}
