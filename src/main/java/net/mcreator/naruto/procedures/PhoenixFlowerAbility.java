package net.mcreator.naruto.procedures;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.entity.PhoenixFlowerProjectileEntity;
import net.mcreator.naruto.init.NarutoModEntities;
import net.mcreator.naruto.network.NarutoModVariables;
import net.minecraft.world.phys.Vec2;

public class PhoenixFlowerAbility {
	private static final String JUTSU_ID = "phoenix_flower";
	private static final int PROJECTILE_COUNT = 6;
	private static final double TARGET_RANGE = 64.0D;
	private static final double SPAWN_FORWARD_DISTANCE = 0.75D;
	private static final double SPREAD_MIN = 0.25D;
	private static final double SPREAD_MAX = 0.3D;

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
		PhoenixTarget target = findTarget(player);
		Vec3 look = player.getLookAngle().normalize();
		Vec3 spawn = player.getEyePosition().add(0.0D, -0.18D, 0.0D).add(look.scale(SPAWN_FORWARD_DISTANCE));
		double speed = Mth.lerp(chargePercent, PhoenixFlowerProjectileEntity.MIN_SPEED, PhoenixFlowerProjectileEntity.MAX_SPEED);
		float damage = NarutoDamageFormula.calculatePhoenixFlowerDamage(player instanceof LivingEntity livingPlayer ? livingPlayer : null, chargePercent);
		for (int i = 0; i < PROJECTILE_COUNT; i++) {
			PhoenixFlowerProjectileEntity projectile = new PhoenixFlowerProjectileEntity(NarutoModEntities.PHOENIX_FLOWER_PROJECTILE.get(), player.level());
			projectile.setOwner(player);
			projectile.setChargePercent(chargePercent);
			projectile.setDamage(damage);
			projectile.setFlightSpeed(speed);
			if (target.entity() != null) {
				projectile.setTargetEntity(target.entity());
			} else {
				projectile.setFixedTarget(target.position());
			}
			Vec3 offsetSpawn = spawn.add(randomSpreadDirection(player, look, 0.16D).scale(0.18D));
			Vec3 initialDirection = randomSpreadDirection(player, look, Mth.lerp(player.getRandom().nextDouble(), SPREAD_MIN, SPREAD_MAX));
			projectile.setPos(offsetSpawn.x, offsetSpawn.y, offsetSpawn.z);
			projectile.setDeltaMovement(initialDirection.scale(speed));
			projectile.setYRot(player.getYRot());
			projectile.setXRot(player.getXRot());
			player.level().addFreshEntity(projectile);
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		return true;
	}

	private static PhoenixTarget findTarget(Player player) {
		Vec3 start = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		Vec3 end = start.add(look.scale(TARGET_RANGE));
		BlockHitResult blockHit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 rayEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
		EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player.level(), player, start, rayEnd, new AABB(start, rayEnd).inflate(1.0D),
				target -> target instanceof LivingEntity && target.isAlive() && target.isPickable() && target != player);
		if (entityHit != null) {
			return new PhoenixTarget(entityHit.getEntity(), entityHit.getEntity().position().add(0.0D, entityHit.getEntity().getBbHeight() * 0.55D, 0.0D));
		}
		return new PhoenixTarget(null, rayEnd);
	}

	private static Vec3 randomSpreadDirection(Player player, Vec3 look, double spread) {
		Vec3 basis = Math.abs(look.y) < 0.95D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
		Vec3 right = look.cross(basis).normalize();
		Vec3 up = right.cross(look).normalize();
		double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
		return look.add(right.scale(Math.cos(angle) * spread)).add(up.scale(Math.sin(angle) * spread)).normalize();
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}

	private record PhoenixTarget(Entity entity, Vec3 position) {
	}
}
