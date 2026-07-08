package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import net.mcreator.naruto.network.NarutoModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
public class ApplyRankScalingProcedure {
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
		if (world.dayTime() % 20 == 0) {
			if ((entity.getData(NarutoModVariables.PLAYER_VARIABLES).rank).equals("Genin")) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, 1, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, -1, 1, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 0, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 3, true, false));
				if (entity instanceof LivingEntity _livingEntity5 && _livingEntity5.getAttributes().hasAttribute(Attributes.ARMOR))
					_livingEntity5.getAttribute(Attributes.ARMOR).setBaseValue(2);
				if (entity instanceof LivingEntity _livingEntity6 && _livingEntity6.getAttributes().hasAttribute(Attributes.ARMOR_TOUGHNESS))
					_livingEntity6.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(1);
				if (entity instanceof LivingEntity _livingEntity7 && _livingEntity7.getAttributes().hasAttribute(Attributes.KNOCKBACK_RESISTANCE))
					_livingEntity7.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.05);
				if (entity instanceof LivingEntity _livingEntity8 && _livingEntity8.getAttributes().hasAttribute(Attributes.STEP_HEIGHT))
					_livingEntity8.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(2);
				{
					NarutoModVariables.PlayerVariables _vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
					_vars.maxChakra = 200;
					_vars.markSyncDirty();
				}
			}
			if ((entity.getData(NarutoModVariables.PLAYER_VARIABLES).rank).equals("Chunin")) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, 3, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, -1, 4, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 1, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 6, true, false));
				if (entity instanceof LivingEntity _livingEntity13 && _livingEntity13.getAttributes().hasAttribute(Attributes.ARMOR))
					_livingEntity13.getAttribute(Attributes.ARMOR).setBaseValue(6);
				if (entity instanceof LivingEntity _livingEntity14 && _livingEntity14.getAttributes().hasAttribute(Attributes.ARMOR_TOUGHNESS))
					_livingEntity14.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(3);
				if (entity instanceof LivingEntity _livingEntity15 && _livingEntity15.getAttributes().hasAttribute(Attributes.KNOCKBACK_RESISTANCE))
					_livingEntity15.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.15);
				if (entity instanceof LivingEntity _livingEntity16 && _livingEntity16.getAttributes().hasAttribute(Attributes.STEP_HEIGHT))
					_livingEntity16.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(2);
				{
					NarutoModVariables.PlayerVariables _vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
					_vars.maxChakra = 600;
					_vars.markSyncDirty();
				}
			}
			if ((entity.getData(NarutoModVariables.PLAYER_VARIABLES).rank).equals("Jonin")) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, 10, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, -1, 6, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 2, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 9, true, false));
				if (entity instanceof LivingEntity _livingEntity21 && _livingEntity21.getAttributes().hasAttribute(Attributes.ARMOR))
					_livingEntity21.getAttribute(Attributes.ARMOR).setBaseValue(12);
				if (entity instanceof LivingEntity _livingEntity22 && _livingEntity22.getAttributes().hasAttribute(Attributes.ARMOR_TOUGHNESS))
					_livingEntity22.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(6);
				if (entity instanceof LivingEntity _livingEntity23 && _livingEntity23.getAttributes().hasAttribute(Attributes.KNOCKBACK_RESISTANCE))
					_livingEntity23.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.35);
				if (entity instanceof LivingEntity _livingEntity24 && _livingEntity24.getAttributes().hasAttribute(Attributes.STEP_HEIGHT))
					_livingEntity24.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(2);
				{
					NarutoModVariables.PlayerVariables _vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
					_vars.maxChakra = 1000;
					_vars.markSyncDirty();
				}
			}
			if ((entity.getData(NarutoModVariables.PLAYER_VARIABLES).rank).equals("Kage")) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, 20, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, -1, 9, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 2, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 12, true, false));
				if (entity instanceof LivingEntity _livingEntity29 && _livingEntity29.getAttributes().hasAttribute(Attributes.ARMOR))
					_livingEntity29.getAttribute(Attributes.ARMOR).setBaseValue(22);
				if (entity instanceof LivingEntity _livingEntity30 && _livingEntity30.getAttributes().hasAttribute(Attributes.ARMOR_TOUGHNESS))
					_livingEntity30.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(12);
				if (entity instanceof LivingEntity _livingEntity31 && _livingEntity31.getAttributes().hasAttribute(Attributes.KNOCKBACK_RESISTANCE))
					_livingEntity31.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6);
				if (entity instanceof LivingEntity _livingEntity32 && _livingEntity32.getAttributes().hasAttribute(Attributes.STEP_HEIGHT))
					_livingEntity32.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(2);
				{
					NarutoModVariables.PlayerVariables _vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
					_vars.maxChakra = 2000;
					_vars.markSyncDirty();
				}
			}
			if ((entity.getData(NarutoModVariables.PLAYER_VARIABLES).rank).equals("Legend")) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, 29, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, -1, 9, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 3, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 13, true, false));
				if (entity instanceof LivingEntity _livingEntity37 && _livingEntity37.getAttributes().hasAttribute(Attributes.ARMOR))
					_livingEntity37.getAttribute(Attributes.ARMOR).setBaseValue(30);
				if (entity instanceof LivingEntity _livingEntity38 && _livingEntity38.getAttributes().hasAttribute(Attributes.ARMOR_TOUGHNESS))
					_livingEntity38.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(15);
				if (entity instanceof LivingEntity _livingEntity39 && _livingEntity39.getAttributes().hasAttribute(Attributes.KNOCKBACK_RESISTANCE))
					_livingEntity39.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.9);
				if (entity instanceof LivingEntity _livingEntity40 && _livingEntity40.getAttributes().hasAttribute(Attributes.STEP_HEIGHT))
					_livingEntity40.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(2);
				{
					NarutoModVariables.PlayerVariables _vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
					_vars.maxChakra = 4000;
					_vars.markSyncDirty();
				}
			}
		}
	}
}