package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.network.VacuumWaveSlashPacket;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VacuumWaveAbility {
	private static final String JUTSU_ID = "vacuum_wave";
	private static final int SLASH_DELAY_TICKS = 3;

	public static boolean tryRelease(Entity entity, JutsuData jutsuData) {
		if (!(entity instanceof Player player) || jutsuData == null) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		float chargePercent = Mth.clamp((float) (vars.chargeTime / Math.max(1.0D, jutsuData.getChargeTime())), 0.0F, 1.0F);
		clearCharge(player);
		if (vars.currentChakra < jutsuData.getChakraCost()) {
			JutsuFeedback.notEnoughChakra(player);
			return true;
		}
		int cooldown = Math.max(1, jutsuData.getCooldown());
		if (!JutsuCooldowns.tryStart(player, JUTSU_ID, cooldown)) {
			JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, JUTSU_ID));
			return true;
		}
		if (player.level() instanceof ServerLevel serverLevel) {
			releaseSlashes(serverLevel, player, chargePercent);
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		return true;
	}

	private static void releaseSlashes(ServerLevel level, Player player, float chargePercent) {
		int slashCount = 2 + Mth.floor(chargePercent * 7.0F);
		float range = Mth.lerp(chargePercent, 6.5F, 15.5F);
		float width = Mth.lerp(chargePercent, 1.8F, 4.2F);
		float damage = NarutoDamageFormula.calculateJutsuDamage(player, 5.2F, chargePercent, NarutoDamageFormula.TYPE_SCALE_MULTI_HIT_HIGH);
		Set<Integer> hitEntityIds = new HashSet<>();
		for (int i = 0; i < slashCount; i++) {
			int index = i;
			NarutoMod.queueServerWork(index * SLASH_DELAY_TICKS, () -> releaseSingleSlash(level, player, chargePercent, range, width, damage, index, hitEntityIds));
		}
	}

	private static void releaseSingleSlash(ServerLevel level, Player player, float chargePercent, float range, float width, float damage, int index, Set<Integer> hitEntityIds) {
		if (player == null || !player.isAlive() || player.level() != level) {
			return;
		}
		Vec3 look = player.getLookAngle();
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		look = look.normalize();
		Vec3 origin = player.getEyePosition().add(look.scale(0.65D)).add(0.0D, -0.08D, 0.0D);
		float roll = (level.random.nextFloat() - 0.5F) * 3F;
		damageTargets(level, player, origin, look, range, width, damage, hitEntityIds);
		spawnServerParticles(level, origin, look, range, width, chargePercent);
		level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.72F, 1.35F + index * 0.08F + chargePercent * 0.2F);
		PacketDistributor.sendToPlayersInDimension(level, new VacuumWaveSlashPacket(origin.x, origin.y, origin.z, look.x, look.y, look.z, range, width, roll, 9 + Mth.floor(chargePercent * 3.0F)));
	}

	private static void damageTargets(ServerLevel level, Player player, Vec3 origin, Vec3 look, float range, float width, float damage, Set<Integer> hitEntityIds) {
		Vec3 end = origin.add(look.scale(range));
		AABB searchArea = new AABB(origin, end).inflate(width + 1.0D);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchArea, target -> target.isAlive() && target != player && !target.isSpectator());
		for (LivingEntity target : targets) {
			Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
			Vec3 offset = center.subtract(origin);
			double forward = offset.dot(look);
			if (forward < 0.0D || forward > range) {
				continue;
			}
			Vec3 closest = origin.add(look.scale(forward));
			double allowed = width * 0.58D + target.getBbWidth() * 0.55D;
			if (center.distanceTo(closest) > allowed) {
				continue;
			}
			if (!hitEntityIds.add(target.getId())) {
				continue;
			}
			double falloff = 1.0D - Mth.clamp(forward / Math.max(1.0D, range), 0.0D, 1.0D) * 0.18D;
			target.invulnerableTime = 0;
			target.hurt(player.damageSources().playerAttack(player), (float) (damage * falloff));
			target.invulnerableTime = 0;
			target.setDeltaMovement(target.getDeltaMovement().scale(0.45D).add(look.scale(0.45D).add(0.0D, 0.06D, 0.0D)));
			target.hurtMarked = true;
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 28, 0, true, true));
		}
	}

	private static void spawnServerParticles(ServerLevel level, Vec3 origin, Vec3 look, float range, float width, float chargePercent) {
		int steps = 4 + Mth.floor(chargePercent * 4.0F);
		for (int i = 0; i < steps; i++) {
			double t = (i + 1.0D) / steps;
			Vec3 pos = origin.add(look.scale(range * t));
			level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 1, width * 0.16D, width * 0.08D, width * 0.16D, 0.025D);
			if (i % 2 == 0) {
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			}
		}
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}
}
