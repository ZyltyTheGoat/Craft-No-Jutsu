package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.client.LightningChakraModeClientEffects;

import java.util.UUID;

public record LightningChakraModeStatePacket(UUID playerId, boolean active, int ticks) implements CustomPacketPayload {
	public static final Type<LightningChakraModeStatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "lightning_chakra_mode_state"));
	public static final StreamCodec<FriendlyByteBuf, LightningChakraModeStatePacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, LightningChakraModeStatePacket packet) -> {
		buffer.writeUUID(packet.playerId);
		buffer.writeBoolean(packet.active);
		buffer.writeInt(packet.ticks);
	}, buffer -> new LightningChakraModeStatePacket(buffer.readUUID(), buffer.readBoolean(), buffer.readInt()));

	@Override
	public Type<LightningChakraModeStatePacket> type() {
		return TYPE;
	}

	public static void handle(LightningChakraModeStatePacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(LightningChakraModeStatePacket packet) {
			LightningChakraModeClientEffects.setActive(packet.playerId, packet.active, packet.ticks);
		}
	}
}
