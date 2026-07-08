package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.mcreator.naruto.NarutoMod;

@EventBusSubscriber
public record DashKeyMessage(int eventType, int pressedms) implements CustomPacketPayload {
	public static final Type<DashKeyMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "key_dash_key"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DashKeyMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, DashKeyMessage message) -> {
		buffer.writeInt(message.eventType);
		buffer.writeInt(message.pressedms);
	}, (RegistryFriendlyByteBuf buffer) -> new DashKeyMessage(buffer.readInt(), buffer.readInt()));

	@Override
	public Type<DashKeyMessage> type() {
		return TYPE;
	}

	public static void handleData(final DashKeyMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
			}).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		NarutoMod.addNetworkMessage(DashKeyMessage.TYPE, DashKeyMessage.STREAM_CODEC, DashKeyMessage::handleData);
	}
}