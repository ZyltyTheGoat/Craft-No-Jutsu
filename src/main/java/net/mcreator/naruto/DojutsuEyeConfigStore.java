package net.mcreator.naruto;

import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class DojutsuEyeConfigStore {
	private static final Map<Integer, Config> CONFIGS = new ConcurrentHashMap<>();
	private static final Config DEFAULT_CONFIG = new Config(2.0D, 2.0D, 4.0D, -3.2D, -4.08D);
	private static final String TAG_NAME = "NarutoDojutsuEyeConfig";
	private static final String WIDTH = "Width";
	private static final String HEIGHT = "Height";
	private static final String SPACING = "Spacing";
	private static final String Y_OFFSET = "YOffset";
	private static final String Z_OFFSET = "ZOffset";

	public static Config get(int playerId) {
		return CONFIGS.getOrDefault(playerId, DEFAULT_CONFIG);
	}

	public static Config set(int playerId, double width, double height, double spacing, double yOffset, double zOffset) {
		Config config = new Config(clamp(width, 1.0D, 3.0D), clamp(height, 1.0D, 3.0D), clamp(spacing, 1.0D, 7.0D), clamp(yOffset, -7.0D, -1.0D), clamp(zOffset, -4.35D, -3.75D));
		CONFIGS.put(playerId, config);
		return config;
	}

	public static Config load(ServerPlayer player) {
		CompoundTag root = player.getPersistentData();
		if (!root.contains(TAG_NAME)) {
			CONFIGS.put(player.getId(), DEFAULT_CONFIG);
			return DEFAULT_CONFIG;
		}
		CompoundTag tag = root.getCompound(TAG_NAME);
		return set(player.getId(), getOrDefault(tag, WIDTH, DEFAULT_CONFIG.width()), getOrDefault(tag, HEIGHT, DEFAULT_CONFIG.height()), getOrDefault(tag, SPACING, DEFAULT_CONFIG.spacing()), getOrDefault(tag, Y_OFFSET, DEFAULT_CONFIG.yOffset()),
				getOrDefault(tag, Z_OFFSET, DEFAULT_CONFIG.zOffset()));
	}

	public static void save(ServerPlayer player, Config config) {
		CompoundTag tag = new CompoundTag();
		tag.putDouble(WIDTH, config.width());
		tag.putDouble(HEIGHT, config.height());
		tag.putDouble(SPACING, config.spacing());
		tag.putDouble(Y_OFFSET, config.yOffset());
		tag.putDouble(Z_OFFSET, config.zOffset());
		player.getPersistentData().put(TAG_NAME, tag);
		CONFIGS.put(player.getId(), config);
	}

	public static Config setAndSave(ServerPlayer player, double width, double height, double spacing, double yOffset, double zOffset) {
		Config config = set(player.getId(), width, height, spacing, yOffset, zOffset);
		save(player, config);
		return config;
	}

	private static double getOrDefault(CompoundTag tag, String key, double fallback) {
		return tag.contains(key) ? tag.getDouble(key) : fallback;
	}

	private static double clamp(double value, double min, double max) {
		if (!Double.isFinite(value)) {
			return min;
		}
		return Mth.clamp(value, min, max);
	}

	public record Config(double width, double height, double spacing, double yOffset, double zOffset) {
	}
}
