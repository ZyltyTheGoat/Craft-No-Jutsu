package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.naruto.DojutsuEyeConfigStore;
import net.mcreator.naruto.NarutoMod;

public record SetDojutsuEyeConfigPacket(double width, double height, double spacing, double yOffset, double zOffset) implements CustomPacketPayload {
	public static final Type<SetDojutsuEyeConfigPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "set_dojutsu_eye_config"));
	public static final StreamCodec<FriendlyByteBuf, SetDojutsuEyeConfigPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, SetDojutsuEyeConfigPacket packet) -> {
		buffer.writeDouble(packet.width);
		buffer.writeDouble(packet.height);
		buffer.writeDouble(packet.spacing);
		buffer.writeDouble(packet.yOffset);
		buffer.writeDouble(packet.zOffset);
	}, (FriendlyByteBuf buffer) -> new SetDojutsuEyeConfigPacket(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));

	@Override
	public Type<SetDojutsuEyeConfigPacket> type() {
		return TYPE;
	}

	public static void handle(SetDojutsuEyeConfigPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer serverPlayer) {
				DojutsuEyeConfigStore.Config config = DojutsuEyeConfigStore.setAndSave(serverPlayer, packet.width, packet.height, packet.spacing, packet.yOffset, packet.zOffset);
				PacketDistributor.sendToPlayersInDimension((ServerLevel) serverPlayer.level(), new DojutsuEyeConfigSyncPacket(serverPlayer.getId(), config.width(), config.height(), config.spacing(), config.yOffset(), config.zOffset()));
			}
		});
	}
}
