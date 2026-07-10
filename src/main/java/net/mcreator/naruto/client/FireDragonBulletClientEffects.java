package net.mcreator.naruto.client;

import org.joml.Matrix4f;

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
import java.util.UUID;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

@EventBusSubscriber(value = Dist.CLIENT)
public class FireDragonBulletClientEffects {
	private static final List<DragonState> DRAGONS = new ArrayList<>();

	public static void addDragon(UUID casterId, Vec3 origin, Vec3 direction, float range, float radius, float chargePercent, int ticks) {
		if (origin == null || direction == null || direction.lengthSqr() < 0.001D) {
			return;
		}
		DragonState dragon = new DragonState();
		dragon.casterId = casterId;
		dragon.origin = origin;
		dragon.direction = direction.normalize();
		dragon.range = Math.max(0.1F, range);
		dragon.radius = Math.max(0.08F, radius);
		dragon.chargePercent = Mth.clamp(chargePercent, 0.0F, 1.0F);
		dragon.maxAge = Math.max(6, ticks);
		dragon.travelTicks = Math.max(6, ticks - 10);
		dragon.seed = (float) (origin.x * 0.61D + origin.y * 0.37D + origin.z * 0.79D);
		DRAGONS.add(dragon);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<DragonState> iterator = DRAGONS.iterator();
		while (iterator.hasNext()) {
			DragonState dragon = iterator.next();
			dragon.age++;
			if (dragon.age > dragon.maxAge) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void renderFireDragons(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || DRAGONS.isEmpty()) {
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
		NarutoWheelShaders.prepareFireDragonBulletShader(time * 0.09F);
		for (DragonState dragon : DRAGONS) {
			renderDragon(poseStack, camera, dragon, time, partialTick);
		}
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableBlend();
	}

	private static void renderDragon(PoseStack poseStack, Vec3 camera, DragonState dragon, float time, float partialTick) {
		float life = Mth.clamp((dragon.age + partialTick) / Math.max(1.0F, dragon.maxAge), 0.0F, 1.0F);
		float travel = Mth.clamp((dragon.age + partialTick) / Math.max(1.0F, dragon.travelTicks), 0.0F, 1.0F);
		float fade = 1.0F - smoothstep(0.74F, 1.0F, life);
		float headDistance = dragon.range * travel;
		float bodyLength = Math.min(dragon.range, dragon.radius * (5.2F + dragon.chargePercent * 3.0F));
		float tailDistance = Math.max(0.0F, headDistance - bodyLength);
		float visibleRange = Math.max(0.05F, headDistance - tailDistance);
		Vec3 direction = dragon.direction;
		Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(direction);
		if (right.lengthSqr() < 0.001D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		}
		right = right.normalize();
		Vec3 up = direction.cross(right).normalize();
		Vec3 renderOrigin = dragon.origin.add(direction.scale(tailDistance));
		Minecraft minecraft = Minecraft.getInstance();
		boolean firstPersonSelf = minecraft.options.getCameraType().isFirstPerson() && minecraft.player != null && minecraft.player.getUUID().equals(dragon.casterId);
		if (firstPersonSelf) {
			renderOrigin = renderOrigin.add(direction.scale(0.85D));
			visibleRange = Math.max(0.1F, visibleRange - 0.85F);
		}
		poseStack.pushPose();
		poseStack.translate(renderOrigin.x - camera.x, renderOrigin.y - camera.y, renderOrigin.z - camera.z);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		renderBody(buffer, matrix, direction, right, up, dragon, time, visibleRange, fade);
		renderCrest(buffer, matrix, direction, right, up, dragon, time, visibleRange, fade);
		renderHead(buffer, matrix, direction, right, up, dragon, time, visibleRange, fade);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		poseStack.popPose();
	}

	private static void renderBody(BufferBuilder buffer, Matrix4f matrix, Vec3 direction, Vec3 right, Vec3 up, DragonState dragon, float time, float range, float fade) {
		int segments = 28;
		for (int i = 0; i < segments; i++) {
			float t0 = i / (float) segments;
			float t1 = (i + 1) / (float) segments;
			Vec3 c0 = dragonPoint(direction, right, up, dragon, time, range, t0);
			Vec3 c1 = dragonPoint(direction, right, up, dragon, time, range, t1);
			float r0 = bodyRadius(dragon, t0) * fade;
			float r1 = bodyRadius(dragon, t1) * fade;
			int colorA = alphaColor(0xCFFF7B16, fade);
			int colorB = alphaColor(0x92FF2A04, fade * 0.86F);
			addRibbon(buffer, matrix, c0, c1, right, r0, r1, colorA);
			addRibbon(buffer, matrix, c0, c1, up, r0 * 0.72F, r1 * 0.72F, colorB);
			addRibbon(buffer, matrix, c0.add(right.scale(r0 * 0.34F)), c1.add(right.scale(r1 * 0.34F)), up, r0 * 0.42F, r1 * 0.42F, alphaColor(0x84FFE2A0, fade));
		}
	}

	private static void renderCrest(BufferBuilder buffer, Matrix4f matrix, Vec3 direction, Vec3 right, Vec3 up, DragonState dragon, float time, float range, float fade) {
		int segments = 18;
		for (int i = 0; i < segments; i++) {
			float t0 = i / (float) segments;
			float t1 = (i + 1) / (float) segments;
			Vec3 c0 = dragonPoint(direction, right, up, dragon, time, range, t0);
			Vec3 c1 = dragonPoint(direction, right, up, dragon, time, range, t1);
			float wave0 = Mth.sin(time * 0.5F + t0 * 18.0F + dragon.seed) * 0.5F + 0.5F;
			float wave1 = Mth.sin(time * 0.5F + t1 * 18.0F + dragon.seed) * 0.5F + 0.5F;
			float r0 = bodyRadius(dragon, t0);
			float r1 = bodyRadius(dragon, t1);
			Vec3 p0 = c0.add(up.scale(r0 * (0.75F + wave0 * 0.28F)));
			Vec3 p1 = c1.add(up.scale(r1 * (0.75F + wave1 * 0.28F)));
			addRibbon(buffer, matrix, p0, p1, right, r0 * 0.28F, r1 * 0.28F, alphaColor(0x88FFF2A8, fade * 0.9F));
		}
	}

	private static void renderHead(BufferBuilder buffer, Matrix4f matrix, Vec3 direction, Vec3 right, Vec3 up, DragonState dragon, float time, float range, float fade) {
		Vec3 head = dragonPoint(direction, right, up, dragon, time, range, 1.0F);
		float size = dragon.radius * (1.55F + dragon.chargePercent * 0.5F) * fade;
		int outer = alphaColor(0xD8FF5308, fade);
		int inner = alphaColor(0xECFFF0AF, fade);
		addRibbon(buffer, matrix, head.subtract(direction.scale(size * 1.2F)), head.add(direction.scale(size * 0.55F)), right, size, size * 0.38F, outer);
		addRibbon(buffer, matrix, head.subtract(direction.scale(size * 0.9F)), head.add(direction.scale(size * 0.4F)), up, size * 0.7F, size * 0.28F, inner);
		addHorn(buffer, matrix, head, direction, right, up, size, 1.0F, fade);
		addHorn(buffer, matrix, head, direction, right, up, size, -1.0F, fade);
	}

	private static void addHorn(BufferBuilder buffer, Matrix4f matrix, Vec3 head, Vec3 direction, Vec3 right, Vec3 up, float size, float sign, float fade) {
		Vec3 base = head.subtract(direction.scale(size * 0.2F)).add(right.scale(sign * size * 0.38F)).add(up.scale(size * 0.34F));
		Vec3 tip = head.add(direction.scale(size * 0.35F)).add(right.scale(sign * size * 0.88F)).add(up.scale(size * 0.78F));
		addRibbon(buffer, matrix, base, tip, up, size * 0.12F, size * 0.02F, alphaColor(0xB8FFE8A0, fade));
	}

	private static Vec3 dragonPoint(Vec3 direction, Vec3 right, Vec3 up, DragonState dragon, float time, float range, float t) {
		float wave = Mth.sin(time * 0.36F + t * 9.8F + dragon.seed) * dragon.radius * 0.34F;
		float coil = Mth.sin(time * 0.44F + t * 15.5F + dragon.seed * 0.7F) * dragon.radius * 0.22F;
		return direction.scale(range * t).add(right.scale(wave * Mth.sin(t * Mth.PI))).add(up.scale(coil * Mth.sin(t * Mth.PI)));
	}

	private static float bodyRadius(DragonState dragon, float t) {
		float swell = Mth.sin(t * Mth.PI);
		return dragon.radius * (0.28F + swell * 0.76F + t * 0.22F);
	}

	private static void addRibbon(BufferBuilder buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 axis, float widthA, float widthB, int color) {
		Vec3 sideA = axis.normalize().scale(widthA);
		Vec3 sideB = axis.normalize().scale(widthB);
		addQuad(buffer, matrix, a.add(sideA), a.subtract(sideA), b.subtract(sideB), b.add(sideB), color);
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

	private static float easeOutCubic(float value) {
		value = Mth.clamp(value, 0.0F, 1.0F);
		float inverse = 1.0F - value;
		return 1.0F - inverse * inverse * inverse;
	}

	private static float smoothstep(float edge0, float edge1, float value) {
		float x = Mth.clamp((value - edge0) / Math.max(0.0001F, edge1 - edge0), 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}

	private static int alphaColor(int argb, float alphaMultiplier) {
		int alpha = (argb >> 24) & 255;
		alpha = Mth.clamp(Mth.floor(alpha * alphaMultiplier), 0, 255);
		return (argb & 0x00FFFFFF) | (alpha << 24);
	}

	private static final class DragonState {
		private UUID casterId;
		private Vec3 origin = Vec3.ZERO;
		private Vec3 direction = new Vec3(0.0D, 0.0D, 1.0D);
		private float range;
		private float radius;
		private float chargePercent;
		private float seed;
		private int age;
		private int maxAge;
		private int travelTicks;
	}
}
