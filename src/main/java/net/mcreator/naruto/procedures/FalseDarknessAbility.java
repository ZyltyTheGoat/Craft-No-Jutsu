package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.network.FalseDarknessBeamPacket;
import net.mcreator.naruto.init.NarutoModGameRules;
import net.mcreator.naruto.JutsuData;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

public class FalseDarknessAbility {
	private static final String JUTSU_ID = "false_darkness";
	private static final double MIN_RANGE = 11.0D;
	private static final double MAX_RANGE = 26.0D;
	private static final double MIN_THICKNESS = 0.18D;
	private static final double MAX_THICKNESS = 0.42D;
	private static final float MIN_DAMAGE = 10.0F;
	private static final float MAX_DAMAGE = 24.0F;
	private static final int MIN_SIDE_SPEARS = 0;
	private static final int MAX_SIDE_SPEARS = 4;

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
		if (!JutsuCooldowns.tryStart(player, JUTSU_ID, Math.max(1, jutsuData.getCooldown()))) {
			JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, JUTSU_ID));
			return true;
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		if (player.level() instanceof ServerLevel level) {
			release(level, player, chargePercent);
		}
		return true;
	}

	private static void release(ServerLevel level, Player player, float chargePercent) {
		Vec3 look = safeLook(player);
		Vec3 origin = mouthOrigin(player, look);
		double range = Mth.lerp(chargePercent, MIN_RANGE, MAX_RANGE);
		double thickness = Mth.lerp(chargePercent, MIN_THICKNESS, MAX_THICKNESS);
		float damage = scaledDamage(player, chargePercent);
		BlockHitResult mainHit = level.clip(new ClipContext(origin, origin.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 mainEnd = mainHit.getType() == HitResult.Type.BLOCK ? mainHit.getLocation() : origin.add(look.scale(range));
		double mainRange = origin.distanceTo(mainEnd);
		fireSpear(level, player, origin, look, mainRange, thickness, damage, chargePercent, true, 0);
		cutSoftBlocks(level, origin, look, mainRange, thickness, chargePercent);
		int sideSpears = Mth.floor(Mth.lerp(chargePercent, MIN_SIDE_SPEARS, MAX_SIDE_SPEARS));
		for (int i = 0; i < sideSpears; i++) {
			float spreadYaw = ((i % 2 == 0) ? 1.0F : -1.0F) * (5.5F + i * 1.6F);
			float spreadPitch = ((i % 3) - 1) * 2.2F;
			Vec3 sideDirection = rotateLook(player.getXRot() + spreadPitch, player.getYRot() + spreadYaw).normalize();
			Vec3 sideOrigin = origin.add(sideDirection.scale(0.15D));
			double sideRange = range * (0.72D + chargePercent * 0.12D);
			BlockHitResult sideHit = level.clip(new ClipContext(sideOrigin, sideOrigin.add(sideDirection.scale(sideRange)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			Vec3 sideEnd = sideHit.getType() == HitResult.Type.BLOCK ? sideHit.getLocation() : sideOrigin.add(sideDirection.scale(sideRange));
			double actualSideRange = sideOrigin.distanceTo(sideEnd);
			fireSpear(level, player, sideOrigin, sideDirection, actualSideRange, thickness * 0.68D, damage * 0.45F, chargePercent, false, i + 1);
			cutSoftBlocks(level, sideOrigin, sideDirection, actualSideRange, thickness * 0.55D, chargePercent * 0.75F);
		}
		level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.75F, 1.75F + chargePercent * 0.25F);
		level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.6F, 1.9F + chargePercent * 0.25F);
		level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.55F, 1.65F);
	}

	private static void fireSpear(ServerLevel level, Player player, Vec3 origin, Vec3 direction, double range, double thickness, float damage, float chargePercent, boolean main, int spearIndex) {
		Vec3 end = origin.add(direction.scale(range));
		int visualTicks = main ? 18 : 14;
		PacketDistributor.sendToPlayersInDimension(level,
				new FalseDarknessBeamPacket(player.getUUID(), origin.x, origin.y, origin.z, direction.x, direction.y, direction.z, (float) range, (float) thickness, chargePercent, main, spearIndex, visualTicks));
		spawnMouthBurst(level, origin, direction, chargePercent, main);
		spawnImpact(level, end, direction, chargePercent, main);
		damageEntities(level, player, origin, direction, range, thickness, damage, chargePercent, main);
	}

	private static void damageEntities(ServerLevel level, Player player, Vec3 origin, Vec3 direction, double range, double thickness, float damage, float chargePercent, boolean main) {
		Vec3 end = origin.add(direction.scale(range));
		AABB area = new AABB(origin, end).inflate(thickness + 1.35D);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, target -> target.isAlive() && target != player && !target.isSpectator());
		Set<Integer> pierced = new HashSet<>();
		for (LivingEntity target : targets) {
			Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
			Vec3 offset = center.subtract(origin);
			double forward = offset.dot(direction);
			if (forward < 0.0D || forward > range) {
				continue;
			}
			Vec3 closest = origin.add(direction.scale(forward));
			double beamDistance = center.distanceTo(closest);
			double allowed = thickness + target.getBbWidth() * 0.65D;
			if (beamDistance > allowed) {
				continue;
			}
			int bucket = Mth.floor(forward * 2.0D);
			if (!pierced.add(bucket) && !main) {
				continue;
			}
			double falloff = 1.0D - Mth.clamp(forward / Math.max(1.0D, range), 0.0D, 1.0D) * 0.18D;
			float dealt = (float) (damage * falloff);
			target.invulnerableTime = 0;
			target.hurt(player.damageSources().playerAttack(player), dealt);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 35, 0, true, true));
			Vec3 shock = direction.scale(0.28D + chargePercent * 0.35D).add(0.0D, 0.08D, 0.0D);
			target.setDeltaMovement(target.getDeltaMovement().scale(0.35D).add(shock));
			target.hurtMarked = true;
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, main ? 18 : 9, 0.34D, 0.28D, 0.34D, 0.18D);
			level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, main ? 8 : 4, 0.25D, 0.22D, 0.25D, 0.08D);
		}
	}

	private static void spawnMouthBurst(ServerLevel level, Vec3 origin, Vec3 direction, float chargePercent, boolean main) {
		int sparks = main ? 18 + Mth.floor(chargePercent * 16.0F) : 7;
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, origin.x, origin.y, origin.z, sparks, 0.16D, 0.12D, 0.16D, 0.16D);
		level.sendParticles(ParticleTypes.FLASH, origin.x + direction.x * 0.2D, origin.y + direction.y * 0.2D, origin.z + direction.z * 0.2D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
	}

	private static void spawnImpact(ServerLevel level, Vec3 end, Vec3 direction, float chargePercent, boolean main) {
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, main ? 24 : 10, 0.28D, 0.22D, 0.28D, 0.22D);
		level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, main ? 1 : 0, 0.0D, 0.0D, 0.0D, 0.0D);
		level.sendParticles(ParticleTypes.CLOUD, end.x - direction.x * 0.15D, end.y - direction.y * 0.15D, end.z - direction.z * 0.15D, main ? 4 : 2, 0.12D, 0.1D, 0.12D, 0.03D);
	}

	private static void cutSoftBlocks(ServerLevel level, Vec3 origin, Vec3 direction, double range, double thickness, float chargePercent) {
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
					double forward = offset.dot(direction);
					if (forward < 0.0D || forward > range) {
						continue;
					}
					Vec3 closest = origin.add(direction.scale(forward));
					if (center.distanceTo(closest) > thickness + 0.14D) {
						continue;
					}
					BlockPos immutable = cursor.immutable();
					if (!changed.add(immutable)) {
						continue;
					}
					BlockState state = level.getBlockState(immutable);
					if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.COBWEB) || state.is(Blocks.VINE) || state.is(Blocks.GLOW_LICHEN) || (chargePercent > 0.7F && state.is(Blocks.BAMBOO))) {
						level.destroyBlock(immutable, true);
						level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 5, 0.12D, 0.12D, 0.12D, 0.08D);
					}
				}
			}
		}
	}

	private static float scaledDamage(Player player, float chargePercent) {
		float base = Mth.lerp(chargePercent, MIN_DAMAGE, MAX_DAMAGE);
		int strengthAmplifier = 0;
		MobEffectInstance strength = player.getEffect(MobEffects.DAMAGE_BOOST);
		if (strength != null) {
			strengthAmplifier = Math.max(0, strength.getAmplifier());
		}
		return base * (1.0F + strengthAmplifier * 0.333F);
	}

	private static Vec3 safeLook(Player player) {
		Vec3 look = player.getLookAngle();
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		return look.normalize();
	}

	private static Vec3 rotateLook(float pitch, float yaw) {
		return Vec3.directionFromRotation(pitch, yaw);
	}

	private static Vec3 mouthOrigin(Player player, Vec3 look) {
		return player.getEyePosition().add(look.scale(0.48D)).add(0.0D, -0.08D, 0.0D);
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}
}