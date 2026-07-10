package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.JutsuIdList;
import net.mcreator.naruto.JutsuRegistry;
import net.mcreator.naruto.init.NarutoModParticleTypes;
import net.mcreator.naruto.init.NarutoModSounds;
import net.mcreator.naruto.network.ChidoriBurstPacket;
import net.mcreator.naruto.network.LightningChakraModeStatePacket;
import net.mcreator.naruto.network.NarutoModVariables;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber
public class LightningChakraModeAbility {
	private static final String JUTSU_ID = "lightning_chakra_mode";
	private static final String LEGACY_JUTSU_ID = "lighthing_chakra_mode";
	private static final int DRAIN_INTERVAL_TICKS = 10;
	private static final int SYNC_INTERVAL_TICKS = 16;
	private static final Set<UUID> ACTIVE_CASTERS = new HashSet<>();

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide() || !(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		boolean active = isToggled(vars.activeToggleJutsu);
		if (!active) {
			if (ACTIVE_CASTERS.remove(player.getUUID())) {
				sendState(serverLevel, player, false);
			}
			return;
		}
		JutsuRegistry.initializeJutsus();
		JutsuData jutsu = JutsuRegistry.getJutsu(JUTSU_ID);
		int chakraCost = jutsu == null ? 4 : Math.max(1, jutsu.getChakraCost());
		if (vars.currentChakra < chakraCost) {
			vars.activeToggleJutsu = JutsuIdList.remove(JutsuIdList.remove(vars.activeToggleJutsu, JUTSU_ID), LEGACY_JUTSU_ID);
			vars.markSyncDirty();
			if (ACTIVE_CASTERS.remove(player.getUUID())) {
				sendState(serverLevel, player, false);
			}
			JutsuFeedback.show(player, "Lightning Chakra Mode faded: no chakra", 50);
			return;
		}
		boolean firstTick = ACTIVE_CASTERS.add(player.getUUID());
		applyModeEffects(player);
		spawnAuraParticles(serverLevel, player, firstTick);
		if (firstTick || player.tickCount % SYNC_INTERVAL_TICKS == 0) {
			sendState(serverLevel, player, true);
		}
		if (firstTick || player.tickCount % DRAIN_INTERVAL_TICKS == 0) {
			vars.currentChakra = Math.max(0.0D, vars.currentChakra - chakraCost);
			vars.markSyncDirty();
		}
		if (firstTick) {
			serverLevel.playSound(null, player.blockPosition(), NarutoModSounds.ELECTRIC_SPARK.get(), SoundSource.PLAYERS, 0.85F, 1.25F);
		} else if (player.tickCount % 26 == 0) {
			serverLevel.playSound(null, player.blockPosition(), NarutoModSounds.ELECTRIC_SPARK.get(), SoundSource.PLAYERS, 0.28F, 1.45F + player.getRandom().nextFloat() * 0.25F);
		}
	}

	@SubscribeEvent
	public static void onAttackEntity(AttackEntityEvent event) {
		Player player = event.getEntity();
		if (player.level().isClientSide() || !(event.getTarget() instanceof LivingEntity target) || target == player) {
			return;
		}
		if (!isToggled(player.getData(NarutoModVariables.PLAYER_VARIABLES).activeToggleJutsu)) {
			return;
		}
		target.invulnerableTime = 0;
		target.hurt(player.damageSources().playerAttack(player), NarutoDamageFormula.calculateInstantJutsuDamage(player, 3.6F, 0.85F));
		target.invulnerableTime = 0;
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 2, true, true));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 24, 0, true, true));
		Vec3 hit = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
		Vec3 push = player.getLookAngle();
		if (push.lengthSqr() < 0.001D) {
			push = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		target.setDeltaMovement(target.getDeltaMovement().scale(0.35D).add(push.normalize().scale(0.35D)).add(0.0D, 0.08D, 0.0D));
		target.hurtMarked = true;
		if (player.level() instanceof ServerLevel serverLevel) {
			PacketDistributor.sendToPlayersInDimension(serverLevel, new ChidoriBurstPacket(target.getId(), hit.x, hit.y, hit.z, target.getXRot(), target.getYRot(), 8, 0.7F, 0.95F));
			serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, hit.x, hit.y, hit.z, 14, 0.26D, 0.28D, 0.26D, 0.08D);
			serverLevel.playSound(null, target.blockPosition(), NarutoModSounds.ELECTRIC_SPARK.get(), SoundSource.PLAYERS, 0.65F, 1.1F);
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		Player player = event.getEntity();
		if (ACTIVE_CASTERS.remove(player.getUUID()) && player.level() instanceof ServerLevel serverLevel) {
			sendState(serverLevel, player, false);
		}
	}

	private static boolean isToggled(String toggles) {
		return JutsuIdList.has(toggles, JUTSU_ID) || JutsuIdList.has(toggles, LEGACY_JUTSU_ID);
	}

	private static void applyModeEffects(Player player) {
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 8, Math.max(4, chidoriLikeSpeedAmplifier(player) - 2), true, false));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 8, 1, true, false));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 8, 0, true, false));
	}

	private static int chidoriLikeSpeedAmplifier(Player player) {
		String rank = player.getData(NarutoModVariables.PLAYER_VARIABLES).rank;
		return switch (rank) {
			case "Genin" -> 3;
			case "Chunin" -> 5;
			case "Jonin" -> 7;
			case "Kage" -> 9;
			case "Legend" -> 10;
			default -> 3;
		};
	}

	private static void spawnAuraParticles(ServerLevel level, Player player, boolean firstTick) {
		if (!firstTick && player.tickCount % 2 != 0) {
			return;
		}
		int sparks = firstTick ? 26 : 4;
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.9D, player.getZ(), sparks, 0.45D, 0.75D, 0.45D, 0.055D);
		level.sendParticles(NarutoModParticleTypes.LIGHTNING.get(), player.getX(), player.getY() + 0.95D, player.getZ(), firstTick ? 12 : 2, 0.36D, 0.65D, 0.36D, 0.02D);
		if (player.isSprinting() && player.tickCount % 4 == 0) {
			Vec3 back = player.getLookAngle();
			if (back.lengthSqr() < 0.001D) {
				back = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
			}
			back = back.normalize().scale(-0.55D);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX() + back.x, player.getY() + 0.25D, player.getZ() + back.z, 2, 0.18D, 0.08D, 0.18D, 0.02D);
		}
	}

	private static void sendState(ServerLevel level, Player player, boolean active) {
		PacketDistributor.sendToPlayersInDimension(level, new LightningChakraModeStatePacket(player.getUUID(), active, 24));
	}
}
