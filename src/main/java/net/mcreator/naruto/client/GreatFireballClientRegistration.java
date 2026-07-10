package net.mcreator.naruto.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.mcreator.naruto.init.NarutoModEntities;
import net.mcreator.naruto.client.renderer.ShinobiCloneRenderer;
import net.mcreator.naruto.client.renderer.PhoenixFlowerProjectileRenderer;
import net.mcreator.naruto.client.renderer.GreatFireballRenderer;
import net.mcreator.naruto.client.renderer.ElementalBulletRenderer;
import net.mcreator.naruto.NarutoMod;

@EventBusSubscriber(modid = NarutoMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class GreatFireballClientRegistration {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(NarutoModEntities.GREAT_FIREBALL.get(), GreatFireballRenderer::new);
		event.registerEntityRenderer(NarutoModEntities.PHOENIX_FLOWER_PROJECTILE.get(), PhoenixFlowerProjectileRenderer::new);
		event.registerEntityRenderer(NarutoModEntities.ELEMENTAL_BULLET.get(), ElementalBulletRenderer::new);
		event.registerEntityRenderer(NarutoModEntities.SHINOBI_CLONE.get(), ShinobiCloneRenderer::new);
	}
}
