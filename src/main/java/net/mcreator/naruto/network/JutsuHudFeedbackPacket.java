package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.naruto.client.screens.DisplayChakraOverlay;
import net.mcreator.naruto.NarutoMod;

public record JutsuHudFeedbackPacket(String message, int ticks) implements CustomPacketPayload {
	public static final Type<JutsuHudFeedbackPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "jutsu_hud_feedback"));
	public static final StreamCodec<FriendlyByteBuf, JutsuHudFeedbackPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, JutsuHudFeedbackPacket packet) -> {
		buffer.writeUtf(packet.message);
		buffer.writeInt(packet.ticks);
	}, (FriendlyByteBuf buffer) -> new JutsuHudFeedbackPacket(buffer.readUtf(), buffer.readInt()));

	public static JutsuHudFeedbackPacket cooldown(String message, int ticks) {
		return new JutsuHudFeedbackPacket(message, ticks);
	}

	public static JutsuHudFeedbackPacket feedback(String message, int ticks) {
		return new JutsuHudFeedbackPacket(message, ticks);
	}

	@Override
	public Type<JutsuHudFeedbackPacket> type() {
		return TYPE;
	}

	public static void handle(JutsuHudFeedbackPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.show(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void show(JutsuHudFeedbackPacket packet) {
			DisplayChakraOverlay.showCooldown(packet.message, packet.ticks);
		}
	}
}
