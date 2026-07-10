package net.mcreator.naruto.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.MagnetSandReserve;

public record AdjustSandReservePacket(int direction) implements CustomPacketPayload {
	public static final Type<AdjustSandReservePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "adjust_sand_reserve"));
	public static final StreamCodec<FriendlyByteBuf, AdjustSandReservePacket> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buffer, AdjustSandReservePacket packet) -> buffer.writeInt(packet.direction),
			(FriendlyByteBuf buffer) -> new AdjustSandReservePacket(buffer.readInt()));

	@Override
	public Type<AdjustSandReservePacket> type() {
		return TYPE;
	}

	public static void handle(AdjustSandReservePacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player))
				return;
			int direction = Integer.compare(packet.direction, 0);
			if (direction == 0)
				return;
			double before = MagnetSandReserve.get(player);
			MagnetSandReserve.add(player, direction * MagnetSandReserve.TEST_STEP);
			double after = MagnetSandReserve.get(player);
			int delta = (int) Math.round(Math.abs(after - before));
			player.displayClientMessage(Component.literal((direction > 0 ? "Added " : "Removed ") + delta + " sand"), true);
		});
	}
}
