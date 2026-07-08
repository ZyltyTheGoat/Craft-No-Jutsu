package net.mcreator.naruto.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

@EventBusSubscriber(value = Dist.CLIENT)
public class ChidoriClientEffects {
	private static final Map<UUID, ClientState> ACTIVE = new HashMap<>();
	private static final List<HitBurst> HIT_BURSTS = new ArrayList<>();
	private static final int WHITE_CORE = 0xFFFFFFFF;
	private static final int PALE_BLUE = 0xFFF0FBFF;
	private static final int ELECTRIC_BLUE = 0xFF00D8FF;
	private static final int DEEP_BLUE = 0xEE006BFF;
	private static final int BLUE_SHADOW = 0xD00026FF;
	private static final Basis IDENTITY_BASIS = new Basis(new BoltVec(1.0F, 0.0F, 0.0F), new BoltVec(0.0F, 1.0F, 0.0F), new BoltVec(0.0F, 0.0F, 1.0F));

	public static void setActive(UUID playerId, boolean active, int ticks, float chargePercent) {
		if (playerId == null) {
			return;
		}
		if (active) {
			ACTIVE.put(playerId, new ClientState(Math.max(1, ticks), Mth.clamp(chargePercent, 0.0F, 1.0F)));
		} else {
			ACTIVE.remove(playerId);
		}
	}

	public static void addHitBurst(int entityId, double x, double y, double z, float xRot, float yRot, int ticks, float chargePercent, float scale) {
		int safeTicks = Math.max(3, ticks);
		int seed = (int) ((Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) % 10000L);
		HIT_BURSTS.add(new HitBurst(entityId, new Vec3(x, y, z), xRot, yRot, safeTicks, safeTicks, Mth.clamp(chargePercent, 0.0F, 1.0F), Math.max(0.1F, scale), seed));
	}

	public static void renderCloneLightning(PoseStack poseStack, MultiBufferSource bufferSource, float age) {
		renderHandStorm(poseStack, bufferSource, age, 0.75F, false, false, IDENTITY_BASIS, 1.0F);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<Map.Entry<UUID, ClientState>> stateIterator = ACTIVE.entrySet().iterator();
		while (stateIterator.hasNext()) {
			ClientState state = stateIterator.next().getValue();
			state.ticks--;
			if (state.ticks <= 0) {
				stateIterator.remove();
			}
		}
		Iterator<HitBurst> burstIterator = HIT_BURSTS.iterator();
		while (burstIterator.hasNext()) {
			HitBurst burst = burstIterator.next();
			burst.ticks--;
			if (burst.ticks <= 0) {
				burstIterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void renderFirstPerson(RenderHandEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (player == null || event.getHand() != InteractionHand.MAIN_HAND || !minecraft.options.getCameraType().isFirstPerson()) {
			return;
		}
		ClientState state = ACTIVE.get(player.getUUID());
		if (state == null) {
			return;
		}
		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		float swing = event.getSwingProgress();
		float equip = event.getEquipProgress();
		poseStack.translate(0.44D - swing * 0.08D, -0.46D + (1.0F - equip) * 0.08D, -0.74D - swing * 0.06D);
		poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F - swing * 18.0F));
		poseStack.mulPose(Axis.YP.rotationDegrees(10.0F + swing * 12.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(-6.0F));
		float age = player.tickCount + event.getPartialTick();
		renderHandStorm(poseStack, event.getMultiBufferSource(), age, state.chargePercent, true, !event.getItemStack().isEmpty(), IDENTITY_BASIS, -1.0F);
		poseStack.popPose();
	}

	@SubscribeEvent
	public static void renderWorldEffects(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || (ACTIVE.isEmpty() && HIT_BURSTS.isEmpty())) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}
		Vec3 camera = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		for (Player player : minecraft.level.players()) {
			ClientState state = ACTIVE.get(player.getUUID());
			if (state == null || player == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
				continue;
			}
			Vec3 hand = worldHandPosition(player);
			poseStack.pushPose();
			poseStack.translate(hand.x - camera.x, hand.y - camera.y, hand.z - camera.z);
			float age = player.tickCount + partialTick;
			renderHandStorm(poseStack, bufferSource, age, state.chargePercent, false, !player.getMainHandItem().isEmpty(), basisFromPlayer(player), 1.0F);
			poseStack.popPose();
		}
		for (HitBurst burst : HIT_BURSTS) {
			Entity entity = minecraft.level.getEntity(burst.entityId);
			Vec3 position = burst.position(entity);
			Basis basis = burst.basis(entity);
			poseStack.pushPose();
			poseStack.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
			poseStack.scale(burst.scale, burst.scale, burst.scale);
			float age = burst.seed + (burst.totalTicks - burst.ticks) + partialTick;
			renderHandStorm(poseStack, bufferSource, age, burst.chargePercent, false, false, basis, 1.0F);
			poseStack.popPose();
		}
		bufferSource.endBatch(RenderType.lightning());
	}

	private static Vec3 worldHandPosition(Player player) {
		Vec3 look = player.getLookAngle();
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		look = look.normalize();
		Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(look);
		if (right.lengthSqr() < 0.001D) {
			right = Vec3.directionFromRotation(0.0F, player.getYRot() + 90.0F);
		}
		right = right.normalize();
		return player.getEyePosition().add(look.scale(0.58D)).add(right.scale(0.28D)).add(0.0D, -0.34D, 0.0D);
	}

	private static Basis basisFromPlayer(Player player) {
		Vec3 forward = player.getLookAngle();
		if (forward.lengthSqr() < 0.001D) {
			forward = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		return basisFromForward(forward, player.getYRot());
	}

	private static Basis basisFromRotation(float xRot, float yRot) {
		return basisFromForward(Vec3.directionFromRotation(xRot, yRot), yRot);
	}

	private static Basis basisFromForward(Vec3 forward, float yRot) {
		forward = forward.normalize();
		Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
		if (right.lengthSqr() < 0.001D) {
			right = Vec3.directionFromRotation(0.0F, yRot + 90.0F);
		}
		right = right.normalize();
		Vec3 up = forward.cross(right).normalize();
		return new Basis(fromVec3(right), fromVec3(up), fromVec3(forward));
	}

	private static BoltVec fromVec3(Vec3 vec) {
		return new BoltVec((float) vec.x, (float) vec.y, (float) vec.z);
	}

	private static void renderHandStorm(PoseStack poseStack, MultiBufferSource bufferSource, float age, float chargePercent, boolean firstPerson, boolean weapon, Basis basis, float weaponForwardSign) {
		VertexConsumer buffer = bufferSource.getBuffer(RenderType.lightning());
		float seed = Mth.floor(age * 12.0F);
		float radius = (firstPerson ? 0.38F : 0.3F) + chargePercent * 0.12F;
		float pulse = 0.85F + 0.22F * Mth.sin(age * 2.2F);
		renderCoreStorm(poseStack, buffer, basis, seed, radius * pulse, firstPerson);
		int bolts = firstPerson ? 18 : 12;
		for (int i = 0; i < bolts; i++) {
			BoltVec direction = randomDirection(seed, i, 1);
			BoltVec start = randomDirection(seed, i, 2).scale(radius * Mth.lerp(random(seed, i, 3), 0.02F, 0.14F));
			BoltVec end = direction.scale(radius * Mth.lerp(random(seed, i, 4), 0.85F, 1.75F));
			float chaos = radius * Mth.lerp(chargePercent, 0.22F, 0.42F);
			renderJaggedBolt(poseStack, buffer, basis, start, end, chaos, 5 + (i % 4), seed + i * 17.0F, firstPerson ? 0.016F : 0.011F, PALE_BLUE, ELECTRIC_BLUE);
			if (i % 2 == 0) {
				BoltVec branchDirection = randomDirection(seed, i, 9);
				BoltVec branchStart = start.lerp(end, Mth.lerp(random(seed, i, 10), 0.35F, 0.72F));
				BoltVec branchEnd = branchStart.add(branchDirection.scale(radius * Mth.lerp(random(seed, i, 11), 0.28F, 0.62F)));
				renderJaggedBolt(poseStack, buffer, basis, branchStart, branchEnd, chaos * 0.55F, 3, seed + i * 31.0F, firstPerson ? 0.01F : 0.007F, WHITE_CORE, DEEP_BLUE);
			}
		}
		if (weapon) {
			renderWeaponStorm(poseStack, buffer, basis, seed, radius, firstPerson, weaponForwardSign);
		}
	}

	private static void renderCoreStorm(PoseStack poseStack, VertexConsumer buffer, Basis basis, float seed, float radius, boolean firstPerson) {
		int coreBolts = firstPerson ? 8 : 6;
		for (int i = 0; i < coreBolts; i++) {
			BoltVec direction = randomDirection(seed, i, 30);
			BoltVec start = direction.scale(-radius * Mth.lerp(random(seed, i, 31), 0.18F, 0.42F));
			BoltVec end = direction.scale(radius * Mth.lerp(random(seed, i, 32), 0.35F, 0.72F));
			renderJaggedBolt(poseStack, buffer, basis, start, end, radius * 0.14F, 3, seed + i * 13.0F, firstPerson ? 0.015F : 0.01F, WHITE_CORE, PALE_BLUE);
		}
	}

	private static void renderWeaponStorm(PoseStack poseStack, VertexConsumer buffer, Basis basis, float seed, float radius, boolean firstPerson, float forwardSign) {
		float length = firstPerson ? 1.18F : 0.86F;
		BoltVec root = new BoltVec(0.0F, 0.0F, 0.0F);
		BoltVec tip = new BoltVec(0.0F, 0.0F, forwardSign * length);
		renderJaggedBolt(poseStack, buffer, basis, root, tip, radius * 0.18F, 6, seed + 401.0F, firstPerson ? 0.019F : 0.013F, WHITE_CORE, ELECTRIC_BLUE);
		for (int i = 0; i < 13; i++) {
			float t = Mth.lerp(random(seed, i, 47), 0.12F, 0.98F);
			BoltVec start = new BoltVec(randomSigned(seed, i, 41) * radius * 0.14F, randomSigned(seed, i, 42) * radius * 0.12F, forwardSign * length * t);
			BoltVec end = new BoltVec(randomSigned(seed, i, 44) * radius * 0.22F, randomSigned(seed, i, 45) * radius * 0.2F, forwardSign * (length * t + randomSigned(seed, i, 46) * radius * 0.45F));
			renderJaggedBolt(poseStack, buffer, basis, start, end, radius * 0.24F, 4, seed + i * 43.0F, firstPerson ? 0.013F : 0.009F, PALE_BLUE, ELECTRIC_BLUE);
			if (i % 3 == 0) {
				BoltVec wrapEnd = new BoltVec(-end.x * 0.75F, end.y * 0.75F, end.z + forwardSign * radius * 0.08F);
				renderJaggedBolt(poseStack, buffer, basis, start, wrapEnd, radius * 0.18F, 3, seed + i * 57.0F, firstPerson ? 0.009F : 0.006F, WHITE_CORE, DEEP_BLUE);
			}
		}
	}

	private static void renderJaggedBolt(PoseStack poseStack, VertexConsumer buffer, Basis basis, BoltVec start, BoltVec end, float chaos, int segments, float seed, float width, int innerColor, int outerColor) {
		BoltVec previous = start;
		BoltVec tangent = end.subtract(start).normalize();
		BoltVec normalA = perpendicular(tangent);
		BoltVec normalB = tangent.cross(normalA).normalize();
		for (int i = 1; i <= segments; i++) {
			float t = i / (float) segments;
			BoltVec point = start.lerp(end, t);
			if (i < segments) {
				float taper = 1.0F - Math.abs(t - 0.5F) * 0.55F;
				point = point.add(normalA.scale(randomSigned(seed, i, 5) * chaos * taper)).add(normalB.scale(randomSigned(seed, i, 6) * chaos * taper)).add(tangent.scale(randomSigned(seed, i, 7) * chaos * 0.2F));
			}
			float outerAlpha = ((outerColor >>> 24) & 0xFF) / 255.0F;
			renderSegment(poseStack, buffer, basis, previous, point, width * 4.2F, withAlpha(BLUE_SHADOW, outerAlpha));
			renderSegment(poseStack, buffer, basis, previous, point, width * 2.4F, outerColor);
			renderSegment(poseStack, buffer, basis, previous, point, width, innerColor);
			previous = point;
		}
	}

	private static void renderSegment(PoseStack poseStack, VertexConsumer buffer, Basis basis, BoltVec start, BoltVec end, float width, int color) {
		BoltVec direction = end.subtract(start);
		if (direction.lengthSqr() < 0.00001F || width <= 0.0F) {
			return;
		}
		BoltVec tangent = direction.normalize();
		BoltVec normalA = perpendicular(tangent).scale(width * 0.5F);
		BoltVec normalB = tangent.cross(normalA).normalize().scale(width * 0.5F);
		renderRibbon(poseStack, buffer, basis, start, end, normalA, color);
		renderRibbon(poseStack, buffer, basis, start, end, normalB, color);
	}

	private static void renderRibbon(PoseStack poseStack, VertexConsumer buffer, Basis basis, BoltVec start, BoltVec end, BoltVec normal, int color) {
		Matrix4f matrix = poseStack.last().pose();
		vertex(buffer, matrix, basis, start.add(normal), color);
		vertex(buffer, matrix, basis, start.subtract(normal), color);
		vertex(buffer, matrix, basis, end.subtract(normal), color);
		vertex(buffer, matrix, basis, end.add(normal), color);
	}

	private static void vertex(VertexConsumer buffer, Matrix4f matrix, Basis basis, BoltVec point, int color) {
		BoltVec transformed = basis.transform(point);
		buffer.addVertex(matrix, transformed.x, transformed.y, transformed.z).setColor(color);
	}

	private static BoltVec perpendicular(BoltVec direction) {
		BoltVec axis = Math.abs(direction.y) < 0.8F ? new BoltVec(0.0F, 1.0F, 0.0F) : new BoltVec(1.0F, 0.0F, 0.0F);
		BoltVec normal = direction.cross(axis);
		if (normal.lengthSqr() < 0.00001F) {
			normal = direction.cross(new BoltVec(0.0F, 0.0F, 1.0F));
		}
		return normal.normalize();
	}

	private static BoltVec randomDirection(float seed, int index, int salt) {
		float z = randomSigned(seed, index, salt);
		float angle = random(seed, index, salt + 1) * Mth.TWO_PI;
		float ring = Mth.sqrt(Math.max(0.0F, 1.0F - z * z));
		return new BoltVec(Mth.cos(angle) * ring, z, Mth.sin(angle) * ring).normalize();
	}

	private static int withAlpha(int color, float alphaScale) {
		int alpha = Mth.clamp((int) (((color >>> 24) & 0xFF) * Mth.clamp(alphaScale, 0.0F, 1.0F)), 0, 255);
		return (alpha << 24) | (color & 0xFFFFFF);
	}

	private static float random(float seed, int index, int salt) {
		float value = Mth.sin(seed * 12.9898F + index * 78.233F + salt * 37.719F) * 43758.5453F;
		return value - Mth.floor(value);
	}

	private static float randomSigned(float seed, int index, int salt) {
		return random(seed, index, salt) * 2.0F - 1.0F;
	}

	private static final class ClientState {
		private int ticks;
		private final float chargePercent;

		private ClientState(int ticks, float chargePercent) {
			this.ticks = ticks;
			this.chargePercent = chargePercent;
		}
	}

	private static final class HitBurst {
		private final int entityId;
		private final Vec3 fallbackPosition;
		private final float fallbackXRot;
		private final float fallbackYRot;
		private int ticks;
		private final int totalTicks;
		private final float chargePercent;
		private final float scale;
		private final int seed;

		private HitBurst(int entityId, Vec3 fallbackPosition, float fallbackXRot, float fallbackYRot, int ticks, int totalTicks, float chargePercent, float scale, int seed) {
			this.entityId = entityId;
			this.fallbackPosition = fallbackPosition;
			this.fallbackXRot = fallbackXRot;
			this.fallbackYRot = fallbackYRot;
			this.ticks = ticks;
			this.totalTicks = totalTicks;
			this.chargePercent = chargePercent;
			this.scale = scale;
			this.seed = seed;
		}

		private Vec3 position(Entity entity) {
			if (entity != null) {
				return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
			}
			return fallbackPosition;
		}

		private Basis basis(Entity entity) {
			if (entity != null) {
				return basisFromRotation(entity.getXRot(), entity.getYRot());
			}
			return basisFromRotation(fallbackXRot, fallbackYRot);
		}
	}

	private record Basis(BoltVec right, BoltVec up, BoltVec forward) {
		private BoltVec transform(BoltVec point) {
			return right.scale(point.x).add(up.scale(point.y)).add(forward.scale(point.z));
		}
	}

	private record BoltVec(float x, float y, float z) {
		private BoltVec add(BoltVec other) {
			return new BoltVec(this.x + other.x, this.y + other.y, this.z + other.z);
		}

		private BoltVec subtract(BoltVec other) {
			return new BoltVec(this.x - other.x, this.y - other.y, this.z - other.z);
		}

		private BoltVec scale(float value) {
			return new BoltVec(this.x * value, this.y * value, this.z * value);
		}

		private BoltVec lerp(BoltVec other, float t) {
			return new BoltVec(Mth.lerp(t, this.x, other.x), Mth.lerp(t, this.y, other.y), Mth.lerp(t, this.z, other.z));
		}

		private BoltVec cross(BoltVec other) {
			return new BoltVec(this.y * other.z - this.z * other.y, this.z * other.x - this.x * other.z, this.x * other.y - this.y * other.x);
		}

		private float lengthSqr() {
			return this.x * this.x + this.y * this.y + this.z * this.z;
		}

		private BoltVec normalize() {
			float length = Mth.sqrt(lengthSqr());
			return length < 0.0001F ? new BoltVec(0.0F, 1.0F, 0.0F) : new BoltVec(this.x / length, this.y / length, this.z / length);
		}
	}
}
