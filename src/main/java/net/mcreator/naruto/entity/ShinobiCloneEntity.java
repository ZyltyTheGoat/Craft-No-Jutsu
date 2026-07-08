package net.mcreator.naruto.entity;

import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.network.NarutoModVariables;

import java.util.List;
import java.util.UUID;

public class ShinobiCloneEntity extends PathfinderMob {
	private static final EntityDataAccessor<Integer> CLONE_TYPE = SynchedEntityData.defineId(ShinobiCloneEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<String> OWNER_ID = SynchedEntityData.defineId(ShinobiCloneEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> ATTACK_ANIMATION_TICKS = SynchedEntityData.defineId(ShinobiCloneEntity.class, EntityDataSerializers.INT);
	private static final float LIGHTNING_STUN_RADIUS = 3.0F;
	private static final float WATER_SLOW_RADIUS = 3.2F;
	private static final float EXPLOSION_RADIUS = 2.4F;
	private int lifeTicks;
	private boolean triggeredTypeEffect;

	public ShinobiCloneEntity(EntityType<ShinobiCloneEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
		setNoAi(false);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CLONE_TYPE, CloneType.SHADOW.ordinal());
		builder.define(OWNER_ID, "");
		builder.define(ATTACK_ANIMATION_TICKS, 0);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, true) {
			@Override
			protected boolean canPerformAttack(LivingEntity target) {
				boolean canAttack = super.canPerformAttack(target);
				if (canAttack) {
					ShinobiCloneEntity.this.startAttackAnimation();
				}
				return canAttack;
			}
		});
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.9D));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true, target -> target != this && target != getOwner()));
	}

	@Override
	public void tick() {
		super.tick();
		if (getAttackAnimationTicks() > 0) {
			this.entityData.set(ATTACK_ANIMATION_TICKS, getAttackAnimationTicks() - 1);
		}
		if (!this.level().isClientSide()) {
			lifeTicks++;
			LivingEntity owner = getOwner();
			if (lifeTicks > getCloneType().lifetimeTicks || getOwnerUUID() != null && (owner == null || !owner.isAlive())) {
				poofAndDiscard(false);
				return;
			}
			if (lifeTicks % 20 == 0 && !payChakraUpkeep(owner)) {
				poofAndDiscard(false);
				return;
			}
			updateTargetFromOwner();
			if (getCloneType() == CloneType.EXPLOSION && getTarget() != null && this.distanceToSqr(getTarget()) < 4.0D) {
				triggerExplosion();
				discard();
			}
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		Entity attacker = source.getEntity();
		if (attacker != null && attacker.getUUID().equals(getOwnerUUID())) {
			return false;
		}
		boolean result = super.hurt(source, amount);
		if (result && !this.level().isClientSide() && this.isAlive()) {
			CloneType type = getCloneType();
			if (type.popsOnDamage) {
				triggerDefeatEffect();
				poofAndDiscard(type == CloneType.LIGHTNING || type == CloneType.EXPLOSION || type == CloneType.WATER);
			}
		}
		return result;
	}

	@Override
	public void die(DamageSource source) {
		if (!this.level().isClientSide()) {
			triggerDefeatEffect();
		}
		super.die(source);
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		if (target instanceof LivingEntity living && getOwnerUUID() != null && living.getUUID().equals(getOwnerUUID())) {
			return false;
		}
		boolean result = super.doHurtTarget(target);
		if (result) {
			startAttackAnimation();
			this.swing(InteractionHand.MAIN_HAND, true);
			if (!this.level().isClientSide() && getCloneType() == CloneType.EXPLOSION) {
				triggerExplosion();
				discard();
			}
		}
		return result;
	}

	@Override
	public SoundEvent getHurtSound(DamageSource source) {
		return null;
	}

	@Override
	public SoundEvent getDeathSound() {
		return null;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putInt("CloneType", getCloneType().ordinal());
		compound.putString("OwnerId", this.entityData.get(OWNER_ID));
		compound.putInt("LifeTicks", lifeTicks);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		setCloneType(CloneType.byOrdinal(compound.getInt("CloneType")));
		if (compound.contains("OwnerId")) {
			this.entityData.set(OWNER_ID, compound.getString("OwnerId"));
		}
		lifeTicks = compound.getInt("LifeTicks");
	}

	public void setCloneType(CloneType type) {
		this.entityData.set(CLONE_TYPE, type.ordinal());
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(type.maxHealth);
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(type.attackDamage);
		this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(type.movementSpeed);
		this.getAttribute(Attributes.ARMOR).setBaseValue(type.armor);
		this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(type.knockbackResistance);
		this.setHealth((float) type.maxHealth);
	}

	public CloneType getCloneType() {
		return CloneType.byOrdinal(this.entityData.get(CLONE_TYPE));
	}

	public int getAttackAnimationTicks() {
		return this.entityData.get(ATTACK_ANIMATION_TICKS);
	}

	public void startAttackAnimation() {
		this.entityData.set(ATTACK_ANIMATION_TICKS, 8);
	}

	public void setOwner(LivingEntity owner) {
		this.entityData.set(OWNER_ID, owner == null ? "" : owner.getUUID().toString());
	}

	public UUID getOwnerUUID() {
		String ownerId = this.entityData.get(OWNER_ID);
		if (ownerId == null || ownerId.isEmpty()) {
			return null;
		}
		try {
			return UUID.fromString(ownerId);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	public LivingEntity getOwner() {
		UUID ownerId = getOwnerUUID();
		if (ownerId == null || !(this.level() instanceof ServerLevel serverLevel)) {
			return null;
		}
		if (serverLevel.getPlayerByUUID(ownerId) instanceof LivingEntity player) {
			return player;
		}
		Entity owner = serverLevel.getEntity(ownerId);
		return owner instanceof LivingEntity living ? living : null;
	}

	private void updateTargetFromOwner() {
		LivingEntity owner = getOwner();
		if (owner == null) {
			return;
		}
		LivingEntity ownerTarget = owner.getLastHurtMob();
		if (ownerTarget == null || !ownerTarget.isAlive()) {
			ownerTarget = owner.getLastHurtByMob();
		}
		if (ownerTarget != null && ownerTarget.isAlive() && ownerTarget != owner) {
			this.setTarget(ownerTarget);
		}
	}

	private boolean payChakraUpkeep(LivingEntity owner) {
		if (owner == null || getOwnerUUID() == null || getCloneType().upkeepChakra <= 0.0D) {
			return true;
		}
		NarutoModVariables.PlayerVariables vars = owner.getData(NarutoModVariables.PLAYER_VARIABLES);
		if (vars.currentChakra < getCloneType().upkeepChakra) {
			return false;
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - getCloneType().upkeepChakra);
		vars.markSyncDirty();
		return true;
	}

	private void triggerDefeatEffect() {
		switch (getCloneType()) {
			case LIGHTNING -> triggerLightningStun();
			case WATER -> triggerWaterSlow();
			case EXPLOSION -> triggerExplosion();
			default -> {
			}
		}
	}

	private void triggerLightningStun() {
		if (triggeredTypeEffect || !(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		triggeredTypeEffect = true;
		Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
		serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 32, 0.65D, 0.45D, 0.65D, 0.08D);
		serverLevel.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		AABB area = this.getBoundingBox().inflate(LIGHTNING_STUN_RADIUS);
		List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, area, target -> target.isAlive() && target != this && !target.getUUID().equals(getOwnerUUID()));
		for (LivingEntity target : victims) {
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 4, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45, 1, true, true));
			target.hurt(this.damageSources().mobAttack(this), 3.0F);
			target.setDeltaMovement(target.getDeltaMovement().scale(0.25D));
		}
	}

	private void triggerWaterSlow() {
		if (triggeredTypeEffect || !(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		triggeredTypeEffect = true;
		Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.45D, 0.0D);
		serverLevel.sendParticles(ParticleTypes.SPLASH, center.x, center.y, center.z, 42, 0.75D, 0.35D, 0.75D, 0.12D);
		serverLevel.sendParticles(ParticleTypes.BUBBLE, center.x, center.y, center.z, 24, 0.55D, 0.28D, 0.55D, 0.04D);
		AABB area = this.getBoundingBox().inflate(WATER_SLOW_RADIUS);
		List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, area, target -> target.isAlive() && target != this && !target.getUUID().equals(getOwnerUUID()));
		for (LivingEntity target : victims) {
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 70, 0, true, true));
		}
	}

	private void triggerExplosion() {
		if (triggeredTypeEffect || !(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		triggeredTypeEffect = true;
		Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.35D, 0.0D);
		serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		serverLevel.explode(this, center.x, center.y, center.z, EXPLOSION_RADIUS, Level.ExplosionInteraction.NONE);
	}

	private void poofAndDiscard(boolean lightningOnly) {
		if (this.level() instanceof ServerLevel serverLevel) {
			Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.45D, 0.0D);
			if (!lightningOnly) {
				serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 16, 0.35D, 0.45D, 0.35D, 0.03D);
				serverLevel.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 6, 0.22D, 0.28D, 0.22D, 0.015D);
			}
		}
		this.discard();
	}

	public static void init(RegisterSpawnPlacementsEvent event) {
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.32D)
				.add(Attributes.MAX_HEALTH, CloneType.SHADOW.maxHealth)
				.add(Attributes.ARMOR, 0.0D)
				.add(Attributes.ATTACK_DAMAGE, CloneType.SHADOW.attackDamage)
				.add(Attributes.FOLLOW_RANGE, 24.0D)
				.add(Attributes.STEP_HEIGHT, 0.6D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.2D);
	}

	public enum CloneType {
		SHADOW(8.0D, 3.0D, 0.32D, 0.0D, 0.2D, 20 * 25, 0.35D, true),
		WATER(7.0D, 2.5D, 0.31D, 0.0D, 0.15D, 20 * 22, 0.25D, true),
		LIGHTNING(8.0D, 3.0D, 0.34D, 0.0D, 0.2D, 20 * 20, 0.45D, true),
		WOOD(22.0D, 4.0D, 0.28D, 3.0D, 0.55D, 20 * 35, 0.55D, false),
		EXPLOSION(6.0D, 2.0D, 0.36D, 0.0D, 0.1D, 20 * 16, 0.4D, true);

		private final double maxHealth;
		private final double attackDamage;
		private final double movementSpeed;
		private final double armor;
		private final double knockbackResistance;
		private final int lifetimeTicks;
		private final double upkeepChakra;
		private final boolean popsOnDamage;

		CloneType(double maxHealth, double attackDamage, double movementSpeed, double armor, double knockbackResistance, int lifetimeTicks, double upkeepChakra, boolean popsOnDamage) {
			this.maxHealth = maxHealth;
			this.attackDamage = attackDamage;
			this.movementSpeed = movementSpeed;
			this.armor = armor;
			this.knockbackResistance = knockbackResistance;
			this.lifetimeTicks = lifetimeTicks;
			this.upkeepChakra = upkeepChakra;
			this.popsOnDamage = popsOnDamage;
		}

		public static CloneType byOrdinal(int ordinal) {
			CloneType[] values = values();
			return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SHADOW;
		}
	}
}
