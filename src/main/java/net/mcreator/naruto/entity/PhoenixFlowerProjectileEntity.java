package net.mcreator.naruto.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.procedures.NarutoDamageFormula;

import java.util.Optional;
import java.util.UUID;

public class PhoenixFlowerProjectileEntity extends Entity {
	private static final EntityDataAccessor<Float> CHARGE_PERCENT = SynchedEntityData.defineId(PhoenixFlowerProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(PhoenixFlowerProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(PhoenixFlowerProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(PhoenixFlowerProjectileEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Optional<UUID>> TARGET_UUID = SynchedEntityData.defineId(PhoenixFlowerProjectileEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	public static final int MAX_LIFETIME = 76;
	public static final int HOMING_DELAY_TICKS = 4;
	public static final double VISUAL_SCALE = 0.46D;
	public static final double ENTITY_COLLISION_RADIUS = 0.34D;
	public static final double BLOCK_COLLISION_RADIUS = 0.25D;
	public static final double WATER_COLLISION_RADIUS = 0.34D;
	public static final double TARGET_REACHED_RADIUS = 0.45D;
	public static final double MIN_SPEED = 0.75D;
	public static final double MAX_SPEED = 1.25D;
	public static final double MIN_HOMING_STRENGTH = 0.1D;
	public static final double MAX_HOMING_STRENGTH = 0.2D;
	private Vec3 fixedTarget = Vec3.ZERO;
	private boolean hasFixedTarget;

	public PhoenixFlowerProjectileEntity(EntityType<? extends PhoenixFlowerProjectileEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(CHARGE_PERCENT, 0.0F);
		builder.define(DAMAGE, 0.0F);
		builder.define(SPEED, (float) MIN_SPEED);
		builder.define(OWNER_UUID, Optional.empty());
		builder.define(TARGET_UUID, Optional.empty());
	}

	public void setOwner(Player owner) {
		this.entityData.set(OWNER_UUID, owner == null ? Optional.empty() : Optional.of(owner.getUUID()));
	}

	public Entity getOwner() {
		Optional<UUID> ownerId = this.entityData.get(OWNER_UUID);
		if (ownerId.isEmpty() || !(this.level() instanceof ServerLevel serverLevel)) {
			return null;
		}
		return serverLevel.getEntity(ownerId.get());
	}

	public void setTargetEntity(Entity target) {
		this.entityData.set(TARGET_UUID, target == null ? Optional.empty() : Optional.of(target.getUUID()));
		this.hasFixedTarget = false;
	}

	public void setFixedTarget(Vec3 target) {
		this.fixedTarget = target == null ? Vec3.ZERO : target;
		this.hasFixedTarget = target != null;
		this.entityData.set(TARGET_UUID, Optional.empty());
	}

	public void setChargePercent(float chargePercent) {
		this.entityData.set(CHARGE_PERCENT, Mth.clamp(chargePercent, 0.0F, 1.0F));
	}

	public float getChargePercent() {
		return Mth.clamp(this.entityData.get(CHARGE_PERCENT), 0.0F, 1.0F);
	}

	public void setDamage(float damage) {
		this.entityData.set(DAMAGE, Math.max(0.0F, damage));
	}

	public float getDamage() {
		return Math.max(0.0F, this.entityData.get(DAMAGE));
	}

	public void setFlightSpeed(double speed) {
		this.entityData.set(SPEED, (float) Math.max(0.05D, speed));
	}

	public double getFlightSpeed() {
		return Math.max(0.05D, this.entityData.get(SPEED));
	}

	public double getVisualScale() {
		return VISUAL_SCALE;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.tickCount > MAX_LIFETIME) {
			this.discard();
			return;
		}
		if (!this.level().isClientSide() && this.tickCount >= HOMING_DELAY_TICKS) {
			homeTowardTarget();
		}
		Vec3 start = this.position();
		Vec3 end = start.add(this.getDeltaMovement());
		if (!this.level().isClientSide()) {
			if (touchesWater(getSweptCollisionBox(start, end, WATER_COLLISION_RADIUS))) {
				extinguish(end);
				return;
			}
			HitResult hit = findHit(start, end);
			if (hit.getType() == HitResult.Type.BLOCK) {
				BlockHitResult blockHit = (BlockHitResult) hit;
				if (this.level().getFluidState(blockHit.getBlockPos()).is(FluidTags.WATER)) {
					extinguish(blockHit.getLocation());
				} else {
					impact(blockHit.getLocation(), null);
				}
				return;
			}
			if (hit.getType() == HitResult.Type.ENTITY) {
				EntityHitResult entityHit = (EntityHitResult) hit;
				impact(entityHit.getLocation(), entityHit.getEntity());
				return;
			}
			if (touchesBlock(getSweptCollisionBox(start, end, BLOCK_COLLISION_RADIUS))) {
				impact(end, null);
				return;
			}
			if (hasFixedTarget && end.distanceToSqr(fixedTarget) <= TARGET_REACHED_RADIUS * TARGET_REACHED_RADIUS) {
				impact(fixedTarget, null);
				return;
			}
			spawnTrail(start);
		}
		this.setPos(end);
	}

	private void homeTowardTarget() {
		Vec3 targetPosition = getTargetPosition();
		if (targetPosition == null) {
			return;
		}
		Vec3 toTarget = targetPosition.subtract(this.position());
		if (toTarget.lengthSqr() < 0.0001D) {
			return;
		}
		double homingStrength = Mth.clamp(MIN_HOMING_STRENGTH + (this.tickCount - HOMING_DELAY_TICKS) * 0.006D, MIN_HOMING_STRENGTH, MAX_HOMING_STRENGTH);
		Vec3 desiredVelocity = toTarget.normalize().scale(getFlightSpeed());
		this.setDeltaMovement(this.getDeltaMovement().lerp(desiredVelocity, homingStrength));
	}

	private Vec3 getTargetPosition() {
		Entity target = getTargetEntity();
		if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
			return livingTarget.position().add(0.0D, livingTarget.getBbHeight() * 0.55D, 0.0D);
		}
		return hasFixedTarget ? fixedTarget : null;
	}

	private Entity getTargetEntity() {
		Optional<UUID> targetId = this.entityData.get(TARGET_UUID);
		if (targetId.isEmpty() || !(this.level() instanceof ServerLevel serverLevel)) {
			return null;
		}
		return serverLevel.getEntity(targetId.get());
	}

	private HitResult findHit(Vec3 start, Vec3 end) {
		BlockHitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, this));
		Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
		EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(this.level(), this, start, entityEnd,
				getSweptCollisionBox(start, entityEnd, ENTITY_COLLISION_RADIUS), entity -> entity.isAlive() && entity.isPickable() && !isOwner(entity));
		return entityHit != null ? entityHit : blockHit;
	}

	private boolean isOwner(Entity entity) {
		if (entity == null) {
			return false;
		}
		Optional<UUID> ownerId = this.entityData.get(OWNER_UUID);
		return ownerId.isPresent() && ownerId.get().equals(entity.getUUID());
	}

	private AABB getSweptCollisionBox(Vec3 start, Vec3 end, double radius) {
		return new AABB(Math.min(start.x, end.x), Math.min(start.y, end.y), Math.min(start.z, end.z), Math.max(start.x, end.x), Math.max(start.y, end.y), Math.max(start.z, end.z)).inflate(radius);
	}

	private boolean touchesWater(AABB box) {
		BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
		BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
		for (BlockPos blockPos : BlockPos.betweenClosed(min, max)) {
			if (this.level().getFluidState(blockPos).is(FluidTags.WATER)) {
				return true;
			}
		}
		return false;
	}

	private boolean touchesBlock(AABB box) {
		return !this.level().noCollision(box);
	}

	private void impact(Vec3 position, Entity target) {
		Entity owner = getOwner();
		if (target instanceof LivingEntity livingTarget) {
			float damage = this.getDamage();
			if (damage <= 0.0F) {
				damage = owner instanceof LivingEntity livingOwner ? NarutoDamageFormula.calculatePhoenixFlowerDamage(livingOwner, this.getChargePercent())
						: NarutoDamageFormula.calculateJutsuDamage(null, 3.5F, this.getChargePercent(), NarutoDamageFormula.TYPE_SCALE_MULTI_HIT_HIGH);
			}
			livingTarget.invulnerableTime = 0;
			livingTarget.hurt(this.damageSources().mobProjectile(this, owner instanceof LivingEntity livingOwner ? livingOwner : null), damage);
			livingTarget.invulnerableTime = 0;
			livingTarget.igniteForSeconds(Mth.lerp(this.getChargePercent(), 1.0F, 2.5F));
		}
		if (this.level() instanceof ServerLevel serverLevel) {
			int flameCount = Mth.floor(Mth.lerp(this.getChargePercent(), 5.0F, 9.0F));
			int smokeCount = Mth.floor(Mth.lerp(this.getChargePercent(), 2.0F, 4.0F));
			serverLevel.sendParticles(ParticleTypes.FLAME, position.x, position.y, position.z, flameCount, 0.18D, 0.14D, 0.18D, 0.035D);
			serverLevel.sendParticles(ParticleTypes.SMOKE, position.x, position.y, position.z, smokeCount, 0.14D, 0.1D, 0.14D, 0.02D);
		}
		this.discard();
	}

	private void extinguish(Vec3 position) {
		if (this.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.CLOUD, position.x, position.y, position.z, 8, 0.22D, 0.16D, 0.22D, 0.06D);
			serverLevel.sendParticles(ParticleTypes.POOF, position.x, position.y, position.z, 4, 0.14D, 0.1D, 0.14D, 0.03D);
		}
		this.discard();
	}

	private void spawnTrail(Vec3 position) {
		if (!(this.level() instanceof ServerLevel serverLevel) || this.tickCount % 3 != 0) {
			return;
		}
		serverLevel.sendParticles(ParticleTypes.FLAME, position.x, position.y, position.z, this.getChargePercent() > 0.6F ? 2 : 1, 0.08D, 0.08D, 0.08D, 0.008D);
		if (this.tickCount % 6 == 0) {
			serverLevel.sendParticles(ParticleTypes.SMOKE, position.x, position.y, position.z, 1, 0.07D, 0.07D, 0.07D, 0.004D);
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag compound) {
		compound.putFloat("ChargePercent", getChargePercent());
		compound.putFloat("Damage", getDamage());
		compound.putFloat("Speed", (float) getFlightSpeed());
		compound.putBoolean("HasFixedTarget", hasFixedTarget);
		compound.putDouble("TargetX", fixedTarget.x);
		compound.putDouble("TargetY", fixedTarget.y);
		compound.putDouble("TargetZ", fixedTarget.z);
		this.entityData.get(OWNER_UUID).ifPresent(uuid -> compound.putUUID("Owner", uuid));
		this.entityData.get(TARGET_UUID).ifPresent(uuid -> compound.putUUID("Target", uuid));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag compound) {
		setChargePercent(compound.getFloat("ChargePercent"));
		setDamage(compound.getFloat("Damage"));
		setFlightSpeed(compound.contains("Speed") ? compound.getFloat("Speed") : MIN_SPEED);
		hasFixedTarget = compound.getBoolean("HasFixedTarget");
		fixedTarget = new Vec3(compound.getDouble("TargetX"), compound.getDouble("TargetY"), compound.getDouble("TargetZ"));
		if (compound.hasUUID("Owner")) {
			this.entityData.set(OWNER_UUID, Optional.of(compound.getUUID("Owner")));
		}
		if (compound.hasUUID("Target")) {
			this.entityData.set(TARGET_UUID, Optional.of(compound.getUUID("Target")));
		}
	}
}
