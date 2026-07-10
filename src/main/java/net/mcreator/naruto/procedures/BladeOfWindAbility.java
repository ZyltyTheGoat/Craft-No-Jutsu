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
import net.mcreator.naruto.network.BladeOfWindSlashPacket;
import net.mcreator.naruto.network.NarutoModVariables;

import java.util.List;

public class BladeOfWindAbility {
	private static final String JUTSU_ID = "wind_blade";
	private static final float RANGE = 5.6F;
	private static final float WIDTH = 1.35F;
	private static final float BASE_DAMAGE = 7.0F;

	public static boolean tryUse(Entity entity, JutsuData jutsuData) {
		if (!(entity instanceof Player player) || jutsuData == null) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		clearCharge(player);
		if (vars.currentChakra < jutsuData.getChakraCost()) {
			JutsuFeedback.notEnoughChakra(player);
			return true;
		}
		if (!JutsuCooldowns.tryStart(player, JUTSU_ID, Math.max(1, jutsuData.getCooldown()))) {
			JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, JUTSU_ID));
			return true;
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		if (player.level() instanceof ServerLevel level) {
			release(level, player);
		}
		return true;
	}

	private static void release(ServerLevel level, Player player) {
		Vec3 look = safeLook(player);
		Vec3 origin = player.getEyePosition().add(look.scale(0.55D)).add(0.0D, -0.16D, 0.0D);
		float roll = player.getRandom().nextBoolean() ? 0.28F : -0.28F;
		damageTargets(level, player, origin, look);
		spawnServerParticles(level, origin, look);
		level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.45F);
		level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.55F, 1.65F);
		PacketDistributor.sendToPlayersInDimension(level, new BladeOfWindSlashPacket(origin.x, origin.y, origin.z, look.x, look.y, look.z, RANGE, WIDTH, roll, 12));
	}

	private static void damageTargets(ServerLevel level, Player player, Vec3 origin, Vec3 look) {
		Vec3 end = origin.add(look.scale(RANGE));
		AABB searchArea = new AABB(origin, end).inflate(WIDTH + 0.9D);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchArea, target -> target.isAlive() && target != player && !target.isSpectator());
		float damage = NarutoDamageFormula.calculateInstantJutsuDamage(player, BASE_DAMAGE, 1.05F);
		double minDot = Math.cos(Math.toRadians(33.0D));
		for (LivingEntity target : targets) {
			Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
			Vec3 offset = center.subtract(origin);
			double distance = offset.length();
			if (distance < 0.1D || distance > RANGE + target.getBbWidth()) {
				continue;
			}
			Vec3 direction = offset.normalize();
			if (direction.dot(look) < minDot) {
				continue;
			}
			double forward = offset.dot(look);
			Vec3 closest = origin.add(look.scale(Mth.clamp(forward, 0.0D, RANGE)));
			double allowed = WIDTH * (0.46D + 0.32D * (1.0D - forward / RANGE)) + target.getBbWidth() * 0.7D;
			if (center.distanceTo(closest) > allowed) {
				continue;
			}
			double falloff = 1.0D - Mth.clamp(forward / Math.max(1.0D, RANGE), 0.0D, 1.0D) * 0.18D;
			target.invulnerableTime = 0;
			target.hurt(player.damageSources().playerAttack(player), (float) (damage * falloff));
			target.invulnerableTime = 0;
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 34, 0, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 18, 0, true, true));
			target.setDeltaMovement(target.getDeltaMovement().scale(0.42D).add(look.scale(0.48D).add(0.0D, 0.08D, 0.0D)));
			target.hurtMarked = true;
			level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 5, 0.22D, 0.16D, 0.22D, 0.07D);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.1D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}

	private static void spawnServerParticles(ServerLevel level, Vec3 origin, Vec3 look) {
		for (int i = 0; i < 5; i++) {
			double t = (i + 1.0D) / 5.0D;
			Vec3 pos = origin.add(look.scale(RANGE * t));
			double spread = 0.08D + t * 0.28D;
			level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 2, spread, spread * 0.45D, spread, 0.035D);
			if (i % 2 == 0) {
				level.sendParticles(ParticleTypes.POOF, pos.x, pos.y, pos.z, 1, spread * 0.45D, spread * 0.25D, spread * 0.45D, 0.02D);
			}
		}
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, origin.x + look.x * 1.1D, origin.y + look.y * 1.1D, origin.z + look.z * 1.1D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
	}

	private static Vec3 safeLook(Player player) {
		Vec3 look = player.getLookAngle();
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		return look.normalize();
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}
}
