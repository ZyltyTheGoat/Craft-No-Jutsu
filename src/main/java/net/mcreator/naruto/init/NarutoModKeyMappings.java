/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.naruto.init;

import org.lwjgl.glfw.GLFW;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import net.mcreator.naruto.network.UseAbilityMessage;
import net.mcreator.naruto.network.JutsuWheelMessage;
import net.mcreator.naruto.network.FavouriteJutsuWheelKeybindMessage;

@EventBusSubscriber(Dist.CLIENT)
public class NarutoModKeyMappings {
	public static final KeyMapping JUTSU_WHEEL = new KeyMapping("key.naruto.jutsu_wheel", GLFW.GLFW_KEY_C, "key.categories.craft_no_jutsu") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				PacketDistributor.sendToServer(new JutsuWheelMessage(0, 0));
				JutsuWheelMessage.pressAction(Minecraft.getInstance().player, 0, 0);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping DASH_KEY = new KeyMapping("key.naruto.dash_key", GLFW.GLFW_KEY_G, "key.categories.craft_no_jutsu");
	public static final KeyMapping FAVOURITE_JUTSU_WHEEL_KEYBIND = new KeyMapping("key.naruto.favourite_jutsu_wheel_keybind", GLFW.GLFW_KEY_V, "key.categories.craft_no_jutsu") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				PacketDistributor.sendToServer(new FavouriteJutsuWheelKeybindMessage(0, 0));
				FavouriteJutsuWheelKeybindMessage.pressAction(Minecraft.getInstance().player, 0, 0);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping USE_ABILITY = new KeyMapping("key.naruto.use_ability", GLFW.GLFW_KEY_Z, "key.categories.craft_no_jutsu") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				PacketDistributor.sendToServer(new UseAbilityMessage(0, 0));
				UseAbilityMessage.pressAction(Minecraft.getInstance().player, 0, 0);
				USE_ABILITY_LASTPRESS = System.currentTimeMillis();
			} else if (isDownOld != isDown && !isDown) {
				int dt = (int) (System.currentTimeMillis() - USE_ABILITY_LASTPRESS);
				PacketDistributor.sendToServer(new UseAbilityMessage(1, dt));
				UseAbilityMessage.pressAction(Minecraft.getInstance().player, 1, dt);
			}
			isDownOld = isDown;
		}
	};
	private static long USE_ABILITY_LASTPRESS = 0;

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(JUTSU_WHEEL);
		event.register(DASH_KEY);
		event.register(FAVOURITE_JUTSU_WHEEL_KEYBIND);
		event.register(USE_ABILITY);
	}

	@EventBusSubscriber(Dist.CLIENT)
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			if (Minecraft.getInstance().screen == null) {
				JUTSU_WHEEL.consumeClick();
				FAVOURITE_JUTSU_WHEEL_KEYBIND.consumeClick();
				USE_ABILITY.consumeClick();
			}
		}
	}
}