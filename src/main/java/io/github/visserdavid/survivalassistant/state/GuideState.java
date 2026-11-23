package io.github.visserdavid.survivalassistant.state;

import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;

/** Stores {@link GuideProgress} on the player (saved with the player, kept on death); see {@link GuideProgressCodec}. */
public final class GuideState {
	private GuideState() {}

	public static final AttachmentType<GuideProgress> PROGRESS = AttachmentRegistry.create(SurvivalAssistant.id("progress"),
			builder -> builder.persistent(GuideProgressCodec.CODEC).copyOnDeath().initializer(GuideProgress::new));

	public static GuideProgress of(Player player) {
		return player.getAttachedOrCreate(PROGRESS);
	}

	public static void init() {
		// Forces class loading so the attachment is registered during mod init.
	}
}
