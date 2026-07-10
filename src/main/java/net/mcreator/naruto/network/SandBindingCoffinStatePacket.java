package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.client.SandBindingCoffinClientEffects;

import java.util.UUID;

public record SandBindingCoffinStatePacket(UUID casterId, int targetId, byte action, int ticks) implements CustomPacketPayload {
	public static final byte STOP = 0;
	public static final byte START = 1;
	public static final byte REFRESH = 2;
	public static final Type<SandBindingCoffinStatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "sand_binding_coffin_state"));
	public static final StreamCodec<FriendlyByteBuf, SandBindingCoffinStatePacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, SandBindingCoffinStatePacket packet) -> {
		buffer.writeUUID(packet.casterId);
		buffer.writeInt(packet.targetId);
		buffer.writeByte(packet.action);
		buffer.writeInt(packet.ticks);
	}, buffer -> new SandBindingCoffinStatePacket(buffer.readUUID(), buffer.readInt(), buffer.readByte(), buffer.readInt()));

	@Override
	public Type<SandBindingCoffinStatePacket> type() {
		return TYPE;
	}

	public static void handle(SandBindingCoffinStatePacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(SandBindingCoffinStatePacket packet) {
			SandBindingCoffinClientEffects.setState(packet.casterId, packet.targetId, packet.action, packet.ticks);
		}
	}
}
