package io.github.visserdavid.survivalassistant;

import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import io.github.visserdavid.survivalassistant.entity.ai.FollowOwnerGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The guide characters: at most one per player, spawned next to them and removed when not needed. */
final class GuideEntities {
	private static final Map<UUID, GuideEntity> GUIDES = new HashMap<>();
	/** A guide further away than this (or in another dimension) is replaced by a fresh one next to the player. */
	private static final double RESPAWN_DIST_SQR = 48 * 48;

	private GuideEntities() {}

	/** The player's guide character, or null when it isn't around (character off, retired, not spawned yet). */
	static @Nullable GuideEntity get(ServerPlayer player) {
		GuideEntity guide = GUIDES.get(player.getUUID());
		return guide == null || guide.isRemoved() ? null : guide;
	}

	/** The player's guide, spawned next to them when it is missing, far away or in another dimension. */
	static @Nullable GuideEntity ensure(ServerPlayer player) {
		GuideEntity guide = get(player);
		if (guide != null && guide.level() == player.level() && guide.distanceToSqr(player) < RESPAWN_DIST_SQR) return guide;
		if (guide != null) guide.discard();

		ServerLevel level = player.level();
		GuideEntity fresh = SurvivalAssistant.GUIDE.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (fresh == null) return null;
		fresh.setOwner(player);
		fresh.setCustomName(Component.literal(GuideSpeech.guideName(player)));
		fresh.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0f);
		FollowOwnerGoal.teleportNear(fresh, player.blockPosition());
		level.addFreshEntity(fresh);
		GUIDES.put(player.getUUID(), fresh);
		return fresh;
	}

	/** Removes the player's guide, with a puff of smoke when {@code poof}. */
	static void remove(UUID playerId, boolean poof) {
		GuideEntity guide = GUIDES.remove(playerId);
		if (guide == null || guide.isRemoved()) return;
		if (poof && guide.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.POOF, guide.getX(), guide.getY() + 1, guide.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
		}
		guide.discard();
	}

	/** Shows the guide's current name on its name tag. */
	static void refreshName(ServerPlayer player) {
		GuideEntity guide = get(player);
		if (guide != null) guide.setCustomName(Component.literal(GuideSpeech.guideName(player)));
	}

	static void clear() {
		GUIDES.values().forEach(g -> { if (!g.isRemoved()) g.discard(); });
		GUIDES.clear();
	}
}
