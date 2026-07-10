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
import net.mcreator.naruto.client.IronSandGatheringAssaultClientEffects;

import java.util.UUID;

public record IronSandGatheringAssaultPacket(UUID casterId, int targetId, double x, double y, double z, double dx, double dy, double dz, float range, float radius, float chargePercent, int formationTicks, int travelTicks)
		implements CustomPacketPayload {
	public static final Type<IronSandGatheringAssaultPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "iron_sand_gathering_assault"));
	public static final StreamCodec<FriendlyByteBuf, IronSandGatheringAssaultPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, IronSandGatheringAssaultPacket packet) -> {
		buffer.writeUUID(packet.casterId);
		buffer.writeInt(packet.targetId);
		buffer.writeDouble(packet.x);
		buffer.writeDouble(packet.y);
		buffer.writeDouble(packet.z);
		buffer.writeDouble(packet.dx);
		buffer.writeDouble(packet.dy);
		buffer.writeDouble(packet.dz);
		buffer.writeFloat(packet.range);
		buffer.writeFloat(packet.radius);
		buffer.writeFloat(packet.chargePercent);
		buffer.writeInt(packet.formationTicks);
		buffer.writeInt(packet.travelTicks);
	}, buffer -> new IronSandGatheringAssaultPacket(buffer.readUUID(), buffer.readInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
			buffer.readInt(), buffer.readInt()));

	@Override
	public Type<IronSandGatheringAssaultPacket> type() {
		return TYPE;
	}

	public static void handle(IronSandGatheringAssaultPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(IronSandGatheringAssaultPacket packet) {
			IronSandGatheringAssaultClientEffects.addAssault(packet.casterId(), packet.targetId(), new Vec3(packet.x(), packet.y(), packet.z()), new Vec3(packet.dx(), packet.dy(), packet.dz()), packet.range(), packet.radius(), packet.chargePercent(), packet.formationTicks(),
					packet.travelTicks());
		}
	}
}
