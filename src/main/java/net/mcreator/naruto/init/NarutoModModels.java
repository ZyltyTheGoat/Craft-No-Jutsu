/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.naruto.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.mcreator.naruto.client.model.ModelSubstitution;
import net.mcreator.naruto.client.model.ModelProjectile;
import net.mcreator.naruto.client.model.ModelGourd;

@EventBusSubscriber(Dist.CLIENT)
public class NarutoModModels {
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ModelProjectile.LAYER_LOCATION, ModelProjectile::createBodyLayer);
		event.registerLayerDefinition(ModelGourd.LAYER_LOCATION, ModelGourd::createBodyLayer);
		event.registerLayerDefinition(ModelSubstitution.LAYER_LOCATION, ModelSubstitution::createBodyLayer);
	}
}