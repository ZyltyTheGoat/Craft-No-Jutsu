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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.network.SandBindingCoffinStatePacket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

@EventBusSubscriber(value = Dist.CLIENT)
public final class SandBindingCoffinClientEffects {
	private static final int STREAM_GRAINS = 280;
	private static final int COCOON_GRAINS = 1760;
	private static final int FORM_TICKS = 22;
	private static final int RELEASE_TICKS = 22;
	private static final float GRAIN_BASE_SIZE = 0.028F;
	private static final float GRAIN_SIZE_IMPERFECTION = 0.017F;
	private static final int[] GRAIN_COLORS = {0xFF252729, 0xFF2b2d2e, 0xFF333436, 0xFF3b3d3e, 0xFF434547, 0xFF4b4d4f};
	private static final Map<Integer, CoffinState> ACTIVE = new HashMap<>();

	private SandBindingCoffinClientEffects() {
	}

	public static void setState(UUID casterId, int targetId, byte action, int ticks) {
		if (targetId < 0) {
			return;
		}
		CoffinState state = ACTIVE.get(targetId);
		if (action == SandBindingCoffinStatePacket.START) {
			ACTIVE.put(targetId, new CoffinState(casterId, targetId, Math.max(8, ticks)));
		} else if (action == SandBindingCoffinStatePacket.REFRESH) {
			if (state == null) {
				state = new CoffinState(casterId, targetId, Math.max(8, ticks));
				state.age = FORM_TICKS;
				ACTIVE.put(targetId, state);
			}
			state.casterId = casterId;
			state.active = true;
			state.life = Math.max(8, ticks);
		} else if (state != null) {
			state.active = false;
			state.releaseAge = 0;
		}
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<CoffinState> iterator = ACTIVE.values().iterator();
		while (iterator.hasNext()) {
			CoffinState state = iterator.next();
			state.age++;
			if (state.active) {
				state.life--;
				if (state.life <= 0) {
					state.active = false;
					state.releaseAge = 0;
				}
			} else if (++state.releaseAge > RELEASE_TICKS) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void render(RenderLevelStageEvent event) {
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
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.depthMask(true);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Iterator<Map.Entry<Integer, CoffinState>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			CoffinState state = iterator.next().getValue();
			Entity entity = minecraft.level.getEntity(state.targetId);
			if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
				iterator.remove();
				continue;
			}
			Player caster = state.casterId == null ? null : minecraft.level.getPlayerByUUID(state.casterId);
			renderCoffin(poseStack, camera, caster, target, state, partialTick);
		}
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static void renderCoffin(PoseStack poseStack, Vec3 camera, Player caster, LivingEntity target, CoffinState state, float partialTick) {
		double x = Mth.lerp(partialTick, target.xOld, target.getX());
		double y = Mth.lerp(partialTick, target.yOld, target.getY());
		double z = Mth.lerp(partialTick, target.zOld, target.getZ());
		float height = target.getBbHeight();
		float width = Math.max(0.65F, target.getBbWidth() * 0.72F);
		float age = state.age + partialTick;
		float releaseAge = state.releaseAge + partialTick;
		float formation = smoothStep(Mth.clamp(age / FORM_TICKS, 0.0F, 1.0F));
		float release = state.active ? 0.0F : smoothStep(Mth.clamp(releaseAge / RELEASE_TICKS, 0.0F, 1.0F));
		float visibility = state.active ? 1.0F : 1.0F - release;
		boolean renderStream = caster != null && state.active && age < FORM_TICKS + 8.0F;
		boolean renderReturnStream = caster != null && !state.active && releaseAge < RELEASE_TICKS;
		boolean renderCocoon = visibility > 0.02F;
		if (!renderStream && !renderReturnStream && !renderCocoon) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(x - camera.x, y - camera.y, z - camera.z);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		if (renderStream) {
			renderGourdStream(buffer, matrix, caster, target, partialTick, age, false);
		}
		if (renderReturnStream) {
			renderGourdStream(buffer, matrix, caster, target, partialTick, releaseAge, true);
		}
		if (renderCocoon) {
			renderCocoonGrains(buffer, matrix, target, width, height, target.tickCount + partialTick, formation, release, visibility);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		poseStack.popPose();
	}

	private static void renderGourdStream(BufferBuilder buffer, Matrix4f matrix, Player caster, LivingEntity target, float partialTick, float age, boolean returning) {
		Vec3 casterPos = caster.getPosition(partialTick);
		Vec3 targetBase = target.getPosition(partialTick);
		float bodyYaw = Mth.rotLerp(partialTick, caster.yBodyRotO, caster.yBodyRot);
		Vec3 forward = Vec3.directionFromRotation(0.0F, bodyYaw).multiply(1.0D, 0.0D, 1.0D).normalize();
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		Vec3 source = casterPos.add(forward.scale(-0.48D)).add(right.scale(0.12D)).add(0.0D, 1.58D, 0.0D).subtract(targetBase);
		Vec3 exit = casterPos.add(forward.scale(-0.76D)).add(0.0D, 2.08D, 0.0D).subtract(targetBase);
		Vec3 targetPoint = new Vec3(0.0D, target.getBbHeight() * 0.34D, 0.0D);
		for (int i = 0; i < STREAM_GRAINS; i++) {
			float delay = (i % 30) * 0.22F + (i / 30) * 0.72F;
			float progress = (age - delay) / 19.0F;
			if (progress <= 0.0F || progress >= 1.0F) {
				continue;
			}
			float eased = returning ? 1.0F - smoothStep(progress) : smoothStep(progress);
			Vec3 point = streamPath(source, exit, targetPoint, eased);
			double turbulence = 0.045D + Math.sin(progress * Math.PI) * 0.045D;
			double phase = i * 2.399963D + age * (returning ? -0.34D : 0.34D);
			point = point.add(Math.cos(phase) * turbulence, Math.sin(phase * 1.27D) * turbulence, Math.sin(phase) * turbulence);
			float size = grainSize(i * 17 + 5);
			addBox(buffer, matrix, point, size, size * 0.7F, color(i, 1.0F));
		}
	}

	private static Vec3 streamPath(Vec3 source, Vec3 exit, Vec3 target, float progress) {
		if (progress < 0.22F) {
			float rise = smoothStep(progress / 0.22F);
			return source.lerp(exit, rise);
		}
		float travel = smoothStep((progress - 0.22F) / 0.54F);
		Vec3 mid = exit.lerp(target.add(0.0D, 1.1D, 0.0D), travel);
		double arc = Math.sin(travel * Math.PI) * 0.8D;
		if (progress < 0.76F) {
			return mid.add(0.0D, arc, 0.0D);
		}
		float wrap = smoothStep((progress - 0.76F) / 0.24F);
		double angle = wrap * Math.PI * 2.2D;
		double radius = (1.0D - wrap) * 0.72D;
		return target.add(Math.cos(angle) * radius, Mth.lerp(wrap, 1.1D, 0.16D), Math.sin(angle) * radius);
	}

	private static void renderCocoonGrains(BufferBuilder buffer, Matrix4f matrix, LivingEntity target, float width, float height, float time, float formation, float release, float visibility) {
		int count = Mth.clamp((int) (COCOON_GRAINS * visibility), 0, COCOON_GRAINS);
		float formedHeight = Mth.lerp(formation, 0.12F, height * 0.94F);
		for (int i = 0; i < count; i++) {
			float seed = hash(i * 37);
			float vertical = (i + 0.5F) / COCOON_GRAINS;
			if (vertical > formation + 0.08F) {
				continue;
			}
			float angle = i * 2.399963F + hash(i * 53 + 9) * 0.7F + time * (0.012F + hash(i * 11) * 0.01F);
			float band = Mth.sin(vertical * Mth.PI);
			float radius = width * (0.62F + band * 0.36F) + hash(i * 19) * 0.06F;
			float collapse = 1.0F - release * 0.58F;
			float x = Mth.cos(angle) * radius * collapse;
			float z = Mth.sin(angle) * radius * collapse;
			float y = Mth.clamp(vertical * formedHeight, 0.04F, height * 0.96F);
			y += Mth.sin(time * 0.16F + i * 0.29F) * 0.018F;
			y = Mth.lerp(release, y, height * 0.22F + seed * height * 0.16F);
			float size = grainSize(i * 17 + 5) * (0.82F + visibility * 0.18F);
			addBox(buffer, matrix, new Vec3(x, y, z), size, size * 0.7F, color(i, visibility));
		}
		for (int i = 0; i < 34 && visibility > 0.08F; i++) {
			float angle = i * 2.399963F - time * 0.035F;
			float radius = width * (0.72F + hash(i * 29) * 0.42F) * (1.0F - release * 0.35F);
			float y = height * (0.18F + hash(i * 31) * 0.66F);
			float size = grainSize(i * 23 - 1);
			addBox(buffer, matrix, new Vec3(Mth.cos(angle) * radius, y, Mth.sin(angle) * radius), size, size * 0.7F, color(i + 41, visibility * 0.84F));
		}
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

	private static final class CoffinState {
		private UUID casterId;
		private final int targetId;
		private int age;
		private int life;
		private int releaseAge;
		private boolean active = true;

		private CoffinState(UUID casterId, int targetId, int life) {
			this.casterId = casterId;
			this.targetId = targetId;
			this.life = life;
		}
	}
}
