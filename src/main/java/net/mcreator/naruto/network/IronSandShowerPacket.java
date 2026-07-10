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
import net.mcreator.naruto.client.IronSandShowerClientEffects;

import java.util.UUID;

public record IronSandShowerPacket(UUID casterId, double x, double y, double z, double dx, double dy, double dz, float range, float spread, int needleCount, int ticks) implements CustomPacketPayload {
	public static final Type<IronSandShowerPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "iron_sand_shower"));
	public static final StreamCodec<FriendlyByteBuf, IronSandShowerPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, IronSandShowerPacket packet) -> {
		buffer.writeUUID(packet.casterId);
		buffer.writeDouble(packet.x);
		buffer.writeDouble(packet.y);
		buffer.writeDouble(packet.z);
		buffer.writeDouble(packet.dx);
		buffer.writeDouble(packet.dy);
		buffer.writeDouble(packet.dz);
		buffer.writeFloat(packet.range);
		buffer.writeFloat(packet.spread);
		buffer.writeInt(packet.needleCount);
		buffer.writeInt(packet.ticks);
	}, buffer -> new IronSandShowerPacket(buffer.readUUID(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readInt(), buffer.readInt()));

	@Override
	public Type<IronSandShowerPacket> type() {
		return TYPE;
	}

	public static void handle(IronSandShowerPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(IronSandShowerPacket packet) {
			IronSandShowerClientEffects.addShower(packet.casterId, new Vec3(packet.x, packet.y, packet.z), new Vec3(packet.dx, packet.dy, packet.dz), packet.range, packet.spread, packet.needleCount, packet.ticks);
		}
	}
}
