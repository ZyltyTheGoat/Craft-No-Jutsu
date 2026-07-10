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

import net.mcreator.naruto.network.SandLevitationStatePacket;

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
public final class SandLevitationClientEffects {
	private static final int FORM_TICKS = 34;
	private static final int DISSOLVE_TICKS = 34;
	private static final int PLATFORM_GRAINS = 1250;
	private static final int FLOW_GRAINS = 125;
	private static final float FLOW_TRAVEL_TICKS = 23.0F;
	private static final float RETURN_FLOW_END = 32.5F;
	private static final float GRAIN_BASE_SIZE = 0.028F;
	private static final float GRAIN_SIZE_IMPERFECTION = 0.017F;
	private static final Map<UUID, ClientState> ACTIVE = new HashMap<>();
	private static final int[] GRAIN_COLORS = {0xFF252729, 0xFF2b2d2e, 0xFF333436, 0xFF3b3d3e, 0xFF434547, 0xFF4b4d4f};

	private SandLevitationClientEffects() {
	}

	public static void setState(UUID playerId, byte action, int ticks) {
		if (playerId == null) {
			return;
		}
		ClientState state = ACTIVE.get(playerId);
		if (action == SandLevitationStatePacket.START) {
			ACTIVE.put(playerId, new ClientState(Math.max(8, ticks)));
		} else if (action == SandLevitationStatePacket.REFRESH) {
			if (state == null) {
				state = new ClientState(Math.max(FORM_TICKS, ticks));
				state.age = FORM_TICKS;
				ACTIVE.put(playerId, state);
			}
			state.active = true;
			state.life = Math.max(8, ticks);
		} else if (state != null) {
			state.active = false;
			state.dissolveAge = 0;
		}
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<ClientState> iterator = ACTIVE.values().iterator();
		while (iterator.hasNext()) {
			ClientState state = iterator.next();
			state.age++;
			if (state.active) {
				state.life--;
				if (state.life <= 0) {
					state.active = false;
					state.dissolveAge = 0;
				}
			} else if (++state.dissolveAge > DISSOLVE_TICKS) {
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
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		PoseStack poseStack = event.getPoseStack();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.depthMask(true);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		for (Map.Entry<UUID, ClientState> entry : ACTIVE.entrySet()) {
			Player player = minecraft.level.getPlayerByUUID(entry.getKey());
			if (player != null) {
				renderPlayerSand(poseStack, camera, player, entry.getValue(), partialTick);
			}
		}
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static void renderPlayerSand(PoseStack poseStack, Vec3 camera, Player player, ClientState state, float partialTick) {
		float age = state.age + partialTick;
		float formation = Mth.clamp((age - 12.0F) / (FORM_TICKS - 12.0F), 0.0F, 1.0F);
		float returnAge = state.dissolveAge + partialTick;
		float visibility = state.active ? smoothStep(formation) : 1.0F - smoothStep(Mth.clamp(returnAge / DISSOLVE_TICKS, 0.0F, 1.0F));
		boolean renderFlow = state.active && age > 0.0F && age < FORM_TICKS + 9.0F;
		boolean renderReturnFlow = !state.active && returnAge > 0.0F && returnAge < RETURN_FLOW_END;
		boolean renderPlatform = visibility > 0.01F;
		if (!renderFlow && !renderReturnFlow && !renderPlatform) {
			return;
		}
		Vec3 position = player.getPosition(partialTick);
		float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
		poseStack.pushPose();
		poseStack.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		if (renderFlow) {
			renderGourdFlow(buffer, matrix, bodyYaw, age, false);
		}
		if (renderReturnFlow) {
			renderGourdFlow(buffer, matrix, bodyYaw, returnAge, true);
		}
		if (renderPlatform) {
			renderPlatform(buffer, matrix, bodyYaw, player.tickCount + partialTick, visibility, state);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		poseStack.popPose();
	}

	private static void renderGourdFlow(BufferBuilder buffer, Matrix4f matrix, float bodyYaw, float age, boolean returning) {
		Vec3 forward = Vec3.directionFromRotation(0.0F, bodyYaw).multiply(1.0D, 0.0D, 1.0D).normalize();
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		for (int i = 0; i < FLOW_GRAINS; i++) {
			float delay = (i % 23) * 0.32F + (i / 23) * 0.9F;
			float progress = (age - delay) / FLOW_TRAVEL_TICKS;
			if (progress <= 0.0F || progress >= 1.0F) {
				continue;
			}
			float eased = returning ? 1.0F - smoothStep(progress) : smoothStep(progress);
			Vec3 point = flowPath(forward, right, eased);
			double turbulence = 0.035D + Math.sin(progress * Math.PI) * 0.035D;
			double phase = i * 2.399963D + age * (returning ? -0.24D : 0.24D);
			point = point.add(right.scale(Math.cos(phase) * turbulence)).add(forward.scale(Math.sin(phase) * turbulence * 0.55D)).add(0.0D, Math.sin(phase * 1.37D) * turbulence, 0.0D);
			float size = grainSize(i * 17 + 5);
			addBox(buffer, matrix, point, size, size * 0.7F, color(i, 1.0F));
		}
	}

	private static Vec3 flowPath(Vec3 forward, Vec3 right, float progress) {
		Vec3 source = forward.scale(-0.48D).add(right.scale(0.12D)).add(0.0D, 1.58D, 0.0D);
		Vec3 exit = forward.scale(-0.76D).add(0.0D, 2.08D, 0.0D);
		if (progress < 0.2F) {
			float rise = smoothStep(progress / 0.2F);
			return source.lerp(exit, rise).add(right.scale(Math.sin(rise * Math.PI) * 0.12D));
		}

		float orbitProgress = Mth.clamp((progress - 0.2F) / 0.52F, 0.0F, 1.0F);
		double orbitAngle = -Math.PI * 0.5D + orbitProgress * Math.PI * 2.3D;
		double radius = 0.76D + Math.sin(orbitProgress * Math.PI) * 0.28D;
		double orbitY = Mth.lerp(orbitProgress, 2.08D, 0.42D);
		Vec3 orbit = right.scale(Math.cos(orbitAngle) * radius).add(forward.scale(Math.sin(orbitAngle) * radius)).add(0.0D, orbitY, 0.0D);
		if (progress < 0.72F) {
			return orbit;
		}

		float gather = smoothStep((progress - 0.72F) / 0.28F);
		double gatherAngle = orbitAngle + gather * Math.PI * 1.35D;
		double gatherRadius = radius * (1.0D - gather);
		double gatherY = Mth.lerp(gather, orbitY, -0.12D) + Math.sin(gather * Math.PI) * 0.16D;
		return right.scale(Math.cos(gatherAngle) * gatherRadius).add(forward.scale(Math.sin(gatherAngle) * gatherRadius)).add(0.0D, gatherY, 0.0D);
	}

	private static void renderPlatform(BufferBuilder buffer, Matrix4f matrix, float bodyYaw, float time, float visibility, ClientState state) {
		if (visibility <= 0.01F) {
			return;
		}
		float yaw = bodyYaw * Mth.DEG_TO_RAD;
		float sin = Mth.sin(yaw);
		float cos = Mth.cos(yaw);
		float dissolve = state.active ? 0.0F : smoothStep(Mth.clamp(state.dissolveAge / (float) DISSOLVE_TICKS, 0.0F, 1.0F));
		int visibleGrains = Mth.clamp((int) (PLATFORM_GRAINS * visibility * 1.15F), 0, PLATFORM_GRAINS);
		for (int i = 0; i < visibleGrains; i++) {
			float radial = Mth.sqrt((i + 0.5F) / PLATFORM_GRAINS);
			float angle = i * 2.399963F + hash(i * 37) * 0.28F;
			float edgeNoise = 0.86F + hash(i * 53 + 7) * 0.25F;
			float contraction = 1.0F - dissolve * 0.48F;
			float localX = Mth.cos(angle) * radial * 0.92F * edgeNoise * contraction;
			float localZ = Mth.sin(angle) * radial * 0.72F * edgeNoise * contraction;
			float x = localX * cos - localZ * sin;
			float z = localX * sin + localZ * cos;
			float wave = Mth.sin(time * 0.12F + angle * 2.0F + radial * 4.0F) * (0.018F + radial * 0.025F);
			float y = -0.16F - radial * 0.09F + wave + (hash(i * 71 + 3) - 0.5F) * 0.07F;
			y = Mth.lerp(dissolve, y, -0.12F + wave * 0.35F);
			float size = grainSize(i * 17 + 5) * (0.72F + visibility * 0.28F);
			addBox(buffer, matrix, new Vec3(x, y, z), size, size * 0.7F, color(i, visibility));
		}
		for (int i = 0; i < 18; i++) {
			float angle = i * 2.399963F + time * (0.015F + (i % 3) * 0.004F);
			float radius = 0.72F + hash(i * 19) * 0.25F;
			float x = Mth.cos(angle) * radius;
			float z = Mth.sin(angle) * radius * 0.78F;
			float y = -0.13F + Mth.sin(time * 0.09F + i) * 0.08F + hash(i * 31) * 0.08F;
			float size = grainSize(i * 23 - 1);
			addBox(buffer, matrix, new Vec3(x, y, z), size, size * 0.7F, color(i + 41, visibility * 0.82F));
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

	private static final class ClientState {
		private int age;
		private int life;
		private int dissolveAge;
		private boolean active = true;

		private ClientState(int life) {
			this.life = life;
		}
	}
}
