package net.mcreator.naruto.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.JutsuData;

public class BodyFlickerAbility {
	private static final String JUTSU_ID = "body_flicker";
	private static final double FLICKER_DISTANCE = 7.0D;
	private static final double STEP = 0.45D;

	public static boolean tryUse(Entity entity, JutsuData jutsuData) {
		if (!(entity instanceof Player player) || jutsuData == null) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		int cooldown = Math.max(1, jutsuData.getCooldown());
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		if (vars.currentChakra < jutsuData.getChakraCost()) {
			JutsuFeedback.notEnoughChakra(player);
			return true;
		}
		Vec3 start = player.position();
		Vec3 destination = findDestination(player);
		if (destination.distanceToSqr(start) < 0.35D) {
			return true;
		}
		if (!JutsuCooldowns.tryStart(player, JUTSU_ID, cooldown)) {
			showCooldown(player, jutsuData);
			return true;
		}
		spawnFlickerParticles(player.level(), start, player.getBbHeight());
		player.teleportTo(destination.x, destination.y, destination.z);
		player.resetFallDistance();
		spawnFlickerParticles(player.level(), destination, player.getBbHeight());
		vars.currentChakra = Math.max(0, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		return true;
	}

	private static Vec3 findDestination(Player player) {
		Vec3 look = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(0.0F, player.getYRot()).multiply(1.0D, 0.0D, 1.0D);
		}
		look = look.normalize();
		Vec3 start = player.position();
		Vec3 best = start;
		for (double distance = STEP; distance <= FLICKER_DISTANCE; distance += STEP) {
			Vec3 candidate = start.add(look.scale(distance));
			if (canStandAt(player, candidate)) {
				best = candidate;
			} else {
				break;
			}
		}
		return best;
	}

	private static boolean canStandAt(Player player, Vec3 position) {
		return player.level().noCollision(player, player.getBoundingBox().move(position.subtract(player.position())));
	}

	private static void spawnFlickerParticles(Level level, Vec3 position, float height) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		serverLevel.sendParticles(ParticleTypes.CLOUD, position.x, position.y + height * 0.5D, position.z, 4, 0.35D, height * 0.28D, 0.35D, 0.02D);
		serverLevel.sendParticles(ParticleTypes.POOF, position.x, position.y + height * 0.35D, position.z, 2, 0.22D, height * 0.18D, 0.22D, 0.01D);
	}

	private static void showCooldown(Player player, JutsuData jutsuData) {
		JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, JUTSU_ID));
	}
}
