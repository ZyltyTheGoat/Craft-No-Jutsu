package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.JutsuRegistry;
import net.mcreator.naruto.DojutsuStateHelper;

public record SetToggleJutsuPacket(String jutsuId) implements CustomPacketPayload {
	public static final Type<SetToggleJutsuPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "set_active_toggle_jutsu"));
	public static final StreamCodec<FriendlyByteBuf, SetToggleJutsuPacket> STREAM_CODEC = StreamCodec.composite(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, SetToggleJutsuPacket::jutsuId, SetToggleJutsuPacket::new);

	@Override
	public Type<SetToggleJutsuPacket> type() {
		return TYPE;
	}

	public static void handle(SetToggleJutsuPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer serverPlayer) {
				// Get player variables
				NarutoModVariables.PlayerVariables playerVars = serverPlayer.getData(NarutoModVariables.PLAYER_VARIABLES);
				// Set the active toggle jutsu
				JutsuRegistry.initializeJutsus();
				playerVars.activeToggleJutsu = DojutsuStateHelper.normalize(packet.jutsuId);
				// Sync to client (assumes your PlayerVariables has a sync method)
				playerVars.markSyncDirty();
			}
		});
	}
}
