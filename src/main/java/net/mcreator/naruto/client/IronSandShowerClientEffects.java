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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

@EventBusSubscriber(value = Dist.CLIENT)
public final class IronSandShowerClientEffects {
	private static final List<Shower> SHOWERS = new ArrayList<>();
	private static final float GRAIN_BASE_SIZE = 0.028F;
	private static final float GRAIN_SIZE_IMPERFECTION = 0.017F;
	private static final int[] GRAIN_COLORS = {0xFF252729, 0xFF2b2d2e, 0xFF333436, 0xFF3b3d3e, 0xFF434547, 0xFF4b4d4f};

	private IronSandShowerClientEffects() {
	}

	public static void addShower(UUID casterId, Vec3 origin, Vec3 direction, float range, float spread, int needleCount, int ticks) {
		if (origin == null || direction == null || direction.lengthSqr() < 0.001D) {
			return;
		}
		int safeTicks = Math.max(8, ticks);
		SHOWERS.add(new Shower(casterId, origin, direction.normalize(), Math.max(1.0F, range), Math.max(1.0F, spread), Mth.clamp(needleCount, 8, 120), safeTicks));
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<Shower> iterator = SHOWERS.iterator();
		while (iterator.hasNext()) {
			Shower shower = iterator.next();
			if (--shower.ticksLeft <= 0) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void render(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || SHOWERS.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}
		Vec3 camera = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		boolean hasRenderableGeometry = false;
		for (Shower shower : SHOWERS) {
			if (hasRenderableGeometry(minecraft, shower, partialTick)) {
				hasRenderableGeometry = true;
				break;
			}
		}
		if (!hasRenderableGeometry) {
			return;
		}
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.depthMask(true);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		for (Shower shower : SHOWERS) {
			renderShower(buffer, matrix, minecraft, camera, shower, partialTick);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static boolean hasRenderableGeometry(Minecraft minecraft, Shower shower, float partialTick) {
		float age = shower.totalTicks - shower.ticksLeft + partialTick;
		Player caster = shower.casterId == null ? null : minecraft.level.getPlayerByUUID(shower.casterId);
		if (caster != null && age > 0.0F && age < 9.0F) {
			return true;
		}
		for (int i = 0; i < shower.needleCount; i++) {
			float delay = (i % 18) * 0.34F + (i / 18) * 0.78F;
			float travel = (age - delay) / 13.0F;
			if (travel > 0.0F && travel < 1.0F) {
				return true;
			}
		}
		return false;
	}

	private static void renderShower(BufferBuilder buffer, Matrix4f matrix, Minecraft minecraft, Vec3 camera, Shower shower, float partialTick) {
		float age = shower.totalTicks - shower.ticksLeft + partialTick;
		float life = Mth.clamp(age / shower.totalTicks, 0.0F, 1.0F);
		float fade = life < 0.18F ? life / 0.18F : 1.0F - Mth.clamp((life - 0.78F) / 0.22F, 0.0F, 1.0F);
		Basis basis = basis(shower.direction);
		Vec3 origin = shower.origin;
		Player caster = shower.casterId == null ? null : minecraft.level.getPlayerByUUID(shower.casterId);
		if (caster != null && age < 9.0F) {
			renderGourdGather(buffer, matrix, camera, caster, origin, basis, age, fade, partialTick);
		}
		for (int i = 0; i < shower.needleCount; i++) {
			float delay = (i % 18) * 0.34F + (i / 18) * 0.78F;
			float travel = (age - delay) / 13.0F;
			if (travel <= 0.0F || travel >= 1.0F) {
				continue;
			}
			float seedA = hash(i * 37 + 3);
			float seedB = hash(i * 53 + 7);
			float seedC = hash(i * 71 + 11);
			double spreadRadius = Math.tan(Math.toRadians(shower.spread)) * shower.range * (0.18D + seedA * 0.82D);
			double angle = seedB * Math.PI * 2.0D;
			Vec3 sideOffset = basis.right.scale(Math.cos(angle) * spreadRadius).add(basis.up.scale(Math.sin(angle) * spreadRadius * 0.72D));
			Vec3 end = origin.add(shower.direction.scale(shower.range)).add(sideOffset);
			Vec3 start = origin.add(basis.right.scale(Math.cos(angle) * 0.22D)).add(basis.up.scale(0.38D + seedC * 0.34D));
			Vec3 tip = start.lerp(end, smoothStep(travel));
			Vec3 tail = start.lerp(end, Math.max(0.0F, smoothStep(travel) - 0.09F - seedA * 0.04F));
			float alpha = fade * Mth.clamp(Mth.sin(travel * Mth.PI) * 1.35F, 0.0F, 1.0F);
			renderNeedle(buffer, matrix, tail.subtract(camera), tip.subtract(camera), basis, grainSize(i * 17 + 5), color(i, alpha));
		}
	}

	private static void renderGourdGather(BufferBuilder buffer, Matrix4f matrix, Vec3 camera, Player caster, Vec3 origin, Basis basis, float age, float fade, float partialTick) {
		Vec3 casterPos = caster.getPosition(partialTick);
		float bodyYaw = Mth.rotLerp(partialTick, caster.yBodyRotO, caster.yBodyRot);
		Vec3 forward = Vec3.directionFromRotation(0.0F, bodyYaw).multiply(1.0D, 0.0D, 1.0D).normalize();
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		Vec3 source = casterPos.add(forward.scale(-0.52D)).add(right.scale(0.12D)).add(0.0D, 1.58D, 0.0D);
		for (int i = 0; i < 54; i++) {
			float delay = (i % 18) * 0.16F;
			float progress = Mth.clamp((age - delay) / 8.0F, 0.0F, 1.0F);
			if (progress <= 0.0F || progress >= 1.0F) {
				continue;
			}
			float eased = smoothStep(progress);
			double orbit = i * 2.399963D + age * 0.32D;
			Vec3 gather = source.lerp(origin.add(basis.up.scale(0.38D)), eased);
			double radius = Math.sin(progress * Math.PI) * (0.28D + hash(i * 29) * 0.18D);
			Vec3 point = gather.add(basis.right.scale(Math.cos(orbit) * radius)).add(basis.up.scale(Math.sin(orbit * 1.3D) * radius * 0.55D));
			float size = grainSize(i * 17 + 5);
			addBox(buffer, matrix, point.subtract(camera), size, size * 0.7F, color(i, fade));
		}
	}

	private static void renderNeedle(BufferBuilder buffer, Matrix4f matrix, Vec3 tail, Vec3 tip, Basis basis, float radius, int color) {
		Vec3 axis = tip.subtract(tail);
		if (axis.lengthSqr() < 0.0001D) {
			return;
		}
		Vec3 right = basis.right.scale(radius);
		Vec3 up = basis.up.scale(radius * 0.55F);
		addQuad(buffer, matrix, tail.subtract(right), tail.add(right), tip.add(right.scale(0.38D)), tip.subtract(right.scale(0.38D)), color);
		addQuad(buffer, matrix, tail.subtract(up), tail.add(up), tip.add(up.scale(0.38D)), tip.subtract(up.scale(0.38D)), darken(color, 0.72F));
	}

	private static Basis basis(Vec3 direction) {
		Vec3 upHint = Math.abs(direction.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 right = upHint.cross(direction).normalize();
		Vec3 up = direction.cross(right).normalize();
		return new Basis(right, up);
	}

	private static float grainSize(int seed) {
		return GRAIN_BASE_SIZE + (hash(seed) - 0.5F) * GRAIN_SIZE_IMPERFECTION;
	}

	private static void addBox(BufferBuilder buffer, Matrix4f matrix, Vec3 center, float radius, float halfHeight, int color) {
		Vec3 nnn = center.add(-radius, -halfHeight, -radius);
		Vec3 nnp = center.add(-radius, -halfHeight, radius);
		Vec3 npn = center.add(-radius, halfHeight, -radius);
		Vec3 npp = center.add(-radius, halfHeight, radius);
		Vec3 pnn = center.add(radius, -halfHeight, -radius);
		Vec3 pnp = center.add(radius, -halfHeight, radius);
		Vec3 ppn = center.add(radius, halfHeight, -radius);
		Vec3 ppp = center.add(radius, halfHeight, radius);
		addQuad(buffer, matrix, npn, ppn, ppp, npp, color);
		addQuad(buffer, matrix, pnn, nnn, nnp, pnp, darken(color, 0.54F));
		addQuad(buffer, matrix, nnn, npn, npp, nnp, darken(color, 0.73F));
		addQuad(buffer, matrix, pnp, ppp, ppn, pnn, darken(color, 0.82F));
		addQuad(buffer, matrix, nnp, npp, ppp, pnp, darken(color, 0.68F));
		addQuad(buffer, matrix, pnn, ppn, npn, nnn, darken(color, 0.61F));
	}

	private static int color(int seed, float alpha) {
		int rgb = GRAIN_COLORS[Math.floorMod(seed * 31 + 7, GRAIN_COLORS.length)] & 0x00FFFFFF;
		return (Mth.clamp((int) (255.0F * alpha), 0, 255) << 24) | rgb;
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
		addVertex(buffer, matrix, a, color);
		addVertex(buffer, matrix, b, color);
		addVertex(buffer, matrix, c, color);
		addVertex(buffer, matrix, d, color);
	}

	private static void addVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, int color) {
		buffer.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z).setColor(color);
	}

	private record Basis(Vec3 right, Vec3 up) {
	}

	private static final class Shower {
		private final UUID casterId;
		private final Vec3 origin;
		private final Vec3 direction;
		private final float range;
		private final float spread;
		private final int needleCount;
		private final int totalTicks;
		private int ticksLeft;

		private Shower(UUID casterId, Vec3 origin, Vec3 direction, float range, float spread, int needleCount, int totalTicks) {
			this.casterId = casterId;
			this.origin = origin;
			this.direction = direction;
			this.range = range;
			this.spread = spread;
			this.needleCount = needleCount;
			this.totalTicks = totalTicks;
			this.ticksLeft = totalTicks;
		}
	}
}
