package net.mcreator.naruto.client;

import org.joml.Matrix4f;

import org.checkerframework.checker.units.qual.t;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;

import net.mcreator.naruto.client.gui.NarutoWheelShaders;

import java.util.UUID;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;

@EventBusSubscriber(value = Dist.CLIENT)
public class FalseDarknessClientEffects {
	private static final List<BeamState> BEAMS = new ArrayList<>();

	public static void addBeam(UUID casterId, Vec3 origin, Vec3 direction, float range, float thickness, float chargePercent, boolean mainSpear, int spearIndex, int ticks) {
		if (origin == null || direction == null || direction.lengthSqr() < 0.001D) {
			return;
		}
		BeamState beam = new BeamState();
		beam.casterId = casterId;
		beam.origin = origin;
		beam.direction = direction.normalize();
		beam.range = Math.max(0.1F, range);
		beam.thickness = Math.max(0.04F, thickness);
		beam.chargePercent = chargePercent;
		beam.mainSpear = mainSpear;
		beam.spearIndex = spearIndex;
		beam.seed = spearIndex * 91.0F + (float) (origin.x * 0.37D + origin.y * 0.73D + origin.z * 0.19D);
		beam.maxAge = Math.max(3, ticks);
		beam.age = 0;
		BEAMS.add(beam);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<BeamState> iterator = BEAMS.iterator();
		while (iterator.hasNext()) {
			BeamState beam = iterator.next();
			beam.age++;
			if (beam.age > beam.maxAge) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void renderFalseDarkness(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BEAMS.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}
		Vec3 camera = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		float time = minecraft.level.getGameTime() + partialTick;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		NarutoWheelShaders.prepareFalseDarknessShader(time * 0.11F);
		for (BeamState beam : BEAMS) {
			renderBeam(poseStack, camera, beam, time, partialTick);
		}
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableBlend();
	}

	private static void renderBeam(PoseStack poseStack, Vec3 camera, BeamState beam, float time, float partialTick) {
		float life = Mth.clamp((beam.age + partialTick) / Math.max(1.0F, beam.maxAge), 0.0F, 1.0F);
		float travelProgress = easeOutCubic(Mth.clamp(life / 0.55F, 0.0F, 1.0F));
		float fade = 1.0F - Mth.clamp((life - 0.45F) / 0.55F, 0.0F, 1.0F);
		Vec3 origin = beam.origin;
		Vec3 direction = beam.direction;
		float headDistance = beam.range * travelProgress;
		float trailLength = beam.range * Mth.lerp(beam.chargePercent, 0.58F, 0.98F);
		float tailDistance = Math.max(0.0F, headDistance - trailLength);
		float visibleRange = Math.max(0.05F, headDistance - tailDistance);
		Vec3 renderOrigin = origin.add(direction.scale(tailDistance));
		if (visibleRange <= 0.05F) {
			return;
		}
		Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(direction);
		if (right.lengthSqr() < 0.001D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		}
		right = right.normalize();
		Vec3 up = direction.cross(right).normalize();
		Minecraft minecraft = Minecraft.getInstance();
		boolean firstPersonSelf = minecraft.options.getCameraType().isFirstPerson() && minecraft.player != null && minecraft.player.getUUID().equals(beam.casterId);
		if (firstPersonSelf) {
			renderOrigin = renderOrigin.add(direction.scale(0.85D));
			visibleRange = Math.max(0.1F, visibleRange - 0.85F);
		}
		poseStack.pushPose();
		poseStack.translate(renderOrigin.x - camera.x, renderOrigin.y - camera.y, renderOrigin.z - camera.z);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		renderLightningCore(buffer, matrix, direction, right, up, beam, time, tailDistance, headDistance, fade);
		renderOuterForks(buffer, matrix, direction, right, up, beam, time, tailDistance, headDistance, fade);
		renderSpearTip(buffer, matrix, direction, right, up, beam, visibleRange, fade);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		poseStack.popPose();
	}

	private static float easeOutCubic(float value) {
		value = Mth.clamp(value, 0.0F, 1.0F);
		float inverse = 1.0F - value;
		return 1.0F - inverse * inverse * inverse;
	}

	private static void renderLightningCore(BufferBuilder buffer, Matrix4f matrix, Vec3 direction, Vec3 right, Vec3 up, BeamState beam, float time, float tailDistance, float headDistance, float fade) {
		int segments = beam.mainSpear ? 13 : 8;
		int color = beam.mainSpear ? 0xFFFFFFFF : 0xEE006BFF;
		Vec3[] points = buildMovingLightningPath(direction, right, up, beam, time, tailDistance, headDistance, segments);
		for (int i = 0; i < points.length - 1; i++) {
			float t = i / (float) Math.max(1, points.length - 2);
			float taper = Mth.lerp(t, 1.0F, 0.55F);
			float width = beam.thickness * 0.58F * taper;
			Vec3 a = points[i];
			Vec3 b = points[i + 1];
			renderBoltSegment(buffer, matrix, a, b, right, up, width, alphaColor(color, fade));
			renderBoltSegment(buffer, matrix, a, b, right, up, width * 0.42F, alphaColor(0xF4FFFFFF, fade));
		}
		renderJointFlashes(buffer, matrix, points, beam, fade);
	}

	private static void renderBoltSegment(BufferBuilder buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 fallbackRight, Vec3 fallbackUp, float width, int color) {
		Vec3 dir = b.subtract(a);
		if (dir.lengthSqr() < 0.001D) {
			return;
		}
		dir = dir.normalize();
		Vec3 side = dir.cross(fallbackUp);
		if (side.lengthSqr() < 0.001D) {
			side = fallbackRight;
		}
		side = side.normalize();
		Vec3 vertical = dir.cross(side);
		if (vertical.lengthSqr() < 0.001D) {
			vertical = fallbackUp;
		}
		vertical = vertical.normalize();
		Vec3 sideOffset = side.scale(width);
		Vec3 verticalOffset = vertical.scale(width * 0.55F);
		addQuad(buffer, matrix, a.add(sideOffset), a.subtract(sideOffset), b.subtract(sideOffset), b.add(sideOffset), color);
		addQuad(buffer, matrix, a.add(verticalOffset), a.subtract(verticalOffset), b.subtract(verticalOffset), b.add(verticalOffset), color);
	}

	private static void renderJointFlashes(BufferBuilder buffer, Matrix4f matrix, Vec3[] points, BeamState beam, float fade) {
		int color = alphaColor(0xE8FFFFFF, fade * 0.9F);
		for (int i = 1; i < points.length - 1; i++) {
			Vec3 p = points[i];
			float size = beam.thickness * (beam.mainSpear ? 0.68F : 0.38F);
			Vec3 x = new Vec3(size, 0.0D, 0.0D);
			Vec3 y = new Vec3(0.0D, size * 0.55F, 0.0D);
			Vec3 z = new Vec3(0.0D, 0.0D, size);
			addQuad(buffer, matrix, p.add(x), p.add(y), p.subtract(x), p.subtract(y), color);
			addQuad(buffer, matrix, p.add(z), p.add(y), p.subtract(z), p.subtract(y), color);
		}
	}

	private static void renderOuterForks(BufferBuilder buffer, Matrix4f matrix, Vec3 direction, Vec3 right, Vec3 up, BeamState beam, float time, float tailDistance, float headDistance, float fade) {
		int forks = beam.mainSpear ? 5 : 2;
		int color = beam.mainSpear ? 0xC8BFEFFF : 0x96B8E8FF;
		float visibleLength = Math.max(0.1F, headDistance - tailDistance);
		for (int i = 0; i < forks; i++) {
			float seed = beam.seed + beam.spearIndex * 31.0F + i * 9.17F;
			float localStart = 0.16F + pseudo(seed) * 0.68F;
			float startDistance = tailDistance + visibleLength * localStart;
			if (startDistance >= headDistance) {
				continue;
			}
			float absoluteT = Mth.clamp(startDistance / Math.max(0.1F, beam.range), 0.0F, 1.0F);
			Vec3 branchStart = lightningPointFull(direction, right, up, beam, startDistance, absoluteT);
			float branchLength = beam.range * (0.045F + pseudo(seed + 2.0F) * 0.09F);
			float sideSign = pseudo(seed + 4.0F) > 0.5F ? 1.0F : -1.0F;
			float upSign = pseudo(seed + 6.0F) > 0.5F ? 1.0F : -1.0F;
			Vec3 branchDirection = direction.scale(0.35D).add(right.scale(sideSign * (0.75D + pseudo(seed + 8.0F) * 1.1D))).add(up.scale(upSign * (0.35D + pseudo(seed + 10.0F) * 0.8D)));
			if (branchDirection.lengthSqr() < 0.001D) {
				branchDirection = direction;
			}
			branchDirection = branchDirection.normalize();
			Vec3[] branchPoints = buildBranchLightningPath(branchStart, branchDirection, right, up, beam, time, branchLength, beam.mainSpear ? 4 : 3, beam.mainSpear ? 0.85F : 0.45F, seed);
			for (int p = 0; p < branchPoints.length - 1; p++) {
				float t = p / (float) Math.max(1, branchPoints.length - 2);
				float width = beam.thickness * Mth.lerp(t, 0.26F, 0.08F);
				renderBoltSegment(buffer, matrix, branchPoints[p], branchPoints[p + 1], right, up, width, alphaColor(color, fade * 0.75F));
			}
		}
	}

	private static void renderSpearTip(BufferBuilder buffer, Matrix4f matrix, Vec3 direction, Vec3 right, Vec3 up, BeamState beam, float range, float fade) {
		Vec3 tip = direction.scale(range);
		Vec3 back = direction.scale(Math.max(0.0F, range - beam.thickness * 3.2F));
		float width = beam.thickness * (beam.mainSpear ? 1.25F : 0.82F);
		int color = alphaColor(0xF5FFFFFF, fade);
		addQuad(buffer, matrix, tip, back.add(right.scale(width)), back.add(up.scale(width * 0.36F)), tip, color);
		addQuad(buffer, matrix, tip, back.subtract(right.scale(width)), back.subtract(up.scale(width * 0.36F)), tip, color);
		addQuad(buffer, matrix, tip, back.add(up.scale(width)), back.add(right.scale(width * 0.36F)), tip, color);
		addQuad(buffer, matrix, tip, back.subtract(up.scale(width)), back.subtract(right.scale(width * 0.36F)), tip, color);
	}

	private static void addBranch(BufferBuilder buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, Vec3 up, float width, int color) {
		Vec3 sideA = sideForSegment(a, b, up).scale(width);
		Vec3 sideB = sideForSegment(b, c, up).scale(width * 0.7F);
		addQuad(buffer, matrix, a.add(sideA), a.subtract(sideA), b.subtract(sideA), b.add(sideA), color);
		addQuad(buffer, matrix, b.add(sideB), b.subtract(sideB), c.subtract(sideB), c.add(sideB), color);
	}

	private static Vec3[] buildBranchLightningPath(Vec3 start, Vec3 direction, Vec3 fallbackRight, Vec3 fallbackUp, BeamState beam, float time, float length, int segments, float maxDeviation, float seedOffset) {
		int points = Math.max(3, segments + 1);
		Vec3[] path = new Vec3[points];
		Vec3 right = direction.cross(fallbackUp);
		if (right.lengthSqr() < 0.001D) {
			right = fallbackRight;
		}
		right = right.normalize();
		Vec3 up = direction.cross(right);
		if (up.lengthSqr() < 0.001D) {
			up = fallbackUp;
		}
		up = up.normalize();
		for (int i = 0; i < points; i++) {
			float progress = i / (float) (points - 1);
			Vec3 straight = start.add(direction.scale(length * progress));
			if (i == 0) {
				path[i] = start;
				continue;
			}
			float strength = Mth.sin(progress * Mth.PI);
			float timeCell = Mth.floor(time * 7.0F);
			float seed = seedOffset + i * 11.19F + beam.spearIndex * 23.0F + timeCell * 4.0F;
			float rand1 = pseudo(seed) * 2.0F - 1.0F;
			float rand2 = pseudo(seed + 14.3F) * 2.0F - 1.0F;
			Vec3 offset = right.scale(rand1 * beam.thickness * maxDeviation * strength).add(up.scale(rand2 * beam.thickness * maxDeviation * 0.75F * strength));
			path[i] = straight.add(offset);
		}
		return path;
	}

	private static Vec3[] buildMovingLightningPath(Vec3 direction, Vec3 right, Vec3 up, BeamState beam, float time, float tailDistance, float headDistance, int segments) {
		int points = Math.max(3, segments + 1);
		Vec3[] path = new Vec3[points];
		float visibleLength = Math.max(0.1F, headDistance - tailDistance);
		for (int i = 0; i < points; i++) {
			float localProgress = i / (float) (points - 1);
			float distance = tailDistance + visibleLength * localProgress;
			float absoluteT = Mth.clamp(distance / Math.max(0.1F, beam.range), 0.0F, 1.0F);
			path[i] = lightningPointFull(direction, right, up, beam, distance - tailDistance, absoluteT);
		}
		if (points > 0) {
			path[0] = Vec3.ZERO;
		}
		return path;
	}

	private static Vec3 lightningPointFull(Vec3 direction, Vec3 right, Vec3 up, BeamState beam, float localDistance, float absoluteT) {
		Vec3 straight = direction.scale(localDistance);
		if (absoluteT <= 0.0F || absoluteT >= 1.0F) {
			return straight;
		}
		float middleStrength = Mth.sin(absoluteT * Mth.PI);
		float startStabilizer = smoothstep01(0.015F, 0.08F, absoluteT);
		float tipStabilizer = 1.0F - smoothstep01(0.84F, 1.0F, absoluteT) * 0.95F;
		float strength = middleStrength * startStabilizer * tipStabilizer;
		int cells = beam.mainSpear ? 9 : 6;
		float cell = Mth.floor(absoluteT * cells);
		float seed = beam.seed + beam.spearIndex * 41.0F + cell * 18.37F;
		float rand1 = pseudo(seed) * 2.0F - 1.0F;
		float rand2 = pseudo(seed + 22.91F) * 2.0F - 1.0F;
		if (Math.abs(rand1) < 0.38F) {
			rand1 = rand1 < 0.0F ? -0.38F : 0.38F;
		}
		if (Math.abs(rand2) < 0.28F) {
			rand2 = rand2 < 0.0F ? -0.28F : 0.28F;
		}
		float sideAmount = beam.thickness * (beam.mainSpear ? 4.2F : 2.2F);
		float upAmount = beam.thickness * (beam.mainSpear ? 2.8F : 1.4F);
		return straight.add(right.scale(rand1 * sideAmount * strength)).add(up.scale(rand2 * upAmount * strength));
	}

	private static float smoothstep01(float edge0, float edge1, float value) {
		float x = Mth.clamp((value - edge0) / Math.max(0.0001F, edge1 - edge0), 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}

	private static Vec3 sideForSegment(Vec3 a, Vec3 b, Vec3 up) {
		Vec3 dir = b.subtract(a);
		if (dir.lengthSqr() < 0.001D) {
			return up;
		}
		Vec3 side = dir.normalize().cross(up);
		if (side.lengthSqr() < 0.001D) {
			return new Vec3(1.0D, 0.0D, 0.0D);
		}
		return side.normalize();
	}

	private static float jagAmount(float time, BeamState beam, float t) {
		float a = Mth.sin(time * 1.65F + t * 31.0F + beam.spearIndex * 4.7F);
		float b = Mth.sin(time * 0.95F + t * 17.0F + beam.spearIndex * 2.2F);
		return a * 0.65F + b * 0.35F;
	}

	private static float pseudo(float value) {
		return Mth.frac(Mth.sin(value * 12.9898F) * 43758.5453F);
	}

	private static int alphaColor(int argb, float alphaMultiplier) {
		int alpha = (argb >> 24) & 255;
		alpha = Mth.clamp(Mth.floor(alpha * alphaMultiplier), 0, 255);
		return (argb & 0x00FFFFFF) | (alpha << 24);
	}

	private static void addQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
		addVertex(buffer, matrix, a, color);
		addVertex(buffer, matrix, b, color);
		addVertex(buffer, matrix, c, color);
		addVertex(buffer, matrix, d, color);
	}

	private static void addVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 pos, int color) {
		buffer.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).setColor(color);
	}

	private static final class BeamState {
		private UUID casterId;
		private Vec3 origin = Vec3.ZERO;
		private Vec3 direction = new Vec3(0.0D, 0.0D, 1.0D);
		private float range = 1.0F;
		private float thickness = 0.2F;
		private float chargePercent;
		private boolean mainSpear;
		private int spearIndex;
		private float seed;
		private int age;
		private int maxAge = 8;
	}
}