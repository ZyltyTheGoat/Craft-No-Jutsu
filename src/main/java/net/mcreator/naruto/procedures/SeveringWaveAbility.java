package net.mcreator.naruto.procedures;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.init.NarutoModGameRules;
import net.mcreator.naruto.network.NarutoModVariables;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeveringWaveAbility {
	private static final String JUTSU_ID = "severing_wave";
	private static final float BASE_DAMAGE = 9.0F;
	private static final float FULL_CHARGE_DAMAGE = 17.0F;
	private static final double MIN_RANGE = 9.0D;
	private static final double MAX_RANGE = 19.0D;
	private static final double MIN_THICKNESS = 0.38D;
	private static final double MAX_THICKNESS = 0.82D;

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
			releaseWave(serverLevel, player, chargePercent);
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		return true;
	}

	private static void releaseWave(ServerLevel level, Player player, float chargePercent) {
		Vec3 look = player.getLookAngle();
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		look = look.normalize();
		Vec3 origin = player.getEyePosition().add(look.scale(0.45D)).add(0.0D, -0.08D, 0.0D);
		double range = Mth.lerp(chargePercent, MIN_RANGE, MAX_RANGE);
		BlockHitResult blockHit = level.clip(new ClipContext(origin, origin.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 end = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : origin.add(look.scale(range));
		double actualRange = origin.distanceTo(end);
		double thickness = Mth.lerp(chargePercent, MIN_THICKNESS, MAX_THICKNESS);
		float damage = scaledDamage(player, chargePercent);
		spawnMouthBurst(level, player, origin, look, chargePercent);
		spawnCuttingStream(level, origin, look, actualRange, thickness, chargePercent);
		int hitCount = damageEntities(level, player, origin, look, actualRange, thickness, damage, chargePercent);
		cutSoftBlocks(level, origin, look, actualRange, thickness, chargePercent);
		spawnImpact(level, end, look, blockHit.getType() == HitResult.Type.BLOCK, chargePercent, hitCount);
		level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.05F, 1.35F + chargePercent * 0.25F);
		level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.55F, 1.65F + chargePercent * 0.25F);
	}

	private static int damageEntities(ServerLevel level, Player player, Vec3 origin, Vec3 look, double range, double thickness, float damage, float chargePercent) {
		Vec3 end = origin.add(look.scale(range));
		AABB searchArea = new AABB(origin, end).inflate(thickness + 1.25D);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchArea, target -> target.isAlive() && target != player && !target.isSpectator());
		int hitCount = 0;
		for (LivingEntity target : targets) {
			Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
			Vec3 offset = center.subtract(origin);
			double forward = offset.dot(look);
			if (forward < 0.0D || forward > range) {
				continue;
			}
			Vec3 closest = origin.add(look.scale(forward));
			double distanceFromBeam = center.distanceTo(closest);
			double allowed = thickness + target.getBbWidth() * 0.58D;
			if (distanceFromBeam > allowed) {
				continue;
			}
			double falloff = 1.0D - Mth.clamp(forward / Math.max(1.0D, range), 0.0D, 1.0D) * 0.28D;
			float dealt = (float) (damage * falloff);
			target.hurt(player.damageSources().playerAttack(player), dealt);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35 + Mth.floor(chargePercent * 25.0F), 1, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30 + Mth.floor(chargePercent * 18.0F), 0, true, true));
			Vec3 push = look.scale(0.34D + chargePercent * 0.32D).add(0.0D, 0.05D, 0.0D);
			target.setDeltaMovement(target.getDeltaMovement().scale(0.45D).add(push));
			target.hurtMarked = true;
			spawnEntitySlice(level, center, look, chargePercent);
			hitCount++;
		}
		return hitCount;
	}

	private static float scaledDamage(Player player, float chargePercent) {
		float base = Mth.lerp(chargePercent, BASE_DAMAGE, FULL_CHARGE_DAMAGE);
		int strengthAmplifier = 0;
		MobEffectInstance strength = player.getEffect(MobEffects.DAMAGE_BOOST);
		if (strength != null) {
			strengthAmplifier = Math.max(0, strength.getAmplifier());
		}
		return base * (1.0F + strengthAmplifier * 0.333F);
	}

	private static void spawnMouthBurst(ServerLevel level, Player player, Vec3 origin, Vec3 look, float chargePercent) {
		level.sendParticles(ParticleTypes.SPLASH, origin.x, origin.y, origin.z, 18 + Mth.floor(chargePercent * 16.0F), 0.22D, 0.14D, 0.22D, 0.12D);
		level.sendParticles(ParticleTypes.BUBBLE, origin.x, origin.y, origin.z, 14 + Mth.floor(chargePercent * 12.0F), 0.18D, 0.10D, 0.18D, 0.045D);
		Vec3 chest = player.position().add(0.0D, player.getBbHeight() * 0.62D, 0.0D).add(look.scale(0.28D));
		level.sendParticles(ParticleTypes.CLOUD, chest.x, chest.y, chest.z, 5, 0.18D, 0.10D, 0.18D, 0.035D);
	}

	private static void spawnCuttingStream(ServerLevel level, Vec3 origin, Vec3 look, double range, double thickness, float chargePercent) {
		Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(look);
		if (right.lengthSqr() < 0.001D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		}
		right = right.normalize();
		Vec3 up = look.cross(right).normalize();
		int steps = Math.max(8, Mth.floor(range * 1.45D));
		for (int i = 0; i <= steps; i++) {
			double t = i / (double) steps;
			Vec3 center = origin.add(look.scale(range * t));
			double wave = Math.sin(t * Math.PI * 8.0D + level.getGameTime() * 0.15D) * thickness * 0.22D;
			Vec3 bladeA = center.add(right.scale(wave)).add(up.scale(thickness * 0.28D));
			Vec3 bladeB = center.subtract(right.scale(wave)).subtract(up.scale(thickness * 0.28D));
			level.sendParticles(ParticleTypes.SPLASH, center.x, center.y, center.z, 1, thickness * 0.18D, thickness * 0.10D, thickness * 0.18D, 0.015D);
			level.sendParticles(ParticleTypes.BUBBLE, center.x, center.y, center.z, 1, thickness * 0.12D, thickness * 0.08D, thickness * 0.12D, 0.008D);
			if (i % 2 == 0) {
				level.sendParticles(ParticleTypes.FALLING_WATER, bladeA.x, bladeA.y, bladeA.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
				level.sendParticles(ParticleTypes.FALLING_WATER, bladeB.x, bladeB.y, bladeB.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
			}
			if (chargePercent > 0.65F && i % 4 == 0) {
				Vec3 spray = center.add(right.scale((level.random.nextDouble() - 0.5D) * thickness * 1.9D)).add(up.scale((level.random.nextDouble() - 0.5D) * thickness * 1.9D));
				level.sendParticles(ParticleTypes.CLOUD, spray.x, spray.y, spray.z, 1, 0.04D, 0.04D, 0.04D, 0.025D);
			}
		}
	}

	private static void spawnEntitySlice(ServerLevel level, Vec3 center, Vec3 look, float chargePercent) {
		level.sendParticles(ParticleTypes.SPLASH, center.x, center.y, center.z, 16 + Mth.floor(chargePercent * 12.0F), 0.38D, 0.32D, 0.38D, 0.09D);
		level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 6 + Mth.floor(chargePercent * 5.0F), 0.28D, 0.24D, 0.28D, 0.08D);
		Vec3 behind = center.subtract(look.scale(0.35D));
		level.sendParticles(ParticleTypes.BUBBLE, behind.x, behind.y, behind.z, 10, 0.28D, 0.22D, 0.28D, 0.045D);
	}

	private static void spawnImpact(ServerLevel level, Vec3 end, Vec3 look, boolean hitBlock, float chargePercent, int hitCount) {
		int splash = hitBlock ? 26 : 12;
		level.sendParticles(ParticleTypes.SPLASH, end.x, end.y, end.z, splash + hitCount * 4, 0.48D, 0.34D, 0.48D, 0.12D);
		level.sendParticles(ParticleTypes.CLOUD, end.x, end.y, end.z, 5 + Mth.floor(chargePercent * 6.0F), 0.22D, 0.16D, 0.22D, 0.04D);
		if (chargePercent > 0.75F) {
			Vec3 back = end.subtract(look.scale(0.25D));
			level.sendParticles(ParticleTypes.POOF, back.x, back.y, back.z, 4, 0.2D, 0.14D, 0.2D, 0.045D);
		}
	}

	private static void cutSoftBlocks(ServerLevel level, Vec3 origin, Vec3 look, double range, double thickness, float chargePercent) {
		if (!level.getGameRules().getBoolean(NarutoModGameRules.CRAFT_NO_JUTSU_ENABLE_BLOCK_BREAKING)) {
			return;
		}
		int blockRange = Mth.ceil(range);
		BlockPos base = BlockPos.containing(origin);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		Set<BlockPos> changed = new HashSet<>();
		for (int x = -blockRange; x <= blockRange; x++) {
			for (int y = -blockRange / 2; y <= blockRange / 2; y++) {
				for (int z = -blockRange; z <= blockRange; z++) {
					cursor.set(base.getX() + x, base.getY() + y, base.getZ() + z);
					Vec3 center = Vec3.atCenterOf(cursor);
					Vec3 offset = center.subtract(origin);
					double forward = offset.dot(look);
					if (forward < 0.0D || forward > range) {
						continue;
					}
					Vec3 closest = origin.add(look.scale(forward));
					if (center.distanceTo(closest) > thickness + 0.16D) {
						continue;
					}
					BlockPos immutable = cursor.immutable();
					if (!changed.add(immutable)) {
						continue;
					}
					BlockState state = level.getBlockState(immutable);
					if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.COBWEB) || state.is(Blocks.VINE) || state.is(Blocks.GLOW_LICHEN) || (chargePercent > 0.72F && state.is(Blocks.BAMBOO))) {
						level.destroyBlock(immutable, true);
						level.sendParticles(ParticleTypes.SPLASH, center.x, center.y, center.z, 4, 0.2D, 0.2D, 0.2D, 0.06D);
					}
				}
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
