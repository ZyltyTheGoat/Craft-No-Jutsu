/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.naruto.init;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.GameRules;

@EventBusSubscriber
public class NarutoModGameRules {
	public static GameRules.Key<GameRules.BooleanValue> CRAFT_NO_JUTSU_ENABLE_BLOCK_BREAKING;

	@SubscribeEvent
	public static void registerGameRules(FMLCommonSetupEvent event) {
		CRAFT_NO_JUTSU_ENABLE_BLOCK_BREAKING = GameRules.register("craftNoJutsuEnableBlockBreaking", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	}
}