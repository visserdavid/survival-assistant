package io.github.visserdavid.survivalassistant.state;

import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.advice.HudSnapshot;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/** The task card data, synced from the server to the owning player only. Not saved: it's recomputed every few seconds. */
public final class HudState {
	private HudState() {}

	public static final StreamCodec<ByteBuf, HudSnapshot> STREAM_CODEC = HudCodec.STREAM_CODEC;

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
