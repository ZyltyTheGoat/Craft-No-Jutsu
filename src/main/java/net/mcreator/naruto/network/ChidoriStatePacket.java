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

import java.util.UUID;

public record ChidoriStatePacket(UUID playerId, boolean active, int ticks, float chargePercent) implements CustomPacketPayload {
	public static final Type<ChidoriStatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "chidori_state"));
	public static final StreamCodec<FriendlyByteBuf, ChidoriStatePacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, ChidoriStatePacket packet) -> {
		buffer.writeUUID(packet.playerId);
		buffer.writeBoolean(packet.active);
		buffer.writeInt(packet.ticks);
		buffer.writeFloat(packet.chargePercent);
	}, buffer -> new ChidoriStatePacket(buffer.readUUID(), buffer.readBoolean(), buffer.readInt(), buffer.readFloat()));

	@Override
	public Type<ChidoriStatePacket> type() {
		return TYPE;
	}

	public static void handle(ChidoriStatePacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(ChidoriStatePacket packet) {
			ChidoriClientEffects.setActive(packet.playerId, packet.active, packet.ticks, packet.chargePercent);
		}
	}
}
