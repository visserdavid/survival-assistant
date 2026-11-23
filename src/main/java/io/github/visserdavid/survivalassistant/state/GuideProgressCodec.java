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
 */
final class GuideProgressCodec {
	private GuideProgressCodec() {}

	private static final Codec<Navigation.Mark> MARK = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("dimension").forGetter(Navigation.Mark::dimension),
			Codec.INT.fieldOf("x").forGetter(Navigation.Mark::x),
			Codec.INT.fieldOf("y").forGetter(Navigation.Mark::y),
			Codec.INT.fieldOf("z").forGetter(Navigation.Mark::z)
	).apply(i, Navigation.Mark::new));

	/** Where the player is in the tutorial. */
	private record Course(long startTick, boolean retired, int chapter, List<String> completed, boolean chapterOffered,
			boolean finished, int extraDays, long askedAtTick) {
		static final MapCodec<Course> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.LONG.optionalFieldOf("start_tick", -1L).forGetter(Course::startTick),
				Codec.BOOL.optionalFieldOf("retired", false).forGetter(Course::retired),
				Codec.INT.optionalFieldOf("chapter", 1).forGetter(Course::chapter),
				Codec.STRING.listOf().optionalFieldOf("completed", List.of()).forGetter(Course::completed),
				Codec.BOOL.optionalFieldOf("chapter_offered", false).forGetter(Course::chapterOffered),
				Codec.BOOL.optionalFieldOf("finished", false).forGetter(Course::finished),
				Codec.INT.optionalFieldOf("extra_days", 0).forGetter(Course::extraDays),
				Codec.LONG.optionalFieldOf("asked_at", -1L).forGetter(Course::askedAtTick)
		).apply(i, Course::new));

		static Course of(GuideProgress p) {
			return new Course(p.startTick, p.retired, p.chapter, List.copyOf(p.completed), p.chapterOffered, p.finished,
					p.extraDays, p.askedAtTick);
		}
	}

	/** The player's own choices (empty = the server default). */
	private record Choices(Optional<Boolean> npcEnabled, Optional<Boolean> chatEnabled, Optional<String> musings,
			Optional<String> guideName, int aboutIndex) {
		static final MapCodec<Choices> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.BOOL.optionalFieldOf("npc_enabled").forGetter(Choices::npcEnabled),
				Codec.BOOL.optionalFieldOf("chat_enabled").forGetter(Choices::chatEnabled),
				Codec.STRING.optionalFieldOf("musings").forGetter(Choices::musings),
				Codec.STRING.optionalFieldOf("guide_name").forGetter(Choices::guideName),
				Codec.INT.optionalFieldOf("about_index", 0).forGetter(Choices::aboutIndex)
		).apply(i, Choices::new));

		static Choices of(GuideProgress p) {
			return new Choices(Optional.ofNullable(p.npcEnabled), Optional.ofNullable(p.chatEnabled),
					Optional.ofNullable(p.musings), Optional.ofNullable(p.guideName), p.aboutIndex);
		}
	}

	/** What the guide remembers about the world: notes already given and the fallback home. */
	private record Memory(List<String> depthNotices, Optional<Navigation.Mark> homeFallback) {
		static final MapCodec<Memory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.STRING.listOf().optionalFieldOf("depth_notices", List.of()).forGetter(Memory::depthNotices),
				MARK.optionalFieldOf("home_fallback").forGetter(Memory::homeFallback)
		).apply(i, Memory::new));

		static Memory of(GuideProgress p) {
			return new Memory(List.copyOf(p.depthNotices), Optional.ofNullable(p.homeFallback));
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
		p.extraDays = course.extraDays();
		p.askedAtTick = course.askedAtTick();
		p.npcEnabled = orNull(choices.npcEnabled());
		p.chatEnabled = orNull(choices.chatEnabled());
		p.musings = orNull(choices.musings());
		p.guideName = orNull(choices.guideName());
		p.aboutIndex = choices.aboutIndex();
		p.depthNotices.addAll(memory.depthNotices());
		p.homeFallback = orNull(memory.homeFallback());
		return p;
	}

	private static <T> @Nullable T orNull(Optional<T> value) {
		return value.orElse(null);
	}
}
