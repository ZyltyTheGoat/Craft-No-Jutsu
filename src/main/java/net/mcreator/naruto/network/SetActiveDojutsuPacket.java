package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.naruto.NarutoMod;

public record SetActiveDojutsuPacket(String jutsuId) implements CustomPacketPayload {
	public static final Type<SetActiveDojutsuPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "set_active_dojutsu"));
	public static final StreamCodec<FriendlyByteBuf, SetActiveDojutsuPacket> STREAM_CODEC = StreamCodec.composite(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, SetActiveDojutsuPacket::jutsuId, SetActiveDojutsuPacket::new);

	@Override
	public Type<SetActiveDojutsuPacket> type() {
		return TYPE;
	}

	public static void handle(SetActiveDojutsuPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer serverPlayer) {
				// Get player variables
				NarutoModVariables.PlayerVariables playerVars = serverPlayer.getData(NarutoModVariables.PLAYER_VARIABLES);
				// Set the active jutsu
				playerVars.activeDojutsu = packet.jutsuId;
				// Sync to client (assumes your PlayerVariables has a sync method)
				playerVars.markSyncDirty();
			}
		});
	}
}