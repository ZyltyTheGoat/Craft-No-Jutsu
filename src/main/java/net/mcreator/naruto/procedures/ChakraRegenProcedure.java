package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import net.mcreator.naruto.network.NarutoModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
public class ChakraRegenProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double baseRegen = 0;
		double regen = 0;
		double regenTime = 0;
		if (entity.isShiftKeyDown()) {
			regenTime = 1;
		} else {
			regenTime = 5;
		}
		if (world.dayTime() % regenTime == 0) {
			if ((entity.getData(NarutoModVariables.PLAYER_VARIABLES).rank).equals("Genin")) {
				regen = 0.5;
			} else if ((entity.getData(NarutoModVariables.PLAYER_VARIABLES).rank).equals("Chunin")) {
				regen = 1;
			} else if ((entity.getData(NarutoModVariables.PLAYER_VARIABLES).rank).equals("Jonin")) {
				regen = 2;
			} else if ((entity.getData(NarutoModVariables.PLAYER_VARIABLES).rank).equals("Kage")) {
				regen = 4;
			} else if ((entity.getData(NarutoModVariables.PLAYER_VARIABLES).rank).equals("Legend")) {
				regen = 8;
			}
			if (entity.getData(NarutoModVariables.PLAYER_VARIABLES).currentChakra + regen <= entity.getData(NarutoModVariables.PLAYER_VARIABLES).maxChakra) {
				{
					NarutoModVariables.PlayerVariables _vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
					_vars.currentChakra = entity.getData(NarutoModVariables.PLAYER_VARIABLES).currentChakra + regen;
					_vars.markSyncDirty();
				}
				if (entity.isShiftKeyDown()) {
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 3, true, false));
				}
			} else {
				{
					NarutoModVariables.PlayerVariables _vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
					_vars.currentChakra = entity.getData(NarutoModVariables.PLAYER_VARIABLES).maxChakra;
					_vars.markSyncDirty();
				}
			}
		}
	}
}