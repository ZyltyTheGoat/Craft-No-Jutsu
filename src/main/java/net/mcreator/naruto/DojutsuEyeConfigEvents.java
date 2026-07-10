package net.mcreator.naruto;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.naruto.network.DojutsuEyeConfigSyncPacket;

@EventBusSubscriber
public class DojutsuEyeConfigEvents {
	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			sync(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			sync(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			sync(player);
		}
	}

	private static void sync(ServerPlayer player) {
		DojutsuEyeConfigStore.Config config = DojutsuEyeConfigStore.load(player);
		PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(), new DojutsuEyeConfigSyncPacket(player.getId(), config.width(), config.height(), config.spacing(), config.yOffset(), config.zOffset()));
	}
}
