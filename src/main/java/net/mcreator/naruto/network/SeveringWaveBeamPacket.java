package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.naruto.client.SeveringWaveClientEffects;
import net.mcreator.naruto.NarutoMod;

import java.util.UUID;

public record SeveringWaveBeamPacket(UUID casterId, boolean active, double originX, double originY, double originZ, double directionX, double directionY, double directionZ, float range, float thickness, int ticks) implements CustomPacketPayload {

	public static final Type<SeveringWaveBeamPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "severing_wave_beam"));
	public static final StreamCodec<FriendlyByteBuf, SeveringWaveBeamPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, SeveringWaveBeamPacket packet) -> {
		buffer.writeUUID(packet.casterId);
		buffer.writeBoolean(packet.active);
		buffer.writeDouble(packet.originX);
		buffer.writeDouble(packet.originY);
		buffer.writeDouble(packet.originZ);
		buffer.writeDouble(packet.directionX);
		buffer.writeDouble(packet.directionY);
		buffer.writeDouble(packet.directionZ);
		buffer.writeFloat(packet.range);
		buffer.writeFloat(packet.thickness);
		buffer.writeInt(packet.ticks);
	}, buffer -> new SeveringWaveBeamPacket(buffer.readUUID(), buffer.readBoolean(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readFloat(),
			buffer.readInt()));
	@Override
	public Type<SeveringWaveBeamPacket> type() {
		return TYPE;
	}

	public static void handle(SeveringWaveBeamPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(SeveringWaveBeamPacket packet) {
			SeveringWaveClientEffects.setActive(packet.casterId(), packet.active(), new Vec3(packet.originX(), packet.originY(), packet.originZ()), new Vec3(packet.directionX(), packet.directionY(), packet.directionZ()), packet.range(),
					packet.thickness(), packet.ticks());
		}
	}
}