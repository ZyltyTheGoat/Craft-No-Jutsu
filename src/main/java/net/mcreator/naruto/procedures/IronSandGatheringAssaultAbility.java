package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.MagnetSandReserve;
import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.network.IronSandGatheringAssaultPacket;
import net.mcreator.naruto.network.NarutoModVariables;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = NarutoMod.MODID)
public final class IronSandGatheringAssaultAbility {
	private static final String JUTSU_ID = "iron_sand_gathering_assault";
	private static final double MIN_SAND_COST = 28.0D;
	private static final double MAX_SAND_COST = 42.0D;
	private static final double RETURN_RATE = 0.42D;
	private static final double MIN_RANGE = 8.0D;
	private static final double MAX_RANGE = 15.0D;
	private static final double MIN_RADIUS = 0.92D;
	private static final double MAX_RADIUS = 1.48D;
	private static final float MIN_DAMAGE = 10.0F;
	private static final float MAX_DAMAGE = 16.0F;
	private static final int FORMATION_TICKS = 14;
	private static final int MIN_TRAVEL_TICKS = 13;
	private static final int MAX_TRAVEL_TICKS = 19;
	private static final double HOMING_STRENGTH = 0.075D;
	private static final Map<UUID, ActiveAssault> ACTIVE = new HashMap<>();

	private IronSandGatheringAssaultAbility() {
	}

	public static boolean tryRelease(Entity entity, JutsuData jutsuData) {
		if (!(entity instanceof ServerPlayer player) || jutsuData == null) {
			return false;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		float chargePercent = Mth.clamp((float) (vars.chargeTime / Math.max(1.0D, jutsuData.getChargeTime())), 0.0F, 1.0F);
		clearCharge(player);
		if (vars.currentChakra < jutsuData.getChakraCost()) {
			JutsuFeedback.notEnoughChakra(player);
			return true;
		}
		double sandCost = Mth.lerp(chargePercent, MIN_SAND_COST, MAX_SAND_COST);
		if (MagnetSandReserve.get(player) < sandCost) {
			JutsuFeedback.show(player, "Iron Sand Gathering Assault requires " + (int) Math.ceil(sandCost) + " sand", 60);
			return true;
		}
		if (!JutsuCooldowns.tryStart(player, JUTSU_ID, Math.max(1, jutsuData.getCooldown()))) {
			JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, JUTSU_ID));
			return true;
		}
		MagnetSandReserve.tryConsume(player, sandCost);
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		release(player, chargePercent, sandCost);
		return true;
	}

	private static void release(ServerPlayer player, float chargePercent, double sandCost) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		Vec3 direction = safeLook(player);
		Vec3 origin = player.getEyePosition().add(direction.scale(1.35D)).add(0.0D, 0.18D, 0.0D);
		double range = Mth.lerp(chargePercent, MIN_RANGE, MAX_RANGE);
		double radius = Mth.lerp(chargePercent, MIN_RADIUS, MAX_RADIUS);
		LivingEntity target = acquireTarget(level, player, direction, range + 4.0D);
		int travelTicks = Mth.floor(Mth.lerp(chargePercent, MIN_TRAVEL_TICKS, MAX_TRAVEL_TICKS));
		float damage = NarutoDamageFormula.calculateJutsuDamage(player, Mth.lerp(chargePercent, MIN_DAMAGE, MAX_DAMAGE), chargePercent, NarutoDamageFormula.TYPE_SCALE_HEAVY_CHARGED_PROJECTILE);
		int targetId = target == null ? -1 : target.getId();
		ACTIVE.put(player.getUUID(), new ActiveAssault(origin, direction, range, radius, damage, chargePercent, travelTicks, sandCost, targetId));
		PacketDistributor.sendToPlayersInDimension(level,
				new IronSandGatheringAssaultPacket(player.getUUID(), targetId, origin.x, origin.y, origin.z, direction.x, direction.y, direction.z, (float) range, (float) radius, chargePercent, FORMATION_TICKS, travelTicks));
		level.playSound(null, player.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.0F, 0.52F + chargePercent * 0.12F);
		level.playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 0.72F, 0.58F);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		ActiveAssault assault = ACTIVE.get(player.getUUID());
		if (assault == null) {
			return;
		}
		if (!(player.level() instanceof ServerLevel level) || !player.isAlive()) {
			finish(player, assault);
			return;
		}
		assault.age++;
		if (assault.age == FORMATION_TICKS) {
			level.playSound(null, BlockPos.containing(assault.origin), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.56F, 1.55F);
		}
		if (!assault.impacted && assault.age >= FORMATION_TICKS && assault.age <= FORMATION_TICKS + assault.travelTicks) {
			tickWeapon(level, player, assault);
		}
		if (assault.impacted || assault.age > FORMATION_TICKS + assault.travelTicks + 2) {
			finish(player, assault);
		}
	}

	private static void tickWeapon(ServerLevel level, ServerPlayer player, ActiveAssault assault) {
		Entity target = assault.targetId < 0 ? null : level.getEntity(assault.targetId);
		if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
			Vec3 targetCenter = livingTarget.position().add(0.0D, livingTarget.getBbHeight() * 0.55D, 0.0D);
			Vec3 desiredDirection = targetCenter.subtract(assault.position);
			if (desiredDirection.lengthSqr() > 0.001D) {
				assault.direction = steer(assault.direction, desiredDirection.normalize(), HOMING_STRENGTH);
			}
		}
		double step = Math.min(assault.range - assault.distanceTravelled, assault.range / assault.travelTicks);
		if (step <= 0.001D) {
			impact(level, assault, false);
			return;
		}
		Vec3 previous = assault.position;
		Vec3 candidate = previous.add(assault.direction.scale(step));
		BlockHitResult blockHit = level.clip(new ClipContext(previous, candidate, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (blockHit.getType() == HitResult.Type.BLOCK) {
			assault.position = blockHit.getLocation();
			assault.distanceTravelled += previous.distanceTo(assault.position);
			damageTargets(level, player, assault, previous.subtract(assault.direction.scale(assault.radius * 1.8D)), assault.position);
			impact(level, assault, true);
			return;
		}
		assault.position = candidate;
		assault.distanceTravelled += step;
		damageTargets(level, player, assault, previous.subtract(assault.direction.scale(assault.radius * 1.8D)), candidate);
		if (assault.distanceTravelled >= assault.range - 0.001D) {
			impact(level, assault, false);
		}
	}

	private static void damageTargets(ServerLevel level, ServerPlayer player, ActiveAssault assault, Vec3 start, Vec3 end) {
		Vec3 segment = end.subtract(start);
		double segmentLengthSqr = segment.lengthSqr();
		if (segmentLengthSqr < 0.0001D) {
			return;
		}
		AABB area = new AABB(start, end).inflate(assault.radius + 1.2D);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, target -> target.isAlive() && target != player && !target.isSpectator());
		for (LivingEntity target : targets) {
			Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
			double progress = Mth.clamp(center.subtract(start).dot(segment) / segmentLengthSqr, 0.0D, 1.0D);
			Vec3 closest = start.add(segment.scale(progress));
			double allowedRadius = assault.radius + target.getBbWidth() * 0.55D;
			if (center.distanceToSqr(closest) > allowedRadius * allowedRadius || !assault.hitEntities.add(target.getId())) {
				continue;
			}
			target.invulnerableTime = 0;
			target.hurt(player.damageSources().playerAttack(player), assault.damage);
			target.invulnerableTime = 0;
			target.setDeltaMovement(target.getDeltaMovement().scale(0.2D).add(assault.direction.scale(0.72D + assault.chargePercent * 0.62D)).add(0.0D, 0.16D, 0.0D));
			target.hurtMarked = true;
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 36, 1, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 36, 1, true, true));
			level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 14, assault.radius * 0.25D, target.getBbHeight() * 0.24D, assault.radius * 0.25D, 0.08D);
			level.playSound(null, target.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 0.9F, 0.72F);
		}
	}

	private static LivingEntity acquireTarget(ServerLevel level, ServerPlayer player, Vec3 direction, double range) {
		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(direction.scale(range));
		AABB searchArea = new AABB(start, end).inflate(1.5D);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, start, end, searchArea,
				candidate -> candidate instanceof LivingEntity living && living.isAlive() && candidate.isPickable() && candidate != player);
		if (entityHit == null || !(entityHit.getEntity() instanceof LivingEntity target)) {
			return null;
		}
		BlockHitResult blockHit = level.clip(new ClipContext(start, entityHit.getLocation(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		return blockHit.getType() == HitResult.Type.MISS ? target : null;
	}

	private static Vec3 steer(Vec3 current, Vec3 desired, double strength) {
		Vec3 steered = current.scale(1.0D - strength).add(desired.scale(strength));
		return steered.lengthSqr() < 0.001D ? current : steered.normalize();
	}

	private static void impact(ServerLevel level, ActiveAssault assault, boolean hitBlock) {
		if (assault.impacted) {
			return;
		}
		assault.impacted = true;
		assault.hitBlock = hitBlock;
		spawnImpact(level, assault.position, assault);
	}

	private static void spawnImpact(ServerLevel level, Vec3 impact, ActiveAssault assault) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()), impact.x, impact.y, impact.z, 34, assault.radius * 0.5D, assault.radius * 0.5D, assault.radius * 0.5D, 0.16D);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, impact.x, impact.y, impact.z, 10, assault.radius * 0.34D, assault.radius * 0.34D, assault.radius * 0.34D, 0.06D);
		level.playSound(null, BlockPos.containing(impact), assault.hitBlock ? SoundEvents.ANVIL_LAND : SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.15F, 0.58F);
	}

	private static void finish(ServerPlayer player, ActiveAssault assault) {
		if (ACTIVE.remove(player.getUUID(), assault)) {
			MagnetSandReserve.add(player, assault.sandCost * RETURN_RATE);
		}
	}

	private static Vec3 safeLook(ServerPlayer player) {
		Vec3 look = player.getLookAngle();
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		return look.normalize();
	}

	private static void clearCharge(ServerPlayer player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}

	private static final class ActiveAssault {
		private final Vec3 origin;
		private Vec3 direction;
		private Vec3 position;
		private final double range;
		private final double radius;
		private final float damage;
		private final float chargePercent;
		private final int travelTicks;
		private final double sandCost;
		private final int targetId;
		private final Set<Integer> hitEntities = new HashSet<>();
		private double distanceTravelled;
		private int age;
		private boolean impacted;
		private boolean hitBlock;

		private ActiveAssault(Vec3 origin, Vec3 direction, double range, double radius, float damage, float chargePercent, int travelTicks, double sandCost, int targetId) {
			this.origin = origin;
			this.direction = direction;
			this.position = origin;
			this.range = range;
			this.radius = radius;
			this.damage = damage;
			this.chargePercent = chargePercent;
			this.travelTicks = Math.max(1, travelTicks);
			this.sandCost = sandCost;
			this.targetId = targetId;
		}
	}
}
