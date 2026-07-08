package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;

import net.mcreator.naruto.entity.DummyEntity;

import javax.annotation.Nullable;

@EventBusSubscriber
public class DummyHitProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingIncomingDamageEvent event) {
		if (event.getEntity() != null) {
			execute(event, event.getEntity(), event.getSource().getEntity(), event.getAmount());
		}
	}

	public static void execute(Entity entity, Entity sourceentity, double amount) {
		execute(null, entity, sourceentity, amount);
	}

	private static void execute(@Nullable LivingIncomingDamageEvent event, Entity entity, Entity sourceentity, double amount) {
		if (entity == null || sourceentity == null)
			return;
		if (entity instanceof DummyEntity) {
			double finalDamage = amount;
			int resistanceLevel = 0;
			if (entity instanceof LivingEntity livingEntity) {
				if (livingEntity.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
					resistanceLevel = livingEntity.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1;
					double resistanceReduction = 1.0 - (0.20 * resistanceLevel);
					resistanceReduction = Math.max(0.0, resistanceReduction);
					finalDamage = amount * resistanceReduction;
				}
			}
			if (sourceentity instanceof Player _player && !_player.level().isClientSide()) {
				String rankScale = sourceentity instanceof LivingEntity livingAttacker ? String.format(" | Rank x%.2f", NarutoDamageFormula.calculateRankScale(livingAttacker)) : "";
				String resistanceText = resistanceLevel > 0 ? String.format(" | Res -%.0f%%", resistanceLevel * 20.0D) : "";
				_player.displayClientMessage(Component.literal(String.format("Raw %.2f -> %.2f%s%s", amount, finalDamage, rankScale, resistanceText)), true);
			}
			if (event != null) {
				event.setCanceled(true);
			}
		}
	}
}
