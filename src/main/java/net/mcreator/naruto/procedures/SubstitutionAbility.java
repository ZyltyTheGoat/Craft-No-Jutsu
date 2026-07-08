package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.JutsuData;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@EventBusSubscriber
public class SubstitutionAbility {
	private static final Map<UUID, Long> PRIMED_UNTIL = new HashMap<>();
	private static final String JUTSU_ID = "substitution";
	private static final int PRIME_WINDOW_TICKS = 80;
	private static final double ESCAPE_DISTANCE = 5.0D;
	private static final double STEP = 0.45D;

	public static boolean tryPrime(Entity entity, JutsuData jutsuData) {
		if (!(entity instanceof Player player) || jutsuData == null) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		long gameTime = player.level().getGameTime();
		int cooldown = Math.max(1, jutsuData.getCooldown());
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		if (vars.currentChakra < jutsuData.getChakraCost()) {
			JutsuFeedback.notEnoughChakra(player);
			return true;
		}
		if (!JutsuCooldowns.tryStart(player, JUTSU_ID, cooldown)) {
			showCooldown(player, jutsuData);
			return true;
		}
		vars.currentChakra = Math.max(0, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		PRIMED_UNTIL.put(player.getUUID(), gameTime + PRIME_WINDOW_TICKS);
		spawnSmoke(player.level(), player.position(), player.getBbHeight(), 8);
		return true;
	}

	@SubscribeEvent
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		LivingEntity living = event.getEntity();
		if (!(living instanceof Player player) || player.level().isClientSide()) {
			return;
		}
		long gameTime = player.level().getGameTime();
		long primedUntil = PRIMED_UNTIL.getOrDefault(player.getUUID(), 0L);
		if (gameTime > primedUntil) {
			PRIMED_UNTIL.remove(player.getUUID());
			return;
		}
		PRIMED_UNTIL.remove(player.getUUID());
		Vec3 start = player.position();
		Vec3 destination = findEscapeDestination(player);
		event.setCanceled(true);
		event.setInvulnerabilityTicks(12);
		spawnSmoke(player.level(), start, player.getBbHeight(), 22);
		player.teleportTo(destination.x, destination.y, destination.z);
		player.resetFallDistance();
		spawnSmoke(player.level(), destination, player.getBbHeight(), 16);
	}

	private static Vec3 findEscapeDestination(Player player) {
		Vec3 sourceDirection = player.getLastHurtByMob() != null ? player.position().subtract(player.getLastHurtByMob().position()).multiply(1.0D, 0.0D, 1.0D) : Vec3.ZERO;
		if (sourceDirection.lengthSqr() < 0.001D) {
			sourceDirection = player.getLookAngle().reverse().multiply(1.0D, 0.0D, 1.0D);
		}
		if (sourceDirection.lengthSqr() < 0.001D) {
			sourceDirection = Vec3.directionFromRotation(0.0F, player.getYRot() + 180.0F).multiply(1.0D, 0.0D, 1.0D);
		}
		sourceDirection = sourceDirection.normalize();
		Vec3 start = player.position();
		Vec3 best = start;
		for (double distance = STEP; distance <= ESCAPE_DISTANCE; distance += STEP) {
			Vec3 candidate = start.add(sourceDirection.scale(distance));
			if (player.level().noCollision(player, player.getBoundingBox().move(candidate.subtract(start)))) {
				best = candidate;
			} else {
				break;
			}
		}
		return best;
	}

	private static void spawnSmoke(Level level, Vec3 position, float height, int count) {
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.CLOUD, position.x, position.y + height * 0.45D, position.z, count, 0.35D, height * 0.26D, 0.35D, 0.03D);
			serverLevel.sendParticles(ParticleTypes.POOF, position.x, position.y + height * 0.35D, position.z, Math.max(2, count / 3), 0.22D, height * 0.18D, 0.22D, 0.015D);
		}
	}

	private static void showCooldown(Player player, JutsuData jutsuData) {
		JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, JUTSU_ID));
	}
}
