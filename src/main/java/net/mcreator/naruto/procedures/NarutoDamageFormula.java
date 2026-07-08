package net.mcreator.naruto.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;

import net.mcreator.naruto.network.NarutoModVariables;

public final class NarutoDamageFormula {
	public static final float TYPE_SCALE_SIMPLE_PROJECTILE = 1.0F;
	public static final float TYPE_SCALE_HEAVY_CHARGED_PROJECTILE = 1.15F;
	public static final float TYPE_SCALE_MULTI_HIT_LOW = 0.45F;
	public static final float TYPE_SCALE_MULTI_HIT_HIGH = 0.65F;

	private NarutoDamageFormula() {
	}

	public static float calculateJutsuDamage(LivingEntity attacker, float baseDamage) {
		return calculateJutsuDamage(attacker, baseDamage, 1.0F, TYPE_SCALE_SIMPLE_PROJECTILE);
	}

	public static float calculateJutsuDamage(LivingEntity attacker, float baseDamage, float chargePercent, float typeScale) {
		float safeBaseDamage = Math.max(0.0F, baseDamage);
		float safeChargePercent = clamp(chargePercent, 0.0F, 1.0F);
		float safeTypeScale = Math.max(0.0F, typeScale);
		float rankScale = calculateRankScale(attacker);
		float chargeScale = 0.65F + 0.80F * safeChargePercent;
		return safeBaseDamage * rankScale * chargeScale * safeTypeScale;
	}

	public static float calculateInstantJutsuDamage(LivingEntity attacker, float baseDamage, float typeScale) {
		float safeBaseDamage = Math.max(0.0F, baseDamage);
		float safeTypeScale = Math.max(0.0F, typeScale);
		return safeBaseDamage * calculateRankScale(attacker) * safeTypeScale;
	}

	public static float calculateGreatFireballDamage(LivingEntity attacker, float chargePercent) {
		return calculateJutsuDamage(attacker, 14.0F, chargePercent, TYPE_SCALE_HEAVY_CHARGED_PROJECTILE);
	}

	public static float calculatePhoenixFlowerDamage(LivingEntity attacker, float chargePercent) {
		return calculateJutsuDamage(attacker, 3.5F, chargePercent, TYPE_SCALE_MULTI_HIT_HIGH);
	}

	public static float calculateChidoriDamage(LivingEntity attacker, float chargePercent) {
		return calculateJutsuDamage(attacker, 16.0F, chargePercent, 1.2F);
	}

	public static float calculateRankScale(LivingEntity attacker) {
		int strengthLevel = getStrengthLevelFromRank(attacker);
		if (strengthLevel <= 0 && attacker != null && attacker.hasEffect(MobEffects.DAMAGE_BOOST)) {
			strengthLevel = attacker.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1;
		}
		if (strengthLevel <= 0) {
			strengthLevel = 1;
		}
		return Math.max(0.75F, (float) Math.sqrt(strengthLevel / 2.0F));
	}

	private static int getStrengthLevelFromRank(LivingEntity attacker) {
		if (attacker == null) {
			return 0;
		}
		String rank = attacker.getData(NarutoModVariables.PLAYER_VARIABLES).rank;
		if ("Genin".equals(rank)) {
			return 2;
		}
		if ("Chunin".equals(rank)) {
			return 4;
		}
		if ("Jonin".equals(rank)) {
			return 11;
		}
		if ("Kage".equals(rank)) {
			return 21;
		}
		if ("Legend".equals(rank)) {
			return 30;
		}
		return 0;
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
