package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.naruto.client.ChidoriClientEffects;
import net.mcreator.naruto.NarutoMod;

public record ChidoriBurstPacket(int entityId, double x, double y, double z, float xRot, float yRot, int ticks, float chargePercent, float scale) implements CustomPacketPayload {
	public static final Type<ChidoriBurstPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "chidori_burst"));
	public static final StreamCodec<FriendlyByteBuf, ChidoriBurstPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, ChidoriBurstPacket packet) -> {
		buffer.writeInt(packet.entityId);
		buffer.writeDouble(packet.x);
		buffer.writeDouble(packet.y);
		buffer.writeDouble(packet.z);
		buffer.writeFloat(packet.xRot);
		buffer.writeFloat(packet.yRot);
		buffer.writeInt(packet.ticks);
		buffer.writeFloat(packet.chargePercent);
		buffer.writeFloat(packet.scale);
	}, buffer -> new ChidoriBurstPacket(buffer.readInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readInt(), buffer.readFloat(), buffer.readFloat()));

	@Override
	public Type<ChidoriBurstPacket> type() {
		return TYPE;
	}

	public static void handle(ChidoriBurstPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(ChidoriBurstPacket packet) {
			ChidoriClientEffects.addHitBurst(packet.entityId, packet.x, packet.y, packet.z, packet.xRot, packet.yRot, packet.ticks, packet.chargePercent, packet.scale);
		}
	}
}
