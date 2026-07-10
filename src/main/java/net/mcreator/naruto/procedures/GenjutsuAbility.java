package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.naruto.network.SharinganGenjutsuHitPacket;
import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.init.NarutoModMobEffects;
import net.mcreator.naruto.JutsuData;

import java.util.Comparator;
import java.util.List;

public class GenjutsuAbility {
	private static final String SHARINGAN_ID = "genjutsu";
	private static final String YIN_ID = "yin_genjutsu";

	public static boolean tryRelease(Entity entity, JutsuData jutsuData, String jutsuId) {
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
		if (!JutsuCooldowns.tryStart(player, jutsuId, Math.max(1, jutsuData.getCooldown()))) {
			JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, jutsuId));
			return true;
		}
		if (!(player.level() instanceof ServerLevel level)) {
			return true;
		}
		boolean sharingan = SHARINGAN_ID.equals(jutsuId);
		LivingEntity target = findTarget(level, player, sharingan ? Mth.lerp(chargePercent, 10.0D, 20.0D) : Mth.lerp(chargePercent, 7.0D, 13.0D), sharingan ? 0.72D : 0.48D);
		if (target == null && sharingan && player.isShiftKeyDown()) {
			target = player;
		}
		if (target == null) {
			JutsuFeedback.show(player, "No target in sight");
			return true;
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		apply(level, player, target, chargePercent, sharingan);
		return true;
	}

	private static void apply(ServerLevel level, Player caster, LivingEntity target, float chargePercent, boolean sharingan) {
		int duration = sharingan ? 120 + Mth.floor(chargePercent * 100.0F) : 60 + Mth.floor(chargePercent * 55.0F);
		int amplifier = sharingan ? 3 : 0;
		target.addEffect(new MobEffectInstance(NarutoModMobEffects.GENJUTSU, duration, amplifier, false, true));
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, sharingan ? 2 : 0, false, true));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, sharingan ? 1 : 0, false, true));
		target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, sharingan ? Math.min(duration, 70) : Math.min(duration, 30), 0, false, true));
		if (sharingan) {
			target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, Math.min(duration, 120), 0, false, true));
		}
		Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
		level.sendParticles(sharingan ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, center.x, center.y, center.z, sharingan ? 32 : 14, 0.35D, 0.35D, 0.35D, 0.03D);
		level.sendParticles(ParticleTypes.CRIMSON_SPORE, center.x, center.y + 0.15D, center.z, sharingan ? 36 : 8, 0.5D, 0.45D, 0.5D, 0.02D);
		level.playSound(null, target.blockPosition(), sharingan ? SoundEvents.ELDER_GUARDIAN_CURSE : SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, sharingan ? 0.72F : 0.45F, sharingan ? 0.62F : 0.92F);
		level.playSound(null, caster.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.55F, sharingan ? 0.55F : 0.9F);
		if (sharingan && target instanceof ServerPlayer targetPlayer) {
			PacketDistributor.sendToPlayer(targetPlayer, new SharinganGenjutsuHitPacket(Math.min(duration, 110)));
		}
		JutsuFeedback.show(caster, sharingan ? "Sharingan Genjutsu" : "Genjutsu");
	}

	private static LivingEntity findTarget(ServerLevel level, Player player, double range, double forgiveness) {
		Vec3 origin = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		BlockHitResult blockHit = level.clip(new ClipContext(origin, origin.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		double maxRange = blockHit.getType() == HitResult.Type.BLOCK ? origin.distanceTo(blockHit.getLocation()) : range;
		AABB area = player.getBoundingBox().expandTowards(look.scale(maxRange)).inflate(1.35D + forgiveness);
		List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area, target -> target.isAlive() && target != player && !target.isSpectator());
		return candidates.stream().filter(target -> canSee(origin, look, maxRange, forgiveness, target)).min(Comparator.comparingDouble(target -> origin.distanceToSqr(target.getEyePosition()))).orElse(null);
	}

	private static boolean canSee(Vec3 origin, Vec3 look, double range, double forgiveness, LivingEntity target) {
		Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.58D, 0.0D);
		Vec3 offset = center.subtract(origin);
		double forward = offset.dot(look);
		if (forward < 0.0D || forward > range) {
			return false;
		}
		Vec3 closest = origin.add(look.scale(forward));
		double allowed = target.getBbWidth() * 0.65D + forgiveness;
		return center.distanceTo(closest) <= allowed;
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}
}
