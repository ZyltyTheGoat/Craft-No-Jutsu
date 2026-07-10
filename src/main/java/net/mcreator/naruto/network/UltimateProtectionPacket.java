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
import net.mcreator.naruto.client.UltimateProtectionClientEffects;

import java.util.UUID;

public record UltimateProtectionPacket(UUID playerId, byte protectionType, double directionX, double directionY, double directionZ, int holdTicks) implements CustomPacketPayload {
	public static final byte FEET = 0;
	public static final byte DIRECTIONAL = 1;
	public static final Type<UltimateProtectionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "ultimate_protection"));
	public static final StreamCodec<FriendlyByteBuf, UltimateProtectionPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, UltimateProtectionPacket packet) -> {
		buffer.writeUUID(packet.playerId);
		buffer.writeByte(packet.protectionType);
		buffer.writeDouble(packet.directionX);
		buffer.writeDouble(packet.directionY);
		buffer.writeDouble(packet.directionZ);
		buffer.writeInt(packet.holdTicks);
	}, buffer -> new UltimateProtectionPacket(buffer.readUUID(), buffer.readByte(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readInt()));

	@Override
	public Type<UltimateProtectionPacket> type() {
		return TYPE;
	}

	public static void handle(UltimateProtectionPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> ClientHandler.handle(packet));
	}

	@OnlyIn(Dist.CLIENT)
	private static final class ClientHandler {
		private static void handle(UltimateProtectionPacket packet) {
			UltimateProtectionClientEffects.addProtection(packet.playerId(), packet.protectionType(), new Vec3(packet.directionX(), packet.directionY(), packet.directionZ()), packet.holdTicks());
		}
	}
}
