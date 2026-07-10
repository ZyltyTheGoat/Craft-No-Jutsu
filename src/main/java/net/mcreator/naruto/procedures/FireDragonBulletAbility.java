package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.init.NarutoModGameRules;
import net.mcreator.naruto.network.FireDragonBulletPacket;
import net.mcreator.naruto.network.NarutoModVariables;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = NarutoMod.MODID)
public class FireDragonBulletAbility {
	private static final String JUTSU_ID = "fire_dragon_bullet";
	private static final double MIN_RANGE = 9.0D;
	private static final double MAX_RANGE = 23.0D;
	private static final double MIN_RADIUS = 0.42D;
	private static final double MAX_RADIUS = 1.08D;
	private static final float MIN_DAMAGE = 9.0F;
	private static final float MAX_DAMAGE = 23.0F;
	private static final int MIN_TRAVEL_TICKS = 32;
	private static final int MAX_TRAVEL_TICKS = 56;
	private static final Map<UUID, ActiveDragon> ACTIVE = new HashMap<>();

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
		double radius = Mth.lerp(chargePercent, MIN_RADIUS, MAX_RADIUS);
		BlockHitResult blockHit = level.clip(new ClipContext(origin, origin.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 end = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : origin.add(look.scale(range));
		double actualRange = origin.distanceTo(end);
		int travelTicks = Mth.floor(Mth.lerp(chargePercent, MIN_TRAVEL_TICKS, MAX_TRAVEL_TICKS));
		ActiveDragon dragon = new ActiveDragon(origin, look, actualRange, radius, scaledDamage(player, chargePercent), chargePercent, travelTicks, blockHit.getType() == HitResult.Type.BLOCK);
		ACTIVE.put(player.getUUID(), dragon);
		PacketDistributor.sendToPlayersInDimension(level, new FireDragonBulletPacket(player.getUUID(), origin.x, origin.y, origin.z, look.x, look.y, look.z, (float) actualRange, (float) radius, chargePercent, travelTicks + 10));
		spawnMouthBurst(level, origin, look, chargePercent);
		level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.78F + chargePercent * 0.22F);
		level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.95F, 0.72F + chargePercent * 0.2F);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide()) {
			return;
		}
		ActiveDragon dragon = ACTIVE.get(player.getUUID());
		if (dragon == null) {
			return;
		}
		if (!(player.level() instanceof ServerLevel level) || !player.isAlive()) {
			ACTIVE.remove(player.getUUID());
			return;
		}
		tickDragon(level, player, dragon);
		dragon.age++;
		if (dragon.age > dragon.travelTicks + 3) {
			ACTIVE.remove(player.getUUID());
		}
	}

	private static void tickDragon(ServerLevel level, Player player, ActiveDragon dragon) {
		float previousProgress = dragon.age <= 0 ? 0.0F : travelProgress((dragon.age - 1.0F) / dragon.travelTicks);
		float progress = travelProgress(dragon.age / (float) dragon.travelTicks);
		double previousDistance = dragon.range * previousProgress;
		double headDistance = dragon.range * progress;
		double segmentStart = Math.max(0.0D, previousDistance - dragon.radius * 1.8D);
		double segmentEnd = Math.min(dragon.range, headDistance);
		if (segmentEnd > segmentStart) {
			damageEntities(level, player, dragon, segmentStart, segmentEnd);
			if (dragon.age % 2 == 0) {
				burnBlocks(level, dragon, segmentStart, segmentEnd);
			}
		}
		spawnTrailParticles(level, dragon, headDistance);
		if (!dragon.impactSpawned && progress >= 1.0F) {
			dragon.impactSpawned = true;
			spawnImpact(level, dragon.origin.add(dragon.direction.scale(dragon.range)), dragon.direction, dragon.radius, dragon.chargePercent, dragon.hitBlock);
			level.playSound(null, BlockPos.containing(dragon.origin.add(dragon.direction.scale(dragon.range))), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.55F, 1.4F);
		}
	}

	private static float travelProgress(float value) {
		return Mth.clamp(value, 0.0F, 1.0F);
	}

	private static void damageEntities(ServerLevel level, Player player, ActiveDragon dragon, double segmentStart, double segmentEnd) {
		Vec3 start = dragon.origin.add(dragon.direction.scale(segmentStart));
		Vec3 end = dragon.origin.add(dragon.direction.scale(segmentEnd));
		AABB area = new AABB(start, end).inflate(dragon.radius + 1.6D);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, target -> target.isAlive() && target != player && !target.isSpectator());
		for (LivingEntity target : targets) {
			Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
			Vec3 offset = center.subtract(dragon.origin);
			double forward = offset.dot(dragon.direction);
			if (forward < segmentStart || forward > segmentEnd) {
				continue;
			}
			Vec3 closest = dragon.origin.add(dragon.direction.scale(forward));
			double distance = center.distanceTo(closest);
			double dragonWidth = dragon.radius * Mth.lerp((float) (forward / Math.max(1.0D, dragon.range)), 0.65F, 1.2F) + target.getBbWidth() * 0.65D;
			if (distance > dragonWidth) {
				continue;
			}
			if (!dragon.hitEntities.add(target.getId())) {
				continue;
			}
			float falloff = 1.0F - (float) Mth.clamp(forward / Math.max(1.0D, dragon.range), 0.0D, 1.0D) * 0.16F;
			target.invulnerableTime = 0;
			target.hurt(player.damageSources().onFire(), dragon.damage * falloff);
			target.igniteForSeconds(4.0F + dragon.chargePercent * 4.0F);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 0, true, true));
			target.setDeltaMovement(target.getDeltaMovement().scale(0.45D).add(dragon.direction.scale(0.28D + dragon.chargePercent * 0.38D)).add(0.0D, 0.08D, 0.0D));
			target.hurtMarked = true;
			level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 16, 0.34D, 0.28D, 0.34D, 0.08D);
			level.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.15D, center.z, 2, 0.16D, 0.12D, 0.16D, 0.02D);
		}
	}

	private static void burnBlocks(ServerLevel level, ActiveDragon dragon, double segmentStart, double segmentEnd) {
		if (!level.getGameRules().getBoolean(NarutoModGameRules.CRAFT_NO_JUTSU_ENABLE_BLOCK_BREAKING)) {
			return;
		}
		int blockRange = Mth.ceil(dragon.range + dragon.radius);
		BlockPos base = BlockPos.containing(dragon.origin);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int x = -blockRange; x <= blockRange; x++) {
			for (int y = -blockRange / 2; y <= blockRange / 2; y++) {
				for (int z = -blockRange; z <= blockRange; z++) {
					cursor.set(base.getX() + x, base.getY() + y, base.getZ() + z);
					Vec3 center = Vec3.atCenterOf(cursor);
					Vec3 offset = center.subtract(dragon.origin);
					double forward = offset.dot(dragon.direction);
					if (forward < segmentStart || forward > segmentEnd) {
						continue;
					}
					Vec3 closest = dragon.origin.add(dragon.direction.scale(forward));
					if (center.distanceTo(closest) > dragon.radius * 0.72D + 0.22D) {
						continue;
					}
					BlockPos pos = cursor.immutable();
					if (!dragon.burnedBlocks.add(pos)) {
						continue;
					}
					BlockState state = level.getBlockState(pos);
					if (state.is(Blocks.COBWEB) || state.is(Blocks.VINE) || state.is(Blocks.BAMBOO) || state.is(Blocks.DEAD_BUSH) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)) {
						level.destroyBlock(pos, true);
						level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 5, 0.12D, 0.12D, 0.12D, 0.035D);
					} else if (dragon.chargePercent > 0.45F && level.isEmptyBlock(pos.above()) && level.random.nextFloat() < 0.18F + dragon.chargePercent * 0.18F) {
						BlockState fire = FireBlock.getState(level, pos.above());
						if (fire.canSurvive(level, pos.above())) {
							level.setBlock(pos.above(), fire, 3);
						}
					}
				}
			}
		}
	}

	private static void spawnMouthBurst(ServerLevel level, Vec3 origin, Vec3 direction, float chargePercent) {
		level.sendParticles(ParticleTypes.FLASH, origin.x + direction.x * 0.2D, origin.y + direction.y * 0.2D, origin.z + direction.z * 0.2D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		level.sendParticles(ParticleTypes.FLAME, origin.x, origin.y, origin.z, 12 + Mth.floor(chargePercent * 10.0F), 0.18D, 0.12D, 0.18D, 0.08D);
		level.sendParticles(ParticleTypes.SMOKE, origin.x, origin.y, origin.z, 5, 0.14D, 0.1D, 0.14D, 0.04D);
	}

	private static void spawnTrailParticles(ServerLevel level, ActiveDragon dragon, double headDistance) {
		double tailDistance = Math.max(0.0D, headDistance - dragon.radius * (2.4D + dragon.chargePercent * 1.8D));
		Vec3 head = dragon.origin.add(dragon.direction.scale(headDistance));
		Vec3 tail = dragon.origin.add(dragon.direction.scale(tailDistance));
		double wakeRadius = dragon.radius * (0.42D + dragon.chargePercent * 0.22D);
		level.sendParticles(ParticleTypes.FLAME, tail.x, tail.y, tail.z, 3 + Mth.floor(dragon.chargePercent * 3.0F), wakeRadius * 0.38D, wakeRadius * 0.24D, wakeRadius * 0.38D, 0.035D);
		level.sendParticles(ParticleTypes.SMOKE, tail.x, tail.y, tail.z, 2 + Mth.floor(dragon.chargePercent * 2.0F), wakeRadius * 0.34D, wakeRadius * 0.22D, wakeRadius * 0.34D, 0.025D);
		if (dragon.age % 3 == 0) {
			level.sendParticles(ParticleTypes.LAVA, head.x, head.y, head.z, 1, dragon.radius * 0.16D, dragon.radius * 0.1D, dragon.radius * 0.16D, 0.01D);
		}
	}

	private static void spawnImpact(ServerLevel level, Vec3 end, Vec3 direction, double radius, float chargePercent, boolean impact) {
		if (impact) {
			level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
		level.sendParticles(ParticleTypes.FLAME, end.x - direction.x * 0.18D, end.y - direction.y * 0.18D, end.z - direction.z * 0.18D, 18 + Mth.floor(chargePercent * 12.0F), radius * 0.46D, radius * 0.3D, radius * 0.46D, 0.08D);
		level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 9 + Mth.floor(chargePercent * 6.0F), radius * 0.42D, radius * 0.28D, radius * 0.42D, 0.04D);
		level.sendParticles(ParticleTypes.LAVA, end.x, end.y, end.z, 5 + Mth.floor(chargePercent * 4.0F), radius * 0.38D, radius * 0.25D, radius * 0.38D, 0.04D);
	}

	private static float scaledDamage(Player player, float chargePercent) {
		float base = Mth.lerp(chargePercent, MIN_DAMAGE, MAX_DAMAGE);
		MobEffectInstance strength = player.getEffect(MobEffects.DAMAGE_BOOST);
		int strengthAmplifier = strength == null ? 0 : Math.max(0, strength.getAmplifier());
		return base * (1.0F + strengthAmplifier * 0.333F);
	}

	private static Vec3 safeLook(Player player) {
		Vec3 look = player.getLookAngle();
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		return look.normalize();
	}

	private static Vec3 mouthOrigin(Player player, Vec3 look) {
		return player.getEyePosition().add(look.scale(0.52D)).add(0.0D, -0.1D, 0.0D);
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}

	private static final class ActiveDragon {
		private final Vec3 origin;
		private final Vec3 direction;
		private final double range;
		private final double radius;
		private final float damage;
		private final float chargePercent;
		private final int travelTicks;
		private final boolean hitBlock;
		private final Set<Integer> hitEntities = new HashSet<>();
		private final Set<BlockPos> burnedBlocks = new HashSet<>();
		private int age;
		private boolean impactSpawned;

		private ActiveDragon(Vec3 origin, Vec3 direction, double range, double radius, float damage, float chargePercent, int travelTicks, boolean hitBlock) {
			this.origin = origin;
			this.direction = direction;
			this.range = range;
			this.radius = radius;
			this.damage = damage;
			this.chargePercent = chargePercent;
			this.travelTicks = Math.max(1, travelTicks);
			this.hitBlock = hitBlock;
		}
	}
}
