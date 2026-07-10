package net.mcreator.naruto.procedures;

import org.checkerframework.checker.units.qual.t;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

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

import net.mcreator.naruto.network.SeveringWaveBeamPacket;
import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.init.NarutoModGameRules;
import net.mcreator.naruto.JutsuData;

import java.util.*;

@EventBusSubscriber
public class SeveringWaveAbility {
	private static final String JUTSU_ID = "severing_wave";
	private static final float BASE_TOTAL_DAMAGE = 9.0F;
	private static final float FULL_TOTAL_DAMAGE = 19.0F;
	private static final double MIN_RANGE = 8.5D;
	private static final double MAX_RANGE = 18.5D;
	private static final double MIN_THICKNESS = 0.34D;
	private static final double MAX_THICKNESS = 0.82D;
	private static final int MIN_DURATION = 10;
	private static final int MAX_DURATION = 46;
	private static final Map<UUID, ActiveWave> ACTIVE = new HashMap<>();

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
		if (ACTIVE.containsKey(player.getUUID())) {
			return true;
		}
		if (vars.currentChakra < jutsuData.getChakraCost()) {
			JutsuFeedback.notEnoughChakra(player);
			return true;
		}
		int cooldown = Math.max(1, jutsuData.getCooldown());
		if (!JutsuCooldowns.tryStart(player, JUTSU_ID, cooldown)) {
			JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, JUTSU_ID));
			return true;
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		int duration = Mth.floor(Mth.lerp(chargePercent, MIN_DURATION, MAX_DURATION));
		double range = Mth.lerp(chargePercent, MIN_RANGE, MAX_RANGE);
		double thickness = Mth.lerp(chargePercent, MIN_THICKNESS, MAX_THICKNESS);
		float totalDamage = scaledDamage(player, chargePercent);
		ACTIVE.put(player.getUUID(), new ActiveWave(duration, range, thickness, chargePercent, totalDamage));
		if (player.level() instanceof ServerLevel level) {
			Vec3 look = safeLook(player);
			Vec3 origin = beamOrigin(player, look);
			PacketDistributor.sendToPlayersInDimension(level, new SeveringWaveBeamPacket(player.getUUID(), true, origin.x, origin.y, origin.z, look.x, look.y, look.z, (float) range, (float) thickness, 4));
			level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.1F, 1.25F + chargePercent * 0.35F);
			level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.55F, 1.55F + chargePercent * 0.25F);
		}
		return true;
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide()) {
			return;
		}
		ActiveWave wave = ACTIVE.get(player.getUUID());
		if (wave == null) {
			return;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		if (!player.isAlive() || !JUTSU_ID.equals(vars.activeJutsu) || wave.ticksLeft <= 0) {
			endWave(player);
			return;
		}
		if (!(player.level() instanceof ServerLevel level)) {
			endWave(player);
			return;
		}
		tickWave(level, player, wave);
		wave.age++;
		wave.ticksLeft--;
		if (wave.ticksLeft <= 0) {
			endWave(player);
		}
	}

	private static void tickWave(ServerLevel level, Player player, ActiveWave wave) {
		Vec3 look = safeLook(player);
		Vec3 origin = beamOrigin(player, look);
		BlockHitResult blockHit = level.clip(new ClipContext(origin, origin.add(look.scale(wave.range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 end = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : origin.add(look.scale(wave.range));
		double actualRange = origin.distanceTo(end);
		player.setDeltaMovement(player.getDeltaMovement().scale(0.45D));
		player.hurtMarked = true;
		if (wave.age % 2 == 0) {
			PacketDistributor.sendToPlayersInDimension(level, new SeveringWaveBeamPacket(player.getUUID(), true, origin.x, origin.y, origin.z, look.x, look.y, look.z, (float) actualRange, (float) wave.thickness, 4));
		}
		if (wave.age % 2 == 0) {
			spawnLightServerParticles(level, origin, look, actualRange, wave);
		}
		if (wave.age % 4 == 0) {
			damageEntities(level, player, origin, look, actualRange, wave);
		}
		if (wave.age % 5 == 0) {
			cutSoftBlocks(level, origin, look, actualRange, wave);
		}
		if (blockHit.getType() == HitResult.Type.BLOCK && wave.age % 3 == 0) {
			level.sendParticles(ParticleTypes.SPLASH, end.x, end.y, end.z, 7, 0.28D, 0.2D, 0.28D, 0.08D);
			level.sendParticles(ParticleTypes.CLOUD, end.x, end.y, end.z, 2, 0.16D, 0.12D, 0.16D, 0.03D);
		}
	}

	private static void damageEntities(ServerLevel level, Player player, Vec3 origin, Vec3 look, double range, ActiveWave wave) {
		Vec3 end = origin.add(look.scale(range));
		AABB searchArea = new AABB(origin, end).inflate(wave.thickness + 1.25D);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchArea, target -> target.isAlive() && target != player && !target.isSpectator());
		float damagePerHit = wave.totalDamage / Math.max(1.0F, wave.durationTicks / 4.0F);
		for (LivingEntity target : targets) {
			Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
			Vec3 offset = center.subtract(origin);
			double forward = offset.dot(look);
			if (forward < 0.0D || forward > range) {
				continue;
			}
			Vec3 closest = origin.add(look.scale(forward));
			double distanceFromBeam = center.distanceTo(closest);
			double allowed = wave.thickness + target.getBbWidth() * 0.58D;
			if (distanceFromBeam > allowed) {
				continue;
			}
			double distanceFalloff = 1.0D - Mth.clamp(forward / Math.max(1.0D, range), 0.0D, 1.0D) * 0.22D;
			float dealt = (float) (damagePerHit * distanceFalloff);
			target.invulnerableTime = 0;
			target.hurt(player.damageSources().playerAttack(player), dealt);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 26, 1, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 24, 0, true, true));
			Vec3 push = look.scale(0.18D + wave.chargePercent * 0.22D).add(0.0D, 0.025D, 0.0D);
			target.setDeltaMovement(target.getDeltaMovement().scale(0.55D).add(push));
			target.hurtMarked = true;
			level.sendParticles(ParticleTypes.SPLASH, center.x, center.y, center.z, 9, 0.28D, 0.24D, 0.28D, 0.075D);
			level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 3, 0.18D, 0.16D, 0.18D, 0.06D);
		}
	}

	private static void spawnLightServerParticles(ServerLevel level, Vec3 origin, Vec3 look, double range, ActiveWave wave) {
		level.sendParticles(ParticleTypes.SPLASH, origin.x, origin.y, origin.z, 4, 0.16D, 0.1D, 0.16D, 0.08D);
		level.sendParticles(ParticleTypes.BUBBLE, origin.x, origin.y, origin.z, 3, 0.12D, 0.08D, 0.12D, 0.03D);
		int steps = 3 + Mth.floor(wave.chargePercent * 3.0F);
		for (int i = 1; i <= steps; i++) {
			double t = i / (double) steps;
			Vec3 pos = origin.add(look.scale(range * t));
			level.sendParticles(ParticleTypes.FALLING_WATER, pos.x, pos.y, pos.z, 1, wave.thickness * 0.18D, wave.thickness * 0.12D, wave.thickness * 0.18D, 0.0D);
		}
	}

	private static void cutSoftBlocks(ServerLevel level, Vec3 origin, Vec3 look, double range, ActiveWave wave) {
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
					if (center.distanceTo(closest) > wave.thickness + 0.16D) {
						continue;
					}
					BlockPos immutable = cursor.immutable();
					if (!changed.add(immutable)) {
						continue;
					}
					BlockState state = level.getBlockState(immutable);
					if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.COBWEB) || state.is(Blocks.VINE) || state.is(Blocks.GLOW_LICHEN) || (wave.chargePercent > 0.72F && state.is(Blocks.BAMBOO))) {
						level.destroyBlock(immutable, true);
						level.sendParticles(ParticleTypes.SPLASH, center.x, center.y, center.z, 3, 0.16D, 0.16D, 0.16D, 0.05D);
					}
				}
			}
		}
	}

	private static void endWave(Player player) {
		ActiveWave removed = ACTIVE.remove(player.getUUID());
		if (removed != null && player.level() instanceof ServerLevel level) {
			PacketDistributor.sendToPlayersInDimension(level, new SeveringWaveBeamPacket(player.getUUID(), false, player.getX(), player.getY(), player.getZ(), 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0));
		}
	}

	private static Vec3 safeLook(Player player) {
		Vec3 look = player.getLookAngle();
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		return look.normalize();
	}

	private static Vec3 beamOrigin(Player player, Vec3 look) {
		return player.getEyePosition().add(look.scale(-0.5D)).add(0.0D, -0.08D, 0.0D);
	}

	private static float scaledDamage(Player player, float chargePercent) {
		float base = Mth.lerp(chargePercent, BASE_TOTAL_DAMAGE, FULL_TOTAL_DAMAGE);
		int strengthAmplifier = 0;
		MobEffectInstance strength = player.getEffect(MobEffects.DAMAGE_BOOST);
		if (strength != null) {
			strengthAmplifier = Math.max(0, strength.getAmplifier());
		}
		return base * (1.0F + strengthAmplifier * 0.333F);
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}

	private static final class ActiveWave {
		private final int durationTicks;
		private final double range;
		private final double thickness;
		private final float chargePercent;
		private final float totalDamage;
		private int ticksLeft;
		private int age;

		private ActiveWave(int durationTicks, double range, double thickness, float chargePercent, float totalDamage) {
			this.durationTicks = durationTicks;
			this.range = range;
			this.thickness = thickness;
			this.chargePercent = chargePercent;
			this.totalDamage = totalDamage;
			this.ticksLeft = durationTicks;
			this.age = 0;
		}
	}
}