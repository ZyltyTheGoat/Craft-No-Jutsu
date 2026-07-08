package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.mcreator.naruto.procedures.JutsuWheelOnKeyPressedProcedure;
import net.mcreator.naruto.NarutoMod;

@EventBusSubscriber
public record JutsuWheelMessage(int eventType, int pressedms) implements CustomPacketPayload {
	public static final Type<JutsuWheelMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "key_jutsu_wheel"));
	public static final StreamCodec<RegistryFriendlyByteBuf, JutsuWheelMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, JutsuWheelMessage message) -> {
		buffer.writeInt(message.eventType);
		buffer.writeInt(message.pressedms);
	}, (RegistryFriendlyByteBuf buffer) -> new JutsuWheelMessage(buffer.readInt(), buffer.readInt()));

	@Override
	public Type<JutsuWheelMessage> type() {
		return TYPE;
	}

	public static void handleData(final JutsuWheelMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
				pressAction(context.player(), message.eventType, message.pressedms);
			}).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	public static void pressAction(Player entity, int type, int pressedms) {
		Level world = entity.level();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(entity.blockPosition()))
			return;
		if (type == 0) {

			JutsuWheelOnKeyPressedProcedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		NarutoMod.addNetworkMessage(JutsuWheelMessage.TYPE, JutsuWheelMessage.STREAM_CODEC, JutsuWheelMessage::handleData);
	}
}