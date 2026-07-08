package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.init.NarutoModSounds;
import net.mcreator.naruto.network.ChidoriBurstPacket;
import net.mcreator.naruto.network.ChidoriStatePacket;
import net.mcreator.naruto.network.NarutoModVariables;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class ChidoriAbility {
	private static final String JUTSU_ID = "chidori";
	private static final int ACTIVE_TICKS = 200;
	private static final int SHOCK_TICKS = 25;
	private static final int WET_SHOCK_TICKS = 40;
	private static final float HIT_BURST_SCALE = 1.5F;
	private static final Map<UUID, ChidoriState> ACTIVE = new HashMap<>();

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
			showCooldown(player, jutsuData);
			return true;
		}
		float damage = NarutoDamageFormula.calculateChidoriDamage(player instanceof LivingEntity livingPlayer ? livingPlayer : null, chargePercent);
		ACTIVE.put(player.getUUID(), new ChidoriState(ACTIVE_TICKS, chargePercent, damage, chidoriSpeedAmplifier(player), !player.getMainHandItem().isEmpty()));
		syncState(player, true, ACTIVE_TICKS, chargePercent);
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		if (player.level() instanceof ServerLevel serverLevel) {
			serverLevel.playSound(null, player.blockPosition(), NarutoModSounds.ELECTRIC_SPARK.get(), SoundSource.PLAYERS, 0.75F, 1.45F);
		}
		return true;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide()) {
			return;
		}
		ChidoriState state = ACTIVE.get(player.getUUID());
		if (state == null) {
			return;
		}
		if (!player.isAlive() || state.remainingTicks <= 0) {
			deactivate(player);
			return;
		}
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6, state.speedAmplifier, true, false));
		if (player.level() instanceof ServerLevel serverLevel) {
			if (player.isSprinting() && state.remainingTicks % 3 == 0) {
				Vec3 back = player.getLookAngle().normalize().scale(-0.35D);
				serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX() + back.x, player.getY() + 0.7D, player.getZ() + back.z, 1, 0.16D, 0.18D, 0.16D, 0.01D);
			}
			if (state.remainingTicks % 22 == 0) {
				serverLevel.playSound(null, player.blockPosition(), NarutoModSounds.ELECTRIC_SPARK.get(), SoundSource.PLAYERS, 0.32F, 1.25F + player.getRandom().nextFloat() * 0.25F);
			}
		}
		state.remainingTicks--;
	}

	@SubscribeEvent
	public static void onAttackEntity(AttackEntityEvent event) {
		Player player = event.getEntity();
		if (player.level().isClientSide()) {
			return;
		}
		ChidoriState state = ACTIVE.get(player.getUUID());
		if (state == null || !(event.getTarget() instanceof LivingEntity target) || target == player) {
			return;
		}
		deactivate(player);
		target.invulnerableTime = 0;
		target.hurt(player.damageSources().playerAttack(player), state.damage);
		target.invulnerableTime = 0;
		boolean weaponStrike = state.weaponImbued || !player.getMainHandItem().isEmpty();
		int shockTicks = (target.isInWaterOrRain() || target.isInWater() ? WET_SHOCK_TICKS : SHOCK_TICKS) + (weaponStrike ? 8 : 0);
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, shockTicks, weaponStrike ? 7 : 6, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, shockTicks, weaponStrike ? 2 : 1, true, true));
		target.setDeltaMovement(target.getDeltaMovement().scale(0.08D));
		if (target.level() instanceof ServerLevel serverLevel) {
			Vec3 hit = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
			int burstTicks = Mth.ceil((weaponStrike ? 11 : 9) * HIT_BURST_SCALE);
			PacketDistributor.sendToPlayersInDimension(serverLevel, new ChidoriBurstPacket(target.getId(), hit.x, hit.y, hit.z, target.getXRot(), target.getYRot(), burstTicks, state.chargePercent, HIT_BURST_SCALE));
			serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, hit.x, hit.y, hit.z, 24, 0.28D, 0.34D, 0.28D, 0.08D);
			serverLevel.sendParticles(ParticleTypes.FLASH, hit.x, hit.y, hit.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			serverLevel.playSound(null, target.blockPosition(), NarutoModSounds.ELECTRIC_SPARK.get(), SoundSource.PLAYERS, 1.0F, 0.85F);
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		ACTIVE.remove(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof Player player) {
			deactivate(player);
		}
	}

	public static boolean isActive(Player player) {
		return player != null && ACTIVE.containsKey(player.getUUID());
	}

	public static void clearInvalidStates() {
		Iterator<Map.Entry<UUID, ChidoriState>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getValue().remainingTicks <= 0) {
				iterator.remove();
			}
		}
	}

	private static void deactivate(Player player) {
		if (player == null) {
			return;
		}
		if (ACTIVE.remove(player.getUUID()) != null) {
			syncState(player, false, 0, 0.0F);
		}
	}

	private static int chidoriSpeedAmplifier(Player player) {
		String rank = player.getData(NarutoModVariables.PLAYER_VARIABLES).rank;
		int base = switch (rank) {
			case "Genin" -> 3;
			case "Chunin" -> 6;
			case "Jonin" -> 9;
			case "Kage" -> 12;
			case "Legend" -> 13;
			default -> 0;
		};
		return Math.min(18, base + 2);
	}

	private static void showCooldown(Player player, JutsuData jutsuData) {
		JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, JUTSU_ID));
	}

	private static void syncState(Player player, boolean active, int ticks, float chargePercent) {
		if (player.level() instanceof ServerLevel serverLevel) {
			PacketDistributor.sendToPlayersInDimension(serverLevel, new ChidoriStatePacket(player.getUUID(), active, ticks, chargePercent));
		}
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}

	private static final class ChidoriState {
		private int remainingTicks;
		private final float chargePercent;
		private final float damage;
		private final int speedAmplifier;
		private final boolean weaponImbued;

		private ChidoriState(int remainingTicks, float chargePercent, float damage, int speedAmplifier, boolean weaponImbued) {
			this.remainingTicks = remainingTicks;
			this.chargePercent = chargePercent;
			this.damage = damage;
			this.speedAmplifier = speedAmplifier;
			this.weaponImbued = weaponImbued;
		}
	}
}
