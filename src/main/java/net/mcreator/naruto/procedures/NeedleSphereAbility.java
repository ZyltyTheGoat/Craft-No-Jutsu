package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.network.PacketDistributor;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.MagnetSandReserve;
import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.network.NeedleSpherePacket;

import java.util.List;

public final class NeedleSphereAbility {
	private static final String JUTSU_ID = "needle_sphere";
	private static final double MIN_SAND_COST = 18.0D;
	private static final double MAX_SAND_COST = 32.0D;
	private static final double RETURN_RATE = 0.44D;
	private static final float MIN_RADIUS = 3.25F;
	private static final float MAX_RADIUS = 5.15F;
	private static final float MIN_DAMAGE = 5.5F;
	private static final float MAX_DAMAGE = 9.25F;
	private static final int EFFECT_TICKS = 48;
	private static final int DAMAGE_DELAY_TICKS = 7;

	private NeedleSphereAbility() {
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
			JutsuFeedback.show(player, "Needle Sphere requires " + (int) Math.ceil(sandCost) + " sand", 60);
			return true;
		}
		int cooldown = Math.max(1, jutsuData.getCooldown());
		if (!JutsuCooldowns.tryStart(player, JUTSU_ID, cooldown)) {
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
		float radius = Mth.lerp(chargePercent, MIN_RADIUS, MAX_RADIUS);
		float damage = NarutoDamageFormula.calculateJutsuDamage(player, Mth.lerp(chargePercent, MIN_DAMAGE, MAX_DAMAGE), chargePercent, NarutoDamageFormula.TYPE_SCALE_SIMPLE_PROJECTILE);
		MagnetSandReserve.add(player, sandCost * RETURN_RATE);
		level.playSound(null, player.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.86F, 0.62F + chargePercent * 0.16F);
		PacketDistributor.sendToPlayersInDimension(level, new NeedleSpherePacket(player.getUUID(), radius, chargePercent, EFFECT_TICKS));
		NarutoMod.queueServerWork(DAMAGE_DELAY_TICKS, () -> {
			if (!player.isAlive() || player.level() != level) {
				return;
			}
			Vec3 center = player.position().add(0.0D, player.getBbHeight() + 0.72D, 0.0D);
			float hitRadius = radius * 1.18F + 0.55F;
			damageTargets(level, player, center, hitRadius, damage, chargePercent);
			level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.66F, 0.72F + chargePercent * 0.12F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), center.x, center.y, center.z, 22, 0.48D, 0.42D, 0.48D, 0.08D);
		});
	}

	private static void damageTargets(ServerLevel level, Player player, Vec3 center, float radius, float damage, float chargePercent) {
		AABB area = new AABB(center, center).inflate(radius + 0.9D);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, target -> target.isAlive() && target != player && !target.isSpectator());
		for (LivingEntity target : targets) {
			Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
			Vec3 offset = targetCenter.subtract(center);
			double distance = offset.length();
			if (distance > radius + target.getBbWidth() * 0.5D || isBlocked(level, center, targetCenter, target)) {
				continue;
			}
			double falloff = 1.0D - Mth.clamp(distance / Math.max(1.0D, radius), 0.0D, 1.0D) * 0.32D;
			Vec3 knockback = distance > 0.001D ? offset.normalize() : player.getLookAngle();
			target.invulnerableTime = 0;
			target.hurt(player.damageSources().playerAttack(player), (float) (damage * falloff));
			target.invulnerableTime = 0;
			target.setDeltaMovement(target.getDeltaMovement().scale(0.42D).add(knockback.scale(0.34D + chargePercent * 0.18D)).add(0.0D, 0.08D, 0.0D));
			target.hurtMarked = true;
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 38, 1, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 34, 0, true, true));
		}
	}

	private static boolean isBlocked(ServerLevel level, Vec3 center, Vec3 targetCenter, LivingEntity target) {
		BlockHitResult hit = level.clip(new ClipContext(center, targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
		return hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceToSqr(center) + 0.18D < targetCenter.distanceToSqr(center);
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}
}
