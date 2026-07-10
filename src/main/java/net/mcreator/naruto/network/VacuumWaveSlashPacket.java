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
import net.mcreator.naruto.client.VacuumWaveClientEffects;

public record VacuumWaveSlashPacket(double x, double y, double z, double dx, double dy, double dz, float range, float width, float roll, int ticks) implements CustomPacketPayload {
	public static final Type<VacuumWaveSlashPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "vacuum_wave_slash"));
	public static final StreamCodec<FriendlyByteBuf, VacuumWaveSlashPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, VacuumWaveSlashPacket packet) -> {
		buffer.writeDouble(packet.x);
		buffer.writeDouble(packet.y);
		buffer.writeDouble(packet.z);
		buffer.writeDouble(packet.dx);
		buffer.writeDouble(packet.dy);
		buffer.writeDouble(packet.dz);
		buffer.writeFloat(packet.range);
		buffer.writeFloat(packet.width);
		buffer.writeFloat(packet.roll);
		buffer.writeInt(packet.ticks);
	}, buffer -> new VacuumWaveSlashPacket(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readInt()));

	@Override
	public Type<VacuumWaveSlashPacket> type() {
		return TYPE;
	}

	public static void handle(VacuumWaveSlashPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(VacuumWaveSlashPacket packet) {
			VacuumWaveClientEffects.addSlash(new Vec3(packet.x, packet.y, packet.z), new Vec3(packet.dx, packet.dy, packet.dz), packet.range, packet.width, packet.roll, packet.ticks);
		}
	}
}
