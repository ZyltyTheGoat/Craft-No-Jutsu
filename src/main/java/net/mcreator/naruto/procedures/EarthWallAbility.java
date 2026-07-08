package net.mcreator.naruto.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.network.NarutoModVariables;

import java.util.ArrayList;
import java.util.List;

public class EarthWallAbility {
	private static final String JUTSU_ID = "earth_wall";
	private static final int RISE_INTERVAL_TICKS = 2;

	public static boolean tryRelease(Entity entity, JutsuData jutsuData) {
		if (!(entity instanceof Player player) || jutsuData == null) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		float chargePercent = Mth.clamp((float) (vars.chargeTime / Math.max(1.0D, jutsuData.getChargeTime())), 0.0F, 1.0F);
		clearCharge(player);
		if (vars.currentChakra < jutsuData.getChakraCost()) {
			JutsuFeedback.notEnoughChakra(player);
			return true;
		}
		int cooldown = Math.max(1, jutsuData.getCooldown());
		if (!JutsuCooldowns.tryStart(player, JUTSU_ID, cooldown)) {
			showCooldown(player, jutsuData);
			return true;
		}
		if (player.level() instanceof ServerLevel serverLevel) {
			buildWall(serverLevel, player, chargePercent);
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		return true;
	}

	private static void buildWall(ServerLevel level, Player player, float chargePercent) {
		Vec3 forward = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
		if (forward.lengthSqr() < 0.001D) {
			forward = Vec3.directionFromRotation(0.0F, player.getYRot()).multiply(1.0D, 0.0D, 1.0D);
		}
		forward = forward.normalize();
		Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward).normalize();
		Vec3 center = player.position().add(forward.scale(3.0D + chargePercent * 1.4D));
		int width = 3 + Mth.floor(chargePercent * 4.0F);
		int height = 2 + Mth.floor(chargePercent * 3.0F);
		int thickness = chargePercent > 0.7F ? 2 : 1;
		int duration = 140 + Mth.floor(chargePercent * 100.0F);
		List<WallBlock> wallBlocks = collectWallBlocks(level, center, right, forward, width, height, thickness);
		for (int layer = 0; layer < height; layer++) {
			int currentLayer = layer;
			NarutoMod.queueServerWork(currentLayer * RISE_INTERVAL_TICKS + 1, () -> placeLayer(level, wallBlocks, currentLayer));
		}
		NarutoMod.queueServerWork(duration + height * RISE_INTERVAL_TICKS, () -> restoreWall(level, wallBlocks));
	}

	private static List<WallBlock> collectWallBlocks(ServerLevel level, Vec3 center, Vec3 right, Vec3 forward, int width, int height, int thickness) {
		List<WallBlock> blocks = new ArrayList<>();
		int halfWidth = width / 2;
		int backOffset = thickness / 2;
		for (int side = -halfWidth; side <= halfWidth; side++) {
			for (int thick = 0; thick < thickness; thick++) {
				Vec3 columnCenter = center.add(right.scale(side)).add(forward.scale(thick - backOffset));
				BlockPos ground = findGround(level, BlockPos.containing(columnCenter));
				BlockState wallState = wallStateFromGround(level.getBlockState(ground));
				for (int layer = 0; layer < height; layer++) {
					BlockPos pos = ground.above(layer + 1);
					BlockState original = level.getBlockState(pos);
					if (canReplace(original)) {
						blocks.add(new WallBlock(pos, original, wallState, layer));
					}
				}
			}
		}
		return blocks;
	}

	private static BlockPos findGround(ServerLevel level, BlockPos origin) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(origin.getX(), origin.getY() + 2, origin.getZ());
		int minY = Math.max(level.getMinBuildHeight() + 1, origin.getY() - 8);
		for (int y = cursor.getY(); y >= minY; y--) {
			cursor.setY(y);
			BlockState state = level.getBlockState(cursor);
			if (!state.isAir() && state.getFluidState().isEmpty() && state.isFaceSturdy(level, cursor, Direction.UP)) {
				return cursor.immutable();
			}
		}
		return origin.below();
	}

	private static BlockState wallStateFromGround(BlockState groundState) {
		if (groundState.isAir() || !groundState.getFluidState().isEmpty()) {
			return Blocks.DIRT.defaultBlockState();
		}
		return groundState;
	}

	private static boolean canReplace(BlockState state) {
		return state.isAir() || !state.getFluidState().isEmpty();
	}

	private static void placeLayer(ServerLevel level, List<WallBlock> wallBlocks, int layer) {
		for (WallBlock block : wallBlocks) {
			if (block.layer != layer || !level.isLoaded(block.pos)) {
				continue;
			}
			BlockState current = level.getBlockState(block.pos);
			if (!canReplace(current)) {
				continue;
			}
			level.setBlock(block.pos, block.wallState, 3);
			spawnBlockParticles(level, block.pos, block.wallState, 18, 0.18D);
		}
	}

	private static void restoreWall(ServerLevel level, List<WallBlock> wallBlocks) {
		for (int i = wallBlocks.size() - 1; i >= 0; i--) {
			WallBlock block = wallBlocks.get(i);
			if (!level.isLoaded(block.pos) || level.getBlockState(block.pos).getBlock() != block.wallState.getBlock()) {
				continue;
			}
			spawnBlockParticles(level, block.pos, block.wallState, 8, 0.08D);
			level.setBlock(block.pos, block.originalState, 3);
		}
	}

	private static void spawnBlockParticles(ServerLevel level, BlockPos pos, BlockState state, int count, double speed) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5D, pos.getY() + 0.12D, pos.getZ() + 0.5D, count, 0.34D, 0.16D, 0.34D, speed);
	}

	private static void showCooldown(Player player, JutsuData jutsuData) {
		JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, JUTSU_ID));
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}

	private record WallBlock(BlockPos pos, BlockState originalState, BlockState wallState, int layer) {
	}
}
