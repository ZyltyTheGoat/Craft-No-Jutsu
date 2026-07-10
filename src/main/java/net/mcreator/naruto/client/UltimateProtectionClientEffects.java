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

import net.mcreator.naruto.network.UltimateProtectionPacket;

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
public final class UltimateProtectionClientEffects {
	private static final int FORM_TICKS = 8;
	private static final int RETURN_TICKS = 13;
	private static final int BARRIER_GRAINS = 680;
	private static final float DEFENDED_PLAYER_ALPHA_MULTIPLIER = 0.34F;
	private static final float GRAIN_BASE_SIZE = 0.028F;
	private static final float GRAIN_SIZE_IMPERFECTION = 0.017F;
	private static final int[] GRAIN_COLORS = {0xFF252729, 0xFF2b2d2e, 0xFF333436, 0xFF3b3d3e, 0xFF434547, 0xFF4b4d4f};
	private static final List<ProtectionEffect> EFFECTS = new ArrayList<>();

	private UltimateProtectionClientEffects() {
	}

	public static void addProtection(UUID playerId, byte protectionType, Vec3 direction, int holdTicks) {
		if (playerId == null) {
			return;
		}
		Vec3 safeDirection = direction == null || direction.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
		EFFECTS.add(new ProtectionEffect(playerId, protectionType, safeDirection, Math.max(1, holdTicks)));
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<ProtectionEffect> iterator = EFFECTS.iterator();
		while (iterator.hasNext()) {
			ProtectionEffect effect = iterator.next();
			effect.age++;
			if (effect.age > effect.totalTicks) {
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
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		boolean hasGeometry = false;
		for (ProtectionEffect effect : EFFECTS) {
			if (hasVisibleGrain(effect, partialTick) && minecraft.level.getPlayerByUUID(effect.playerId) != null) {
				hasGeometry = true;
				break;
			}
		}
		if (!hasGeometry) {
			return;
		}
		Vec3 camera = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.depthMask(true);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		for (ProtectionEffect effect : EFFECTS) {
			Player player = minecraft.level.getPlayerByUUID(effect.playerId);
			if (player != null) {
				renderEffect(buffer, matrix, camera, player, effect, partialTick);
			}
		}
		MeshData mesh = buffer.build();
		if (mesh != null) {
			BufferUploader.drawWithShader(mesh);
		}
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static boolean hasVisibleGrain(ProtectionEffect effect, float partialTick) {
		float age = effect.age + partialTick;
		float returnStart = FORM_TICKS + effect.holdTicks;
		for (int i = 0; i < BARRIER_GRAINS; i++) {
			float delay = (i % 23) * 0.035F + hash(i * 43 + 5) * 0.36F;
			if (age < FORM_TICKS && age > delay) {
				return true;
			}
			if (age >= FORM_TICKS && age < returnStart) {
				return true;
			}
			if (age >= returnStart && (age - returnStart - delay) / RETURN_TICKS < 1.0F) {
				return true;
			}
		}
		return false;
	}

	private static void renderEffect(BufferBuilder buffer, Matrix4f matrix, Vec3 camera, Player player, ProtectionEffect effect, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		float localAlphaMultiplier = minecraft.player != null && minecraft.player.getUUID().equals(effect.playerId) ? DEFENDED_PLAYER_ALPHA_MULTIPLIER : 1.0F;
		float age = effect.age + partialTick;
		Vec3 playerPosition = player.getPosition(partialTick);
		float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
		Vec3 bodyForward = Vec3.directionFromRotation(0.0F, bodyYaw).multiply(1.0D, 0.0D, 1.0D).normalize();
		Vec3 bodyRight = new Vec3(-bodyForward.z, 0.0D, bodyForward.x);
		Vec3 source = bodyForward.scale(-0.48D).add(bodyRight.scale(0.12D)).add(0.0D, 1.58D, 0.0D);
		BarrierBasis barrier = barrierBasis(effect);
		float returnStart = FORM_TICKS + effect.holdTicks;

		for (int i = 0; i < BARRIER_GRAINS; i++) {
			float delay = (i % 23) * 0.035F + hash(i * 43 + 5) * 0.36F;
			Vec3 finalPoint = barrierPoint(effect, barrier, i, player.getBbHeight());
			Vec3 point;
			float alpha = 1.0F;
			if (age < FORM_TICKS) {
				float progress = Mth.clamp((age - delay) / Math.max(1.0F, FORM_TICKS - 1.2F), 0.0F, 1.0F);
				if (progress <= 0.0F) {
					continue;
				}
				point = flowPath(source, bodyForward, bodyRight, finalPoint, smoothStep(progress), i, age);
				alpha = Mth.clamp(progress * 1.8F, 0.0F, 1.0F);
			} else if (age < returnStart) {
				point = finalPoint;
			} else {
				float returnProgress = Mth.clamp((age - returnStart - delay) / RETURN_TICKS, 0.0F, 1.0F);
				if (returnProgress >= 1.0F) {
					continue;
				}
				point = flowPath(source, bodyForward, bodyRight, finalPoint, 1.0F - smoothStep(returnProgress), i, age);
				alpha = 1.0F - smoothStep(Mth.clamp((returnProgress - 0.72F) / 0.28F, 0.0F, 1.0F));
			}
			float shimmer = 0.92F + Mth.sin(age * 0.19F + i * 0.37F) * 0.08F;
			float size = grainSize(i * 17 + 5) * shimmer;
			addBox(buffer, matrix, playerPosition.add(point).subtract(camera), size, size * (0.68F + hash(i * 31 + 7) * 0.3F), color(i, alpha * localAlphaMultiplier));
		}
	}

	private static BarrierBasis barrierBasis(ProtectionEffect effect) {
		if (effect.protectionType == UltimateProtectionPacket.FEET) {
			return new BarrierBasis(new Vec3(0.0D, 1.0D, 0.0D), new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D));
		}
		Vec3 normal = effect.direction;
		Vec3 upHint = Math.abs(normal.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 horizontal = upHint.cross(normal).normalize();
		Vec3 vertical = normal.cross(horizontal).normalize();
		return new BarrierBasis(normal, horizontal, vertical);
	}

	private static Vec3 barrierPoint(ProtectionEffect effect, BarrierBasis basis, int index, float playerHeight) {
		float radial = Mth.sqrt((index + 0.5F) / BARRIER_GRAINS);
		float angle = index * 2.399963F + (hash(index * 37 + 11) - 0.5F) * 0.22F;
		float edgeNoise = 0.9F + hash(index * 53 + 3) * 0.18F;
		float localX = Mth.cos(angle) * radial * edgeNoise;
		float localY = Mth.sin(angle) * radial * edgeNoise;
		float roughness = (hash(index * 71 + 9) - 0.5F) * 0.07F;
		if (effect.protectionType == UltimateProtectionPacket.FEET) {
			Vec3 center = new Vec3(0.0D, 0.035D, 0.0D);
			double bowl = (1.0D - radial * radial) * 0.13D + roughness;
			return center.add(basis.horizontal.scale(localX * 0.78D)).add(basis.vertical.scale(localY * 0.72D)).add(basis.normal.scale(bowl));
		}
		Vec3 center = basis.normal.scale(0.74D).add(0.0D, playerHeight * 0.52D, 0.0D);
		double curve = (1.0D - radial * radial) * 0.15D + roughness;
		return center.add(basis.horizontal.scale(localX * 0.72D)).add(basis.vertical.scale(localY * 0.92D)).add(basis.normal.scale(curve));
	}

	private static Vec3 flowPath(Vec3 source, Vec3 forward, Vec3 right, Vec3 destination, float progress, int seed, float age) {
		Vec3 exit = forward.scale(-0.76D).add(0.0D, 2.08D, 0.0D);
		Vec3 point;
		if (progress < 0.2F) {
			float rise = smoothStep(progress / 0.2F);
			point = source.lerp(exit, rise).add(right.scale(Math.sin(rise * Math.PI) * 0.12D));
		} else {
			float orbitProgress = Mth.clamp((progress - 0.2F) / 0.5F, 0.0F, 1.0F);
			double orbitAngle = -Math.PI * 0.5D + orbitProgress * Math.PI * 2.3D;
			double radius = 0.76D + Math.sin(orbitProgress * Math.PI) * 0.24D;
			double orbitY = Mth.lerp(orbitProgress, 2.08D, 1.35D);
			Vec3 orbit = right.scale(Math.cos(orbitAngle) * radius).add(forward.scale(Math.sin(orbitAngle) * radius)).add(0.0D, orbitY, 0.0D);
			if (progress < 0.7F) {
				point = orbit;
			} else {
				float gather = smoothStep((progress - 0.7F) / 0.3F);
				point = orbit.lerp(destination, gather).add(0.0D, Math.sin(gather * Math.PI) * 0.16D, 0.0D);
			}
		}
		double turbulence = Math.sin(progress * Math.PI) * (0.025D + hash(seed * 29 + 4) * 0.04D);
		double phase = seed * 2.399963D + age * 0.34D;
		return point.add(right.scale(Math.cos(phase) * turbulence)).add(forward.scale(Math.sin(phase) * turbulence * 0.55D));
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

	private record BarrierBasis(Vec3 normal, Vec3 horizontal, Vec3 vertical) {
	}

	private static final class ProtectionEffect {
		private final UUID playerId;
		private final byte protectionType;
		private final Vec3 direction;
		private final int holdTicks;
		private final int totalTicks;
		private int age;

		private ProtectionEffect(UUID playerId, byte protectionType, Vec3 direction, int holdTicks) {
			this.playerId = playerId;
			this.protectionType = protectionType;
			this.direction = direction;
			this.holdTicks = holdTicks;
			this.totalTicks = FORM_TICKS + holdTicks + RETURN_TICKS + 2;
		}
	}
}
