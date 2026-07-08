package net.mcreator.naruto.procedures;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.entity.GreatFireballEntity;
import net.mcreator.naruto.init.NarutoModEntities;
import net.mcreator.naruto.network.NarutoModVariables;

public class GreatFireballAbility {
	private static final String JUTSU_ID = "fireball";

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
		Vec3 look = player.getLookAngle().normalize();
		double spawnDistance = GreatFireballEntity.getSpawnForwardDistance(chargePercent);
		Vec3 spawn = player.getEyePosition().add(0.0D, -0.18D, 0.0D).add(look.scale(spawnDistance));
		GreatFireballEntity fireball = new GreatFireballEntity(NarutoModEntities.GREAT_FIREBALL.get(), player.level());
		fireball.setOwner(player);
		fireball.setChargePercent(chargePercent);
		fireball.setDamage(NarutoDamageFormula.calculateGreatFireballDamage(player instanceof LivingEntity livingPlayer ? livingPlayer : null, chargePercent));
		fireball.setPos(spawn.x, spawn.y, spawn.z);
		fireball.setDeltaMovement(look.scale(Mth.lerp(chargePercent, 1.35D, 1.85D) * 0.85D));
		fireball.setYRot(player.getYRot());
		fireball.setXRot(player.getXRot());
		player.level().addFreshEntity(fireball);
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		return true;
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}
}
