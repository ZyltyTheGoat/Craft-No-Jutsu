package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.client.WaterPrisonClientEffects;

import java.util.UUID;

public record WaterPrisonStatePacket(UUID casterId, int targetId, boolean active) implements CustomPacketPayload {
	public static final Type<WaterPrisonStatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "water_prison_state"));
	public static final StreamCodec<FriendlyByteBuf, WaterPrisonStatePacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, WaterPrisonStatePacket packet) -> {
		buffer.writeUUID(packet.casterId);
		buffer.writeInt(packet.targetId);
		buffer.writeBoolean(packet.active);
	}, buffer -> new WaterPrisonStatePacket(buffer.readUUID(), buffer.readInt(), buffer.readBoolean()));

	@Override
	public Type<WaterPrisonStatePacket> type() {
		return TYPE;
	}

	public static void handle(WaterPrisonStatePacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(WaterPrisonStatePacket packet) {
			WaterPrisonClientEffects.setActive(packet.casterId, packet.targetId, packet.active);
		}
	}
}
