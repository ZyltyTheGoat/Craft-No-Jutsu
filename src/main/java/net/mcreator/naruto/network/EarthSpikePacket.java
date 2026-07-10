package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.client.EarthSpikeClientEffects;

public record EarthSpikePacket(double x, double y, double z, int blockStateId, float chargePercent, float scale, float rotation, int ticks) implements CustomPacketPayload {
	public static final Type<EarthSpikePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "earth_spike"));
	public static final StreamCodec<FriendlyByteBuf, EarthSpikePacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, EarthSpikePacket packet) -> {
		buffer.writeDouble(packet.x);
		buffer.writeDouble(packet.y);
		buffer.writeDouble(packet.z);
		buffer.writeInt(packet.blockStateId);
		buffer.writeFloat(packet.chargePercent);
		buffer.writeFloat(packet.scale);
		buffer.writeFloat(packet.rotation);
		buffer.writeInt(packet.ticks);
	}, buffer -> new EarthSpikePacket(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readInt(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readInt()));

	@Override
	public Type<EarthSpikePacket> type() {
		return TYPE;
	}

	public static void handle(EarthSpikePacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(EarthSpikePacket packet) {
			EarthSpikeClientEffects.addSpike(new Vec3(packet.x, packet.y, packet.z), packet.blockStateId, packet.chargePercent, packet.scale, packet.rotation, packet.ticks);
		}
	}
}
