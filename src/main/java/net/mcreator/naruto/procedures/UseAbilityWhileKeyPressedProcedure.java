package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.Entity;

import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.JutsuRegistry;
import net.mcreator.naruto.JutsuData;

import javax.annotation.Nullable;

@EventBusSubscriber
public class UseAbilityWhileKeyPressedProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		double maxChargeTime = 0;
		if (entity.getData(NarutoModVariables.PLAYER_VARIABLES).chargingAbility) {
			JutsuRegistry.initializeJutsus();
			NarutoModVariables.PlayerVariables vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
			JutsuData jutsuData = JutsuRegistry.getJutsu(vars.activeJutsu);
			if (jutsuData == null)
				return;
			maxChargeTime = jutsuData.getChargeTime();;
			if (maxChargeTime <= 0) {
				return;
			}
			double previousChargeTime = vars.chargeTime;
			if (vars.chargeTime < maxChargeTime) {
				{
					NarutoModVariables.PlayerVariables _vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
					_vars.chargeTime = vars.chargeTime + 1;
					_vars.markSyncDirty();
				}
			}
			double currentChargeTime = entity.getData(NarutoModVariables.PLAYER_VARIABLES).chargeTime;
			JutsuChargeEffects.tick(entity, jutsuData, currentChargeTime, maxChargeTime, previousChargeTime < maxChargeTime && currentChargeTime >= maxChargeTime);
		}
	}
}
