package net.mcreator.naruto.init;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.mcreator.naruto.entity.ShinobiCloneEntity;
import net.mcreator.naruto.NarutoMod;

@EventBusSubscriber(modid = NarutoMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NarutoCustomEntityAttributes {
	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(NarutoModEntities.SHINOBI_CLONE.get(), ShinobiCloneEntity.createAttributes().build());
	}
}
