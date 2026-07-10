package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@EventBusSubscriber
public class DisableNormalHostilesSpawningProcedure {
	@SubscribeEvent
	public static void onEntitySpawned(EntityJoinLevelEvent event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable EntityJoinLevelEvent event, Entity entity) {
		if (entity == null)
			return;
		/*
		if (entity instanceof Zombie || entity instanceof ZombieHorse || entity instanceof ZombieVillager|| entity instanceof Husk  || entity instanceof Skeleton || entity instanceof SkeletonHorse || entity instanceof Creeper
				|| entity instanceof CaveSpider || entity instanceof Spider || entity instanceof EnderMan || entity instanceof Pillager || entity instanceof Ravager || entity instanceof Evoker || entity instanceof Witch) {
			if (event != null) {
				event.setCanceled(true);
			}
		}
		*/	
	}
}