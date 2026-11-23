package io.github.visserdavid.survivalassistant.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.advice.Navigation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** Stores {@link GuideProgress} on the player (saved with the player, kept on death). */
public final class GuideState {
	private GuideState() {}

	private static final Codec<Navigation.Mark> MARK_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("dimension").forGetter(Navigation.Mark::dimension),
			Codec.INT.fieldOf("x").forGetter(Navigation.Mark::x),
			Codec.INT.fieldOf("y").forGetter(Navigation.Mark::y),
			Codec.INT.fieldOf("z").forGetter(Navigation.Mark::z)
	).apply(i, Navigation.Mark::new));

	public static final Codec<GuideProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.optionalFieldOf("start_tick", -1L).forGetter(p -> p.startTick),
			Codec.BOOL.optionalFieldOf("retired", false).forGetter(p -> p.retired),
			Codec.STRING.listOf().optionalFieldOf("completed", List.of()).forGetter(p -> List.copyOf(p.completed)),
			Codec.BOOL.optionalFieldOf("npc_enabled").forGetter(p -> Optional.ofNullable(p.npcEnabled)),
			Codec.STRING.listOf().optionalFieldOf("depth_notices", List.of()).forGetter(p -> List.copyOf(p.depthNotices)),
			MARK_CODEC.optionalFieldOf("home_fallback").forGetter(p -> Optional.ofNullable(p.homeFallback)),
			Codec.INT.optionalFieldOf("extra_days", 0).forGetter(p -> p.extraDays),
			Codec.LONG.optionalFieldOf("asked_at", -1L).forGetter(p -> p.askedAtTick),
			Codec.INT.optionalFieldOf("chapter", 1).forGetter(p -> p.chapter),
			Codec.BOOL.optionalFieldOf("chapter_offered", false).forGetter(p -> p.chapterOffered),
			Codec.BOOL.optionalFieldOf("finished", false).forGetter(p -> p.finished),
			Codec.BOOL.optionalFieldOf("chat_enabled").forGetter(p -> Optional.ofNullable(p.chatEnabled)),
			Codec.STRING.optionalFieldOf("musings").forGetter(p -> Optional.ofNullable(p.musings)),
			Codec.STRING.optionalFieldOf("guide_name").forGetter(p -> Optional.ofNullable(p.guideName)),
			Codec.INT.optionalFieldOf("about_index", 0).forGetter(p -> p.aboutIndex)
	).apply(i, (start, retired, completed, npc, notices, home, extra, asked, chapter, offered, finished, chat, musings, name, about) -> {
		GuideProgress p = new GuideProgress(start, retired, new LinkedHashSet<>(completed), npc.orElse(null), new LinkedHashSet<>(notices));
		p.homeFallback = home.orElse(null);
		p.extraDays = extra;
		p.askedAtTick = asked;
		p.chapter = chapter;
		p.chapterOffered = offered;
		p.finished = finished;
		p.chatEnabled = chat.orElse(null);
		p.musings = musings.orElse(null);
		p.guideName = name.orElse(null);
		p.aboutIndex = about;
		return p;
	}));

	public static final AttachmentType<GuideProgress> PROGRESS = AttachmentRegistry.create(SurvivalAssistant.id("progress"),
			builder -> builder.persistent(CODEC).copyOnDeath().initializer(GuideProgress::new));

	public static GuideProgress of(Player player) {
		return player.getAttachedOrCreate(PROGRESS);
	}

	public static void init() {
		// Forces class loading so the attachment is registered during mod init.
	}
}
