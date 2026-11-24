package io.github.visserdavid.survivalassistant.state;

import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.Navigation;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

/** How the task card data travels to the client. Kept apart from {@link HudState} so it can be tested on its own. */
public final class HudCodec {
	private HudCodec() {}

	private static final StreamCodec<ByteBuf, HudSnapshot.Entry> ENTRY_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, HudSnapshot.Entry::id,
			ByteBufCodecs.VAR_INT, HudSnapshot.Entry::value,
			ByteBufCodecs.VAR_INT, HudSnapshot.Entry::target,
			HudSnapshot.Entry::new);

	private static final StreamCodec<ByteBuf, Navigation.Mark> MARK_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, Navigation.Mark::dimension,
			ByteBufCodecs.VAR_INT, Navigation.Mark::x,
			ByteBufCodecs.VAR_INT, Navigation.Mark::y,
			ByteBufCodecs.VAR_INT, Navigation.Mark::z,
			Navigation.Mark::new);

	private static final StreamCodec<ByteBuf, Navigation.Kind> KIND_CODEC =
			ByteBufCodecs.idMapper(ByIdMap.continuous(Navigation.Kind::ordinal, Navigation.Kind.values(), ByIdMap.OutOfBoundsStrategy.ZERO),
					Navigation.Kind::ordinal);

	private static final StreamCodec<ByteBuf, Navigation.Waypoint> WAYPOINT_CODEC = StreamCodec.composite(
			KIND_CODEC, Navigation.Waypoint::kind,
			MARK_CODEC, Navigation.Waypoint::target,
			ByteBufCodecs.VAR_INT, Navigation.Waypoint::ticksLeft,
			Navigation.Waypoint::new);

	public static final StreamCodec<ByteBuf, HudSnapshot> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, HudSnapshot::active,
			ByteBufCodecs.VAR_INT, HudSnapshot::chapter,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(HudSnapshot.MAX_COMPLETED)), HudSnapshot::completed,
			ENTRY_CODEC.apply(ByteBufCodecs.list(HudSnapshot.SHOWN_TASKS)), HudSnapshot::next,
			ENTRY_CODEC, HudSnapshot::step,
			ByteBufCodecs.STRING_UTF8, HudSnapshot::urgentId,
			ByteBufCodecs.optional(WAYPOINT_CODEC), HudSnapshot::waypoint,
			ByteBufCodecs.BOOL, HudSnapshot::safe,
			HudSnapshot::new);
}
