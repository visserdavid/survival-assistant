package io.github.visserdavid.survivalassistant.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.visserdavid.survivalassistant.advice.GuideProgress;
import io.github.visserdavid.survivalassistant.advice.Navigation;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * How {@link GuideProgress} is saved with the player. The fields are read in three groups, but all keys stay flat
 * in one map, so the saved data looks exactly like it always did. Never rename a key: old worlds use them.
 * Keys that are no longer used (such as {@code extra_days} from the old day limit) are simply ignored.
 */
final class GuideProgressCodec {
	private GuideProgressCodec() {}

	private static final Codec<Navigation.Mark> MARK = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("dimension").forGetter(Navigation.Mark::dimension),
			Codec.INT.fieldOf("x").forGetter(Navigation.Mark::x),
			Codec.INT.fieldOf("y").forGetter(Navigation.Mark::y),
			Codec.INT.fieldOf("z").forGetter(Navigation.Mark::z)
	).apply(i, Navigation.Mark::new));

	/**
	 * Where the player is in the tutorial. {@code asked_at} is the old overworld-clock version of {@code asked_played}:
	 * still read (an open question from an older version gets a fresh answer window), never written.
	 */
	private record Course(long startTick, boolean retired, int chapter, List<String> completed, boolean chapterOffered,
			boolean finished, long legacyAskedAt, long playedTicks, Optional<Long> chapterStarted, Optional<Long> askedPlayed) {
		static final MapCodec<Course> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.LONG.lenientOptionalFieldOf("start_tick", -1L).forGetter(Course::startTick),
				Codec.BOOL.lenientOptionalFieldOf("retired", false).forGetter(Course::retired),
				Codec.INT.lenientOptionalFieldOf("chapter", 1).forGetter(Course::chapter),
				Codec.STRING.listOf().lenientOptionalFieldOf("completed", List.of()).forGetter(Course::completed),
				Codec.BOOL.lenientOptionalFieldOf("chapter_offered", false).forGetter(Course::chapterOffered),
				Codec.BOOL.lenientOptionalFieldOf("finished", false).forGetter(Course::finished),
				Codec.LONG.lenientOptionalFieldOf("asked_at", -1L).forGetter(Course::legacyAskedAt),
				Codec.LONG.lenientOptionalFieldOf("played_ticks", 0L).forGetter(Course::playedTicks),
				Codec.LONG.lenientOptionalFieldOf("chapter_started").forGetter(Course::chapterStarted),
				Codec.LONG.lenientOptionalFieldOf("asked_played").forGetter(Course::askedPlayed)
		).apply(i, Course::new));

		static Course of(GuideProgress p) {
			return new Course(p.startTick, p.retired, p.chapter, List.copyOf(p.completed), p.chapterOffered, p.finished,
					-1L, p.playedTicks, Optional.of(p.chapterStartedAt), Optional.of(p.askedAtTick));
		}
	}

	/** The player's own choices (empty = the server default). */
	private record Choices(Optional<Boolean> npcEnabled, Optional<Boolean> chatEnabled, Optional<String> musings,
			Optional<String> guideName, int aboutIndex) {
		static final MapCodec<Choices> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.BOOL.lenientOptionalFieldOf("npc_enabled").forGetter(Choices::npcEnabled),
				Codec.BOOL.lenientOptionalFieldOf("chat_enabled").forGetter(Choices::chatEnabled),
				Codec.STRING.lenientOptionalFieldOf("musings").forGetter(Choices::musings),
				Codec.STRING.lenientOptionalFieldOf("guide_name").forGetter(Choices::guideName),
				Codec.INT.lenientOptionalFieldOf("about_index", 0).forGetter(Choices::aboutIndex)
		).apply(i, Choices::new));

		static Choices of(GuideProgress p) {
			return new Choices(Optional.ofNullable(p.npcEnabled), Optional.ofNullable(p.chatEnabled),
					Optional.ofNullable(p.musings), Optional.ofNullable(p.guideName), p.aboutIndex);
		}
	}

	/** What the guide remembers: notes already given, the fallback home, a running mute and the chapter reminder. */
	private record Memory(List<String> depthNotices, Optional<Navigation.Mark> homeFallback, long muteUntil, boolean reminded) {
		static final MapCodec<Memory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.STRING.listOf().lenientOptionalFieldOf("depth_notices", List.of()).forGetter(Memory::depthNotices),
				MARK.lenientOptionalFieldOf("home_fallback").forGetter(Memory::homeFallback),
				Codec.LONG.lenientOptionalFieldOf("mute_until", 0L).forGetter(Memory::muteUntil),
				Codec.BOOL.lenientOptionalFieldOf("reminded", false).forGetter(Memory::reminded)
		).apply(i, Memory::new));

		static Memory of(GuideProgress p) {
			return new Memory(List.copyOf(p.depthNotices), Optional.ofNullable(p.homeFallback), p.muteUntil, p.reminded);
		}
	}

	static final Codec<GuideProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
			Course.CODEC.forGetter(Course::of),
			Choices.CODEC.forGetter(Choices::of),
			Memory.CODEC.forGetter(Memory::of)
	).apply(i, GuideProgressCodec::progress));

	private static GuideProgress progress(Course course, Choices choices, Memory memory) {
		GuideProgress p = new GuideProgress();
		p.startTick = course.startTick();
		p.retired = course.retired();
		p.chapter = course.chapter();
		p.completed.addAll(course.completed());
		p.chapterOffered = course.chapterOffered();
		p.finished = course.finished();
		p.playedTicks = course.playedTicks();
		// From before played time was saved: the chapter's days and an open question start counting now.
		p.chapterStartedAt = course.chapterStarted().orElse(p.startTick >= 0 ? p.playedTicks : -1L);
		p.askedAtTick = course.askedPlayed().orElse(course.legacyAskedAt() >= 0 ? p.playedTicks : -1L);
		p.npcEnabled = orNull(choices.npcEnabled());
		p.chatEnabled = orNull(choices.chatEnabled());
		p.musings = orNull(choices.musings());
		p.guideName = orNull(choices.guideName());
		p.aboutIndex = choices.aboutIndex();
		p.depthNotices.addAll(memory.depthNotices());
		p.homeFallback = orNull(memory.homeFallback());
		p.muteUntil = memory.muteUntil();
		p.reminded = memory.reminded();
		return p;
	}

	private static <T> @Nullable T orNull(Optional<T> value) {
		return value.orElse(null);
	}
}
