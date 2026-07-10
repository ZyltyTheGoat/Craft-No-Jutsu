package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.client.SandLevitationClientEffects;

import java.util.UUID;

public record SandLevitationStatePacket(UUID playerId, byte action, int ticks) implements CustomPacketPayload {
	public static final byte STOP = 0;
	public static final byte START = 1;
	public static final byte REFRESH = 2;
	public static final Type<SandLevitationStatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "sand_levitation_state"));
	public static final StreamCodec<FriendlyByteBuf, SandLevitationStatePacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, SandLevitationStatePacket packet) -> {
		buffer.writeUUID(packet.playerId);
		buffer.writeByte(packet.action);
		buffer.writeInt(packet.ticks);
	}, buffer -> new SandLevitationStatePacket(buffer.readUUID(), buffer.readByte(), buffer.readInt()));

	@Override
	public Type<SandLevitationStatePacket> type() {
		return TYPE;
	}

	public static void handle(SandLevitationStatePacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(SandLevitationStatePacket packet) {
			SandLevitationClientEffects.setState(packet.playerId, packet.action, packet.ticks);
		}
	}
}
