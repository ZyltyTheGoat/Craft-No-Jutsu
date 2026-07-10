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
import net.mcreator.naruto.client.SolidFogJutsuClientEffects;

import java.util.UUID;

public record SolidFogJutsuPacket(UUID casterId, double x, double y, double z, double dx, double dy, double dz, float length, float radius, int ticks) implements CustomPacketPayload {
	public static final Type<SolidFogJutsuPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "solid_fog_jutsu"));
	public static final StreamCodec<FriendlyByteBuf, SolidFogJutsuPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, SolidFogJutsuPacket packet) -> {
		buffer.writeUUID(packet.casterId);
		buffer.writeDouble(packet.x);
		buffer.writeDouble(packet.y);
		buffer.writeDouble(packet.z);
		buffer.writeDouble(packet.dx);
		buffer.writeDouble(packet.dy);
		buffer.writeDouble(packet.dz);
		buffer.writeFloat(packet.length);
		buffer.writeFloat(packet.radius);
		buffer.writeVarInt(packet.ticks);
	}, buffer -> new SolidFogJutsuPacket(buffer.readUUID(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt()));

	@Override
	public Type<SolidFogJutsuPacket> type() {
		return TYPE;
	}

	public static void handle(SolidFogJutsuPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(SolidFogJutsuPacket packet) {
			SolidFogJutsuClientEffects.addFog(packet.casterId(), new Vec3(packet.x(), packet.y(), packet.z()), new Vec3(packet.dx(), packet.dy(), packet.dz()), packet.length(), packet.radius(), packet.ticks());
		}
	}
}
