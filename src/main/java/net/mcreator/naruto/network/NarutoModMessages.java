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
		registrar.playToServer(SetDojutsuEyeConfigPacket.TYPE, SetDojutsuEyeConfigPacket.STREAM_CODEC, SetDojutsuEyeConfigPacket::handle);
		registrar.playToServer(SetToggleJutsuPacket.TYPE, SetToggleJutsuPacket.STREAM_CODEC, SetToggleJutsuPacket::handle);
		registrar.playToServer(SetFavouriteJutsuPacket.TYPE, SetFavouriteJutsuPacket.STREAM_CODEC, SetFavouriteJutsuPacket::handle);
		registrar.playToServer(AdjustSandReservePacket.TYPE, AdjustSandReservePacket.STREAM_CODEC, AdjustSandReservePacket::handle);
		registrar.playToClient(DojutsuEyeConfigSyncPacket.TYPE, DojutsuEyeConfigSyncPacket.STREAM_CODEC, DojutsuEyeConfigSyncPacket::handle);
		registrar.playToClient(SharinganGenjutsuHitPacket.TYPE, SharinganGenjutsuHitPacket.STREAM_CODEC, SharinganGenjutsuHitPacket::handle);
		registrar.playToClient(JutsuHudFeedbackPacket.TYPE, JutsuHudFeedbackPacket.STREAM_CODEC, JutsuHudFeedbackPacket::handle);
		registrar.playToClient(JutsuCooldownSyncPacket.TYPE, JutsuCooldownSyncPacket.STREAM_CODEC, JutsuCooldownSyncPacket::handle);
		registrar.playToClient(ChidoriStatePacket.TYPE, ChidoriStatePacket.STREAM_CODEC, ChidoriStatePacket::handle);
		registrar.playToClient(ChidoriBurstPacket.TYPE, ChidoriBurstPacket.STREAM_CODEC, ChidoriBurstPacket::handle);
		registrar.playToClient(LightningChakraModeStatePacket.TYPE, LightningChakraModeStatePacket.STREAM_CODEC, LightningChakraModeStatePacket::handle);
		registrar.playToClient(WaterPrisonStatePacket.TYPE, WaterPrisonStatePacket.STREAM_CODEC, WaterPrisonStatePacket::handle);
		registrar.playToClient(WindBurstPacket.TYPE, WindBurstPacket.STREAM_CODEC, WindBurstPacket::handle);
		registrar.playToClient(BladeOfWindSlashPacket.TYPE, BladeOfWindSlashPacket.STREAM_CODEC, BladeOfWindSlashPacket::handle);
		registrar.playToClient(MistFieldStatePacket.TYPE, MistFieldStatePacket.STREAM_CODEC, MistFieldStatePacket::handle);
		registrar.playToClient(AshFieldStatePacket.TYPE, AshFieldStatePacket.STREAM_CODEC, AshFieldStatePacket::handle);
		registrar.playToClient(SeveringWaveBeamPacket.TYPE, SeveringWaveBeamPacket.STREAM_CODEC, SeveringWaveBeamPacket::handle);
		registrar.playToClient(FalseDarknessBeamPacket.TYPE, FalseDarknessBeamPacket.STREAM_CODEC, FalseDarknessBeamPacket::handle);
		registrar.playToClient(FireDragonBulletPacket.TYPE, FireDragonBulletPacket.STREAM_CODEC, FireDragonBulletPacket::handle);
		registrar.playToClient(EarthSpikePacket.TYPE, EarthSpikePacket.STREAM_CODEC, EarthSpikePacket::handle);
		registrar.playToClient(SandReserveSyncPacket.TYPE, SandReserveSyncPacket.STREAM_CODEC, SandReserveSyncPacket::handle);
		registrar.playToClient(SandLevitationStatePacket.TYPE, SandLevitationStatePacket.STREAM_CODEC, SandLevitationStatePacket::handle);
		registrar.playToClient(SandBindingCoffinStatePacket.TYPE, SandBindingCoffinStatePacket.STREAM_CODEC, SandBindingCoffinStatePacket::handle);
		registrar.playToClient(IronSandShowerPacket.TYPE, IronSandShowerPacket.STREAM_CODEC, IronSandShowerPacket::handle);
		registrar.playToClient(IronSandGatheringAssaultPacket.TYPE, IronSandGatheringAssaultPacket.STREAM_CODEC, IronSandGatheringAssaultPacket::handle);
		registrar.playToClient(NeedleSpherePacket.TYPE, NeedleSpherePacket.STREAM_CODEC, NeedleSpherePacket::handle);
		registrar.playToClient(UltimateProtectionPacket.TYPE, UltimateProtectionPacket.STREAM_CODEC, UltimateProtectionPacket::handle);
		registrar.playToClient(SolidFogJutsuPacket.TYPE, SolidFogJutsuPacket.STREAM_CODEC, SolidFogJutsuPacket::handle);
		registrar.playToClient(VacuumWaveSlashPacket.TYPE, VacuumWaveSlashPacket.STREAM_CODEC, VacuumWaveSlashPacket::handle);
	}
}
