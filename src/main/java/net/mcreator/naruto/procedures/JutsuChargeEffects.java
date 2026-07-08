package net.mcreator.naruto.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;

public final class JutsuChargeEffects {
	private static final double TAU = Math.PI * 2.0D;

	private JutsuChargeEffects() {
	}

	public static void tick(Entity entity, JutsuData jutsuData, double chargeTime, double maxChargeTime, boolean justFullyCharged) {
		if (!(entity instanceof Player player) || !(player.level() instanceof ServerLevel serverLevel) || jutsuData == null || maxChargeTime <= 0.0D) {
			return;
		}
		float progress = Mth.clamp((float) (chargeTime / maxChargeTime), 0.0F, 1.0F);
		int interval = progress > 0.75F ? 4 : 6;
		if (justFullyCharged) {
			spawnReadyBurst(serverLevel, player, jutsuData, progress);
			return;
		}
		if (player.tickCount % interval != 0) {
			return;
		}
		spawnAura(serverLevel, player, jutsuData, progress);
	}

	private static void spawnAura(ServerLevel level, Player player, JutsuData jutsuData, float progress) {
		String nature = normalizedNature(jutsuData);
		double radius = 0.75D + progress * 0.35D;
		double baseY = player.getY() + 0.12D;
		int points = 1 + Mth.floor(progress * 2.0F);
		for (int i = 0; i < points; i++) {
			double angle = player.tickCount * (0.22D + progress * 0.12D) + (TAU * i / points);
			Vec3 pos = new Vec3(player.getX() + Math.cos(angle) * radius, baseY + player.getRandom().nextDouble() * (0.35D + progress * 0.8D), player.getZ() + Math.sin(angle) * radius);
			spawnNatureParticle(level, player, nature, pos, progress, false);
		}
	}

	private static void spawnReadyBurst(ServerLevel level, Player player, JutsuData jutsuData, float progress) {
		String nature = normalizedNature(jutsuData);
		Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.48D, 0.0D);
		level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.45F, 0.85F + progress * 0.35F);
		switch (nature) {
			case "FIRE" -> {
				level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 14, 0.5D, 0.36D, 0.5D, 0.05D);
				level.sendParticles(ParticleTypes.LAVA, center.x, center.y - 0.1D, center.z, 2, 0.32D, 0.08D, 0.32D, 0.015D);
				level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 6, 0.38D, 0.24D, 0.38D, 0.02D);
			}
			case "LIGHTNING" -> {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 16, 0.5D, 0.42D, 0.5D, 0.08D);
				level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			}
			case "EARTH" -> {
				BlockState state = groundState(level, player.blockPosition());
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), center.x, player.getY() + 0.08D, center.z, 18, 0.6D, 0.12D, 0.6D, 0.12D);
				level.sendParticles(ParticleTypes.POOF, center.x, player.getY() + 0.18D, center.z, 6, 0.42D, 0.16D, 0.42D, 0.025D);
			}
			case "WATER" -> {
				level.sendParticles(ParticleTypes.SPLASH, center.x, center.y, center.z, 16, 0.52D, 0.3D, 0.52D, 0.08D);
				level.sendParticles(ParticleTypes.BUBBLE, center.x, center.y - 0.15D, center.z, 6, 0.32D, 0.14D, 0.32D, 0.025D);
			}
			case "WIND" -> {
				level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 16, 0.68D, 0.3D, 0.68D, 0.08D);
				level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 6, 0.4D, 0.2D, 0.4D, 0.035D);
			}
			case "WOOD" -> {
				BlockState state = Blocks.OAK_LEAVES.defaultBlockState();
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), center.x, center.y, center.z, 14, 0.52D, 0.36D, 0.52D, 0.05D);
				level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 5, 0.32D, 0.2D, 0.32D, 0.025D);
			}
			case "EXPLOSION" -> {
				level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 7, 0.38D, 0.24D, 0.38D, 0.05D);
				level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 14, 0.52D, 0.3D, 0.52D, 0.035D);
			}
			default -> {
				level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 10, 0.4D, 0.28D, 0.4D, 0.035D);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 3, 0.32D, 0.24D, 0.32D, 0.025D);
			}
		}
	}

	private static void spawnNatureParticle(ServerLevel level, Player player, String nature, Vec3 pos, float progress, boolean centerPulse) {
		int count = centerPulse ? 5 : 1;
		double speed = centerPulse ? 0.04D + progress * 0.04D : 0.01D + progress * 0.02D;
		switch (nature) {
			case "FIRE" -> {
				level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.15D, pos.z, count, 0.05D, 0.08D, 0.05D, speed);
				if (player.tickCount % 5 == 0) {
					level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.04D, 0.04D, 0.04D, 0.01D);
				}
			}
			case "LIGHTNING" -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.35D, pos.z, count + 1, 0.06D, 0.18D, 0.06D, speed + 0.03D);
			case "EARTH" -> level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState(level, player.blockPosition())), pos.x, player.getY() + 0.08D, pos.z, count + 1, 0.06D, 0.04D, 0.06D, speed + 0.04D);
			case "WATER" -> level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y + 0.08D, pos.z, count + 1, 0.08D, 0.05D, 0.08D, speed + 0.03D);
			case "WIND" -> level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.12D, pos.z, count + 1, 0.08D, 0.04D, 0.08D, speed + 0.04D);
			case "WOOD" -> level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), pos.x, pos.y + 0.1D, pos.z, count + 1, 0.08D, 0.05D, 0.08D, speed);
			case "EXPLOSION" -> {
				level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.12D, pos.z, count + 1, 0.08D, 0.05D, 0.08D, speed + 0.03D);
				if (centerPulse) {
					level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.2D, pos.z, 3, 0.12D, 0.08D, 0.12D, 0.04D);
				}
			}
			default -> level.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.12D, pos.z, count, 0.08D, 0.05D, 0.08D, speed);
		}
	}

	private static String normalizedNature(JutsuData jutsuData) {
		String nature = jutsuData.getNature();
		return nature == null ? "" : nature.trim().toUpperCase(java.util.Locale.ROOT);
	}

	private static BlockState groundState(ServerLevel level, BlockPos playerPos) {
		BlockState state = level.getBlockState(playerPos.below());
		if (state.isAir() || !state.getFluidState().isEmpty()) {
			return Blocks.DIRT.defaultBlockState();
		}
		return state;
	}
}
