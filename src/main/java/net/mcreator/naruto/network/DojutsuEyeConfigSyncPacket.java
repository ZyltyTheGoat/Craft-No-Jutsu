package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.naruto.DojutsuEyeConfigStore;
import net.mcreator.naruto.NarutoMod;

public record DojutsuEyeConfigSyncPacket(int playerId, double width, double height, double spacing, double yOffset, double zOffset) implements CustomPacketPayload {
	public static final Type<DojutsuEyeConfigSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "dojutsu_eye_config_sync"));
	public static final StreamCodec<FriendlyByteBuf, DojutsuEyeConfigSyncPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, DojutsuEyeConfigSyncPacket packet) -> {
		buffer.writeInt(packet.playerId);
		buffer.writeDouble(packet.width);
		buffer.writeDouble(packet.height);
		buffer.writeDouble(packet.spacing);
		buffer.writeDouble(packet.yOffset);
		buffer.writeDouble(packet.zOffset);
	}, (FriendlyByteBuf buffer) -> new DojutsuEyeConfigSyncPacket(buffer.readInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));

	@Override
	public Type<DojutsuEyeConfigSyncPacket> type() {
		return TYPE;
	}

	public static void handle(DojutsuEyeConfigSyncPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> DojutsuEyeConfigStore.set(packet.playerId, packet.width, packet.height, packet.spacing, packet.yOffset, packet.zOffset));
	}
}
