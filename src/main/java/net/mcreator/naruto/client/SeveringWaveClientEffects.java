package net.mcreator.naruto.client;

import org.joml.Matrix4f;

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
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;

@EventBusSubscriber(value = Dist.CLIENT)
public class SeveringWaveClientEffects {
	private static final Map<UUID, BeamState> ACTIVE = new HashMap<>();

	public static void setActive(UUID casterId, boolean active, Vec3 origin, Vec3 direction, float range, float thickness, int ticks) {
		if (casterId == null) {
			return;
		}
		if (!active) {
			ACTIVE.remove(casterId);
			return;
		}
		if (direction == null || direction.lengthSqr() < 0.001D || origin == null) {
			return;
		}
		BeamState beam = ACTIVE.computeIfAbsent(casterId, id -> new BeamState());
		beam.casterId = casterId;
		Vec3 normalizedDirection = direction.normalize();
		if (!beam.initialized) {
			beam.origin = origin;
			beam.prevOrigin = origin;
			beam.targetOrigin = origin;
			beam.direction = normalizedDirection;
			beam.prevDirection = normalizedDirection;
			beam.targetDirection = normalizedDirection;
			beam.range = Math.max(0.1F, range);
			beam.prevRange = beam.range;
			beam.targetRange = beam.range;
			beam.thickness = Math.max(0.05F, thickness);
			beam.prevThickness = beam.thickness;
			beam.targetThickness = beam.thickness;
			beam.initialized = true;
		} else {
			beam.prevOrigin = origin;
			beam.origin = origin;
			beam.targetOrigin = origin;
			beam.prevDirection = normalizedDirection;
			beam.direction = normalizedDirection;
			beam.targetDirection = normalizedDirection;
			beam.prevRange = Math.max(0.1F, range);
			beam.range = beam.prevRange;
			beam.targetRange = beam.range;
			beam.prevThickness = Math.max(0.05F, thickness);
			beam.thickness = beam.prevThickness;
			beam.targetThickness = beam.thickness;
		}
		beam.ticksLeft = Math.max(4, ticks + 2);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<Map.Entry<UUID, BeamState>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			BeamState beam = iterator.next().getValue();
			beam.prevOrigin = beam.origin;
			beam.prevDirection = beam.direction;
			beam.prevRange = beam.range;
			beam.prevThickness = beam.thickness;
			beam.origin = beam.targetOrigin;
			beam.direction = beam.targetDirection;
			beam.range = beam.targetRange;
			beam.thickness = beam.targetThickness;
			beam.age++;
			beam.ticksLeft--;
			if (beam.ticksLeft <= 0) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void renderSeveringWaves(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE.isEmpty()) {
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
		NarutoWheelShaders.prepareSeveringWaveShader(time * 0.07F);
		for (BeamState beam : ACTIVE.values()) {
			renderBeam(poseStack, camera, beam, time, partialTick);
		}
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableBlend();
	}

	private static void renderBeam(PoseStack poseStack, Vec3 camera, BeamState beam, float time, float partialTick) {
		Vec3 renderOrigin = lerpVec(beam.prevOrigin, beam.origin, partialTick);
		Vec3 renderDirection = lerpVec(beam.prevDirection, beam.direction, partialTick);
		if (renderDirection.lengthSqr() < 0.001D) {
			return;
		}
		renderDirection = renderDirection.normalize();
		float renderRange = Mth.lerp(partialTick, beam.prevRange, beam.range);
		float renderThickness = Mth.lerp(partialTick, beam.prevThickness, beam.thickness);
		Vec3 direction = renderDirection;
		Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(direction);
		if (right.lengthSqr() < 0.001D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		}
		right = right.normalize();
		Vec3 up = direction.cross(right).normalize();
		Minecraft minecraft = Minecraft.getInstance();
		boolean firstPersonSelf = minecraft.options.getCameraType().isFirstPerson() && minecraft.player != null && minecraft.player.getUUID().equals(beam.casterId);
		if (firstPersonSelf) {
			renderOrigin = renderOrigin.add(direction.scale(1.15D));
			renderRange = Math.max(0.1F, renderRange - 1.15F);
			renderThickness *= 0.82F;
		}
		poseStack.pushPose();
		poseStack.translate(renderOrigin.x - camera.x, renderOrigin.y - camera.y, renderOrigin.z - camera.z);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		renderSquareCore(buffer, matrix, direction, right, up, beam, time, renderRange, renderThickness);
		renderInnerFlowShell(buffer, matrix, direction, right, up, beam, time, renderRange, renderThickness);
		renderOuterMist(buffer, matrix, direction, right, up, beam, time, renderRange, renderThickness);
		renderEdgeRibbons(buffer, matrix, direction, right, up, beam, time, renderRange, renderThickness);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		poseStack.popPose();
	}

	private static void renderSquareCore(BufferBuilder buffer, Matrix4f matrix, Vec3 direction, Vec3 right, Vec3 up, BeamState beam, float time, float renderRange, float renderThickness) {
		int segments = 24;
		int faceColorA = 0x9A63B7D6;
		int faceColorB = 0x8A4D9FBD;
		int faceColorC = 0x78558EA8;
		for (int i = 0; i < segments; i++) {
			float t0 = i / (float) segments;
			float t1 = (i + 1) / (float) segments;
			float taper0 = Mth.lerp(t0, 1.0F, 1.25F);
			float taper1 = Mth.lerp(t1, 1.0F, 1.25F);
			Vec3 c0 = direction.scale(renderRange * t0);
			Vec3 c1 = direction.scale(renderRange * t1);
			float pulse0 = Mth.sin(time * 0.28F + t0 * 9.0F + beam.age * 0.05F) * renderThickness * 0.015F;
			float pulse1 = Mth.sin(time * 0.28F + t1 * 9.0F + beam.age * 0.05F) * renderThickness * 0.015F;
			float half0 = renderThickness * 0.23F * taper0 + pulse0;
			float half1 = renderThickness * 0.23F * taper1 + pulse1;
			Vec3 r0 = right.scale(half0);
			Vec3 u0 = up.scale(half0);
			Vec3 r1 = right.scale(half1);
			Vec3 u1 = up.scale(half1);
			Vec3 p0_tr = c0.add(r0).add(u0);
			Vec3 p0_tl = c0.subtract(r0).add(u0);
			Vec3 p0_bl = c0.subtract(r0).subtract(u0);
			Vec3 p0_br = c0.add(r0).subtract(u0);
			Vec3 p1_tr = c1.add(r1).add(u1);
			Vec3 p1_tl = c1.subtract(r1).add(u1);
			Vec3 p1_bl = c1.subtract(r1).subtract(u1);
			Vec3 p1_br = c1.add(r1).subtract(u1);
			addQuad(buffer, matrix, p0_tr, p0_tl, p1_tl, p1_tr, faceColorA); // top
			addQuad(buffer, matrix, p0_br, p0_bl, p1_bl, p1_br, faceColorB); // bottom
			addQuad(buffer, matrix, p0_tl, p0_bl, p1_bl, p1_tl, faceColorC); // left
			addQuad(buffer, matrix, p0_br, p0_tr, p1_tr, p1_br, faceColorC); // right
		}
	}

	private static void renderInnerFlowShell(BufferBuilder buffer, Matrix4f matrix, Vec3 direction, Vec3 right, Vec3 up, BeamState beam, float time, float renderRange, float renderThickness) {
		int segments = 26;
		int flowColor = 0x6268BCD6;
		for (int face = 0; face < 4; face++) {
			for (int i = 0; i < segments; i++) {
				float t0 = i / (float) segments;
				float t1 = (i + 1) / (float) segments;
				float taper0 = Mth.lerp(t0, 1.0F, 1.25F);
				float taper1 = Mth.lerp(t1, 1.0F, 1.25F);
				Vec3 c0 = direction.scale(renderRange * t0);
				Vec3 c1 = direction.scale(renderRange * t1);
				float baseHalf0 = renderThickness * 0.23F * taper0;
				float baseHalf1 = renderThickness * 0.23F * taper1;
				float wave0 = innerWaveAmount(time, beam, t0);
				float wave1 = innerWaveAmount(time, beam, t1);
				Vec3 outward0;
				Vec3 tangent0a;
				Vec3 outward1;
				Vec3 tangent1a;
				if (face == 0) {
					outward0 = up;
					tangent0a = right;
					outward1 = up;
					tangent1a = right;
				} else if (face == 1) {
					outward0 = up.scale(-1.0D);
					tangent0a = right;
					outward1 = up.scale(-1.0D);
					tangent1a = right;
				} else if (face == 2) {
					outward0 = right.scale(-1.0D);
					tangent0a = up;
					outward1 = right.scale(-1.0D);
					tangent1a = up;
				} else {
					outward0 = right;
					tangent0a = up;
					outward1 = right;
					tangent1a = up;
				}
				float shellInset0 = baseHalf0 * 0.92F;
				float shellInset1 = baseHalf1 * 0.92F;
				float shellWidth0 = baseHalf0 * 0.7F;
				float shellWidth1 = baseHalf1 * 0.7F;
				float push0 = wave0 * renderThickness * 0.16F;
				float push1 = wave1 * renderThickness * 0.16F;
				Vec3 center0 = c0.add(outward0.scale(shellInset0 + push0));
				Vec3 center1 = c1.add(outward1.scale(shellInset1 + push1));
				Vec3 side0 = tangent0a.scale(shellWidth0 * 0.5F);
				Vec3 side1 = tangent1a.scale(shellWidth1 * 0.5F);
				Vec3 a0 = center0.add(side0);
				Vec3 b0 = center0.subtract(side0);
				Vec3 a1 = center1.add(side1);
				Vec3 b1 = center1.subtract(side1);
				addQuad(buffer, matrix, a0, b0, b1, a1, flowColor);
			}
		}
	}

	private static float innerWaveAmount(float time, BeamState beam, float t) {
		float waveA = Mth.sin(time * 0.75F + t * 18.0F + beam.age * 0.08F);
		float waveB = Mth.sin(time * 0.46F + t * 9.0F + 1.7F + beam.age * 0.05F);
		float combined = waveA * 0.65F + waveB * 0.35F;
		combined = (combined + 1.0F) * 0.5F;
		combined = combined * combined;
		return combined;
	}

	private static void addQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
		addVertex(buffer, matrix, a, color);
		addVertex(buffer, matrix, b, color);
		addVertex(buffer, matrix, c, color);
		addVertex(buffer, matrix, d, color);
	}

	private static void renderOuterMist(BufferBuilder buffer, Matrix4f matrix, Vec3 direction, Vec3 right, Vec3 up, BeamState beam, float time, float renderRange, float renderThickness) {
		int segments = 18;
		int color = 0x343A8BA3;
		for (int i = 0; i < segments; i++) {
			float t0 = i / (float) segments;
			float t1 = (i + 1) / (float) segments;
			float taper0 = Mth.lerp(t0, 1.0F, 1.25F);
			float taper1 = Mth.lerp(t1, 1.0F, 1.25F);
			Vec3 c0 = direction.scale(renderRange * t0).add(up.scale(Mth.sin(time * 0.24F + t0 * 8.0F) * renderThickness * 0.06F));
			Vec3 c1 = direction.scale(renderRange * t1).add(up.scale(Mth.sin(time * 0.24F + t1 * 8.0F) * renderThickness * 0.06F));
			float halfWidth0 = renderThickness * 0.72F * taper0;
			float halfWidth1 = renderThickness * 0.72F * taper1;
			Vec3 side0 = right.scale(halfWidth0);
			Vec3 side1 = right.scale(halfWidth1);
			addVertex(buffer, matrix, c0.add(side0), color);
			addVertex(buffer, matrix, c0.subtract(side0), color);
			addVertex(buffer, matrix, c1.subtract(side1), color);
			addVertex(buffer, matrix, c1.add(side1), color);
		}
	}

	private static void renderEdgeRibbons(BufferBuilder buffer, Matrix4f matrix, Vec3 direction, Vec3 right, Vec3 up, BeamState beam, float time, float renderRange, float renderThickness) {
		int segments = 24;
		for (int ribbon = 0; ribbon < 2; ribbon++) {
			float sign = ribbon == 0 ? 1.0F : -1.0F;
			int color = ribbon == 0 ? 0x76589FB8 : 0x5E9AB9C7;
			for (int i = 0; i < segments; i++) {
				float t0 = i / (float) segments;
				float t1 = (i + 1) / (float) segments;
				Vec3 p0 = ribbonPoint(direction, right, up, beam, time, t0, sign, renderRange, renderThickness);
				Vec3 p1 = ribbonPoint(direction, right, up, beam, time, t1, sign, renderRange, renderThickness);
				float width = renderThickness * 0.08F;
				Vec3 side = up.scale(width);
				addVertex(buffer, matrix, p0.add(side), color);
				addVertex(buffer, matrix, p0.subtract(side), color);
				addVertex(buffer, matrix, p1.subtract(side), color);
				addVertex(buffer, matrix, p1.add(side), color);
			}
		}
	}

	private static Vec3 ribbonPoint(Vec3 direction, Vec3 right, Vec3 up, BeamState beam, float time, float t, float sign, float renderRange, float renderThickness) {
		float taper = Mth.lerp(t, 1.0F, 1.25F);
		float spin = time * 0.45F + t * 10.0F + beam.age * 0.035F;
		double side = sign * renderThickness * 0.48F * taper;
		double vertical = Mth.sin(spin) * renderThickness * 0.11F;
		return direction.scale(renderRange * t).add(right.scale(side)).add(up.scale(vertical));
	}

	private static Vec3 lerpVec(Vec3 from, Vec3 to, double amount) {
		if (from == null) {
			return to == null ? Vec3.ZERO : to;
		}
		if (to == null) {
			return from;
		}
		return from.add(to.subtract(from).scale(amount));
	}

	private static void addVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 pos, int color) {
		buffer.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).setColor(color);
	}

	private static final class BeamState {
		private UUID casterId;
		private Vec3 origin = Vec3.ZERO;
		private Vec3 prevOrigin = Vec3.ZERO;
		private Vec3 targetOrigin = Vec3.ZERO;
		private Vec3 direction = new Vec3(0.0D, 0.0D, 1.0D);
		private Vec3 prevDirection = new Vec3(0.0D, 0.0D, 1.0D);
		private Vec3 targetDirection = new Vec3(0.0D, 0.0D, 1.0D);
		private float range = 1.0F;
		private float prevRange = 1.0F;
		private float targetRange = 1.0F;
		private float thickness = 0.4F;
		private float prevThickness = 0.4F;
		private float targetThickness = 0.4F;
		private int ticksLeft = 0;
		private int age = 0;
		private boolean initialized = false;
	}
}
