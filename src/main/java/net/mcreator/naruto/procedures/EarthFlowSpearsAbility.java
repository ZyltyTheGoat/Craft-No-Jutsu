package net.mcreator.naruto.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Block;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.network.EarthSpikePacket;
import net.mcreator.naruto.network.NarutoModVariables;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

public class EarthFlowSpearsAbility {
	private static final String JUTSU_ID = "earth_flow_spears";
	private static final int ERUPTION_INTERVAL_TICKS = 2;
	private static final double HIT_RADIUS = 1.15D;

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
			releaseSpears(serverLevel, player, chargePercent);
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		return true;
	}

	private static void releaseSpears(ServerLevel level, Player player, float chargePercent) {
		Vec3 forward = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
		if (forward.lengthSqr() < 0.001D) {
			forward = Vec3.directionFromRotation(0.0F, player.getYRot()).multiply(1.0D, 0.0D, 1.0D);
		}
		forward = forward.normalize();
		int spearCount = 3 + Mth.floor(chargePercent * 7.0F);
		double spacing = Mth.lerp(chargePercent, 1.05D, 1.35D);
		float damage = NarutoDamageFormula.calculateJutsuDamage(player, 4.8F, chargePercent, NarutoDamageFormula.TYPE_SCALE_MULTI_HIT_HIGH);
		Set<Integer> hitEntityIds = new HashSet<>();
		for (int i = 0; i < spearCount; i++) {
			int index = i;
			Vec3 center = player.position().add(forward.scale(1.6D + index * spacing));
			Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward).normalize();
			NarutoMod.queueServerWork(index * ERUPTION_INTERVAL_TICKS, () -> eruptSpear(level, player, center, right, chargePercent, damage, hitEntityIds));
		}
		level.playSound(null, player.blockPosition(), SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 0.95F, 0.7F + chargePercent * 0.2F);
	}

	private static void eruptSpear(ServerLevel level, Player player, Vec3 center, Vec3 right, float chargePercent, float damage, Set<Integer> hitEntityIds) {
		if (player == null || !player.isAlive() || player.level() != level) {
			return;
		}
		BlockPos ground = findGround(level, BlockPos.containing(center));
		if (!level.isLoaded(ground)) {
			return;
		}
		BlockState groundState = level.getBlockState(ground);
		BlockState particleState = groundState.isAir() || !groundState.getFluidState().isEmpty() ? Blocks.DIRT.defaultBlockState() : groundState;
		Vec3 impact = Vec3.atCenterOf(ground).add(0.0D, 0.65D, 0.0D);
		spawnSpikeVisuals(level, impact, right, particleState, chargePercent);
		int blockCount = 24 + Mth.floor(chargePercent * 22.0F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, particleState), impact.x, impact.y, impact.z, blockCount, 0.42D + chargePercent * 0.25D, 0.55D + chargePercent * 0.35D, 0.42D + chargePercent * 0.25D, 0.22D);
		level.sendParticles(ParticleTypes.POOF, impact.x, impact.y + 0.15D, impact.z, 5 + Mth.floor(chargePercent * 5.0F), 0.18D, 0.24D, 0.18D, 0.04D);
		level.playSound(null, ground, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.55F, 0.8F + level.random.nextFloat() * 0.2F);
		double hitRadius = HIT_RADIUS + chargePercent * 0.55D;
		AABB hitBox = new AABB(impact.x - hitRadius, ground.getY(), impact.z - hitRadius, impact.x + hitRadius, ground.getY() + 2.7D + chargePercent * 0.9D, impact.z + hitRadius);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, hitBox, target -> target.isAlive() && target != player && !target.isSpectator())) {
			if (!hitEntityIds.add(target.getId())) {
				continue;
			}
			target.invulnerableTime = 0;
			target.hurt(player.damageSources().playerAttack(player), damage);
			target.invulnerableTime = 0;
			Vec3 launch = target.position().subtract(impact).multiply(0.18D, 0.0D, 0.18D).add(0.0D, 0.48D + chargePercent * 0.22D, 0.0D);
			target.setDeltaMovement(target.getDeltaMovement().scale(0.25D).add(launch));
			target.hurtMarked = true;
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 28 + Mth.floor(chargePercent * 18.0F), 1, true, true));
		}
	}

	private static void spawnSpikeVisuals(ServerLevel level, Vec3 impact, Vec3 right, BlockState particleState, float chargePercent) {
		int stateId = Block.getId(particleState);
		float baseRotation = level.random.nextFloat() * Mth.TWO_PI;
		PacketDistributor.sendToPlayersInDimension(level, new EarthSpikePacket(impact.x, impact.y + 0.35D, impact.z, stateId, chargePercent, 1.0F + level.random.nextFloat() * 0.18F, baseRotation, 16));
		int sidePairs = chargePercent > 0.75F ? 2 : chargePercent > 0.28F ? 1 : 0;
		for (int pair = 1; pair <= sidePairs; pair++) {
			double sideOffset = pair * Mth.lerp(chargePercent, 0.45D, 0.82D);
			float scale = pair == 1 ? 0.58F + level.random.nextFloat() * 0.22F : 0.38F + level.random.nextFloat() * 0.18F;
			for (int sign = -1; sign <= 1; sign += 2) {
				Vec3 sidePos = impact.add(right.scale(sideOffset * sign)).add((level.random.nextDouble() - 0.5D) * 0.12D, 0.18D, (level.random.nextDouble() - 0.5D) * 0.12D);
				float rotation = baseRotation + sign * (0.7F + level.random.nextFloat() * 1.1F);
				PacketDistributor.sendToPlayersInDimension(level, new EarthSpikePacket(sidePos.x, sidePos.y + 0.35D, sidePos.z, stateId, chargePercent, scale, rotation, 13 + level.random.nextInt(5)));
			}
		}
	}

	private static BlockPos findGround(ServerLevel level, BlockPos origin) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(origin.getX(), origin.getY() + 3, origin.getZ());
		int minY = Math.max(level.getMinBuildHeight() + 1, origin.getY() - 9);
		for (int y = cursor.getY(); y >= minY; y--) {
			cursor.setY(y);
			BlockState state = level.getBlockState(cursor);
			if (!state.isAir() && state.getFluidState().isEmpty() && state.isFaceSturdy(level, cursor, Direction.UP)) {
				return cursor.immutable();
			}
		}
		return origin.below();
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}
}
