package net.mcreator.naruto.procedures;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.entity.ElementalBulletEntity;
import net.mcreator.naruto.init.NarutoModEntities;
import net.mcreator.naruto.network.NarutoModVariables;

public class ElementalBulletAbility {
	private static final double SPAWN_FORWARD_DISTANCE = 0.72D;
	private static final int SHOT_DELAY_TICKS = 3;
	private static final int MAX_BULLETS = 6;

	public static boolean tryRelease(Entity entity, JutsuData jutsuData, int element, String jutsuId) {
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
		if (!JutsuCooldowns.tryStart(player, jutsuId, cooldown)) {
			JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, jutsuId));
			return true;
		}
		if (player.level() instanceof ServerLevel serverLevel) {
			shootBurst(serverLevel, player, chargePercent, element);
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		return true;
	}

	private static void shootBurst(ServerLevel level, Player player, float chargePercent, int element) {
		int count = 1 + Mth.floor(chargePercent * (MAX_BULLETS - 1));
		double speed = Mth.lerp(chargePercent, 1.05D, 1.55D);
		float baseDamage = element == ElementalBulletEntity.ELEMENT_WIND ? 4.6F : 4.2F;
		float damage = NarutoDamageFormula.calculateJutsuDamage(player instanceof LivingEntity livingPlayer ? livingPlayer : null, baseDamage, chargePercent, NarutoDamageFormula.TYPE_SCALE_MULTI_HIT_HIGH);
		for (int i = 0; i < count; i++) {
			int delay = i * SHOT_DELAY_TICKS;
			NarutoMod.queueServerWork(delay, () -> shootSingle(level, player, chargePercent, element, speed, damage));
		}
	}

	private static void shootSingle(ServerLevel level, Player player, float chargePercent, int element, double speed, float damage) {
		if (player == null || !player.isAlive() || player.level() != level) {
			return;
		}
		Vec3 look = player.getLookAngle();
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		look = look.normalize();
		Vec3 origin = player.getEyePosition().add(0.0D, -0.14D, 0.0D).add(look.scale(SPAWN_FORWARD_DISTANCE));
		ElementalBulletEntity bullet = new ElementalBulletEntity(NarutoModEntities.ELEMENTAL_BULLET.get(), level);
		bullet.setOwner(player);
		bullet.setElement(element);
		bullet.setChargePercent(chargePercent);
		bullet.setDamage(damage);
		bullet.setPos(origin.x, origin.y, origin.z);
		bullet.setDeltaMovement(look.scale(speed));
		bullet.setYRot(player.getYRot());
		bullet.setXRot(player.getXRot());
		level.addFreshEntity(bullet);
		if (element == ElementalBulletEntity.ELEMENT_WATER) {
			level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.55F, 1.35F + chargePercent * 0.25F);
		} else {
			level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 0.9F + chargePercent * 0.35F);
		}
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}
}
