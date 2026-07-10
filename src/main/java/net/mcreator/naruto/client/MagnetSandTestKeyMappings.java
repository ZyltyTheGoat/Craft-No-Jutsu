package net.mcreator.naruto.client;

import org.lwjgl.glfw.GLFW;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import net.mcreator.naruto.network.AdjustSandReservePacket;
import net.mcreator.naruto.NarutoMod;

@EventBusSubscriber(modid = NarutoMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class MagnetSandTestKeyMappings {
	public static final KeyMapping REMOVE_SAND = new KeyMapping("key.naruto.remove_sand", GLFW.GLFW_KEY_MINUS, "key.categories.craft_no_jutsu");
	public static final KeyMapping ADD_SAND = new KeyMapping("key.naruto.add_sand", GLFW.GLFW_KEY_EQUAL, "key.categories.craft_no_jutsu");

	private MagnetSandTestKeyMappings() {
	}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(REMOVE_SAND);
		event.register(ADD_SAND);
	}

	@EventBusSubscriber(modid = NarutoMod.MODID, value = Dist.CLIENT)
	public static final class ClientEvents {
		private ClientEvents() {
		}

		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.screen != null)
				return;
			while (REMOVE_SAND.consumeClick())
				PacketDistributor.sendToServer(new AdjustSandReservePacket(-1));
			while (ADD_SAND.consumeClick())
				PacketDistributor.sendToServer(new AdjustSandReservePacket(1));
		}
	}
}
