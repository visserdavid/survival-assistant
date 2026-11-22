package io.github.visserdavid.survivalassistant.advice;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * A single piece of advice.
 *
 * @param id       stable identifier, also used in translation keys: {@code survivalassistant.tip.<id>.<n>}
 * @param kind     URGENT interrupts and holds back milestone tips while active; WARNING is shown with the same
 *                 emphasis but never holds anything back; MILESTONE forms the progression path
 * @param applies  when should this tip be considered (urgent tips only; milestones apply until done)
 * @param done     milestone completion check (ignored for urgent tips)
 * @param variants number of text variants in the language files
 * @param counter  optional live progress value for the HUD (e.g. logs collected); null = no counter
 * @param target   value at which the counter is "full"; 0 = no counter
 */
public record Tip(String id, Kind kind, Predicate<SurvivalContext> applies, Predicate<SurvivalContext> done, int variants,
		ToIntFunction<SurvivalContext> counter, int target) {
	public enum Kind { URGENT, WARNING, MILESTONE }

	public static Tip urgent(String id, int variants, Predicate<SurvivalContext> applies) {
		return new Tip(id, Kind.URGENT, applies, c -> false, variants, null, 0);
	}

	public static Tip warning(String id, int variants, Predicate<SurvivalContext> applies) {
		return new Tip(id, Kind.WARNING, applies, c -> false, variants, null, 0);
	}

	public static Tip milestone(String id, int variants, Predicate<SurvivalContext> done) {
		return new Tip(id, Kind.MILESTONE, c -> true, done, variants, null, 0);
	}

	/** Milestone with a live counter shown in the HUD, e.g. "logs 6/10". */
	public static Tip milestone(String id, int variants, Predicate<SurvivalContext> done, ToIntFunction<SurvivalContext> counter, int target) {
		return new Tip(id, Kind.MILESTONE, c -> true, done, variants, counter, target);
	}

	public boolean hasCounter() {
		return counter != null && target > 0;
	}

	public String translationKey(int variant) {
		return "survivalassistant.tip." + id + "." + variant;
	}
}
