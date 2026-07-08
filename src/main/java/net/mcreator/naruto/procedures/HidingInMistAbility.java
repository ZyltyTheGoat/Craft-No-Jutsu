package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.JutsuIdList;
import net.mcreator.naruto.JutsuRegistry;
import net.mcreator.naruto.init.NarutoModParticleTypes;
import net.mcreator.naruto.network.MistFieldStatePacket;
import net.mcreator.naruto.network.NarutoModVariables;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber
public class HidingInMistAbility {
	private static final String JUTSU_ID = "hiding_in_mist";
	private static final float FIELD_RADIUS = 16.0F;
	private static final int DRAIN_INTERVAL_TICKS = 10;
	private static final int SYNC_INTERVAL_TICKS = 20;
	private static final Set<UUID> ACTIVE_CASTERS = new HashSet<>();

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide() || !(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		boolean active = JutsuIdList.has(vars.activeToggleJutsu, JUTSU_ID);
		if (!active) {
			if (ACTIVE_CASTERS.remove(player.getUUID())) {
				sendMistState(serverLevel, player, false);
			}
			return;
		}
		JutsuRegistry.initializeJutsus();
		JutsuData jutsu = JutsuRegistry.getJutsu(JUTSU_ID);
		int chakraCost = jutsu == null ? 2 : Math.max(1, jutsu.getChakraCost());
		if (vars.currentChakra < chakraCost) {
			vars.activeToggleJutsu = JutsuIdList.remove(vars.activeToggleJutsu, JUTSU_ID);
			vars.markSyncDirty();
			if (ACTIVE_CASTERS.remove(player.getUUID())) {
				sendMistState(serverLevel, player, false);
			}
			JutsuFeedback.show(player, "Mist faded: no chakra", 50);
			return;
		}
		boolean firstTick = ACTIVE_CASTERS.add(player.getUUID());
		if (firstTick || player.tickCount % SYNC_INTERVAL_TICKS == 0) {
			sendMistState(serverLevel, player, true);
		}
		if (firstTick || player.tickCount % DRAIN_INTERVAL_TICKS == 0) {
			vars.currentChakra = Math.max(0.0D, vars.currentChakra - chakraCost);
			vars.markSyncDirty();
		}
		spawnMist(serverLevel, player, firstTick);
		pressureEntitiesInMist(serverLevel, player);
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		Player player = event.getEntity();
		if (ACTIVE_CASTERS.remove(player.getUUID()) && player.level() instanceof ServerLevel serverLevel) {
			sendMistState(serverLevel, player, false);
		}
	}

	private static void spawnMist(ServerLevel serverLevel, Player player, boolean firstTick) {
		if (!firstTick && player.tickCount % 4 != 0) {
			return;
		}
		int count = firstTick ? 22 : 4;
		double radius = firstTick ? 4.8D : FIELD_RADIUS * 0.55D;
		for (int i = 0; i < count; i++) {
			double angle = serverLevel.random.nextDouble() * Mth.TWO_PI;
			double distance = firstTick ? serverLevel.random.nextDouble() * radius : Math.sqrt(serverLevel.random.nextDouble()) * radius;
			double x = player.getX() + Math.cos(angle) * distance;
			double z = player.getZ() + Math.sin(angle) * distance;
			double y = player.getY() + 0.2D + serverLevel.random.nextDouble() * 2.15D;
			double driftX = (serverLevel.random.nextDouble() - 0.5D) * 0.025D;
			double driftZ = (serverLevel.random.nextDouble() - 0.5D) * 0.025D;
			serverLevel.sendParticles(NarutoModParticleTypes.MIST.get(), x, y, z, 1, 0.45D, 0.18D, 0.45D, 0.01D);
			if (firstTick && i % 3 == 0) {
				serverLevel.sendParticles(ParticleTypes.CLOUD, x, y + 0.1D, z, 1, 0.28D, 0.1D, 0.28D, 0.02D);
			}
			if (!firstTick) {
				serverLevel.sendParticles(NarutoModParticleTypes.MIST.get(), x + driftX, y, z + driftZ, 1, 0.2D, 0.08D, 0.2D, 0.003D);
			}
		}
	}

	private static void pressureEntitiesInMist(ServerLevel serverLevel, Player caster) {
		AABB area = caster.getBoundingBox().inflate(FIELD_RADIUS, 4.0D, FIELD_RADIUS);
		for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, area, entity -> entity.isAlive() && entity != caster)) {
			double distance = living.distanceTo(caster);
			if (distance > FIELD_RADIUS) {
				continue;
			}
			int amplifier = distance < FIELD_RADIUS * 0.45F ? 1 : 0;
			living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, amplifier, true, false));
			if (!(living instanceof Player) && distance < FIELD_RADIUS * 0.65F) {
				living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, true, false));
			}
		}
	}

	private static void sendMistState(ServerLevel serverLevel, Player player, boolean active) {
		PacketDistributor.sendToPlayersInDimension(serverLevel, new MistFieldStatePacket(player.getUUID(), active, FIELD_RADIUS));
	}
}
