package io.github.visserdavid.survivalassistant.advice;

import io.github.visserdavid.survivalassistant.config.ModConfig;
import io.github.visserdavid.survivalassistant.util.Ticks;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides what the guide says. Pure logic: no Minecraft, no randomness, no I/O.
 */
public final class AdviceEngine {
	/** One line the guide wants to say. {@code praise} marks a "well done" message for a just-completed milestone. */
	public record Message(Tip tip, boolean urgent, boolean praise) {}

	/** How long the guide waits before repeating a warning or a goal tip (game ticks). */
	public record Settings(long urgentCooldownTicks, long milestoneCooldownTicks) {
		public static Settings from(ModConfig cfg) {
			return new Settings(Ticks.seconds(cfg.urgentCooldownSeconds), Ticks.seconds(cfg.milestoneCooldownSeconds));
		}
	}

	private AdviceEngine() {}

	/**
	 * Updates milestone completion and returns the messages to send now.
	 *
	 * @param forced true when the player explicitly asked (right-click, /guide tip): ignores cooldowns and mute
	 */
	public static List<Message> evaluate(SurvivalContext ctx, GuideProgress progress, long now, Settings settings, boolean forced) {
		List<Message> out = new ArrayList<>();

		// 1. Record newly completed milestones and praise them (praise is never muted: it's rare and motivating).
		for (Tip m : Chapters.get(progress.chapter).goals()) {
			if (!progress.completed.contains(m.id()) && m.done().test(ctx)) {
				progress.completed.add(m.id());
				if (!forced) out.add(new Message(m, false, true));
			}
		}

		boolean muted = !forced && now < progress.muteUntil;

		// 2. Urgent tips / warnings: the highest-priority applicable one that is off cooldown wins.
		for (Tip u : TipRegistry.URGENT) {
			if (u.applies().test(ctx)) {
				if (forced || (!muted && cooledDown(progress, u, now, settings.urgentCooldownTicks()))) {
					progress.lastShown.put(u.id(), now);
					out.add(new Message(u, true, false));
					return out;
				}
				// A dangerous situation is active but on cooldown: don't distract with milestone tips.
				if (!forced && u.kind() == Tip.Kind.URGENT) return out;
			}
		}

		// 3. Next milestone.
		Tip next = nextMilestone(progress);
		if (next != null && (forced || (!muted && (cooledDown(progress, next, now, settings.milestoneCooldownTicks()) || justPraised(out))))) {
			progress.lastShown.put(next.id(), now);
			out.add(new Message(next, false, false));
		}
		return out;
	}

	/** How a message reaches the player. */
	public enum Delivery { CHAT, ACTIONBAR, NONE }

	/**
	 * With the guide character switched off the task card is the main UI: only urgent tips/warnings still go to chat,
	 * praise becomes a short action bar note, and milestone tips are left to the card. Asked-for tips always use chat.
	 */
	public static Delivery delivery(Message m, boolean npcEnabled, boolean forced) {
		if (forced || npcEnabled || m.urgent()) return Delivery.CHAT;
		return m.praise() ? Delivery.ACTIONBAR : Delivery.NONE;
	}

	/** The highest-priority urgent tip/warning that applies right now, ignoring cooldowns; null if none. */
	public static @Nullable Tip activeUrgent(SurvivalContext ctx) {
		for (Tip u : TipRegistry.URGENT) {
			if (u.applies().test(ctx)) return u;
		}
		return null;
	}

	public static @Nullable Tip nextMilestone(GuideProgress progress) {
		for (Tip m : Chapters.get(progress.chapter).goals()) {
			if (!progress.completed.contains(m.id())) return m;
		}
		return null;
	}

	private static boolean justPraised(List<Message> out) {
		return out.stream().anyMatch(Message::praise);
	}

	private static boolean cooledDown(GuideProgress progress, Tip tip, long now, long cooldown) {
		Long last = progress.lastShown.get(tip.id());
		return last == null || now - last >= cooldown;
	}
}
