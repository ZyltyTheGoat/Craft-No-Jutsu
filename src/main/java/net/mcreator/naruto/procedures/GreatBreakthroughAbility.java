package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.network.PacketDistributor;

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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.network.WindBurstPacket;

import java.util.List;

public class GreatBreakthroughAbility {
	private static final String JUTSU_ID = "great_breakthrough";
	private static final float CONE_HALF_ANGLE_DEGREES = 34.0F;

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
			releaseGust(serverLevel, player, chargePercent);
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		return true;
	}

	private static void releaseGust(ServerLevel level, Player player, float chargePercent) {
		Vec3 look = player.getLookAngle();
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		look = look.normalize();
		Vec3 origin = player.getEyePosition().add(look.scale(0.55D)).add(0.0D, -0.12D, 0.0D);
		float range = Mth.lerp(chargePercent, 4.8F, 9.2F);
		float power = 0.45F + chargePercent * 0.55F;
		applyConeForce(level, player, origin, look, range, power);
		extinguishFires(level, origin, look, range);
		spawnServerParticles(level, origin, look, range, power);
		level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85F, 0.65F + chargePercent * 0.35F);
		PacketDistributor.sendToPlayersInDimension(level, new WindBurstPacket(origin.x, origin.y, origin.z, look.x, look.y, look.z, range, power, 18 + Mth.floor(chargePercent * 8.0F)));
	}

	private static void applyConeForce(ServerLevel level, Player player, Vec3 origin, Vec3 look, float range, float power) {
		double maxRadius = Math.tan(Math.toRadians(CONE_HALF_ANGLE_DEGREES)) * range + 1.25D;
		AABB searchArea = new AABB(origin, origin.add(look.scale(range))).inflate(maxRadius);
		List<Entity> targets = level.getEntities(player, searchArea, target -> target.isAlive() && target != player && (target instanceof LivingEntity || target instanceof Projectile));
		double minDot = Math.cos(Math.toRadians(CONE_HALF_ANGLE_DEGREES));
		for (Entity target : targets) {
			Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
			Vec3 offset = center.subtract(origin);
			double distance = offset.length();
			if (distance < 0.2D || distance > range) {
				continue;
			}
			Vec3 direction = offset.normalize();
			if (direction.dot(look) < minDot) {
				continue;
			}
			double falloff = 1.0D - distance / range;
			double force = (1.25D + power * 1.85D) * (0.72D + falloff * 0.5D);
			Vec3 push = look.scale(force).add(0.0D, 0.15D + power * 0.22D, 0.0D).add(direction.scale(0.18D));
			target.setDeltaMovement(target.getDeltaMovement().scale(0.15D).add(push));
			target.hurtMarked = true;
			target.resetFallDistance();
			if (target instanceof LivingEntity living) {
				float damage = (float) (1.5D + power * 3.0D + falloff * 1.25D);
				living.hurt(player.damageSources().playerAttack(player), damage);
				living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 22 + Mth.floor(power * 12.0F), 1, true, true));
			} else if (target instanceof Projectile projectile && projectile.getOwner() != player) {
				projectile.setDeltaMovement(look.scale(1.8D + power * 2.2D));
			}
		}
	}

	private static void extinguishFires(ServerLevel level, Vec3 origin, Vec3 look, float range) {
		int blockRange = Mth.ceil(range);
		double minDot = Math.cos(Math.toRadians(CONE_HALF_ANGLE_DEGREES + 6.0F));
		BlockPos base = BlockPos.containing(origin);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int x = -blockRange; x <= blockRange; x++) {
			for (int y = -2; y <= 3; y++) {
				for (int z = -blockRange; z <= blockRange; z++) {
					cursor.set(base.getX() + x, base.getY() + y, base.getZ() + z);
					Vec3 center = Vec3.atCenterOf(cursor);
					Vec3 offset = center.subtract(origin);
					double distance = offset.length();
					if (distance < 0.2D || distance > range || offset.normalize().dot(look) < minDot) {
						continue;
					}
					BlockState state = level.getBlockState(cursor);
					if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
						level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 3);
						level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 4, 0.18D, 0.18D, 0.18D, 0.02D);
					}
				}
			}
		}
	}

	private static void spawnServerParticles(ServerLevel level, Vec3 origin, Vec3 look, float range, float power) {
		Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(look);
		if (right.lengthSqr() < 0.001D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		}
		right = right.normalize();
		Vec3 up = look.cross(right).normalize();
		int steps = 5 + Mth.floor(power * 4.0F);
		for (int i = 0; i < steps; i++) {
			double t = (i + 1.0D) / steps;
			double radius = 0.18D + t * (1.05D + power * 0.55D);
			Vec3 center = origin.add(look.scale(range * t));
			level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 3, radius * 0.55D, radius * 0.32D, radius * 0.55D, 0.08D + power * 0.06D);
			if (i % 2 == 0) {
				Vec3 side = right.scale((level.random.nextDouble() - 0.5D) * radius).add(up.scale((level.random.nextDouble() - 0.5D) * radius));
				Vec3 pos = center.add(side);
				level.sendParticles(ParticleTypes.POOF, pos.x, pos.y, pos.z, 2, 0.08D, 0.08D, 0.08D, 0.04D);
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
