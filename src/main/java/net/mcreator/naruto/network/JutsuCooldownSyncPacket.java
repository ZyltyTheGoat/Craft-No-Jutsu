package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.client.screens.DisplayChakraOverlay;

public record JutsuCooldownSyncPacket(String jutsuId, int cooldownTicks) implements CustomPacketPayload {
	public static final Type<JutsuCooldownSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "jutsu_cooldown_sync"));
	public static final StreamCodec<FriendlyByteBuf, JutsuCooldownSyncPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, JutsuCooldownSyncPacket packet) -> {
		buffer.writeUtf(packet.jutsuId);
		buffer.writeInt(packet.cooldownTicks);
	}, (FriendlyByteBuf buffer) -> new JutsuCooldownSyncPacket(buffer.readUtf(), buffer.readInt()));

	@Override
	public Type<JutsuCooldownSyncPacket> type() {
		return TYPE;
	}

	public static void handle(JutsuCooldownSyncPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.sync(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void sync(JutsuCooldownSyncPacket packet) {
			DisplayChakraOverlay.startCooldown(packet.jutsuId, packet.cooldownTicks);
		}
	}
}
