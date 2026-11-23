package io.github.visserdavid.survivalassistant.entity;

import io.github.visserdavid.survivalassistant.GuideManager;
import io.github.visserdavid.survivalassistant.entity.ai.FollowOwnerGoal;
import io.github.visserdavid.survivalassistant.entity.ai.KeepClearGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * The guide NPC. Purely an advisor: invulnerable, ignored by monsters, never saved to the world.
 * {@link GuideManager} (re)spawns one next to each player who still has an active guide.
 *
 * <p>It must never put a beginner in danger, so it is a "ghost" for everything the player does: it doesn't push
 * or block anyone, blocks can be placed where it stands, clicks, hits and arrows pass through it (sneak +
 * right-click to talk to it), and it doesn't trigger pressure plates, tripwires, sculk sensors, farmland,
 * portals or vehicles. It also keeps out of the player's way and view (see {@link KeepClearGoal}).
 */
public class GuideEntity extends PathfinderMob {
	/**
	 * Client-side check whether the local player may click the guide (set by the client entrypoint: while sneaking).
	 * The server checks the owner itself.
	 */
	private static volatile Predicate<GuideEntity> clientPickable = guide -> true;
	/** Path types the guide avoids: places that would hurt a player who follows. */
	private static final PathType[] DANGERS = {PathType.LAVA, PathType.FIRE, PathType.FIRE_IN_NEIGHBOR, PathType.DAMAGING,
			PathType.DAMAGING_IN_NEIGHBOR, PathType.POWDER_SNOW, PathType.ON_TOP_OF_POWDER_SNOW};

	private @Nullable UUID ownerId;
	private @Nullable BlockPos pointAt;
	private int pointTicks;

	public GuideEntity(EntityType<? extends GuideEntity> type, Level level) {
		super(type, level);
		this.setPermanentlyInvulnerable(true);
		this.setCustomNameVisible(true);
		this.blocksBuilding = false; // a block can be placed where the guide stands (sealing a shelter at night)
		for (PathType danger : DANGERS) this.setPathfindingMalus(danger, -1f);
	}

	/** Sets the client-side check whether the local player may click the guide (see {@link #isPickable()}). */
	public static void setClientPickable(Predicate<GuideEntity> check) {
		clientPickable = check;
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
		this.goalSelector.addGoal(1, new KeepClearGoal(this, 1.2));
		this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.1, 7f, 3.5f));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8f));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		// no random strolling: wandering off is how a guide ends up in front of you
	}

	// --- owner ---

	public void setOwner(ServerPlayer player) {
		this.ownerId = player.getUUID();
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
			return;
		}
		// a block was placed where the guide stands: step out of it
		if (!level().isClientSide() && tickCount % 10 == 0 && isInWall()) {
			ServerPlayer owner = getOwner();
			if (owner != null) FollowOwnerGoal.teleportNear(this, owner.blockPosition());
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

	/** Never shove the player (the guide itself can't be pushed either). */
	@Override
	protected void pushEntities() {
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity entity) {
		return false;
	}

	/** Only clickable while the owner sneaks; otherwise hits and clicks pass through to whatever is behind it. */
	@Override
	public boolean isPickable() {
		if (level().isClientSide()) return clientPickable.test(this);
		ServerPlayer owner = getOwner();
		return owner != null && owner.isShiftKeyDown();
	}

	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	// --- don't set anything off ---

	/** Pressure plates, tripwires (temple traps!) and similar blocks ignore the guide. */
	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	/** Its footsteps don't reach sculk sensors and shriekers (no warden because of the guide). */
	@Override
	public boolean dampensVibrations() {
		return true;
	}

	/** Landing doesn't trample farmland (the guide takes no fall damage anyway). */
	@Override
	protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
		if (onGround) resetFallDistance();
	}

	@Override
	public boolean canUsePortal(boolean allowPassengers) {
		return false;
	}

	@Override
	protected boolean canRide(Entity vehicle) {
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
