package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.client.NeedleSphereClientEffects;

import java.util.UUID;

public record NeedleSpherePacket(UUID casterId, float radius, float chargePercent, int ticks) implements CustomPacketPayload {
	public static final Type<NeedleSpherePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "needle_sphere"));
	public static final StreamCodec<FriendlyByteBuf, NeedleSpherePacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, NeedleSpherePacket packet) -> {
		buffer.writeUUID(packet.casterId);
		buffer.writeFloat(packet.radius);
		buffer.writeFloat(packet.chargePercent);
		buffer.writeInt(packet.ticks);
	}, buffer -> new NeedleSpherePacket(buffer.readUUID(), buffer.readFloat(), buffer.readFloat(), buffer.readInt()));

	@Override
	public Type<NeedleSpherePacket> type() {
		return TYPE;
	}

	public static void handle(NeedleSpherePacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(NeedleSpherePacket packet) {
			NeedleSphereClientEffects.addSphere(packet.casterId(), packet.radius(), packet.chargePercent(), packet.ticks());
		}
	}
}
