package net.mcreator.naruto.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.client.gui.NarutoWheelShaders;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

@EventBusSubscriber(value = Dist.CLIENT)
public class WindBreakthroughClientEffects {
	private static final List<WindBurst> BURSTS = new ArrayList<>();
	private static final double TAU = Math.PI * 2.0D;

	public static void addBurst(Vec3 origin, Vec3 direction, float range, float power, int ticks) {
		if (origin == null || direction == null || direction.lengthSqr() < 0.001D) {
			return;
		}
		BURSTS.add(new WindBurst(origin, direction.normalize(), Math.max(1.0F, range), Mth.clamp(power, 0.0F, 1.0F), Math.max(4, ticks)));
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<WindBurst> iterator = BURSTS.iterator();
		while (iterator.hasNext()) {
			WindBurst burst = iterator.next();
			burst.ticks--;
			if (burst.ticks <= 0) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void renderWindBursts(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BURSTS.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}
		Vec3 camera = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		for (WindBurst burst : BURSTS) {
			renderBurst(poseStack, camera, burst, partialTick);
		}
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableBlend();
	}

	private static void renderBurst(PoseStack poseStack, Vec3 camera, WindBurst burst, float partialTick) {
		float age = burst.totalTicks - burst.ticks + partialTick;
		float life = Mth.clamp(age / burst.totalTicks, 0.0F, 1.0F);
		float fade = 1.0F - life;
		Basis basis = Basis.fromForward(burst.direction);
		poseStack.pushPose();
		poseStack.translate(burst.origin.x - camera.x, burst.origin.y - camera.y, burst.origin.z - camera.z);
		NarutoWheelShaders.prepareWindGustShader((burst.seed + age) * 0.08F);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		int ribbons = 4 + Mth.floor(burst.power * 3.0F);
		for (int i = 0; i < ribbons; i++) {
			renderRibbon(buffer, matrix, basis, burst, age, fade, i, ribbons);
		}
		renderShockRings(buffer, matrix, basis, burst, life, fade);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		poseStack.popPose();
	}

	private static void renderRibbon(BufferBuilder buffer, Matrix4f matrix, Basis basis, WindBurst burst, float age, float fade, int ribbon, int ribbons) {
		int segments = 16;
		float phase = (float) (TAU * ribbon / ribbons + burst.seed * 0.13D);
		float width = 0.12F + burst.power * 0.08F;
		for (int i = 0; i < segments; i++) {
			float t0 = i / (float) segments;
			float t1 = (i + 1) / (float) segments;
			Vec3 a = ribbonPoint(basis, burst, age, phase, t0);
			Vec3 b = ribbonPoint(basis, burst, age, phase, t1);
			Vec3 side0 = ribbonSide(basis, phase, t0, width);
			Vec3 side1 = ribbonSide(basis, phase, t1, width);
			int alpha = Mth.floor(150.0F * fade * (1.0F - t0 * 0.72F));
			int color = (Mth.clamp(alpha, 0, 150) << 24) | 0xDFFFF1;
			addVertex(buffer, matrix, a.add(side0), color);
			addVertex(buffer, matrix, a.subtract(side0), color);
			addVertex(buffer, matrix, b.subtract(side1), color);
			addVertex(buffer, matrix, b.add(side1), color);
		}
	}

	private static Vec3 ribbonPoint(Basis basis, WindBurst burst, float age, float phase, float t) {
		float distance = burst.range * t;
		float radius = (0.22F + burst.power * 0.25F) + t * (1.15F + burst.power * 0.75F);
		float angle = phase + t * 6.4F + age * (0.42F + burst.power * 0.28F);
		float wobble = 0.12F * Mth.sin(age * 0.9F + t * 9.0F + phase);
		return basis.forward.scale(distance).add(basis.right.scale(Math.cos(angle) * (radius + wobble))).add(basis.up.scale(Math.sin(angle) * radius));
	}

	private static Vec3 ribbonSide(Basis basis, float phase, float t, float width) {
		float angle = phase + t * 6.4F + (float) Math.PI * 0.5F;
		return basis.right.scale(Math.cos(angle) * width).add(basis.up.scale(Math.sin(angle) * width));
	}

	private static void renderShockRings(BufferBuilder buffer, Matrix4f matrix, Basis basis, WindBurst burst, float life, float fade) {
		for (int ring = 0; ring < 3; ring++) {
			float t = Mth.clamp(life * 1.25F - ring * 0.18F, 0.0F, 1.0F);
			if (t <= 0.0F || t >= 1.0F) {
				continue;
			}
			float distance = burst.range * t;
			float radius = 0.45F + t * (1.8F + burst.power * 1.3F);
			float width = 0.045F + burst.power * 0.025F;
			int alpha = Mth.floor(105.0F * fade * (1.0F - ring * 0.2F));
			int color = (Mth.clamp(alpha, 0, 120) << 24) | 0xEEFFF8;
			renderRing(buffer, matrix, basis, distance, radius, width, color);
		}
	}

	private static void renderRing(BufferBuilder buffer, Matrix4f matrix, Basis basis, float distance, float radius, float width, int color) {
		int segments = 32;
		Vec3 center = basis.forward.scale(distance);
		for (int i = 0; i < segments; i++) {
			float a0 = Mth.TWO_PI * i / segments;
			float a1 = Mth.TWO_PI * (i + 1) / segments;
			Vec3 outer0 = center.add(radial(basis, a0, radius + width));
			Vec3 inner0 = center.add(radial(basis, a0, radius - width));
			Vec3 inner1 = center.add(radial(basis, a1, radius - width));
			Vec3 outer1 = center.add(radial(basis, a1, radius + width));
			addVertex(buffer, matrix, outer0, color);
			addVertex(buffer, matrix, inner0, color);
			addVertex(buffer, matrix, inner1, color);
			addVertex(buffer, matrix, outer1, color);
		}
	}

	private static Vec3 radial(Basis basis, float angle, float radius) {
		return basis.right.scale(Mth.cos(angle) * radius).add(basis.up.scale(Mth.sin(angle) * radius));
	}

	private static void addVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 pos, int color) {
		buffer.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).setColor(color);
	}

	private static final class WindBurst {
		private final Vec3 origin;
		private final Vec3 direction;
		private final float range;
		private final float power;
		private final int totalTicks;
		private final int seed;
		private int ticks;

		private WindBurst(Vec3 origin, Vec3 direction, float range, float power, int ticks) {
			this.origin = origin;
			this.direction = direction;
			this.range = range;
			this.power = power;
			this.ticks = ticks;
			this.totalTicks = ticks;
			this.seed = Math.abs(origin.hashCode() ^ direction.hashCode()) % 10000;
		}
	}

	private record Basis(Vec3 right, Vec3 up, Vec3 forward) {
		private static Basis fromForward(Vec3 forward) {
			Vec3 normalizedForward = forward.normalize();
			Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(normalizedForward);
			if (right.lengthSqr() < 0.001D) {
				right = new Vec3(1.0D, 0.0D, 0.0D);
			}
			right = right.normalize();
			Vec3 up = normalizedForward.cross(right).normalize();
			return new Basis(right, up, normalizedForward);
		}
	}
}
