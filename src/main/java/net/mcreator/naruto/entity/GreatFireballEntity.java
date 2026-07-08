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

public class GreatFireballEntity extends Entity {
	private static final EntityDataAccessor<Float> CHARGE_PERCENT = SynchedEntityData.defineId(GreatFireballEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(GreatFireballEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(GreatFireballEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final int MAX_LIFETIME = 80;
	// Main tuning values for charge-based size/collision. Edit these first when balancing the projectile feel.
	public static final float MIN_VISUAL_SCALE = 0.35F;
	public static final float MAX_VISUAL_SCALE = 2.75F;
	public static final double MIN_ENTITY_COLLISION_RADIUS = 0.3D;
	public static final double MAX_ENTITY_COLLISION_RADIUS = 5D;
	public static final double MIN_BLOCK_COLLISION_RADIUS = 0.18D;
	public static final double MAX_BLOCK_COLLISION_RADIUS = 1D;
	public static final double MIN_WATER_COLLISION_RADIUS = 0.35D;
	public static final double MAX_WATER_COLLISION_RADIUS = 2.75D;
	public static final double SPAWN_FORWARD_BASE = 0.75D;
	public static final double IMPACT_PARTICLE_RADIUS_EXTRA = 0.0D;
	public static final double STEAM_PARTICLE_RADIUS_EXTRA = 0.25D;
	public static final double TRAIL_PARTICLE_RADIUS_SCALE = 0.45D;
	public static final int TRAIL_INTERVAL_TICKS = 3;
	public static final int TRAIL_SMOKE_INTERVAL_TICKS = 6;

	public GreatFireballEntity(EntityType<? extends GreatFireballEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(CHARGE_PERCENT, 0.0F);
		builder.define(DAMAGE, 0.0F);
		builder.define(OWNER_UUID, Optional.empty());
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

	public void setChargePercent(float chargePercent) {
		this.entityData.set(CHARGE_PERCENT, Mth.clamp(chargePercent, 0.0F, 1.0F));
	}

	public float getChargePercent() {
		return Mth.clamp(this.entityData.get(CHARGE_PERCENT), 0.0F, 1.0F);
	}

	public float getVisualScale() {
		return Mth.lerp(this.getChargePercent(), MIN_VISUAL_SCALE, MAX_VISUAL_SCALE);
	}

	public double getCollisionRadius() {
		return getEntityCollisionRadius();
	}

	public double getEntityCollisionRadius() {
		return getEntityCollisionRadiusForCharge(this.getChargePercent());
	}

	public double getBlockCollisionRadius() {
		return lerpRadius(this.getChargePercent(), MIN_BLOCK_COLLISION_RADIUS, MAX_BLOCK_COLLISION_RADIUS);
	}

	public double getWaterCollisionRadius() {
		return lerpRadius(this.getChargePercent(), MIN_WATER_COLLISION_RADIUS, MAX_WATER_COLLISION_RADIUS);
	}

	public static double getEntityCollisionRadiusForCharge(float chargePercent) {
		return lerpRadius(chargePercent, MIN_ENTITY_COLLISION_RADIUS, MAX_ENTITY_COLLISION_RADIUS);
	}

	public static double getSpawnForwardDistance(float chargePercent) {
		return SPAWN_FORWARD_BASE + getEntityCollisionRadiusForCharge(chargePercent);
	}

	private static double lerpRadius(float chargePercent, double minRadius, double maxRadius) {
		return Mth.lerp(Mth.clamp(chargePercent, 0.0F, 1.0F), minRadius, maxRadius);
	}

	public void setDamage(float damage) {
		this.entityData.set(DAMAGE, Math.max(0.0F, damage));
	}

	public float getDamage() {
		return Math.max(0.0F, this.entityData.get(DAMAGE));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.tickCount > MAX_LIFETIME) {
			this.discard();
			return;
		}
		Vec3 start = this.position();
		Vec3 end = start.add(this.getDeltaMovement());
		if (!this.level().isClientSide()) {
			AABB entitySweptBox = getSweptCollisionBox(start, end, getEntityCollisionRadius());
			AABB waterSweptBox = getSweptCollisionBox(start, end, getWaterCollisionRadius());
			AABB blockSweptBox = getSweptCollisionBox(start, end, getBlockCollisionRadius());
			if (touchesWater(waterSweptBox)) {
				extinguish(end);
				return;
			}
			HitResult hit = findHit(start, end, entitySweptBox);
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
			if (touchesBlock(blockSweptBox)) {
				impact(end, null);
				return;
			}
			spawnSparseTrail(start);
		}
		this.setPos(end);
	}

	private HitResult findHit(Vec3 start, Vec3 end, AABB entitySweptBox) {
		BlockHitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, this));
		Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
		AABB entityHitBox = blockHit.getType() == HitResult.Type.MISS ? entitySweptBox : getSweptCollisionBox(start, entityEnd, getEntityCollisionRadius());
		EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(this.level(), this, start, entityEnd,
				entityHitBox, entity -> entity.isAlive() && entity.isPickable() && !isOwner(entity));
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
				damage = owner instanceof LivingEntity livingOwner ? NarutoDamageFormula.calculateGreatFireballDamage(livingOwner, this.getChargePercent())
						: NarutoDamageFormula.calculateJutsuDamage(null, 14.0F, this.getChargePercent(), NarutoDamageFormula.TYPE_SCALE_HEAVY_CHARGED_PROJECTILE);
			}
			livingTarget.hurt(this.damageSources().mobProjectile(this, owner instanceof LivingEntity livingOwner ? livingOwner : null), damage);
			livingTarget.igniteForSeconds(Mth.lerp(this.getChargePercent(), 2.0F, 5.0F));
		}
		if (this.level() instanceof ServerLevel serverLevel) {
			double radius = this.getEntityCollisionRadius() + IMPACT_PARTICLE_RADIUS_EXTRA;
			int flameCount = Mth.floor(Mth.lerp(this.getChargePercent(), 10.0F, 20.0F));
			int smokeCount = Mth.floor(Mth.lerp(this.getChargePercent(), 5.0F, 11.0F));
			serverLevel.sendParticles(ParticleTypes.FLAME, position.x, position.y, position.z, flameCount, radius, radius * 0.6D, radius, 0.05D);
			serverLevel.sendParticles(ParticleTypes.SMOKE, position.x, position.y, position.z, smokeCount, radius, radius * 0.45D, radius, 0.035D);
			serverLevel.sendParticles(ParticleTypes.LAVA, position.x, position.y, position.z, this.getChargePercent() > 0.55F ? 2 : 1, radius * 0.45D, radius * 0.25D, radius * 0.45D, 0.02D);
		}
		this.discard();
	}

	private void extinguish(Vec3 position) {
		if (this.level() instanceof ServerLevel serverLevel) {
			double radius = this.getWaterCollisionRadius() + STEAM_PARTICLE_RADIUS_EXTRA;
			int cloudCount = Mth.floor(Mth.lerp(this.getChargePercent(), 16.0F, 28.0F));
			int poofCount = Mth.floor(Mth.lerp(this.getChargePercent(), 7.0F, 13.0F));
			serverLevel.sendParticles(ParticleTypes.CLOUD, position.x, position.y, position.z, cloudCount, radius, radius * 0.65D, radius, 0.08D);
			serverLevel.sendParticles(ParticleTypes.POOF, position.x, position.y, position.z, poofCount, radius * 0.7D, radius * 0.45D, radius * 0.7D, 0.04D);
			serverLevel.sendParticles(ParticleTypes.SMOKE, position.x, position.y, position.z, Math.max(4, poofCount - 2), radius * 0.45D, radius * 0.3D, radius * 0.45D, 0.02D);
		}
		this.discard();
	}

	private void spawnSparseTrail(Vec3 position) {
		if (!(this.level() instanceof ServerLevel serverLevel) || this.tickCount % TRAIL_INTERVAL_TICKS != 0) {
			return;
		}
		double radius = this.getEntityCollisionRadius() * TRAIL_PARTICLE_RADIUS_SCALE;
		int flameCount = this.getChargePercent() > 0.75F ? 3 : this.getChargePercent() > 0.35F ? 2 : 1;
		serverLevel.sendParticles(ParticleTypes.FLAME, position.x, position.y, position.z, flameCount, radius, radius, radius, 0.01D);
		if (this.tickCount % TRAIL_SMOKE_INTERVAL_TICKS == 0) {
			serverLevel.sendParticles(ParticleTypes.SMOKE, position.x, position.y, position.z, this.getChargePercent() > 0.6F ? 2 : 1, radius * 0.7D, radius * 0.7D, radius * 0.7D, 0.005D);
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag compound) {
		compound.putFloat("ChargePercent", getChargePercent());
		compound.putFloat("Damage", getDamage());
		this.entityData.get(OWNER_UUID).ifPresent(uuid -> compound.putUUID("Owner", uuid));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag compound) {
		setChargePercent(compound.getFloat("ChargePercent"));
		setDamage(compound.getFloat("Damage"));
		if (compound.hasUUID("Owner")) {
			this.entityData.set(OWNER_UUID, Optional.of(compound.getUUID("Owner")));
		}
	}
}
