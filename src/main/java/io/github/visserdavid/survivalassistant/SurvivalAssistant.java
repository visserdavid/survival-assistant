package io.github.visserdavid.survivalassistant;

import io.github.visserdavid.survivalassistant.chat.GuideChat;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import io.github.visserdavid.survivalassistant.state.GuideState;
import io.github.visserdavid.survivalassistant.state.HudState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SurvivalAssistant implements ModInitializer {
	public static final String MOD_ID = "survivalassistant";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final ResourceKey<EntityType<?>> GUIDE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("guide"));
	public static final EntityType<GuideEntity> GUIDE = Registry.register(BuiltInRegistries.ENTITY_TYPE, GUIDE_KEY,
			EntityType.Builder.of(GuideEntity::new, MobCategory.MISC)
					.sized(0.6f, 1.95f)
					.eyeHeight(1.62f)
					.clientTrackingRange(10)
					.noSave()
					.build(GUIDE_KEY));

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModConfig.load();
		GuideState.init();
		HudState.init();
		GuideSettings.register();
		FabricDefaultAttributeRegistry.register(GUIDE, GuideEntity.createAttributes());

		ServerTickEvents.END_SERVER_TICK.register(GuideManager::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> GuideManager.onDisconnect(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) GuideManager.onRespawn(newPlayer);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof net.minecraft.server.level.ServerPlayer player) GuideManager.onDeath(player);
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (player instanceof net.minecraft.server.level.ServerPlayer sp) GuideManager.onBlockBroken(sp, pos);
		});
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> GuideChat.onChat(sender, message.signedContent()));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> GuideManager.onServerStopping());
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> GuideCommand.register(dispatcher));

		LOGGER.info("Survival Assistant loaded");
	}
}
