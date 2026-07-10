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
import net.mcreator.naruto.client.FireDragonBulletClientEffects;

import java.util.UUID;

public record FireDragonBulletPacket(UUID casterId, double originX, double originY, double originZ, double directionX, double directionY, double directionZ, float range, float radius, float chargePercent, int ticks) implements CustomPacketPayload {
	public static final Type<FireDragonBulletPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "fire_dragon_bullet"));
	public static final StreamCodec<FriendlyByteBuf, FireDragonBulletPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, FireDragonBulletPacket packet) -> {
		buffer.writeUUID(packet.casterId);
		buffer.writeDouble(packet.originX);
		buffer.writeDouble(packet.originY);
		buffer.writeDouble(packet.originZ);
		buffer.writeDouble(packet.directionX);
		buffer.writeDouble(packet.directionY);
		buffer.writeDouble(packet.directionZ);
		buffer.writeFloat(packet.range);
		buffer.writeFloat(packet.radius);
		buffer.writeFloat(packet.chargePercent);
		buffer.writeInt(packet.ticks);
	}, buffer -> new FireDragonBulletPacket(buffer.readUUID(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
			buffer.readInt()));

	@Override
	public Type<FireDragonBulletPacket> type() {
		return TYPE;
	}

	public static void handle(FireDragonBulletPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(FireDragonBulletPacket packet) {
			FireDragonBulletClientEffects.addDragon(packet.casterId(), new Vec3(packet.originX(), packet.originY(), packet.originZ()), new Vec3(packet.directionX(), packet.directionY(), packet.directionZ()), packet.range(), packet.radius(),
					packet.chargePercent(), packet.ticks());
		}
	}
}
