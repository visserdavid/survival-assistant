package io.github.visserdavid.survivalassistant.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** Stores {@link GuideProgress} on the player (saved with the player, kept on death). */
public final class GuideState {
	private GuideState() {}

	public static final Codec<GuideProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.optionalFieldOf("start_tick", -1L).forGetter(p -> p.startTick),
			Codec.BOOL.optionalFieldOf("retired", false).forGetter(p -> p.retired),
			Codec.STRING.listOf().optionalFieldOf("completed", List.of()).forGetter(p -> List.copyOf(p.completed)),
			Codec.BOOL.optionalFieldOf("npc_enabled").forGetter(p -> Optional.ofNullable(p.npcEnabled)),
			Codec.STRING.listOf().optionalFieldOf("depth_notices", List.of()).forGetter(p -> List.copyOf(p.depthNotices))
	).apply(i, (start, retired, completed, npc, notices) ->
			new GuideProgress(start, retired, new LinkedHashSet<>(completed), npc.orElse(null), new LinkedHashSet<>(notices))));

	public static final AttachmentType<GuideProgress> PROGRESS = AttachmentRegistry.create(SurvivalAssistant.id("progress"),
			builder -> builder.persistent(CODEC).copyOnDeath().initializer(GuideProgress::new));

	public static GuideProgress of(Player player) {
		return player.getAttachedOrCreate(PROGRESS);
	}

	public static void init() {
		// Forces class loading so the attachment is registered during mod init.
	}
}
