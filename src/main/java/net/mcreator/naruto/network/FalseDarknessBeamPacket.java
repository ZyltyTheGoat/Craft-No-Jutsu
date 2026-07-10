package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.client.FalseDarknessClientEffects;

import java.util.UUID;

public record FalseDarknessBeamPacket(UUID casterId, double originX, double originY, double originZ, double directionX, double directionY, double directionZ, float range, float thickness, float chargePercent, boolean mainSpear, int spearIndex, int ticks) implements CustomPacketPayload {
	public static final Type<FalseDarknessBeamPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "false_darkness_beam"));

	public static final StreamCodec<FriendlyByteBuf, FalseDarknessBeamPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, FalseDarknessBeamPacket packet) -> {
		buffer.writeUUID(packet.casterId);
		buffer.writeDouble(packet.originX);
		buffer.writeDouble(packet.originY);
		buffer.writeDouble(packet.originZ);
		buffer.writeDouble(packet.directionX);
		buffer.writeDouble(packet.directionY);
		buffer.writeDouble(packet.directionZ);
		buffer.writeFloat(packet.range);
		buffer.writeFloat(packet.thickness);
		buffer.writeFloat(packet.chargePercent);
		buffer.writeBoolean(packet.mainSpear);
		buffer.writeInt(packet.spearIndex);
		buffer.writeInt(packet.ticks);
	}, buffer -> new FalseDarknessBeamPacket(buffer.readUUID(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean(), buffer.readInt(), buffer.readInt()));

	@Override
	public Type<FalseDarknessBeamPacket> type() {
		return TYPE;
	}

	public static void handle(FalseDarknessBeamPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(FalseDarknessBeamPacket packet) {
			FalseDarknessClientEffects.addBeam(packet.casterId(), new Vec3(packet.originX(), packet.originY(), packet.originZ()), new Vec3(packet.directionX(), packet.directionY(), packet.directionZ()), packet.range(), packet.thickness(), packet.chargePercent(), packet.mainSpear(), packet.spearIndex(), packet.ticks());
		}
	}
}