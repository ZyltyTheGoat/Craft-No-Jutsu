package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;

import net.mcreator.naruto.JutsuIdList;
import net.mcreator.naruto.MagnetSandReserve;
import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.network.SandLevitationStatePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public final class SandLevitationAbility {
	private static final String JUTSU_ID = "sand_levitation";
	private static final double SAND_COST = 20.0D;
	private static final double RETURN_RATE = 0.75D;
	private static final double CHAKRA_DRAIN = 1.0D;
	private static final int DRAIN_INTERVAL_TICKS = 20;
	private static final int SYNC_INTERVAL_TICKS = 16;
	private static final Map<UUID, FlightState> ACTIVE = new HashMap<>();

	private SandLevitationAbility() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level)) {
			return;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		boolean toggled = JutsuIdList.has(vars.activeToggleJutsu, JUTSU_ID);
		FlightState state = ACTIVE.get(player.getUUID());
		if (!toggled || !player.isAlive()) {
			if (state != null) {
				stop(level, serverPlayer, vars, state, player.isAlive());
			}
			return;
		}
		if (state == null) {
			start(level, serverPlayer, vars);
			return;
		}

		maintainFlight(serverPlayer);
		if (player.tickCount % SYNC_INTERVAL_TICKS == 0) {
			sendState(level, player, SandLevitationStatePacket.REFRESH);
		}
		if (player.tickCount % DRAIN_INTERVAL_TICKS == 0) {
			if (vars.currentChakra < CHAKRA_DRAIN) {
				vars.activeToggleJutsu = JutsuIdList.remove(vars.activeToggleJutsu, JUTSU_ID);
				vars.markSyncDirty();
				stop(level, serverPlayer, vars, state, true);
				JutsuFeedback.show(player, "Sand Levitation ended: no chakra", 50);
				return;
			}
			vars.currentChakra = Math.max(0.0D, vars.currentChakra - CHAKRA_DRAIN);
			vars.markSyncDirty();
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
			return;
		}
		FlightState state = ACTIVE.get(player.getUUID());
		if (state != null) {
			NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
			vars.activeToggleJutsu = JutsuIdList.remove(vars.activeToggleJutsu, JUTSU_ID);
			stop(level, player, vars, state, true);
		}
	}

	private static void start(ServerLevel level, ServerPlayer player, NarutoModVariables.PlayerVariables vars) {
		if (vars.currentChakra < CHAKRA_DRAIN) {
			vars.activeToggleJutsu = JutsuIdList.remove(vars.activeToggleJutsu, JUTSU_ID);
			vars.markSyncDirty();
			JutsuFeedback.notEnoughChakra(player);
			return;
		}
		if (!MagnetSandReserve.tryConsume(player, SAND_COST)) {
			vars.activeToggleJutsu = JutsuIdList.remove(vars.activeToggleJutsu, JUTSU_ID);
			vars.markSyncDirty();
			JutsuFeedback.show(player, "Sand Levitation requires 20 sand", 60);
			return;
		}

		Abilities abilities = player.getAbilities();
		FlightState state = new FlightState(abilities.mayfly, abilities.flying, abilities.getFlyingSpeed());
		ACTIVE.put(player.getUUID(), state);
		maintainFlight(player);
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - CHAKRA_DRAIN);
		vars.markSyncDirty();
		sendState(level, player, SandLevitationStatePacket.START);
		level.playSound(null, player.blockPosition(), SoundEvents.SAND_PLACE, SoundSource.PLAYERS, 0.85F, 0.72F);
	}

	private static void stop(ServerLevel level, ServerPlayer player, NarutoModVariables.PlayerVariables vars, FlightState state, boolean returnSand) {
		ACTIVE.remove(player.getUUID());
		Abilities abilities = player.getAbilities();
		abilities.mayfly = state.hadMayfly || player.isCreative() || player.isSpectator();
		abilities.flying = abilities.mayfly && state.wasFlying;
		abilities.setFlyingSpeed(state.flyingSpeed);
		player.onUpdateAbilities();
		if (returnSand) {
			MagnetSandReserve.add(player, SAND_COST * RETURN_RATE);
		}
		sendState(level, player, SandLevitationStatePacket.STOP);
		level.playSound(null, player.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.65F, 0.62F);
	}

	private static void maintainFlight(ServerPlayer player) {
		Abilities abilities = player.getAbilities();
		boolean changed = !abilities.mayfly || !abilities.flying;
		abilities.mayfly = true;
		abilities.flying = true;
		abilities.setFlyingSpeed(0.045F);
		player.fallDistance = 0.0F;
		if (changed) {
			player.onUpdateAbilities();
		}
	}

	private static void sendState(ServerLevel level, Player player, byte action) {
		PacketDistributor.sendToPlayersInDimension(level, new SandLevitationStatePacket(player.getUUID(), action, 28));
	}

	private record FlightState(boolean hadMayfly, boolean wasFlying, float flyingSpeed) {
	}
}
