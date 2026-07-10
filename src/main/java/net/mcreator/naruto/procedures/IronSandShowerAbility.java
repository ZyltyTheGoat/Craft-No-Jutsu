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
import net.mcreator.naruto.network.IronSandShowerPacket;
import net.mcreator.naruto.network.NarutoModVariables;

import java.util.List;

public final class IronSandShowerAbility {
	private static final String JUTSU_ID = "iron_sand_shower";
	private static final double MIN_SAND_COST = 16.0D;
	private static final double MAX_SAND_COST = 25.0D;
	private static final double RETURN_RATE = 0.48D;
	private static final float MIN_RANGE = 7.0F;
	private static final float MAX_RANGE = 13.0F;
	private static final float MIN_SPREAD_DEGREES = 8.0F;
	private static final float MAX_SPREAD_DEGREES = 17.0F;
	private static final int MIN_NEEDLES = 22;
	private static final int MAX_NEEDLES = 54;

	private IronSandShowerAbility() {
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
			JutsuFeedback.show(player, "Iron Sand Shower requires " + (int) Math.ceil(sandCost) + " sand", 60);
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
		releaseShower(player, chargePercent, sandCost);
		return true;
	}

	private static void releaseShower(ServerPlayer player, float chargePercent, double sandCost) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		Vec3 look = player.getLookAngle();
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		look = look.normalize();
		float range = Mth.lerp(chargePercent, MIN_RANGE, MAX_RANGE);
		float spread = Mth.lerp(chargePercent, MAX_SPREAD_DEGREES, MIN_SPREAD_DEGREES);
		int needleCount = Mth.floor(Mth.lerp(chargePercent, MIN_NEEDLES, MAX_NEEDLES));
		Vec3 origin = player.getEyePosition().add(look.scale(0.6D)).add(0.0D, 0.12D, 0.0D);
		damageTargets(level, player, origin, look, range, spread, needleCount, chargePercent);
		MagnetSandReserve.add(player, sandCost * RETURN_RATE);
		level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.85F, 0.58F + chargePercent * 0.22F);
		level.playSound(null, player.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.72F, 0.78F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), origin.x, origin.y, origin.z, 14, 0.32D, 0.22D, 0.32D, 0.06D);
		PacketDistributor.sendToPlayersInDimension(level, new IronSandShowerPacket(player.getUUID(), origin.x, origin.y, origin.z, look.x, look.y, look.z, range, spread, needleCount, 24));
	}

	private static void damageTargets(ServerLevel level, Player player, Vec3 origin, Vec3 look, float range, float spreadDegrees, int needleCount, float chargePercent) {
		Vec3 end = origin.add(look.scale(range));
		double maxRadius = Math.tan(Math.toRadians(spreadDegrees)) * range + 1.25D;
		AABB searchArea = new AABB(origin, end).inflate(maxRadius);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchArea, target -> target.isAlive() && target != player && !target.isSpectator());
		double minDot = Math.cos(Math.toRadians(spreadDegrees));
		float baseDamage = NarutoDamageFormula.calculateJutsuDamage(player, 2.45F, chargePercent, NarutoDamageFormula.TYPE_SCALE_MULTI_HIT_LOW);
		for (LivingEntity target : targets) {
			Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
			Vec3 offset = center.subtract(origin);
			double distance = offset.length();
			if (distance < 0.25D || distance > range) {
				continue;
			}
			Vec3 direction = offset.normalize();
			double dot = direction.dot(look);
			if (dot < minDot || isBlocked(level, origin, center, target)) {
				continue;
			}
			double distanceFalloff = 1.0D - Mth.clamp(distance / Math.max(1.0D, range), 0.0D, 1.0D) * 0.28D;
			double angleFocus = Mth.clamp((dot - minDot) / Math.max(0.001D, 1.0D - minDot), 0.0D, 1.0D);
			int hits = Mth.clamp(2 + Mth.floor(angleFocus * 3.0D + chargePercent * 2.0F), 1, Math.max(2, needleCount / 8));
			float damage = (float) (baseDamage * hits * distanceFalloff);
			target.invulnerableTime = 0;
			target.hurt(player.damageSources().playerAttack(player), damage);
			target.invulnerableTime = 0;
			target.setDeltaMovement(target.getDeltaMovement().scale(0.45D).add(look.scale(0.18D + chargePercent * 0.15D)).add(0.0D, 0.04D, 0.0D));
			target.hurtMarked = true;
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 26, 1, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 24, 0, true, true));
		}
	}

	private static boolean isBlocked(ServerLevel level, Vec3 origin, Vec3 center, LivingEntity target) {
		BlockHitResult hit = level.clip(new ClipContext(origin, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
		return hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceToSqr(origin) + 0.18D < center.distanceToSqr(origin);
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}
}
