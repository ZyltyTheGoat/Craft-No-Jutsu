package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuIdList;
import net.mcreator.naruto.MagnetSandReserve;
import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.network.UltimateProtectionPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = NarutoMod.MODID)
public final class UltimateProtectionAbility {
	private static final String JUTSU_ID = "ultimate_protection";
	private static final float MAX_MELEE_ABSORPTION = 10.0F;
	private static final double SAND_RETURN_RATE = 0.70D;
	private static final int HOLD_TICKS = 15;
	private static final int SAND_RETURN_DELAY = 36;
	private static final int FEEDBACK_COOLDOWN_TICKS = 40;
	private static final Map<UUID, Long> LAST_FAILURE_FEEDBACK = new HashMap<>();

	private UltimateProtectionAbility() {
	}

	@SubscribeEvent
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
			return;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		if (!JutsuIdList.has(vars.activeToggleJutsu, JUTSU_ID) || !player.isAlive()) {
			return;
		}
		ProtectionType type = classify(event.getSource());
		if (type == ProtectionType.NONE) {
			return;
		}
		float incomingDamage = Math.max(0.0F, event.getAmount());
		if (incomingDamage <= 0.0F) {
			return;
		}
		float absorbedDamage = type == ProtectionType.MELEE ? Math.min(incomingDamage, MAX_MELEE_ABSORPTION) : incomingDamage;
		double sandCost = calculateSandCost(type, absorbedDamage);
		double chakraCost = calculateChakraCost(absorbedDamage);
		if (MagnetSandReserve.get(player) < sandCost || vars.currentChakra < chakraCost) {
			showFailure(player, vars.currentChakra < chakraCost);
			return;
		}

		MagnetSandReserve.tryConsume(player, sandCost);
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - chakraCost);
		vars.markSyncDirty();
		applyProtection(event, player, type, incomingDamage, absorbedDamage);
		Vec3 direction = type == ProtectionType.FALL ? Vec3.ZERO : protectionDirection(player, event.getSource());
		byte visualType = type == ProtectionType.FALL ? UltimateProtectionPacket.FEET : UltimateProtectionPacket.DIRECTIONAL;
		PacketDistributor.sendToPlayersInDimension(level, new UltimateProtectionPacket(player.getUUID(), visualType, direction.x, direction.y, direction.z, HOLD_TICKS));
		spawnReaction(level, player, direction, type);
		scheduleSandReturn(level, player.getUUID(), sandCost * SAND_RETURN_RATE);
	}

	private static ProtectionType classify(DamageSource source) {
		if (source.is(DamageTypeTags.IS_FALL)) {
			return ProtectionType.FALL;
		}
		if (source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof Projectile) {
			return ProtectionType.PROJECTILE;
		}
		if (source.getDirectEntity() instanceof LivingEntity) {
			return ProtectionType.MELEE;
		}
		return ProtectionType.NONE;
	}

	private static void applyProtection(LivingIncomingDamageEvent event, ServerPlayer player, ProtectionType type, float incomingDamage, float absorbedDamage) {
		if (absorbedDamage >= incomingDamage - 0.001F) {
			event.setCanceled(true);
			event.setInvulnerabilityTicks(5);
		} else {
			event.setAmount(Math.max(0.0F, incomingDamage - absorbedDamage));
		}
		if (type == ProtectionType.FALL) {
			player.resetFallDistance();
		} else if (type == ProtectionType.PROJECTILE && event.getSource().getDirectEntity() instanceof Projectile projectile) {
			projectile.discard();
		}
	}

	private static double calculateSandCost(ProtectionType type, float absorbedDamage) {
		double baseCost = type == ProtectionType.FALL ? 4.0D : type == ProtectionType.PROJECTILE ? 6.0D : 5.0D;
		return Mth.clamp(baseCost + absorbedDamage * 0.85D, 4.0D, 18.0D);
	}

	private static double calculateChakraCost(float absorbedDamage) {
		return Mth.clamp(2.0D + Math.ceil(absorbedDamage * 0.2D), 2.0D, 6.0D);
	}

	private static Vec3 protectionDirection(ServerPlayer player, DamageSource source) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof Projectile && direct.getDeltaMovement().lengthSqr() > 0.001D) {
			return direct.getDeltaMovement().reverse().normalize();
		}
		Entity attacker = source.getEntity();
		Entity origin = attacker != null ? attacker : direct;
		if (origin != null) {
			Vec3 direction = origin.getBoundingBox().getCenter().subtract(player.getBoundingBox().getCenter());
			if (direction.lengthSqr() > 0.001D) {
				return direction.normalize();
			}
		}
		Vec3 fallback = player.getLookAngle();
		return fallback.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : fallback.normalize();
	}

	private static void spawnReaction(ServerLevel level, ServerPlayer player, Vec3 direction, ProtectionType type) {
		Vec3 center = type == ProtectionType.FALL ? player.position().add(0.0D, 0.08D, 0.0D) : player.getBoundingBox().getCenter().add(direction.scale(0.68D));
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()), center.x, center.y, center.z, 9, 0.18D, 0.18D, 0.18D, 0.035D);
		level.playSound(null, player.blockPosition(), SoundEvents.SAND_PLACE, SoundSource.PLAYERS, 0.82F, type == ProtectionType.FALL ? 0.68F : 0.82F);
	}

	private static void scheduleSandReturn(ServerLevel level, UUID playerId, double amount) {
		NarutoMod.queueServerWork(SAND_RETURN_DELAY, () -> {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
			if (player != null) {
				MagnetSandReserve.add(player, amount);
			}
		});
	}

	private static void showFailure(ServerPlayer player, boolean chakraFailure) {
		long gameTime = player.level().getGameTime();
		long lastFeedback = LAST_FAILURE_FEEDBACK.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2L);
		if (gameTime - lastFeedback < FEEDBACK_COOLDOWN_TICKS) {
			return;
		}
		LAST_FAILURE_FEEDBACK.put(player.getUUID(), gameTime);
		JutsuFeedback.show(player, chakraFailure ? "Ultimate Protection needs more chakra" : "Ultimate Protection needs more sand", 45);
	}

	private enum ProtectionType {
		NONE,
		FALL,
		PROJECTILE,
		MELEE
	}
}
