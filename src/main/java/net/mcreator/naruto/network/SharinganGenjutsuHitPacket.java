package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.naruto.client.GenjutsuClientEffects;
import net.mcreator.naruto.NarutoMod;

public record SharinganGenjutsuHitPacket(int ticks) implements CustomPacketPayload {
	public static final Type<SharinganGenjutsuHitPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "sharingan_genjutsu_hit"));
	public static final StreamCodec<FriendlyByteBuf, SharinganGenjutsuHitPacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, SharinganGenjutsuHitPacket packet) -> buffer.writeInt(packet.ticks),
			(FriendlyByteBuf buffer) -> new SharinganGenjutsuHitPacket(buffer.readInt()));

	@Override
	public Type<SharinganGenjutsuHitPacket> type() {
		return TYPE;
	}

	public static void handle(SharinganGenjutsuHitPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> GenjutsuClientEffects.triggerSharinganNightmare(packet.ticks));
	}
}
