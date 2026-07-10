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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

@EventBusSubscriber(value = Dist.CLIENT)
public class BladeOfWindClientEffects {
	private static final List<WindBlade> BLADES = new ArrayList<>();

	public static void addSlash(Vec3 origin, Vec3 direction, float range, float width, float roll, int ticks) {
		if (origin == null || direction == null || direction.lengthSqr() < 0.001D) {
			return;
		}
		BLADES.add(new WindBlade(origin, direction.normalize(), Math.max(1.0F, range), Math.max(0.1F, width), roll, Math.max(5, ticks)));
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<WindBlade> iterator = BLADES.iterator();
		while (iterator.hasNext()) {
			WindBlade blade = iterator.next();
			blade.ticks--;
			if (blade.ticks <= 0) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void renderWindBlades(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BLADES.isEmpty()) {
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
		for (WindBlade blade : BLADES) {
			renderBlade(poseStack, camera, blade, partialTick);
		}
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableBlend();
	}

	private static void renderBlade(PoseStack poseStack, Vec3 camera, WindBlade blade, float partialTick) {
		float age = blade.totalTicks - blade.ticks + partialTick;
		float life = Mth.clamp(age / blade.totalTicks, 0.0F, 1.0F);
		float fade = 1.0F - life;
		float draw = easeOut(Mth.clamp(life * 1.75F, 0.0F, 1.0F));
		Basis basis = Basis.fromForward(blade.direction, blade.roll);
		Vec3 renderOrigin = blade.origin.add(blade.direction.scale(life * blade.range * 0.16F)).subtract(camera);
		poseStack.pushPose();
		poseStack.translate(renderOrigin.x, renderOrigin.y, renderOrigin.z);
		NarutoWheelShaders.prepareWindGustShader((blade.seed + age) * 0.08F);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		renderMainRibbons(buffer, matrix, basis, blade, age, fade, draw);
		renderEdgeWisps(buffer, matrix, basis, blade, age, fade, draw);
		renderSoftCrescent(buffer, matrix, basis, blade, fade, draw);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		poseStack.popPose();
	}

	private static void renderMainRibbons(BufferBuilder buffer, Matrix4f matrix, Basis basis, WindBlade blade, float age, float fade, float draw) {
		int ribbons = 4;
		int segments = 14;
		for (int ribbon = 0; ribbon < ribbons; ribbon++) {
			float phase = ribbon * Mth.TWO_PI / ribbons + blade.seed * 0.07F;
			float offset = (ribbon - 1.5F) * blade.width * 0.12F;
			for (int i = 0; i < segments; i++) {
				float t0 = i / (float) segments;
				float t1 = (i + 1) / (float) segments;
				if (t0 > draw) {
					continue;
				}
				t1 = Math.min(t1, draw);
				Vec3 a = bladePoint(basis, blade, age, phase, offset, t0);
				Vec3 b = bladePoint(basis, blade, age, phase, offset, t1);
				Vec3 side0 = ribbonSide(basis, blade, t0, phase, 0.11F);
				Vec3 side1 = ribbonSide(basis, blade, t1, phase, 0.11F);
				int alpha = Mth.floor(142.0F * fade * strength(t0));
				int color = (Mth.clamp(alpha, 0, 150) << 24) | (ribbon == 0 ? 0xEEFFF8 : 0xDFFFF1);
				addQuad(buffer, matrix, a.add(side0), a.subtract(side0), b.subtract(side1), b.add(side1), color);
			}
		}
	}

	private static void renderEdgeWisps(BufferBuilder buffer, Matrix4f matrix, Basis basis, WindBlade blade, float age, float fade, float draw) {
		int segments = 10;
		for (int side = 0; side < 2; side++) {
			float sign = side == 0 ? 1.0F : -1.0F;
			for (int i = 0; i < segments; i++) {
				float t0 = i / (float) segments;
				float t1 = (i + 1) / (float) segments;
				if (t0 > draw) {
					continue;
				}
				t1 = Math.min(t1, draw);
				Vec3 a = edgePoint(basis, blade, age, t0, sign);
				Vec3 b = edgePoint(basis, blade, age, t1, sign);
				Vec3 sideAxis = basis.right.scale(blade.width * 0.045F * strength(t0));
				int alpha = Mth.floor(105.0F * fade * strength(t0));
				int color = (Mth.clamp(alpha, 0, 115) << 24) | 0xF7FFF9;
				addQuad(buffer, matrix, a.add(sideAxis), a.subtract(sideAxis), b.subtract(sideAxis), b.add(sideAxis), color);
			}
		}
	}

	private static void renderSoftCrescent(BufferBuilder buffer, Matrix4f matrix, Basis basis, WindBlade blade, float fade, float draw) {
		int segments = 18;
		for (int i = 0; i < segments; i++) {
			float t0 = i / (float) segments;
			float t1 = (i + 1) / (float) segments;
			if (t0 > draw) {
				continue;
			}
			t1 = Math.min(t1, draw);
			Vec3 a = crescentPoint(basis, blade, t0);
			Vec3 b = crescentPoint(basis, blade, t1);
			Vec3 side0 = basis.right.scale(blade.width * 0.2F * strength(t0));
			Vec3 side1 = basis.right.scale(blade.width * 0.2F * strength(t1));
			int alpha = Mth.floor(58.0F * fade * strength(t0));
			int color = (Mth.clamp(alpha, 0, 65) << 24) | 0xDFFFF1;
			addQuad(buffer, matrix, a.add(side0), a, b, b.add(side1), color);
		}
	}

	private static Vec3 bladePoint(Basis basis, WindBlade blade, float age, float phase, float offset, float t) {
		float forward = blade.range * (0.06F + t * 0.82F);
		float vertical = (t - 0.5F) * blade.width * 2.15F;
		float curve = Mth.sin(t * Mth.PI) * blade.width * 0.34F;
		float wobble = Mth.sin(age * 0.7F + t * 8.0F + phase) * blade.width * 0.07F;
		return basis.forward.scale(forward).add(basis.up.scale(vertical + curve)).add(basis.right.scale(offset + wobble));
	}

	private static Vec3 edgePoint(Basis basis, WindBlade blade, float age, float t, float sign) {
		float forward = blade.range * (0.1F + t * 0.78F);
		float vertical = (t - 0.5F) * blade.width * 2.35F + sign * Mth.sin(t * Mth.PI) * blade.width * 0.25F;
		float side = sign * blade.width * (0.28F + Mth.sin(age * 0.45F + t * 7.0F) * 0.05F);
		return basis.forward.scale(forward).add(basis.up.scale(vertical)).add(basis.right.scale(side));
	}

	private static Vec3 crescentPoint(Basis basis, WindBlade blade, float t) {
		float forward = blade.range * (0.03F + t * 0.78F);
		float vertical = (t - 0.5F) * blade.width * 2.55F;
		float arc = Mth.sin(t * Mth.PI) * blade.width * 0.58F;
		return basis.forward.scale(forward + arc * 0.18F).add(basis.up.scale(vertical + arc));
	}

	private static Vec3 ribbonSide(Basis basis, WindBlade blade, float t, float phase, float scale) {
		float angle = phase + t * 4.6F;
		return basis.right.scale(Mth.cos(angle) * blade.width * scale).add(basis.up.scale(Mth.sin(angle) * blade.width * scale * 0.16F));
	}

	private static float strength(float t) {
		float taper = 1.0F - Math.abs(t - 0.5F) * 1.85F;
		return Mth.clamp(taper, 0.08F, 1.0F);
	}

	private static float easeOut(float value) {
		value = Mth.clamp(value, 0.0F, 1.0F);
		float inverse = 1.0F - value;
		return 1.0F - inverse * inverse * inverse;
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

	private static final class WindBlade {
		private final Vec3 origin;
		private final Vec3 direction;
		private final float range;
		private final float width;
		private final float roll;
		private final int totalTicks;
		private final int seed;
		private int ticks;

		private WindBlade(Vec3 origin, Vec3 direction, float range, float width, float roll, int ticks) {
			this.origin = origin;
			this.direction = direction;
			this.range = range;
			this.width = width;
			this.roll = roll;
			this.ticks = ticks;
			this.totalTicks = ticks;
			this.seed = Math.abs(origin.hashCode() ^ direction.hashCode()) % 10000;
		}
	}

	private record Basis(Vec3 right, Vec3 up, Vec3 forward) {
		private static Basis fromForward(Vec3 forward, float roll) {
			Vec3 normalizedForward = forward.normalize();
			Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(normalizedForward);
			if (right.lengthSqr() < 0.001D) {
				right = new Vec3(1.0D, 0.0D, 0.0D);
			}
			right = right.normalize();
			Vec3 up = normalizedForward.cross(right).normalize();
			float cos = Mth.cos(roll);
			float sin = Mth.sin(roll);
			Vec3 rolledRight = right.scale(cos).add(up.scale(sin)).normalize();
			Vec3 rolledUp = up.scale(cos).subtract(right.scale(sin)).normalize();
			return new Basis(rolledRight, rolledUp, normalizedForward);
		}
	}
}
