package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.client.HidingInMistClientEffects;

import java.util.UUID;

public record MistFieldStatePacket(UUID casterId, boolean active, float radius) implements CustomPacketPayload {
	public static final Type<MistFieldStatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "mist_field_state"));
	public static final StreamCodec<FriendlyByteBuf, MistFieldStatePacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, MistFieldStatePacket packet) -> {
		buffer.writeUUID(packet.casterId);
		buffer.writeBoolean(packet.active);
		buffer.writeFloat(packet.radius);
	}, buffer -> new MistFieldStatePacket(buffer.readUUID(), buffer.readBoolean(), buffer.readFloat()));

	@Override
	public Type<MistFieldStatePacket> type() {
		return TYPE;
	}

	public static void handle(MistFieldStatePacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(MistFieldStatePacket packet) {
			HidingInMistClientEffects.setActive(packet.casterId, packet.active, packet.radius);
		}
	}
}
