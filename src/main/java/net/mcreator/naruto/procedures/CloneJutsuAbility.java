package net.mcreator.naruto.procedures;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.entity.ShinobiCloneEntity;
import net.mcreator.naruto.entity.ShinobiCloneEntity.CloneType;
import net.mcreator.naruto.init.NarutoModEntities;
import net.mcreator.naruto.network.NarutoModVariables;

public class CloneJutsuAbility {
	private static final int MAX_ACTIVE_CLONES = 4;
	private static final double SPAWN_DISTANCE = 1.25D;

	public static boolean tryUse(Entity entity, JutsuData jutsuData, CloneType cloneType, String jutsuId) {
		if (!(entity instanceof Player player) || jutsuData == null || cloneType == null) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		clearCharge(player);
		if (countActiveClones(player) >= MAX_ACTIVE_CLONES) {
			JutsuFeedback.show(player, "Clone limit reached");
			return true;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		if (vars.currentChakra < jutsuData.getChakraCost()) {
			JutsuFeedback.notEnoughChakra(player);
			return true;
		}
		int cooldown = Math.max(1, jutsuData.getCooldown());
		if (!JutsuCooldowns.tryStart(player, jutsuId, cooldown)) {
			showCooldown(player, jutsuData, jutsuId);
			return true;
		}
		ShinobiCloneEntity clone = new ShinobiCloneEntity(NarutoModEntities.SHINOBI_CLONE.get(), player.level());
		clone.setOwner(player);
		clone.setCloneType(cloneType);
		copyEquipment(player, clone);
		Vec3 spawn = findSpawnPosition(player);
		clone.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), player.getXRot());
		clone.setYBodyRot(player.getYRot());
		clone.setYHeadRot(player.getYHeadRot());
		player.level().addFreshEntity(clone);
		spawnParticles(player, cloneType, spawn);
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		return true;
	}

	private static int countActiveClones(Player player) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return 0;
		}
		AABB searchArea = AABB.ofSize(player.position(), 512.0D, 384.0D, 512.0D);
		return serverLevel.getEntitiesOfClass(ShinobiCloneEntity.class, searchArea, clone -> player.getUUID().equals(clone.getOwnerUUID())).size();
	}

	private static void copyEquipment(Player player, ShinobiCloneEntity clone) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack copy = player.getItemBySlot(slot).copy();
			clone.setItemSlot(slot, copy);
		}
	}

	private static Vec3 findSpawnPosition(Player player) {
		Vec3 look = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(0.0F, player.getYRot()).multiply(1.0D, 0.0D, 1.0D);
		}
		look = look.normalize();
		Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(look).normalize();
		Vec3 base = player.position();
		Vec3[] candidates = {
				base.add(right.scale(SPAWN_DISTANCE)),
				base.add(right.scale(-SPAWN_DISTANCE)),
				base.add(look.scale(-SPAWN_DISTANCE)),
				base.add(look.scale(SPAWN_DISTANCE))
		};
		for (Vec3 candidate : candidates) {
			if (player.level().noCollision(player, player.getBoundingBox().move(candidate.subtract(base)))) {
				return candidate;
			}
		}
		return base;
	}

	private static void spawnParticles(Player player, CloneType cloneType, Vec3 position) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		double y = position.y + player.getBbHeight() * 0.45D;
		switch (cloneType) {
			case LIGHTNING -> {
				serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, position.x, y, position.z, 26, 0.38D, 0.55D, 0.38D, 0.08D);
				serverLevel.sendParticles(ParticleTypes.FLASH, position.x, y, position.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			}
			case WATER -> {
				serverLevel.sendParticles(ParticleTypes.SPLASH, position.x, y, position.z, 34, 0.5D, 0.42D, 0.5D, 0.08D);
				serverLevel.sendParticles(ParticleTypes.BUBBLE, position.x, y - 0.1D, position.z, 18, 0.32D, 0.3D, 0.32D, 0.02D);
			}
			case EXPLOSION -> {
				serverLevel.sendParticles(ParticleTypes.SMOKE, position.x, y, position.z, 24, 0.42D, 0.45D, 0.42D, 0.04D);
				serverLevel.sendParticles(ParticleTypes.FLAME, position.x, y, position.z, 8, 0.24D, 0.28D, 0.24D, 0.02D);
			}
			default -> {
				serverLevel.sendParticles(ParticleTypes.CLOUD, position.x, y, position.z, 28, 0.45D, 0.5D, 0.45D, 0.04D);
				serverLevel.sendParticles(ParticleTypes.POOF, position.x, y - 0.12D, position.z, 10, 0.28D, 0.35D, 0.28D, 0.02D);
			}
		}
	}

	private static void showCooldown(Player player, JutsuData jutsuData, String jutsuId) {
		JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, jutsuId));
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}
}
