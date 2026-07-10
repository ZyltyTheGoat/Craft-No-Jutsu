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
import net.mcreator.naruto.network.AshFieldStatePacket;
import net.mcreator.naruto.network.NarutoModVariables;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber
public class HidingInAshAbility {
	private static final String JUTSU_ID = "hiding_in_ash";
	private static final float FIELD_RADIUS = 12.5F;
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
				sendAshState(serverLevel, player, false);
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
				sendAshState(serverLevel, player, false);
			}
			JutsuFeedback.show(player, "Ash faded: no chakra", 50);
			return;
		}
		boolean firstTick = ACTIVE_CASTERS.add(player.getUUID());
		if (firstTick || player.tickCount % SYNC_INTERVAL_TICKS == 0) {
			sendAshState(serverLevel, player, true);
		}
		if (firstTick || player.tickCount % DRAIN_INTERVAL_TICKS == 0) {
			vars.currentChakra = Math.max(0.0D, vars.currentChakra - chakraCost);
			vars.markSyncDirty();
		}
		spawnAsh(serverLevel, player, firstTick);
		pressureEntitiesInAsh(serverLevel, player);
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		Player player = event.getEntity();
		if (ACTIVE_CASTERS.remove(player.getUUID()) && player.level() instanceof ServerLevel serverLevel) {
			sendAshState(serverLevel, player, false);
		}
	}

	private static void spawnAsh(ServerLevel serverLevel, Player player, boolean firstTick) {
		if (!firstTick && player.tickCount % 3 != 0) {
			return;
		}
		int count = firstTick ? 28 : 6;
		double radius = firstTick ? 3.4D : FIELD_RADIUS * 0.48D;
		for (int i = 0; i < count; i++) {
			double angle = serverLevel.random.nextDouble() * Mth.TWO_PI;
			double distance = firstTick ? serverLevel.random.nextDouble() * radius : Math.sqrt(serverLevel.random.nextDouble()) * radius;
			double x = player.getX() + Math.cos(angle) * distance;
			double z = player.getZ() + Math.sin(angle) * distance;
			double y = player.getY() + 0.15D + serverLevel.random.nextDouble() * 1.75D;
			serverLevel.sendParticles(NarutoModParticleTypes.BIG_SMOKE.get(), x, y, z, 1, 0.34D, 0.12D, 0.34D, 0.012D);
			serverLevel.sendParticles(ParticleTypes.SMOKE, x, y + 0.08D, z, firstTick ? 2 : 1, 0.2D, 0.08D, 0.2D, 0.018D);
			if (serverLevel.random.nextFloat() < (firstTick ? 0.35F : 0.14F)) {
				serverLevel.sendParticles(ParticleTypes.FLAME, x, y - 0.05D, z, 1, 0.05D, 0.02D, 0.05D, 0.004D);
			}
		}
	}

	private static void pressureEntitiesInAsh(ServerLevel serverLevel, Player caster) {
		AABB area = caster.getBoundingBox().inflate(FIELD_RADIUS, 3.2D, FIELD_RADIUS);
		for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, area, entity -> entity.isAlive() && entity != caster)) {
			double distance = living.distanceTo(caster);
			if (distance > FIELD_RADIUS) {
				continue;
			}
			living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 28, distance < FIELD_RADIUS * 0.42F ? 1 : 0, true, false));
			living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 28, 0, true, false));
			if (!(living instanceof Player) || distance < FIELD_RADIUS * 0.65F) {
				living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 24, 0, true, false));
			}
		}
	}

	private static void sendAshState(ServerLevel serverLevel, Player player, boolean active) {
		PacketDistributor.sendToPlayersInDimension(serverLevel, new AshFieldStatePacket(player.getUUID(), active, FIELD_RADIUS));
	}
}
