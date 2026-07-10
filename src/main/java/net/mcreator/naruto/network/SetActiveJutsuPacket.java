package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.JutsuRegistry;
import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.DojutsuStateHelper;

public record SetActiveJutsuPacket(String jutsuId) implements CustomPacketPayload {
	public static final Type<SetActiveJutsuPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "set_active_jutsu"));
	public static final StreamCodec<FriendlyByteBuf, SetActiveJutsuPacket> STREAM_CODEC = StreamCodec.composite(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, SetActiveJutsuPacket::jutsuId, SetActiveJutsuPacket::new);

	@Override
	public Type<SetActiveJutsuPacket> type() {
		return TYPE;
	}

	public static void handle(SetActiveJutsuPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer serverPlayer) {
				// Get player variables
				NarutoModVariables.PlayerVariables playerVars = serverPlayer.getData(NarutoModVariables.PLAYER_VARIABLES);
				// Set the active jutsu
				if (!packet.jutsuId.equals(playerVars.activeJutsu)) {
					playerVars.chargingAbility = false;
					playerVars.chargeTime = 0;
				}
				playerVars.activeJutsu = packet.jutsuId;
				JutsuRegistry.initializeJutsus();
				JutsuData jutsuData = JutsuRegistry.getJutsu(packet.jutsuId);
				if (DojutsuStateHelper.isDojutsu(jutsuData)) {
					playerVars.activeDojutsu = jutsuData.getNature();
					playerVars.activeToggleJutsu = DojutsuStateHelper.ensureActive(playerVars.activeToggleJutsu, jutsuData.getNature());
				}
				// Sync to client (assumes your PlayerVariables has a sync method)
				playerVars.markSyncDirty();
			}
		});
	}
}
