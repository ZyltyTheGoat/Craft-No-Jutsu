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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

public class ElementalBulletEntity extends Entity {
	public static final int ELEMENT_WATER = 0;
	public static final int ELEMENT_WIND = 1;
	private static final EntityDataAccessor<Integer> ELEMENT = SynchedEntityData.defineId(ElementalBulletEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> CHARGE_PERCENT = SynchedEntityData.defineId(ElementalBulletEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(ElementalBulletEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(ElementalBulletEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final int MAX_LIFETIME = 42;
	private static final double COLLISION_RADIUS = 0.36D;

	public ElementalBulletEntity(EntityType<? extends ElementalBulletEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(ELEMENT, ELEMENT_WATER);
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

	public void setElement(int element) {
		this.entityData.set(ELEMENT, element == ELEMENT_WIND ? ELEMENT_WIND : ELEMENT_WATER);
	}

	public int getElement() {
		return this.entityData.get(ELEMENT);
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

	public float getVisualScale() {
		return Mth.lerp(getChargePercent(), 0.42F, 0.72F);
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
			HitResult hit = findHit(start, end);
			if (hit.getType() == HitResult.Type.BLOCK) {
				BlockHitResult blockHit = (BlockHitResult) hit;
				impact(blockHit.getLocation(), null);
				return;
			}
			if (hit.getType() == HitResult.Type.ENTITY) {
				EntityHitResult entityHit = (EntityHitResult) hit;
				impact(entityHit.getLocation(), entityHit.getEntity());
				return;
			}
			if (touchesBlock(getSweptCollisionBox(start, end, COLLISION_RADIUS * 0.65D))) {
				impact(end, null);
				return;
			}
			spawnTrail(start);
		}
		this.setPos(end);
	}

	private HitResult findHit(Vec3 start, Vec3 end) {
		BlockHitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
		EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(this.level(), this, start, entityEnd,
				getSweptCollisionBox(start, entityEnd, COLLISION_RADIUS), entity -> entity.isAlive() && entity.isPickable() && !isOwner(entity));
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

	private boolean touchesBlock(AABB box) {
		return !this.level().noCollision(box);
	}

	private void impact(Vec3 position, Entity target) {
		Entity owner = getOwner();
		if (target instanceof LivingEntity livingTarget) {
			float damage = getDamage();
			if (damage <= 0.0F) {
				damage = owner instanceof LivingEntity livingOwner ? NarutoDamageFormula.calculateJutsuDamage(livingOwner, getElement() == ELEMENT_WIND ? 4.6F : 4.2F, getChargePercent(), NarutoDamageFormula.TYPE_SCALE_MULTI_HIT_HIGH) : 3.0F;
			}
			livingTarget.invulnerableTime = 0;
			livingTarget.hurt(this.damageSources().mobProjectile(this, owner instanceof LivingEntity livingOwner ? livingOwner : null), damage);
			livingTarget.invulnerableTime = 0;
			if (getElement() == ELEMENT_WATER) {
				livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 26, 0, true, true));
			} else {
				Vec3 push = this.getDeltaMovement().normalize().scale(0.48D + getChargePercent() * 0.18D).add(0.0D, 0.08D, 0.0D);
				livingTarget.setDeltaMovement(livingTarget.getDeltaMovement().scale(0.35D).add(push));
				livingTarget.hurtMarked = true;
			}
		}
		if (this.level() instanceof ServerLevel serverLevel) {
			if (getElement() == ELEMENT_WATER) {
				serverLevel.sendParticles(ParticleTypes.SPLASH, position.x, position.y, position.z, 10, 0.22D, 0.18D, 0.22D, 0.07D);
				serverLevel.sendParticles(ParticleTypes.FALLING_WATER, position.x, position.y, position.z, 5, 0.16D, 0.12D, 0.16D, 0.0D);
				extinguishFire(serverLevel, position);
			} else {
				serverLevel.sendParticles(ParticleTypes.CLOUD, position.x, position.y, position.z, 8, 0.25D, 0.18D, 0.25D, 0.08D);
				serverLevel.sendParticles(ParticleTypes.POOF, position.x, position.y, position.z, 4, 0.14D, 0.1D, 0.14D, 0.04D);
			}
		}
		this.discard();
	}

	private void spawnTrail(Vec3 position) {
		if (!(this.level() instanceof ServerLevel serverLevel) || this.tickCount % 2 != 0) {
			return;
		}
		if (getElement() == ELEMENT_WATER) {
			serverLevel.sendParticles(ParticleTypes.SPLASH, position.x, position.y, position.z, 1, 0.05D, 0.05D, 0.05D, 0.02D);
			if (this.tickCount % 4 == 0) {
				serverLevel.sendParticles(ParticleTypes.BUBBLE, position.x, position.y, position.z, 2, 0.06D, 0.04D, 0.06D, 0.01D);
			}
		} else {
			serverLevel.sendParticles(ParticleTypes.CLOUD, position.x, position.y, position.z, 1, 0.07D, 0.04D, 0.07D, 0.01D);
		}
	}

	private void extinguishFire(ServerLevel level, Vec3 position) {
		BlockPos center = BlockPos.containing(position);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
			if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.FIRE) || level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.SOUL_FIRE) || level.getFluidState(pos).is(FluidTags.LAVA)) {
				level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
			}
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag compound) {
		compound.putInt("Element", getElement());
		compound.putFloat("ChargePercent", getChargePercent());
		compound.putFloat("Damage", getDamage());
		this.entityData.get(OWNER_UUID).ifPresent(uuid -> compound.putUUID("Owner", uuid));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag compound) {
		setElement(compound.getInt("Element"));
		setChargePercent(compound.getFloat("ChargePercent"));
		setDamage(compound.getFloat("Damage"));
		if (compound.hasUUID("Owner")) {
			this.entityData.set(OWNER_UUID, Optional.of(compound.getUUID("Owner")));
		}
	}
}
