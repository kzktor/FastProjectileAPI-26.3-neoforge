package dev.xkmc.fastprojectileapi.entity;

import dev.xkmc.fastprojectileapi.collision.EntityStorageHelper;
import dev.xkmc.fastprojectileapi.collision.ProjectileHitHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

public abstract class BaseProjectile extends SimplifiedProjectile {

	protected BaseProjectile(EntityType<? extends BaseProjectile> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {

	}

	public abstract boolean checkBlockHit();

	public abstract int lifetime();

	public AABB getBoundingBoxForEntityHit() {
		return getBoundingBox();
	}

	public void tick() {
		super.tick();
		HitResult hitresult = ProjectileHitHelper.getHitResultOnMoveVector(this, checkBlockHit());
		if (hitresult != null) {
			onHit(hitresult);
			if (isRemoved()) {
				return;
			}
		}
		if (tickCount >= lifetime()) {
			if (level() instanceof ServerLevel) {
				projectileMove();
				terminate();
				markErased(false);
				return;
			}
			var owner = getOwner();
			if (tickCount >= lifetime() + 10 || owner == null || !owner.isAlive()) {
				markErased(false);
			}
			return;
		}
		projectileMove();
		if (level() instanceof ServerLevel sl) {
			if (!level().hasChunk(blockPosition().getX() >> 4, blockPosition().getZ() >> 4) ||
					isAddedToLevel() && !EntityStorageHelper.isTicking(sl, this)) {
				markErased(false);
			}
		}
	}

	public void checkBelowWorld() {
		if (this.getY() < (double) (this.level().getMinY() - 64)) {
			markErased(false);
		}
	}

	protected void terminate() {

	}

	protected void projectileMove() {
		ProjectileMovement movement = updateVelocity(getDeltaMovement(), position());
		setDeltaMovement(movement.vec());
		updateRotation(movement.rot());
		double d2 = getX() + movement.vec().x;
		double d0 = getY() + movement.vec().y;
		double d1 = getZ() + movement.vec().z;
		setPos(d2, d0, d1);
	}

	protected ProjectileMovement updateVelocity(Vec3 vec, Vec3 pos) {
		return ProjectileMovement.of(vec);
	}

	public boolean shouldRenderAtSqrDistance(double pDistance) {
		double d0 = getBoundingBox().getSize() * 4;
		if (Double.isNaN(d0)) d0 = 4;
		d0 *= 64;
		return pDistance < d0 * d0;
	}

	protected void onHit(HitResult hitresult) {
		if (hitresult.getType() == HitResult.Type.MISS) return;
		if (hitresult instanceof EntityHitResult ehit) {
			onHitEntity(ehit);
			level().gameEvent(GameEvent.PROJECTILE_LAND, hitresult.getLocation(), GameEvent.Context.of(this, null));
		} else if (hitresult instanceof BlockHitResult bhit) {
			BlockPos pos = bhit.getBlockPos();
			onHitBlock(bhit);
			level().gameEvent(GameEvent.PROJECTILE_LAND, pos, GameEvent.Context.of(this, level().getBlockState(pos)));
		}
	}

	protected void onHitEntity(EntityHitResult pResult) {
	}

	protected void onHitBlock(BlockHitResult pResult) {
	}

	@Override
	public boolean isValid() {
		return tickCount < lifetime();
	}

}
