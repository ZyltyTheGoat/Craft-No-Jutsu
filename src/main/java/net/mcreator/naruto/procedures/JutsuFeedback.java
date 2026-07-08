package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.mcreator.naruto.network.JutsuHudFeedbackPacket;

public final class JutsuFeedback {
	private static final int DEFAULT_TICKS = 50;
	private static final int COOLDOWN_TICKS = 90;

	private JutsuFeedback() {
	}

	public static void show(Player player, String message) {
		show(player, message, DEFAULT_TICKS);
	}

	public static void show(Player player, String message, int ticks) {
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, JutsuHudFeedbackPacket.feedback(message, Math.max(1, ticks)));
		}
	}

	public static void showCooldown(Player player, String displayName, int remainingTicks) {
		show(player, JutsuCooldowns.formatCooldownMessage(displayName, remainingTicks), COOLDOWN_TICKS);
	}

	public static void notEnoughChakra(Player player) {
		show(player, "Not enough chakra");
	}
}
