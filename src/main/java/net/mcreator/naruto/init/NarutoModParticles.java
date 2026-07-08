/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.naruto.init;

import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.mcreator.naruto.client.particle.PhoenixFlowerParticleParticle;
import net.mcreator.naruto.client.particle.MistParticle;
import net.mcreator.naruto.client.particle.LightningParticle;
import net.mcreator.naruto.client.particle.FireballParticleParticle;
import net.mcreator.naruto.client.particle.BigSmokeParticle;

@EventBusSubscriber(Dist.CLIENT)
public class NarutoModParticles {
	@SubscribeEvent
	public static void registerParticles(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(NarutoModParticleTypes.BIG_SMOKE.get(), BigSmokeParticle::provider);
		event.registerSpriteSet(NarutoModParticleTypes.MIST.get(), MistParticle::provider);
		event.registerSpriteSet(NarutoModParticleTypes.PHOENIX_FLOWER_PARTICLE.get(), PhoenixFlowerParticleParticle::provider);
		event.registerSpriteSet(NarutoModParticleTypes.FIREBALL_PARTICLE.get(), FireballParticleParticle::provider);
		event.registerSpriteSet(NarutoModParticleTypes.LIGHTNING.get(), LightningParticle::provider);
	}
}