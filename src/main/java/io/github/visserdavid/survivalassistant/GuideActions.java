package io.github.visserdavid.survivalassistant;

import io.github.visserdavid.survivalassistant.advice.Chapter;
import io.github.visserdavid.survivalassistant.advice.Chapters;
import io.github.visserdavid.survivalassistant.advice.ContextCollector;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import io.github.visserdavid.survivalassistant.entity.ai.FollowOwnerGoal;
import io.github.visserdavid.survivalassistant.state.GuideState;
import io.github.visserdavid.survivalassistant.state.HudState;
import io.github.visserdavid.survivalassistant.util.Ticks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static io.github.visserdavid.survivalassistant.GuideSpeech.say;

/**
 * What the player can ask of the guide: the /guide commands and the settings screen's choices. Each action returns
 * false when it can't be done now (the command then says so).
 */
public final class GuideActions {
	private GuideActions() {}

	/** The guide can only appear for a living player who isn't spectating. */
	private static boolean canHaveGuide(ServerPlayer player) {
		return player.isAlive() && !player.isSpectator();
	}

	/** /guide tip: the most important tip right now (only once the guide has arrived). */
	public static boolean tipNow(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired || !progress.met() || !canHaveGuide(player)) return false;
		GuideEntity guide = null;
		if (npcEnabled(player)) {
			guide = GuideEntities.ensure(player);
			if (guide == null) return false;
		}
		GuideManager.evaluateAndSay(player, guide, progress, true);
		return true;
	}

	/** /guide name: a new name (null = back to the default); the name tag changes at once. */
	public static void rename(ServerPlayer player, @Nullable String name) {
		GuideState.of(player).guideName = name == null ? null : name.strip();
		GuideEntities.refreshName(player);
	}

	/** /guide about: the guide tells the next thing about itself. */
	public static boolean tellAbout(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired || !progress.met() || !canHaveGuide(player)) return false;
		GuideEntity guide = npcEnabled(player) ? GuideEntities.ensure(player) : null;
		say(player, guide, Component.translatable(GuideManager.nextAbout(progress)), false);
		return true;
	}

	public static boolean npcEnabled(ServerPlayer player) {
		return GuideState.of(player).npcEnabled(ModConfig.get().npcEnabledByDefault);
	}

	/** /guide character on|off: stored on the player, takes effect immediately. */
	public static void setNpcEnabled(ServerPlayer player, boolean enabled) {
		GuideProgress progress = GuideState.of(player);
		progress.npcEnabled = enabled;
		if (!enabled) {
			GuideEntities.remove(player.getUUID(), true);
		} else if (!progress.retired && progress.met() && canHaveGuide(player)) {
			GuideEntity guide = GuideEntities.ensure(player);
			if (guide != null) say(player, guide, Component.translatable("survivalassistant.recall"), false);
		}
	}

	/** /guide call: brings the guide back (after a goodbye too, when the server allows that). */
	public static boolean recall(ServerPlayer player) {
		if (!canHaveGuide(player)) return false;
		GuideProgress progress = GuideState.of(player);
		if (progress.retired) {
			if (!ModConfig.get().allowRecall) return false;
			progress.restart();
		} else if (!progress.met()) {
			progress.meet(player.level().getOverworldClockTime());
			progress.restart();
		}
		// Finished everything before new chapters existed (or switched them on)? Then the menu comes up again.
		if (progress.finished && GuideManager.nextChapterAvailable(progress)) progress.finished = false;
		GuideEntity guide = GuideEntities.ensure(player);
		if (guide == null) return false;
		FollowOwnerGoal.teleportNear(guide, player.blockPosition());
		say(player, guide, Component.translatable("survivalassistant.recall"), false);
		HudState.update(player, HudSnapshot.of(ContextCollector.collect(player, progress), progress));
		return true;
	}

	/** /guide dismiss: the guide says goodbye. */
	public static boolean dismiss(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired) return false;
		progress.meet(player.level().getOverworldClockTime());
		GuideManager.retire(player, progress, "dismissed");
		return true;
	}

	/** /guide continue: start the first chapter that is open (after the current one is finished). */
	public static boolean continueChapter(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (!Chapters.get(progress.chapter).done(progress.completed)) return false;
		List<Chapter> open = Chapters.available(progress.chapter, progress.completed);
		return !open.isEmpty() && startChapter(player, open.getFirst());
	}

	/** /guide chapter &lt;id&gt;: start or switch to a chapter that is open. Progress in other chapters is kept. */
	public static boolean startChapter(ServerPlayer player, Chapter chapter) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired || !progress.met() || !ModConfig.get().moreChapters) return false;
		if (Chapters.status(chapter, progress.chapter, progress.completed) != Chapters.Status.AVAILABLE) return false;
		progress.chapter = chapter.number();
		progress.restart();
		say(player, GuideEntities.get(player), Component.translatable("survivalassistant.chapter." + chapter.id() + ".start")
				.withStyle(ChatFormatting.GREEN), true);
		GuideSessions.of(player.getUUID()).evaluateSoon(); // the first tip of the chapter on the next pass
		return true;
	}

	/** /guide chapters: show the chapter menu. */
	public static boolean chapters(ServerPlayer player) {
		GuideProgress progress = GuideState.of(player);
		if (progress.retired || !progress.met()) return false;
		ChapterMenu.show(player, GuideEntities.get(player), progress, "survivalassistant.chapters.header");
		return true;
	}

	/** /guide mute &lt;minutes&gt;: no unsolicited tips for a while (0 ends a mute). */
	public static void mute(ServerPlayer player, int minutes) {
		GuideState.of(player).muteUntil = player.level().getGameTime() + Ticks.minutes(minutes);
	}
}
