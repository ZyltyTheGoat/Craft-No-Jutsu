package net.mcreator.naruto.client;

import org.joml.Matrix4f;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

@EventBusSubscriber(value = Dist.CLIENT)
public final class SolidFogJutsuClientEffects {
	private static final int CLOUD_PUFFS = 560;
	private static final int MOUTH_PUFFS = 110;
	private static final float BASE_PUFF_SIZE = 0.34F;
	private static final float PUFF_SIZE_VARIATION = 0.34F;
	private static final int[] FOG_COLORS = {0xD8DED2, 0xC8D1C2, 0xB9C5B2, 0xD7D4C5, 0xAEBBA8, 0xE1E3D9};
	private static final List<FogEffect> EFFECTS = new ArrayList<>();

	private SolidFogJutsuClientEffects() {
	}

	public static void addFog(UUID casterId, Vec3 origin, Vec3 direction, float length, float radius, int ticks) {
		if (origin == null || direction == null || direction.lengthSqr() < 0.0001D) {
			return;
		}
		EFFECTS.add(new FogEffect(casterId, origin, direction.normalize(), Math.max(1.0F, length), Math.max(0.5F, radius), Math.max(1, ticks)));
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<FogEffect> iterator = EFFECTS.iterator();
		while (iterator.hasNext()) {
			FogEffect fog = iterator.next();
			fog.ticksLeft--;
			if (fog.ticksLeft <= 0) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void render(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || EFFECTS.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}
		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.getPosition();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		PoseStack poseStack = event.getPoseStack();
		Matrix4f matrix = poseStack.last().pose();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (FogEffect fog : EFFECTS) {
			renderFog(buffer, matrix, minecraft, cameraPos, fog, partialTick);
		}
		MeshData mesh = buffer.build();
		if (mesh != null) {
			BufferUploader.drawWithShader(mesh);
		}
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static void renderFog(BufferBuilder buffer, Matrix4f matrix, Minecraft minecraft, Vec3 camera, FogEffect fog, float partialTick) {
		float age = fog.totalTicks - fog.ticksLeft + partialTick;
		float expansion = smoothStep(Mth.clamp(age / 18.0F, 0.0F, 1.0F));
		float fade = Mth.clamp(fog.ticksLeft / 18.0F, 0.0F, 1.0F);
		Basis basis = basis(fog.direction);
		for (int i = 0; i < CLOUD_PUFFS; i++) {
			float delay = hash(i * 47 + 9) * 15.0F;
			float born = Mth.clamp((age - delay) / 9.0F, 0.0F, 1.0F);
			if (born <= 0.0F) {
				continue;
			}
			float longitudinal = (float) Math.pow(hash(i * 29 + 3), 0.72D);
			double forward = fog.length * (0.035D + longitudinal * 0.965D) * expansion;
			double crossRadius = fog.radius * (0.18D + longitudinal * 0.82D) * Math.sqrt(hash(i * 71 + 17)) * expansion;
			double angle = hash(i * 97 + 31) * Mth.TWO_PI + age * (0.0025D + hash(i * 13) * 0.004D);
			double yBias = (hash(i * 113 + 43) - 0.47D) * fog.radius * (0.42D + longitudinal * 0.38D) * expansion;
			double roll = Math.sin(age * 0.035D + i * 1.731D) * (0.08D + longitudinal * 0.13D);
			Vec3 point = fog.origin.add(fog.direction.scale(forward)).add(basis.right.scale(Math.cos(angle) * crossRadius)).add(basis.up.scale(Math.sin(angle) * crossRadius * 0.74D + yBias + roll));
			float edgeFade = Mth.clamp((float) ((fog.length - forward) / Math.max(0.8D, fog.length * 0.13D)), 0.18F, 1.0F);
			float alpha = fade * born * edgeFade * (0.12F + hash(i * 151 + 7) * 0.16F);
			float size = (BASE_PUFF_SIZE + hash(i * 61 + 5) * PUFF_SIZE_VARIATION) * (0.72F + longitudinal * 0.72F) * Math.max(0.25F, expansion);
			addPuff(buffer, matrix, point.subtract(camera), size, color(i, alpha), basis);
		}

		if (age < 26.0F) {
			renderMouthStream(buffer, matrix, minecraft, camera, fog, basis, age, fade, partialTick);
		}
	}

	private static void renderMouthStream(BufferBuilder buffer, Matrix4f matrix, Minecraft minecraft, Vec3 camera, FogEffect fog, Basis basis, float age, float fade, float partialTick) {
		Player caster = fog.casterId == null ? null : minecraft.level.getPlayerByUUID(fog.casterId);
		Vec3 source = fog.origin;
		if (caster != null) {
			source = caster.getEyePosition(partialTick).add(fog.direction.scale(0.2D)).add(0.0D, -0.16D, 0.0D);
		}
		for (int i = 0; i < MOUTH_PUFFS; i++) {
			float delay = (i % 22) * 0.16F + (i / 22) * 0.7F;
			float travel = Mth.clamp((age - delay) / 11.0F, 0.0F, 1.0F);
			if (travel <= 0.0F || travel >= 1.0F) {
				continue;
			}
			float eased = smoothStep(travel);
			double distance = fog.length * (0.08D + hash(i * 37 + 2) * 0.24D);
			Vec3 destination = fog.origin.add(fog.direction.scale(distance));
			double spiral = i * 2.399963D + age * 0.18D;
			double width = Math.sin(travel * Math.PI) * (0.07D + travel * 0.24D);
			Vec3 point = source.lerp(destination, eased).add(basis.right.scale(Math.cos(spiral) * width)).add(basis.up.scale(Math.sin(spiral) * width));
			float size = 0.12F + travel * 0.24F + hash(i * 79) * 0.1F;
			float alpha = fade * Mth.sin(travel * Mth.PI) * 0.38F;
			addPuff(buffer, matrix, point.subtract(camera), size, color(i + 700, alpha), basis);
		}
	}

	private static void addPuff(BufferBuilder buffer, Matrix4f matrix, Vec3 center, float size, int color, Basis basis) {
		Vec3 right = basis.right.scale(size);
		Vec3 up = basis.up.scale(size * 0.78D);
		Vec3 diagonalA = right.add(up).normalize().scale(size * 0.9D);
		Vec3 diagonalB = right.subtract(up).normalize().scale(size * 0.9D);
		addQuad(buffer, matrix, center.subtract(right).subtract(up), center.add(right).subtract(up), center.add(right).add(up), center.subtract(right).add(up), color);
		addQuad(buffer, matrix, center.subtract(diagonalA).add(0.0D, -size * 0.5D, 0.0D), center.add(diagonalB).add(0.0D, -size * 0.5D, 0.0D), center.add(diagonalA).add(0.0D, size * 0.5D, 0.0D), center.subtract(diagonalB).add(0.0D, size * 0.5D, 0.0D), darken(color, 0.9F));
	}

	private static Basis basis(Vec3 direction) {
		Vec3 upHint = Math.abs(direction.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 right = upHint.cross(direction).normalize();
		Vec3 up = direction.cross(right).normalize();
		return new Basis(right, up);
	}

	private static int color(int seed, float alpha) {
		int rgb = FOG_COLORS[Math.floorMod(seed * 31 + 7, FOG_COLORS.length)];
		return (Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24) | rgb;
	}

	private static int darken(int color, float amount) {
		int a = color >>> 24;
		int r = (int) (((color >>> 16) & 255) * amount);
		int g = (int) (((color >>> 8) & 255) * amount);
		int b = (int) ((color & 255) * amount);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static float hash(int value) {
		int mixed = value * 1664525 + 1013904223;
		mixed ^= mixed >>> 16;
		return (mixed & 0x00FFFFFF) / 16777215.0F;
	}

	private static float smoothStep(float value) {
		value = Mth.clamp(value, 0.0F, 1.0F);
		return value * value * (3.0F - 2.0F * value);
	}

	private static void addQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
		buffer.addVertex(matrix, (float) a.x, (float) a.y, (float) a.z).setColor(color);
		buffer.addVertex(matrix, (float) b.x, (float) b.y, (float) b.z).setColor(color);
		buffer.addVertex(matrix, (float) c.x, (float) c.y, (float) c.z).setColor(color);
		buffer.addVertex(matrix, (float) d.x, (float) d.y, (float) d.z).setColor(color);
	}

	private record Basis(Vec3 right, Vec3 up) {
	}

	private static final class FogEffect {
		private final UUID casterId;
		private final Vec3 origin;
		private final Vec3 direction;
		private final float length;
		private final float radius;
		private final int totalTicks;
		private int ticksLeft;

		private FogEffect(UUID casterId, Vec3 origin, Vec3 direction, float length, float radius, int totalTicks) {
			this.casterId = casterId;
			this.origin = origin;
			this.direction = direction;
			this.length = length;
			this.radius = radius;
			this.totalTicks = totalTicks;
			this.ticksLeft = totalTicks;
		}
	}
}
