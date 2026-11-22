package io.github.visserdavid.survivalassistant.state;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.Navigation;

import java.util.Objects;

/** The task card data, synced from the server to the owning player only. Not saved: it's recomputed every few seconds. */
public final class HudState {
	private HudState() {}

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

	private static final StreamCodec<ByteBuf, Navigation.Waypoint> WAYPOINT_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT.map(i -> Navigation.Kind.values()[i], Navigation.Kind::ordinal), Navigation.Waypoint::kind,
			MARK_CODEC, Navigation.Waypoint::target,
			ByteBufCodecs.VAR_INT, Navigation.Waypoint::ticksLeft,
			Navigation.Waypoint::new);

	public static final StreamCodec<ByteBuf, HudSnapshot> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, HudSnapshot::active,
			ByteBufCodecs.VAR_INT, HudSnapshot::dayIndex,
			ByteBufCodecs.VAR_INT, HudSnapshot::maxDays,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(64)), HudSnapshot::completed,
			ENTRY_CODEC.apply(ByteBufCodecs.list(HudSnapshot.SHOWN_TASKS)), HudSnapshot::next,
			ByteBufCodecs.STRING_UTF8, HudSnapshot::urgentId,
			ByteBufCodecs.optional(WAYPOINT_CODEC), HudSnapshot::waypoint,
			HudSnapshot::new);

	public static final AttachmentType<HudSnapshot> HUD = AttachmentRegistry.create(SurvivalAssistant.id("hud"),
			builder -> builder.syncWith(STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	/** Sends the snapshot to the player, but only when it changed (saves network traffic). */
	public static void update(ServerPlayer player, HudSnapshot snapshot) {
		if (!Objects.equals(player.getAttached(HUD), snapshot)) {
			player.setAttached(HUD, snapshot);
		}
	}

	public static void init() {
		// Forces class loading so the attachment is registered during mod init.
	}
}
