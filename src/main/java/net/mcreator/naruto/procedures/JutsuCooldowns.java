package net.mcreator.naruto.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;

import net.mcreator.naruto.network.JutsuCooldownSyncPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class JutsuCooldowns {
	private static final Map<String, Long> COOLDOWNS = new HashMap<>();

	private JutsuCooldowns() {
	}

	public static boolean tryStart(Player player, String jutsuId, int cooldownTicks) {
		if (player == null || jutsuId == null || jutsuId.trim().isEmpty() || cooldownTicks <= 0) {
			return true;
		}
		long gameTime = player.level().getGameTime();
		String key = key(player.getUUID(), jutsuId);
		long readyAt = COOLDOWNS.getOrDefault(key, 0L);
		if (readyAt - gameTime > cooldownTicks) {
			COOLDOWNS.remove(key);
			readyAt = 0L;
		}
		if (gameTime < readyAt) {
			return false;
		}
		int cooldown = Math.max(1, cooldownTicks);
		COOLDOWNS.put(key, gameTime + cooldown);
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, new JutsuCooldownSyncPacket(jutsuId.trim(), cooldown));
		}
		return true;
	}

	public static int getRemainingTicks(Player player, String jutsuId) {
		if (player == null || jutsuId == null || jutsuId.trim().isEmpty()) {
			return 0;
		}
		String key = key(player.getUUID(), jutsuId);
		long readyAt = COOLDOWNS.getOrDefault(key, 0L);
		long remaining = readyAt - player.level().getGameTime();
		if (remaining <= 0) {
			COOLDOWNS.remove(key);
			return 0;
		}
		return (int) Math.min(Integer.MAX_VALUE, remaining);
	}

	public static String formatTicks(int ticks) {
		return ticks < 20 ? ticks + "t" : String.format("%.1fs", ticks / 20.0D);
	}

	public static String formatCooldownMessage(String displayName, int ticks) {
		String name = displayName == null || displayName.isEmpty() ? "Jutsu" : displayName;
		return name + " on cooldown: " + formatTicks(ticks);
	}

	private static String key(UUID playerId, String jutsuId) {
		return playerId + ":" + jutsuId.trim();
	}
}
