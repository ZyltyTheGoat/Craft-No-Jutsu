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
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

@EventBusSubscriber(value = Dist.CLIENT)
public final class NeedleSphereClientEffects {
	private static final List<Sphere> SPHERES = new ArrayList<>();
	private static final int SHELL_GRAINS = 1260;
	private static final int NEEDLE_GRAINS = 220;
	private static final int FLOW_GRAINS = 132;
	private static final float GRAIN_BASE_SIZE = 0.028F;
	private static final float GRAIN_SIZE_IMPERFECTION = 0.017F;
	private static final int[] GRAIN_COLORS = {0xFF252729, 0xFF2b2d2e, 0xFF333436, 0xFF3b3d3e, 0xFF434547, 0xFF4b4d4f};

	private NeedleSphereClientEffects() {
	}

	public static void addSphere(UUID casterId, float radius, float chargePercent, int ticks) {
		SPHERES.add(new Sphere(casterId, Math.max(1.4F, radius), Mth.clamp(chargePercent, 0.0F, 1.0F), Math.max(18, ticks)));
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<Sphere> iterator = SPHERES.iterator();
		while (iterator.hasNext()) {
			Sphere sphere = iterator.next();
			if (--sphere.ticksLeft <= 0) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void render(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || SPHERES.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}
		Vec3 camera = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		if (!hasVisibleSphere(minecraft, partialTick)) {
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
		for (Sphere sphere : SPHERES) {
			renderSphere(buffer, matrix, minecraft, camera, sphere, partialTick);
		}
		MeshData mesh = buffer.build();
		if (mesh != null) {
			BufferUploader.drawWithShader(mesh);
		}
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static boolean hasVisibleSphere(Minecraft minecraft, float partialTick) {
		for (Sphere sphere : SPHERES) {
			if (sphere.casterId != null && minecraft.level.getPlayerByUUID(sphere.casterId) != null) {
				float age = sphere.totalTicks - sphere.ticksLeft + partialTick;
				if (age > 0.0F && age < sphere.totalTicks) {
					return true;
				}
			}
		}
		return false;
	}

	private static void renderSphere(BufferBuilder buffer, Matrix4f matrix, Minecraft minecraft, Vec3 camera, Sphere sphere, float partialTick) {
		Player caster = sphere.casterId == null ? null : minecraft.level.getPlayerByUUID(sphere.casterId);
		if (caster == null) {
			return;
		}
		float age = sphere.totalTicks - sphere.ticksLeft + partialTick;
		float life = Mth.clamp(age / sphere.totalTicks, 0.0F, 1.0F);
		float fade = life < 0.1F ? life / 0.1F : 1.0F - Mth.clamp((life - 0.94F) / 0.06F, 0.0F, 1.0F);
		float form = smoothStep(Mth.clamp(age / 9.0F, 0.0F, 1.0F));
		float sharpen = smoothStep(Mth.clamp((age - 5.0F) / 7.0F, 0.0F, 1.0F));
		float retract = smoothStep(Mth.clamp((age - 30.0F) / 7.0F, 0.0F, 1.0F));
		float returnFlow = smoothStep(Mth.clamp((age - 36.0F) / 11.0F, 0.0F, 1.0F));
		Vec3 casterPos = caster.getPosition(partialTick);
		Vec3 center = casterPos.add(0.0D, caster.getBbHeight() * 0.58D, 0.0D);
		Vec3 ballCenter = casterPos.add(0.0D, caster.getBbHeight() + 0.72D, 0.0D);
		float bodyYaw = Mth.rotLerp(partialTick, caster.yBodyRotO, caster.yBodyRot);
		Vec3 forward = Vec3.directionFromRotation(0.0F, bodyYaw).multiply(1.0D, 0.0D, 1.0D);
		if (forward.lengthSqr() < 0.001D) {
			forward = new Vec3(0.0D, 0.0D, 1.0D);
		}
		forward = forward.normalize();
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 source = gourdSource(casterPos, forward, right);
		renderGourdFlow(buffer, matrix, camera, source, ballCenter, right, up, age, form, fade);
		renderCoreBall(buffer, matrix, camera, ballCenter, right, up, sphere, age, form, sharpen, retract, returnFlow, fade);
		renderNeedles(buffer, matrix, camera, ballCenter, sphere, age, sharpen, retract, returnFlow, fade);
		renderReturnFlow(buffer, matrix, camera, source, ballCenter, sphere, age, returnFlow, fade);
	}

	private static Vec3 gourdSource(Vec3 casterPos, Vec3 forward, Vec3 right) {
		return casterPos.add(forward.scale(-0.54D)).add(right.scale(0.12D)).add(0.0D, 1.58D, 0.0D);
	}

	private static void renderGourdFlow(BufferBuilder buffer, Matrix4f matrix, Vec3 camera, Vec3 source, Vec3 center, Vec3 right, Vec3 up, float age, float form, float fade) {
		for (int i = 0; i < FLOW_GRAINS; i++) {
			float delay = (i % 23) * 0.11F;
			float progress = Mth.clamp((age - delay) / 9.5F, 0.0F, 1.0F);
			if (progress <= 0.0F || progress >= 1.0F || form >= 1.0F) {
				continue;
			}
			float eased = smoothStep(progress);
			double orbit = i * 2.399963D + age * 0.38D;
			double radius = Math.sin(progress * Math.PI) * (0.25D + hash(i * 19) * 0.18D);
			Vec3 point = source.lerp(center.add(up.scale((hash(i * 31) - 0.5F) * 0.7F)), eased)
					.add(right.scale(Math.cos(orbit) * radius))
					.add(up.scale(Math.sin(orbit * 1.3D) * radius * 0.56D));
			addBox(buffer, matrix, point.subtract(camera), grainSize(i * 13 + 5), grainSize(i * 17 + 9) * 0.72F, color(i, fade * (1.0F - form * 0.75F)));
		}
	}

	private static void renderCoreBall(BufferBuilder buffer, Matrix4f matrix, Vec3 camera, Vec3 center, Vec3 right, Vec3 up, Sphere sphere, float age, float form, float sharpen, float retract, float returnFlow, float fade) {
		float shellAlpha = fade * (1.0F - returnFlow);
		double rotation = age * (0.045D + sphere.chargePercent * 0.025D);
		for (int i = 0; i < SHELL_GRAINS; i++) {
			Vec3 normal = sphereNormal(i, rotation);
			double roughness = 0.86D + hash(i * 41 + 3) * 0.2D;
			double ballRadius = (0.42D + sphere.chargePercent * 0.12D) * form * roughness * (1.0D + Math.sin(age * 0.23D + i) * 0.025D);
			double pulse = sharpen * (1.0D - retract) * (0.05D + hash(i * 7) * 0.08D);
			double loosen = retract * (1.0D - returnFlow) * (0.12D + hash(i * 13) * 0.24D);
			Vec3 point = center.add(normal.scale(ballRadius + pulse + loosen));
			double swirl = (1.0D - form) * (0.56D + hash(i * 11) * 0.28D);
			double crumble = retract * (1.0D - returnFlow) * (0.16D + hash(i * 17) * 0.2D);
			point = point.add(right.scale(Math.cos(i * 0.71D + age * 0.24D) * (swirl + crumble))).add(up.scale(Math.sin(i * 0.47D + age * 0.18D) * (swirl + crumble * 0.72D)));
			float size = grainSize(i * 17 + 5) * (0.9F + hash(i * 23) * 0.42F);
			addBox(buffer, matrix, point.subtract(camera), size, size * 0.68F, color(i, shellAlpha));
		}
	}

	private static void renderNeedles(BufferBuilder buffer, Matrix4f matrix, Vec3 camera, Vec3 center, Sphere sphere, float age, float sharpen, float retract, float returnFlow, float fade) {
		if (sharpen <= 0.0F || returnFlow >= 0.98F) {
			return;
		}
		float needlePresence = sharpen * (1.0F - retract);
		float needleAlpha = fade * Mth.clamp(needlePresence * 1.25F, 0.0F, 1.0F);
		double travel = smoothStep(sharpen);
		for (int i = 0; i < NEEDLE_GRAINS; i++) {
			float delay = (i % 17) * 0.018F;
			float localSharpen = Mth.clamp((float) ((sharpen - delay) / 0.86F), 0.0F, 1.0F);
			if (localSharpen <= 0.0F || needlePresence <= 0.0F) {
				continue;
			}
			Vec3 normal = needleNormal(i, NEEDLE_GRAINS, age * 0.035D);
			Vec3 tangent = Math.abs(normal.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D).cross(normal).normalize();
			Vec3 side = normal.cross(tangent).normalize();
			double startRadius = 0.38D + sphere.chargePercent * 0.12D + hash(i * 29) * 0.1D;
			double length = (sphere.radius * (0.78D + hash(i * 31) * 0.32D)) * smoothStep(localSharpen) * (1.0D - retract);
			Vec3 base = center.add(normal.scale(startRadius + travel * 0.08D));
			Vec3 tip = base.add(normal.scale(length));
			float radius = grainSize(i * 19 + 11) * (0.62F + sphere.chargePercent * 0.28F);
			renderNeedle(buffer, matrix, base.subtract(camera), tip.subtract(camera), tangent, side, radius, color(i * 3, needleAlpha));
		}
	}

	private static void renderReturnFlow(BufferBuilder buffer, Matrix4f matrix, Vec3 camera, Vec3 source, Vec3 center, Sphere sphere, float age, float returnFlow, float fade) {
		if (returnFlow <= 0.0F) {
			return;
		}
		for (int i = 0; i < FLOW_GRAINS + 42; i++) {
			float delay = (i % 29) * 0.018F + (i / 29) * 0.12F;
			float localReturn = Mth.clamp((float) ((returnFlow - delay) / 0.88F), 0.0F, 1.0F);
			if (localReturn <= 0.0F || localReturn >= 1.0F) {
				continue;
			}
			Vec3 normal = sphereNormal(i * 11 + 17, age * 0.028D);
			double shellRadius = 0.5D + hash(i * 43) * 0.42D;
			Vec3 loose = center.add(normal.scale(shellRadius));
			Vec3 mid = center.add(normal.scale(0.24D + hash(i * 11) * 0.18D)).add(0.0D, 0.18D + hash(i * 23) * 0.34D, 0.0D);
			Vec3 path = quadratic(loose, mid, source, smoothStep(localReturn));
			double orbit = i * 2.399963D - age * 0.44D;
			double radius = Math.sin(localReturn * Math.PI) * (0.2D + hash(i * 19) * 0.16D);
			Vec3 tangent = Math.abs(normal.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D).cross(normal).normalize();
			Vec3 side = normal.cross(tangent).normalize();
			Vec3 point = path.add(tangent.scale(Math.cos(orbit) * radius)).add(side.scale(Math.sin(orbit) * radius * 0.64D));
			float alpha = fade * Mth.clamp(Mth.sin(localReturn * Mth.PI) * 1.35F, 0.0F, 1.0F);
			float size = grainSize(i * 17 + 5) * (0.9F + hash(i * 13) * 0.28F);
			addBox(buffer, matrix, point.subtract(camera), size, size * 0.68F, color(i * 5, alpha));
		}
	}

	private static Vec3 quadratic(Vec3 a, Vec3 b, Vec3 c, float progress) {
		Vec3 ab = a.lerp(b, progress);
		Vec3 bc = b.lerp(c, progress);
		return ab.lerp(bc, progress);
	}

	private static Vec3 sphereNormal(int seed, double rotation) {
		double y = 1.0D - 2.0D * hash(seed * 37 + 13);
		double radius = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
		double angle = seed * 2.399963229728653D + rotation;
		return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius).normalize();
	}

	private static Vec3 needleNormal(int index, int count, double rotation) {
		double y = 1.0D - 2.0D * ((index + 0.5D) / Math.max(1, count));
		double radius = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
		double jitter = (hash(index * 67 + 19) - 0.5D) * 0.16D;
		double angle = index * 2.399963229728653D + rotation + jitter;
		return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius).normalize();
	}

	private static void renderNeedle(BufferBuilder buffer, Matrix4f matrix, Vec3 base, Vec3 tip, Vec3 tangent, Vec3 side, float radius, int color) {
		Vec3 t = tangent.scale(radius);
		Vec3 s = side.scale(radius * 0.55F);
		addQuad(buffer, matrix, base.subtract(t), base.add(t), tip.add(t.scale(0.28D)), tip.subtract(t.scale(0.28D)), color);
		addQuad(buffer, matrix, base.subtract(s), base.add(s), tip.add(s.scale(0.28D)), tip.subtract(s.scale(0.28D)), darken(color, 0.7F));
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

	private static void addQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
		addVertex(buffer, matrix, a, color);
		addVertex(buffer, matrix, b, color);
		addVertex(buffer, matrix, c, color);
		addVertex(buffer, matrix, d, color);
	}

	private static void addVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, int color) {
		buffer.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z).setColor(color);
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

	private static final class Sphere {
		private final UUID casterId;
		private final float radius;
		private final float chargePercent;
		private final int totalTicks;
		private int ticksLeft;

		private Sphere(UUID casterId, float radius, float chargePercent, int totalTicks) {
			this.casterId = casterId;
			this.radius = radius;
			this.chargePercent = chargePercent;
			this.totalTicks = totalTicks;
			this.ticksLeft = totalTicks;
		}
	}
}
