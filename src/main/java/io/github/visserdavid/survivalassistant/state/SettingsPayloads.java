package io.github.visserdavid.survivalassistant.state;

import io.github.visserdavid.survivalassistant.SurvivalAssistant;
import io.github.visserdavid.survivalassistant.advice.GuideChoices;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The messages of the settings screen. The client asks for the current settings when the screen opens
 * ({@link Request}), the server answers ({@link Sync}), and on "Done" the client sends what changed
 * ({@link ChoicesUpdate}, {@link ConfigUpdate}). The server checks everything again before applying it.
 */
public final class SettingsPayloads {
	private SettingsPayloads() {}

	/** Longest config text accepted (the real one is about 1.5 KB). */
	private static final int MAX_CONFIG_JSON = 16 * 1024;

	public static final StreamCodec<ByteBuf, GuideChoices> CHOICES_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, GuideChoices::character,
			ByteBufCodecs.stringUtf8(64), GuideChoices::name,
			ByteBufCodecs.stringUtf8(8), GuideChoices::musings,
			ByteBufCodecs.BOOL, GuideChoices::chat,
			GuideChoices::new);

	/** Not via SurvivalAssistant.id(): that would load the main class (and its registry work) in the tests. */
	private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
		return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SurvivalAssistant.MOD_ID, path));
	}

	private static final StreamCodec<ByteBuf, String> CONFIG_JSON = ByteBufCodecs.stringUtf8(MAX_CONFIG_JSON);

	/** Client → server: the screen opened, please send the current settings. */
	public record Request() implements CustomPacketPayload {
		public static final Type<Request> TYPE = payloadType("settings_request");
		public static final StreamCodec<ByteBuf, Request> CODEC = StreamCodec.unit(new Request());

		@Override
		public Type<Request> type() {
			return TYPE;
		}
	}

	/**
	 * Server → client: the player's choices, the server's guide name, whether this player may change the server
	 * config (and then the config itself, else ""), and whether the server allows small talk.
	 */
	public record Sync(GuideChoices choices, String serverName, String configJson, boolean canEdit, boolean chatAllowed)
			implements CustomPacketPayload {
		public static final Type<Sync> TYPE = payloadType("settings_sync");
		public static final StreamCodec<ByteBuf, Sync> CODEC = StreamCodec.composite(
				CHOICES_CODEC, Sync::choices,
				ByteBufCodecs.stringUtf8(64), Sync::serverName,
				CONFIG_JSON, Sync::configJson,
				ByteBufCodecs.BOOL, Sync::canEdit,
				ByteBufCodecs.BOOL, Sync::chatAllowed,
				Sync::new);

		@Override
		public Type<Sync> type() {
			return TYPE;
		}
	}

	/** Client → server: the player's new guide choices. */
	public record ChoicesUpdate(GuideChoices choices) implements CustomPacketPayload {
		public static final Type<ChoicesUpdate> TYPE = payloadType("settings_choices");
		public static final StreamCodec<ByteBuf, ChoicesUpdate> CODEC = CHOICES_CODEC.map(ChoicesUpdate::new, ChoicesUpdate::choices);

		@Override
		public Type<ChoicesUpdate> type() {
			return TYPE;
		}
	}

	/** Client → server: a changed server config (only accepted from the host or an operator). */
	public record ConfigUpdate(String configJson) implements CustomPacketPayload {
		public static final Type<ConfigUpdate> TYPE = payloadType("settings_config");
		public static final StreamCodec<ByteBuf, ConfigUpdate> CODEC = CONFIG_JSON.map(ConfigUpdate::new, ConfigUpdate::configJson);

		@Override
		public Type<ConfigUpdate> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ChoicesUpdate.TYPE, ChoicesUpdate.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ConfigUpdate.TYPE, ConfigUpdate.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Sync.TYPE, Sync.CODEC);
	}
}
