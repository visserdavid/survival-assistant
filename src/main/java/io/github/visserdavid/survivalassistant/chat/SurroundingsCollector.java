package io.github.visserdavid.survivalassistant.chat;

import io.github.visserdavid.survivalassistant.GuideManager;
import io.github.visserdavid.survivalassistant.advice.BiomeGroup;
import io.github.visserdavid.survivalassistant.advice.SurvivalContext;
import io.github.visserdavid.survivalassistant.entity.GuideEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Turns the live world around a player into a {@link Surroundings} snapshot. */
public final class SurroundingsCollector {
	private SurroundingsCollector() {}

	public static Surroundings collect(ServerPlayer player, SurvivalContext ctx, int radius) {
		ServerLevel level = player.level();
		String biomeId = GuideManager.biomeId(player);

		// Rain is checked at the top of the column: walking under a roof shouldn't count as "the rain stopped".
		int x = player.getBlockX(), z = player.getBlockZ();
		BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
		boolean raining = level.isRainingAt(top);

		Surroundings.Phase phase = ctx.isNight() ? Surroundings.Phase.NIGHT
				: ctx.isDusk() ? Surroundings.Phase.DUSK : Surroundings.Phase.DAY;

		Map<String, Integer> friendly = new HashMap<>();
		Map<String, Integer> hostile = new HashMap<>();
		AABB area = new AABB(player.blockPosition()).inflate(radius);
		for (Mob mob : level.getEntitiesOfClass(Mob.class, area, m -> m.isAlive() && !(m instanceof GuideEntity))) {
			if (!player.hasLineOfSight(mob)) continue; // the skeleton in the next cave doesn't count
			String id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();
			(mob instanceof Enemy ? hostile : friendly).merge(id, 1, Integer::sum);
		}

		return new Surroundings(biomeId, BiomeGroup.of(biomeId).name().toLowerCase(Locale.ROOT), raining,
				ctx.thundering(), phase, ctx.underground(), friendly, hostile);
	}
}
