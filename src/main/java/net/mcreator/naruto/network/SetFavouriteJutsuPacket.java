package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.naruto.JutsuIdList;
import net.mcreator.naruto.NarutoMod;

public record SetFavouriteJutsuPacket(String jutsuId) implements CustomPacketPayload {
	public static final Type<SetFavouriteJutsuPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "set_favourite_jutsu"));
	public static final StreamCodec<FriendlyByteBuf, SetFavouriteJutsuPacket> STREAM_CODEC = StreamCodec.composite(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, SetFavouriteJutsuPacket::jutsuId, SetFavouriteJutsuPacket::new);

	@Override
	public Type<SetFavouriteJutsuPacket> type() {
		return TYPE;
	}

	public static void handle(SetFavouriteJutsuPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer serverPlayer) {
				// Get player variables
				NarutoModVariables.PlayerVariables playerVars = serverPlayer.getData(NarutoModVariables.PLAYER_VARIABLES);
				// Set the active toggle jutsu
				playerVars.favouriteJutsu = JutsuIdList.join(JutsuIdList.toSet(packet.jutsuId));
				// Sync to client (assumes your PlayerVariables has a sync method)
				playerVars.markSyncDirty();
			}
		});
	}
}
