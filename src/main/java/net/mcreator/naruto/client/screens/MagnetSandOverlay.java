package net.mcreator.naruto.client.screens;

import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.mcreator.naruto.NarutoMod;
import net.mcreator.naruto.MagnetSandReserve;

import javax.imageio.ImageIO;

import java.util.UUID;
import java.util.Optional;

import java.io.IOException;

import java.awt.image.BufferedImage;

@EventBusSubscriber(modid = NarutoMod.MODID, value = Dist.CLIENT)
public final class MagnetSandOverlay {
	private static final ResourceLocation OUTSIDE_TEXTURE = ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "textures/screens/gourd_outside.png");
	private static final ResourceLocation INSIDE_TEXTURE = ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "textures/screens/gourd_inside.png");
	private static final int SIM_WIDTH = 32;
	private static final int SIM_HEIGHT = 64;
	private static final int SIM_CELLS = SIM_WIDTH * SIM_HEIGHT;
	private static final int ALPHA_THRESHOLD = 8;
	private static final int SPAWN_ROW_COUNT = 2;
	private static final int MAX_EJECTED_GRAINS = 256;
	private static final double FIXED_STEPS_PER_SECOND = 60.0D;
	private static final double INFLOW_GRAINS_PER_SECOND = 75.0D;
	private static final double OUTFLOW_GRAINS_PER_SECOND = 110.0D;
	private static final double GRAVITY = 0.08D;
	private static final double LOOK_INERTIA = 0.055D;
	private static final double LOOK_SMOOTHING = 0.70D;
	private static final double LOOK_DECAY = 0.88D;
	private static final double LOOK_DELTA_TO_PIXELS = 0.85D;
	private static final double MAX_LOOK_MOTION = 60.0D;
	private static final double MOVEMENT_SMOOTHING = 0.58D;
	private static final double LATERAL_SPEED_TO_FORCE = 2.6D;
	private static final double LATERAL_ACCELERATION_TO_FORCE = 5.5D;
	private static final double VERTICAL_SPEED_TO_FORCE = 1D;
	private static final double VERTICAL_ACCELERATION_TO_FORCE = 3.0D;
	private static final double MAX_MOVEMENT_FORCE = 1.25D;
	private static final double MAX_FORCE = 1.45D;
	private static final double VELOCITY_DAMPING = 0.84D;
	private static final double RESTING_DAMPING = 0.62D;
	private static final double BURIED_FORCE_SCALE = 0.38D;
	private static final double SURFACE_FORCE_SCALE = 1.0D;
	private static final double MAX_VELOCITY = 2.4D;
	private static final int FLOW_RANGE = 2;
	private static final double SURFACE_TENSION = 0.68D;
	private static final double HORIZONTAL_LIFT_THRESHOLD = 0.42D;
	private static final double HORIZONTAL_LIFT_CHANCE = 0.72D;
	private static final double LANDING_IMPACT_TO_FORCE = 3.8D;
	private static final double LANDING_IMPULSE_DECAY = 0.72D;

	private static final boolean[] insideMask = new boolean[SIM_CELLS];
	private static final int[] insideColor = new int[SIM_CELLS];
	private static final boolean[] sand = new boolean[SIM_CELLS];
	private static final boolean[] moved = new boolean[SIM_CELLS];
	private static final double[] velocityX = new double[SIM_CELLS];
	private static final double[] velocityY = new double[SIM_CELLS];
	private static final int[] topSpawnCells = new int[SIM_CELLS];
	private static final int[] xOrder = new int[SIM_WIDTH];

	private static final boolean[] ejectedActive = new boolean[MAX_EJECTED_GRAINS];
	private static final double[] ejectedX = new double[MAX_EJECTED_GRAINS];
	private static final double[] ejectedY = new double[MAX_EJECTED_GRAINS];
	private static final double[] ejectedVelocityX = new double[MAX_EJECTED_GRAINS];
	private static final double[] ejectedVelocityY = new double[MAX_EJECTED_GRAINS];
	private static final int[] ejectedLife = new int[MAX_EJECTED_GRAINS];
	private static final int[] ejectedColor = new int[MAX_EJECTED_GRAINS];

	private static boolean assetsLoaded = false;
	private static boolean assetsAvailable = false;
	private static boolean sandInitialized = false;
	private static int legalCellCount = 0;
	private static int sandCellCount = 0;
	private static int topSpawnCellCount = 0;
	private static double reserveAmount = MagnetSandReserve.CAPACITY;
	private static double reserveCapacity = MagnetSandReserve.CAPACITY;
	private static double lastSimTime = -1.0D;
	private static double fixedStepAccumulator = 0.0D;
	private static double inflowAccumulator = 0.0D;
	private static double outflowAccumulator = 0.0D;
	private static float lastYaw = 0.0F;
	private static float lastPitch = 0.0F;
	private static boolean lookSampleReady = false;
	private static double lookVectorX = 0.0D;
	private static double lookVectorY = 0.0D;
	private static boolean movementSampleReady = false;
	private static int lastMovementTick = Integer.MIN_VALUE;
	private static double lastLateralMotion = 0.0D;
	private static double lastVerticalMotion = 0.0D;
	private static boolean lastOnGround = true;
	private static double landingImpulseY = 0.0D;
	private static double movementForceX = 0.0D;
	private static double movementForceY = 0.0D;
	private static int randomSeed = 0x53414E44;
	private static UUID activePlayer;

	private MagnetSandOverlay() {
	}

	public static void setReserve(double amount, double capacity) {
		reserveCapacity = Math.max(1.0D, capacity);
		reserveAmount = Math.max(0.0D, Math.min(reserveCapacity, amount));
	}

	@SubscribeEvent
	public static void render(RenderGuiEvent.Pre event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.options.hideGui || player.isSpectator()) {
			resetClock();
			if (player == null)
				activePlayer = null;
			return;
		}
		if (!player.getUUID().equals(activePlayer)) {
			activePlayer = player.getUUID();
			resetSimulation();
		}
		GuiGraphics gui = event.getGuiGraphics();
		int x = gui.guiWidth() / 2 + 98;
		int y = gui.guiHeight() - SIM_HEIGHT - 10;
		double ratio = Math.max(0.0D, Math.min(1.0D, reserveAmount / reserveCapacity));
		double time = player.tickCount + event.getPartialTick().getGameTimeDeltaPartialTick(false);
		renderGourd(minecraft, player, gui, x, y, ratio, time);
	}

	private static void renderGourd(Minecraft minecraft, LocalPlayer player, GuiGraphics gui, int x, int y, double targetRatio, double time) {
		ensureAssetsLoaded(minecraft);
		if (assetsAvailable) {
			stepSimulation(player, targetRatio, time);
			renderEjectedGrains(gui, x, y);
			renderSand(gui, x, y);
		}
		gui.blit(OUTSIDE_TEXTURE, x, y, 0, 0, SIM_WIDTH, SIM_HEIGHT, SIM_WIDTH, SIM_HEIGHT);
	}

	private static void stepSimulation(LocalPlayer player, double targetRatio, double timeTicks) {
		if (legalCellCount <= 0)
			return;
		int targetCells = (int) Math.round(targetRatio * legalCellCount);
		if (!sandInitialized) {
			initializeSand(targetCells);
			sandInitialized = true;
		}
		double elapsedTicks = lastSimTime < 0.0D ? 0.0D : Math.max(0.0D, Math.min(3.0D, timeTicks - lastSimTime));
		lastSimTime = timeTicks;
		double[] lookForce = sampleLookForce(player);
		double[] movementForce = sampleMovementForce(player);
		double forceX = clamp(lookForce[0] + movementForce[0], -MAX_FORCE, MAX_FORCE);
		double forceY = clamp(lookForce[1] + movementForce[1], -MAX_FORCE, MAX_FORCE);
		fixedStepAccumulator += elapsedTicks * FIXED_STEPS_PER_SECOND / 20.0D;
		fixedStepAccumulator = Math.min(8.0D, fixedStepAccumulator);
		int steps = 0;
		while (fixedStepAccumulator >= 1.0D && steps < 8) {
			fixedStepAccumulator -= 1.0D;
			steps++;
			updateReserveFlow(targetCells, 1.0D / FIXED_STEPS_PER_SECOND);
			simulate(forceX, forceY);
			updateEjectedGrains();
		}
	}

	private static void updateReserveFlow(int targetCells, double dt) {
		if (sandCellCount > targetCells) {
			inflowAccumulator = 0.0D;
			outflowAccumulator += dt * OUTFLOW_GRAINS_PER_SECOND;
			int attempts = 0;
			while (outflowAccumulator >= 1.0D && sandCellCount > targetCells && attempts++ < 6) {
				outflowAccumulator -= 1.0D;
				ejectSurfaceGrain();
			}
		} else if (sandCellCount < targetCells) {
			outflowAccumulator = 0.0D;
			inflowAccumulator += dt * INFLOW_GRAINS_PER_SECOND;
			int attempts = 0;
			while (inflowAccumulator >= 1.0D && sandCellCount < targetCells && attempts++ < 6) {
				inflowAccumulator -= 1.0D;
				if (!spawnOneGrain())
					placeFallbackGrain();
			}
		} else {
			inflowAccumulator = 0.0D;
			outflowAccumulator = 0.0D;
		}
	}

	private static boolean spawnOneGrain() {
		if (topSpawnCellCount <= 0)
			return false;
		int start = (int) Math.floor(nextRandom() * topSpawnCellCount);
		for (int i = 0; i < topSpawnCellCount; i++) {
			int index = topSpawnCells[(start + i) % topSpawnCellCount];
			if (sand[index])
				continue;
			sand[index] = true;
			velocityX[index] = -0.08D + nextRandom() * 0.16D;
			velocityY[index] = 0.15D;
			sandCellCount++;
			return true;
		}
		return false;
	}

	private static void placeFallbackGrain() {
		for (int y = SIM_HEIGHT - 1; y >= 0; y--) {
			for (int x = 0; x < SIM_WIDTH; x++) {
				int index = cell(x, y);
				if (insideMask[index] && !sand[index]) {
					sand[index] = true;
					sandCellCount++;
					return;
				}
			}
		}
	}

	private static void ejectSurfaceGrain() {
		int startX = (int) Math.floor(nextRandom() * SIM_WIDTH);
		for (int offset = 0; offset < SIM_WIDTH; offset++) {
			int x = (startX + offset) % SIM_WIDTH;
			for (int y = 0; y < SIM_HEIGHT; y++) {
				int index = cell(x, y);
				if (!sand[index])
					continue;
				sand[index] = false;
				velocityX[index] = 0.0D;
				velocityY[index] = 0.0D;
				sandCellCount--;
				spawnEjectedGrain(x, y, insideColor[index]);
				return;
			}
		}
	}

	private static void spawnEjectedGrain(int x, int y, int color) {
		for (int i = 0; i < MAX_EJECTED_GRAINS; i++) {
			if (ejectedActive[i])
				continue;
			ejectedActive[i] = true;
			ejectedX[i] = x + 0.5D;
			ejectedY[i] = y + 0.5D;
			ejectedVelocityX[i] = (SIM_WIDTH * 0.5D - ejectedX[i]) * 0.045D - 0.12D + nextRandom() * 0.24D;
			ejectedVelocityY[i] = -0.72D - nextRandom() * 0.42D;
			ejectedLife[i] = 30 + (int) Math.floor(nextRandom() * 12.0D);
			ejectedColor[i] = color;
			return;
		}
	}

	private static void updateEjectedGrains() {
		for (int i = 0; i < MAX_EJECTED_GRAINS; i++) {
			if (!ejectedActive[i])
				continue;
			ejectedX[i] += ejectedVelocityX[i];
			ejectedY[i] += ejectedVelocityY[i];
			if (ejectedY[i] > 0.0D)
				ejectedVelocityX[i] += (SIM_WIDTH * 0.5D - ejectedX[i]) * 0.012D;
			ejectedVelocityX[i] *= 0.98D;
			ejectedVelocityY[i] -= 0.012D;
			ejectedLife[i]--;
			if (ejectedLife[i] <= 0 || ejectedY[i] < -12.0D || ejectedX[i] < -8.0D || ejectedX[i] > SIM_WIDTH + 8.0D)
				ejectedActive[i] = false;
		}
	}

	private static double[] sampleLookForce(LocalPlayer player) {
		float yaw = player.getYRot();
		float pitch = player.getXRot();
		if (!lookSampleReady) {
			lastYaw = yaw;
			lastPitch = pitch;
			lookSampleReady = true;
			return new double[]{0.0D, GRAVITY};
		}
		double rawX = clamp(wrapDegrees(yaw - lastYaw) * LOOK_DELTA_TO_PIXELS, -MAX_LOOK_MOTION, MAX_LOOK_MOTION);
		double rawY = clamp((pitch - lastPitch) * LOOK_DELTA_TO_PIXELS, -MAX_LOOK_MOTION, MAX_LOOK_MOTION);
		lastYaw = yaw;
		lastPitch = pitch;
		lookVectorX = (lookVectorX * LOOK_SMOOTHING + rawX * (1.0D - LOOK_SMOOTHING)) * LOOK_DECAY;
		lookVectorY = (lookVectorY * LOOK_SMOOTHING + rawY * (1.0D - LOOK_SMOOTHING)) * LOOK_DECAY;
		// Minecraft yaw decreases when turning left, so invert it to push the sand right.
		double forceX = clamp(-lookVectorX * LOOK_INERTIA, -MAX_FORCE, MAX_FORCE);
		double forceY = clamp(GRAVITY + lookVectorY * LOOK_INERTIA, -MAX_FORCE, MAX_FORCE);
		return new double[]{forceX, forceY};
	}

	private static double[] sampleMovementForce(LocalPlayer player) {
		Vec3 motion = player.getDeltaMovement();
		double yawRadians = Math.toRadians(player.getYRot());
		double rightX = -Math.cos(yawRadians);
		double rightZ = -Math.sin(yawRadians);
		double lateralMotion = motion.x * rightX + motion.z * rightZ;
		boolean onGround = player.onGround();
		double verticalMotion = onGround ? 0.0D : motion.y;
		if (!movementSampleReady) {
			movementSampleReady = true;
			lastMovementTick = player.tickCount;
			lastLateralMotion = lateralMotion;
			lastVerticalMotion = verticalMotion;
			lastOnGround = onGround;
			movementForceX = clamp(-lateralMotion * LATERAL_SPEED_TO_FORCE, -MAX_MOVEMENT_FORCE, MAX_MOVEMENT_FORCE);
			movementForceY = clamp(verticalMotion * VERTICAL_SPEED_TO_FORCE, -MAX_MOVEMENT_FORCE, MAX_MOVEMENT_FORCE);
			return new double[]{movementForceX, movementForceY};
		}
		if (player.tickCount != lastMovementTick) {
			double lateralAcceleration = lateralMotion - lastLateralMotion;
			double verticalAcceleration = verticalMotion - lastVerticalMotion;
			if (onGround && !lastOnGround && lastVerticalMotion < -0.08D) {
				landingImpulseY += clamp(-lastVerticalMotion * LANDING_IMPACT_TO_FORCE, 0.0D, MAX_MOVEMENT_FORCE);
			}
			double targetX = clamp(-lateralMotion * LATERAL_SPEED_TO_FORCE - lateralAcceleration * LATERAL_ACCELERATION_TO_FORCE, -MAX_MOVEMENT_FORCE, MAX_MOVEMENT_FORCE);
			double targetY = clamp(verticalMotion * VERTICAL_SPEED_TO_FORCE + verticalAcceleration * VERTICAL_ACCELERATION_TO_FORCE, -MAX_MOVEMENT_FORCE, MAX_MOVEMENT_FORCE);
			movementForceX = movementForceX * MOVEMENT_SMOOTHING + targetX * (1.0D - MOVEMENT_SMOOTHING);
			movementForceY = movementForceY * MOVEMENT_SMOOTHING + targetY * (1.0D - MOVEMENT_SMOOTHING);
			landingImpulseY *= LANDING_IMPULSE_DECAY;
			lastLateralMotion = lateralMotion;
			lastVerticalMotion = verticalMotion;
			lastOnGround = onGround;
			lastMovementTick = player.tickCount;
		}
		return new double[]{movementForceX, clamp(movementForceY + landingImpulseY, -MAX_MOVEMENT_FORCE, MAX_MOVEMENT_FORCE)};
	}

	private static void simulate(double forceX, double forceY) {
		for (int i = 0; i < SIM_CELLS; i++)
			moved[i] = false;
		for (int x = 0; x < SIM_WIDTH; x++)
			xOrder[x] = x;
		shuffleXOrder();
		if (forceY >= 0.0D) {
			for (int y = SIM_HEIGHT - 1; y >= 0; y--)
				simulateRow(y, forceX, forceY);
		} else {
			for (int y = 0; y < SIM_HEIGHT; y++)
				simulateRow(y, forceX, forceY);
		}
		randomSeed = randomSeed * 1664525 + 1013904223;
	}

	private static void simulateRow(int y, double forceX, double forceY) {
		for (int orderIndex = 0; orderIndex < SIM_WIDTH; orderIndex++) {
			int x = xOrder[orderIndex];
			int index = cell(x, y);
			if (!sand[index] || moved[index])
				continue;
			double surfaceFreedom = surfaceFreedom(x, y, forceY);
			double forceScale = BURIED_FORCE_SCALE + (SURFACE_FORCE_SCALE - BURIED_FORCE_SCALE) * surfaceFreedom;
			double damping = isSupported(x, y, forceY) && Math.abs(forceX) < 0.28D && Math.abs(forceY - GRAVITY) < 0.34D ? RESTING_DAMPING : VELOCITY_DAMPING;
			velocityX[index] = clamp((velocityX[index] + forceX * forceScale) * damping, -MAX_VELOCITY, MAX_VELOCITY);
			velocityY[index] = clamp((velocityY[index] + forceY * forceScale) * damping, -MAX_VELOCITY, MAX_VELOCITY);
			int dx = sign(velocityX[index], 0.20D);
			int dy = sign(velocityY[index], 0.20D);
			if (dy == 0)
				dy = forceY >= 0.0D ? 1 : -1;
			if (dx == 0)
				dx = sign(forceX, 0.14D);
			int side = dx != 0 ? dx : nextRandom() < 0.5D ? -1 : 1;
			int otherSide = -side;
			boolean horizontalDominant = Math.abs(velocityX[index]) > Math.abs(velocityY[index]) * 1.15D;
			if (horizontalDominant) {
				if (tryMoveInOrder(x, y, side, 0, side, dy, 0, dy, otherSide, dy, otherSide, 0))
					continue;
			} else if (tryMoveInOrder(x, y, 0, dy, side, dy, otherSide, dy, side, 0, otherSide, 0)) {
				continue;
			}
			int lifted = findWallLift(x, y, side, forceX, forceY);
			if (lifted >= 0 && moveParticle(index, lifted))
				continue;
			int spread = findSideSpread(x, y, side, forceX);
			if (spread >= 0 && moveParticle(index, spread))
				continue;
			if (trySettleIntoPocket(x, y, forceY))
				continue;
			velocityX[index] *= isSupported(x, y, forceY) ? 0.34D : 0.48D;
			velocityY[index] *= isSupported(x, y, forceY) ? 0.34D : 0.48D;
		}
	}

	private static double surfaceFreedom(int x, int y, double forceY) {
		int open = 0;
		int vertical = forceY >= 0.0D ? -1 : 1;
		if (candidateCell(x, y + vertical) >= 0)
			open += 2;
		if (candidateCell(x - 1, y) >= 0)
			open++;
		if (candidateCell(x + 1, y) >= 0)
			open++;
		if (candidateCell(x - 1, y + vertical) >= 0)
			open++;
		if (candidateCell(x + 1, y + vertical) >= 0)
			open++;
		return Math.min(1.0D, open / 4.0D);
	}

	private static boolean isSupported(int x, int y, double forceY) {
		int down = forceY >= 0.0D ? 1 : -1;
		int supportY = y + down;
		if (supportY < 0 || supportY >= SIM_HEIGHT)
			return true;
		int below = cell(x, supportY);
		return !insideMask[below] || sand[below];
	}

	private static boolean trySettleIntoPocket(int x, int y, double forceY) {
		int down = forceY >= 0.0D ? 1 : -1;
		int from = cell(x, y);
		int leftPocket = candidateCell(x - 1, y + down);
		int rightPocket = candidateCell(x + 1, y + down);
		if (leftPocket < 0 && rightPocket < 0)
			return false;
		if (leftPocket >= 0 && rightPocket >= 0)
			return moveParticle(from, nextRandom() < 0.5D ? leftPocket : rightPocket);
		return moveParticle(from, leftPocket >= 0 ? leftPocket : rightPocket);
	}

	private static boolean tryMoveInOrder(int x, int y, int ox1, int oy1, int ox2, int oy2, int ox3, int oy3, int ox4, int oy4, int ox5, int oy5) {
		return tryOffset(x, y, ox1, oy1) || tryOffset(x, y, ox2, oy2) || tryOffset(x, y, ox3, oy3) || tryOffset(x, y, ox4, oy4) || tryOffset(x, y, ox5, oy5);
	}

	private static boolean tryOffset(int x, int y, int ox, int oy) {
		if (ox == 0 && oy == 0)
			return false;
		int from = cell(x, y);
		int target = candidateCell(x + ox, y + oy);
		return target >= 0 && moveParticle(from, target);
	}

	private static int findSideSpread(int x, int y, int preferredDirection, double horizontalForce) {
		if (nextRandom() < SURFACE_TENSION * (1.0D - Math.min(1.0D, Math.abs(horizontalForce))))
			return -1;
		int first = preferredDirection == 0 ? (nextRandom() < 0.5D ? -1 : 1) : preferredDirection;
		int target = findSideTarget(x, y, first);
		return target >= 0 ? target : findSideTarget(x, y, -first);
	}

	private static int findSideTarget(int x, int y, int direction) {
		int best = -1;
		for (int distance = 1; distance <= FLOW_RANGE; distance++) {
			int target = candidateCell(x + direction * distance, y);
			if (target < 0)
				break;
			best = target;
		}
		return best;
	}

	private static int findWallLift(int x, int y, int side, double forceX, double forceY) {
		double strength = Math.abs(forceX);
		if (strength < HORIZONTAL_LIFT_THRESHOLD)
			return -1;
		double chance = Math.min(HORIZONTAL_LIFT_CHANCE, (strength - HORIZONTAL_LIFT_THRESHOLD) * 0.8D);
		if (nextRandom() > chance)
			return -1;
		int sideCell = candidateCell(x + side, y);
		if (sideCell >= 0)
			return -1;
		int up = forceY >= 0.0D ? -1 : 1;
		int target = candidateCell(x + side, y + up);
		if (target >= 0)
			return target;
		target = candidateCell(x, y + up);
		return target >= 0 ? target : candidateCell(x - side, y + up);
	}

	private static boolean moveParticle(int from, int to) {
		if (to < 0 || sand[to])
			return false;
		sand[to] = true;
		sand[from] = false;
		velocityX[to] = velocityX[from];
		velocityY[to] = velocityY[from];
		velocityX[from] = 0.0D;
		velocityY[from] = 0.0D;
		moved[to] = true;
		return true;
	}

	private static void initializeSand(int targetCells) {
		clearSand();
		int remaining = Math.max(0, Math.min(legalCellCount, targetCells));
		for (int y = SIM_HEIGHT - 1; y >= 0 && remaining > 0; y--) {
			for (int x = 0; x < SIM_WIDTH && remaining > 0; x++) {
				int index = cell(x, y);
				if (!insideMask[index])
					continue;
				sand[index] = true;
				sandCellCount++;
				remaining--;
			}
		}
	}

	private static void renderSand(GuiGraphics gui, int x, int y) {
		for (int index = 0; index < SIM_CELLS; index++) {
			if (!sand[index] || !insideMask[index])
				continue;
			int px = index % SIM_WIDTH;
			int py = index / SIM_WIDTH;
			gui.fill(x + px, y + py, x + px + 1, y + py + 1, insideColor[index]);
		}
	}

	private static void renderEjectedGrains(GuiGraphics gui, int x, int y) {
		for (int i = 0; i < MAX_EJECTED_GRAINS; i++) {
			if (!ejectedActive[i])
				continue;
			int px = x + (int) Math.floor(ejectedX[i]);
			int py = y + (int) Math.floor(ejectedY[i]);
			int alpha = Math.min(255, Math.max(45, ejectedLife[i] * 10));
			int color = (alpha << 24) | (ejectedColor[i] & 0x00FFFFFF);
			gui.fill(px, py, px + 1, py + 1, color);
		}
	}

	private static void ensureAssetsLoaded(Minecraft minecraft) {
		if (assetsLoaded)
			return;
		assetsLoaded = true;
		for (int x = 0; x < SIM_WIDTH; x++)
			xOrder[x] = x;
		try {
			Optional<Resource> resource = minecraft.getResourceManager().getResource(INSIDE_TEXTURE);
			if (resource.isEmpty())
				return;
			BufferedImage image = ImageIO.read(resource.get().open());
			if (image == null)
				return;
			int width = Math.min(SIM_WIDTH, image.getWidth());
			int height = Math.min(SIM_HEIGHT, image.getHeight());
			for (int py = 0; py < height; py++) {
				for (int px = 0; px < width; px++) {
					int color = image.getRGB(px, py);
					if (((color >>> 24) & 255) <= ALPHA_THRESHOLD)
						continue;
					int index = cell(px, py);
					insideMask[index] = true;
					insideColor[index] = color;
					legalCellCount++;
				}
			}
			int rowsFound = 0;
			for (int py = 0; py < height && rowsFound < SPAWN_ROW_COUNT; py++) {
				boolean legalRow = false;
				for (int px = 0; px < width; px++) {
					if (insideMask[cell(px, py)]) {
						legalRow = true;
						break;
					}
				}
				if (!legalRow)
					continue;
				for (int px = 0; px < width; px++) {
					int index = cell(px, py);
					if (insideMask[index])
						topSpawnCells[topSpawnCellCount++] = index;
				}
				rowsFound++;
			}
			assetsAvailable = legalCellCount > 0 && topSpawnCellCount > 0;
		} catch (IOException exception) {
			NarutoMod.LOGGER.warn("Could not load the magnet sand gourd mask", exception);
		}
	}

	private static void resetSimulation() {
		clearSand();
		for (int i = 0; i < MAX_EJECTED_GRAINS; i++)
			ejectedActive[i] = false;
		sandInitialized = false;
		resetClock();
	}

	private static void clearSand() {
		for (int i = 0; i < SIM_CELLS; i++) {
			sand[i] = false;
			moved[i] = false;
			velocityX[i] = 0.0D;
			velocityY[i] = 0.0D;
		}
		sandCellCount = 0;
		inflowAccumulator = 0.0D;
		outflowAccumulator = 0.0D;
	}

	private static void resetClock() {
		lastSimTime = -1.0D;
		fixedStepAccumulator = 0.0D;
		lookSampleReady = false;
		lookVectorX = 0.0D;
		lookVectorY = 0.0D;
		movementSampleReady = false;
		lastMovementTick = Integer.MIN_VALUE;
		lastLateralMotion = 0.0D;
		lastVerticalMotion = 0.0D;
		lastOnGround = true;
		landingImpulseY = 0.0D;
		movementForceX = 0.0D;
		movementForceY = 0.0D;
	}

	private static int candidateCell(int x, int y) {
		if (x < 0 || x >= SIM_WIDTH || y < 0 || y >= SIM_HEIGHT)
			return -1;
		int index = cell(x, y);
		return insideMask[index] && !sand[index] ? index : -1;
	}

	private static int cell(int x, int y) {
		return y * SIM_WIDTH + x;
	}

	private static int sign(double value, double deadzone) {
		if (value > deadzone)
			return 1;
		if (value < -deadzone)
			return -1;
		return 0;
	}

	private static void shuffleXOrder() {
		for (int i = SIM_WIDTH - 1; i > 0; i--) {
			int swap = (int) Math.floor(nextRandom() * (i + 1));
			int value = xOrder[i];
			xOrder[i] = xOrder[swap];
			xOrder[swap] = value;
		}
	}

	private static double wrapDegrees(double degrees) {
		double wrapped = degrees % 360.0D;
		if (wrapped >= 180.0D)
			wrapped -= 360.0D;
		if (wrapped < -180.0D)
			wrapped += 360.0D;
		return wrapped;
	}

	private static double nextRandom() {
		randomSeed = randomSeed * 1664525 + 1013904223;
		return (randomSeed & 0x7FFFFFFF) / (double) 0x80000000L;
	}

	private static double clamp(double value, double minimum, double maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}
}
