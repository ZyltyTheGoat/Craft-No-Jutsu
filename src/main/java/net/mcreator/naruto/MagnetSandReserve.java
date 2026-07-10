package net.mcreator.naruto;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;

import net.mcreator.naruto.network.SandReserveSyncPacket;

@EventBusSubscriber(modid = NarutoMod.MODID)
public final class MagnetSandReserve {
	public static final double CAPACITY = 100.0D;
	public static final double TEST_STEP = 10.0D;
	private static final String SAND_KEY = "narutoMagnetSandReserve";

	private MagnetSandReserve() {
	}

	public static double get(Player player) {
		CompoundTag data = player.getPersistentData();
		if (!data.contains(SAND_KEY))
			data.putDouble(SAND_KEY, CAPACITY);
		return clamp(data.getDouble(SAND_KEY));
	}

	public static void set(ServerPlayer player, double amount) {
		player.getPersistentData().putDouble(SAND_KEY, clamp(amount));
		sync(player);
	}

	public static void add(ServerPlayer player, double amount) {
		set(player, get(player) + amount);
	}

	public static boolean tryConsume(ServerPlayer player, double amount) {
		double cost = Math.max(0.0D, amount);
		double available = get(player);
		if (available < cost)
			return false;
		set(player, available - cost);
		return true;
	}

	public static void sync(ServerPlayer player) {
		PacketDistributor.sendToPlayer(player, new SandReserveSyncPacket(get(player), CAPACITY));
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			sync(player);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			sync(player);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			sync(player);
	}

	@SubscribeEvent
	public static void clonePlayer(PlayerEvent.Clone event) {
		CompoundTag originalData = event.getOriginal().getPersistentData();
		if (originalData.contains(SAND_KEY))
			event.getEntity().getPersistentData().putDouble(SAND_KEY, clamp(originalData.getDouble(SAND_KEY)));
	}

	private static double clamp(double amount) {
		return Math.max(0.0D, Math.min(CAPACITY, amount));
	}
}
