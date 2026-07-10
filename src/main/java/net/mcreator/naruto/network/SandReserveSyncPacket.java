package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.naruto.client.screens.MagnetSandOverlay;
import net.mcreator.naruto.NarutoMod;

public record SandReserveSyncPacket(double amount, double capacity) implements CustomPacketPayload {
	public static final Type<SandReserveSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "sand_reserve_sync"));
	public static final StreamCodec<FriendlyByteBuf, SandReserveSyncPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, SandReserveSyncPacket packet) -> {
		buffer.writeDouble(packet.amount);
		buffer.writeDouble(packet.capacity);
	}, (FriendlyByteBuf buffer) -> new SandReserveSyncPacket(buffer.readDouble(), buffer.readDouble()));

	@Override
	public Type<SandReserveSyncPacket> type() {
		return TYPE;
	}

	public static void handle(SandReserveSyncPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.sync(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void sync(SandReserveSyncPacket packet) {
			MagnetSandOverlay.setReserve(packet.amount, packet.capacity);
		}
	}
}
