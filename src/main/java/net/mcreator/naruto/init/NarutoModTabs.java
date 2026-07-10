/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.naruto.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import net.mcreator.naruto.NarutoMod;

@EventBusSubscriber
public class NarutoModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NarutoMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NARUTO_MOD_SCROLLS = REGISTRY.register("naruto_mod_scrolls",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.naruto.naruto_mod_scrolls")).icon(() -> new ItemStack(NarutoModItems.SCROLL_GREAT_FIRE_ANNIHILATION.get())).displayItems((parameters, tabData) -> {
				tabData.accept(NarutoModItems.SCROLL_GREAT_FIREBALL.get());
				tabData.accept(NarutoModItems.SCROLL_PHOENIX_FIRE.get());
				tabData.accept(NarutoModItems.SCROLL_HIDING_IN_ASH.get());
				tabData.accept(NarutoModItems.SCROLL_GREAT_FIRE_ANNIHILATION.get());
				tabData.accept(NarutoModItems.SCROLL_FIRE_DRAGON_BULLET.get());
				tabData.accept(NarutoModItems.SCROLL_GREAT_FLAME.get());
				tabData.accept(NarutoModItems.SCROLL_HIDING_IN_MIST.get());
				tabData.accept(NarutoModItems.SCROLL_WATER_CLONE.get());
				tabData.accept(NarutoModItems.SCROLL_WATER_PRISON.get());
				tabData.accept(NarutoModItems.SCROLL_WATER_DRAGON_BULLET.get());
				tabData.accept(NarutoModItems.SCROLL_WATER_BULLET.get());
				tabData.accept(NarutoModItems.SCROLL_SEVERING_WAVE.get());
				tabData.accept(NarutoModItems.SCROLL_EARTH_DRAGON_BULLET.get());
				tabData.accept(NarutoModItems.SCROLL_EARTH_WALL.get());
				tabData.accept(NarutoModItems.SCROLL_ROCK_FISTS.get());
				tabData.accept(NarutoModItems.SCROLL_EARTH_FLOW_SPEARS.get());
				tabData.accept(NarutoModItems.SCROLL_EARTH_GOLEM.get());
				tabData.accept(NarutoModItems.SCROLL_GREAT_BREAKTHROUGH.get());
				tabData.accept(NarutoModItems.SCROLL_VACUUM_WAVE.get());
				tabData.accept(NarutoModItems.SCROLL_BLADE_OF_WIND.get());
				tabData.accept(NarutoModItems.SCROLL_AIR_BULLETS.get());
				tabData.accept(NarutoModItems.SCROLL_CHAKRA_FLOW.get());
				tabData.accept(NarutoModItems.SCROLL_RASENSHURIKEN.get());
				tabData.accept(NarutoModItems.SCROLL_BEAST_TRACKING_FANG.get());
				tabData.accept(NarutoModItems.SCROLL_FALSE_DARKNESS.get());
				tabData.accept(NarutoModItems.SCROLL_CHIDORI.get());
				tabData.accept(NarutoModItems.SCROLL_LIGHTNING_CLONE.get());
				tabData.accept(NarutoModItems.SCROLL_LIGHTNING_CHAKRA_MODE.get());
				tabData.accept(NarutoModItems.SCROLL_KIRIN.get());
				tabData.accept(NarutoModItems.SCROLL_HIDING_IN_ROCK.get());
				tabData.accept(NarutoModItems.SCROLL_SHADOW_CLONE.get());
				tabData.accept(NarutoModItems.SCROLL_TRANSFORMATION.get());
				tabData.accept(NarutoModItems.SCROLL_SUBSTITUTION.get());
				tabData.accept(NarutoModItems.SCROLL_BODY_FLICKER.get());
				tabData.accept(NarutoModItems.SCROLL_FLYING_RAIJIN.get());
				tabData.accept(NarutoModItems.SCROLL_GENJUTSU.get());
				tabData.accept(NarutoModItems.SCROLL_MIND_BODY_TRANSFER.get());
				tabData.accept(NarutoModItems.SCROLL_SHADOW_IMITATION.get());
				tabData.accept(NarutoModItems.SCROLL_MULTI_SIZE.get());
				tabData.accept(NarutoModItems.SCROLL_FOUR_SYMBOL_SEAL.get());
			}).build());

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
			tabData.accept(NarutoModItems.DUMMY_SPAWN_EGG.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.COMBAT) {
			tabData.accept(NarutoModItems.GOURD_CHESTPLATE.get());
		}
	}
}