package io.github.visserdavid.survivalassistant.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import io.github.visserdavid.survivalassistant.GuideManager;
import io.github.visserdavid.survivalassistant.entity.ai.FollowOwnerGoal;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * The guide NPC. Purely an advisor: invulnerable, ignored by monsters, never saved to the world.
 * {@link GuideManager} (re)spawns one next to each player who still has an active guide.
 */
public class GuideEntity extends PathfinderMob {
	private @Nullable UUID ownerId;
	private @Nullable BlockPos pointAt;
	private int pointTicks;

	public GuideEntity(EntityType<? extends GuideEntity> type, Level level) {
		super(type, level);
		this.setPermanentlyInvulnerable(true);
		this.setCustomNameVisible(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new FollowOwnerGoal(this, 1.1, 6f, 3f));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8f));
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
	}

	// --- owner ---

	public void setOwner(ServerPlayer player) {
		this.ownerId = player.getUUID();
	}

	public @Nullable UUID getOwnerId() {
		return ownerId;
	}

	public @Nullable ServerPlayer getOwner() {
		if (ownerId == null || !(level() instanceof ServerLevel server)) return null;
		return server.getPlayerByUUID(ownerId) instanceof ServerPlayer p ? p : null;
	}

	// --- "pointing": look at a resource block for a few seconds ---

	public void pointAt(BlockPos pos) {
		this.pointAt = pos;
		this.pointTicks = 100;
	}

	public boolean isPointing() {
		return pointTicks > 0 && pointAt != null;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (isPointing()) {
			pointTicks--;
			getLookControl().setLookAt(pointAt.getX() + 0.5, pointAt.getY() + 0.5, pointAt.getZ() + 0.5, 30f, 30f);
		}
	}

	@Override
	public void tick() {
		super.tick();
		// Without an owner in this level the guide has no purpose; the manager will spawn a fresh one where needed.
		if (!level().isClientSide() && tickCount > 40 && getOwner() == null) {
			discard();
		}
	}

	// --- interaction ---

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (player instanceof ServerPlayer sp && sp.getUUID().equals(ownerId)) {
			GuideManager.onGuideClicked(sp, this);
		}
		return InteractionResult.SUCCESS;
	}

	// --- an advisor, not a fighter or a victim (damage is blocked by setPermanentlyInvulnerable; /kill still works) ---

	@Override
	public boolean canBeSeenAsEnemy() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}
}
