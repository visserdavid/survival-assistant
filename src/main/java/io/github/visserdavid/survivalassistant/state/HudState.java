package io.github.visserdavid.survivalassistant.state;

import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.github.visserdavid.survivalassistant.advice.Navigation;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;

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
			ByteBufCodecs.STRING_UTF8, HudSnapshot::urgentId,
			ByteBufCodecs.optional(WAYPOINT_CODEC), HudSnapshot::waypoint,
			ByteBufCodecs.BOOL, HudSnapshot::safe,
			HudSnapshot::new);

	public static final AttachmentType<HudSnapshot> HUD = AttachmentRegistry.create(Identifier.fromNamespaceAndPath(SurvivalAssistant.MOD_ID, "hud"),
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
