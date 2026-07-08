package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.mcreator.naruto.NarutoMod;

@EventBusSubscriber(modid = NarutoMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NarutoModMessages {
	@SubscribeEvent
	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");
		registrar.playToServer(SetActiveJutsuPacket.TYPE, SetActiveJutsuPacket.STREAM_CODEC, SetActiveJutsuPacket::handle);
		registrar.playToServer(SetActiveDojutsuPacket.TYPE, SetActiveDojutsuPacket.STREAM_CODEC, SetActiveDojutsuPacket::handle);
		registrar.playToServer(SetToggleJutsuPacket.TYPE, SetToggleJutsuPacket.STREAM_CODEC, SetToggleJutsuPacket::handle);
		registrar.playToServer(SetFavouriteJutsuPacket.TYPE, SetFavouriteJutsuPacket.STREAM_CODEC, SetFavouriteJutsuPacket::handle);
		registrar.playToClient(JutsuHudFeedbackPacket.TYPE, JutsuHudFeedbackPacket.STREAM_CODEC, JutsuHudFeedbackPacket::handle);
		registrar.playToClient(JutsuCooldownSyncPacket.TYPE, JutsuCooldownSyncPacket.STREAM_CODEC, JutsuCooldownSyncPacket::handle);
		registrar.playToClient(ChidoriStatePacket.TYPE, ChidoriStatePacket.STREAM_CODEC, ChidoriStatePacket::handle);
		registrar.playToClient(ChidoriBurstPacket.TYPE, ChidoriBurstPacket.STREAM_CODEC, ChidoriBurstPacket::handle);
		registrar.playToClient(WaterPrisonStatePacket.TYPE, WaterPrisonStatePacket.STREAM_CODEC, WaterPrisonStatePacket::handle);
		registrar.playToClient(WindBurstPacket.TYPE, WindBurstPacket.STREAM_CODEC, WindBurstPacket::handle);
		registrar.playToClient(MistFieldStatePacket.TYPE, MistFieldStatePacket.STREAM_CODEC, MistFieldStatePacket::handle);
	}
}
