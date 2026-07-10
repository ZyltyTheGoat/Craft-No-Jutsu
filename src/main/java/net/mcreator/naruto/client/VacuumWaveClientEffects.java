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
public class VacuumWaveClientEffects {
	private static final List<Slash> SLASHES = new ArrayList<>();

	public static void addSlash(Vec3 origin, Vec3 direction, float range, float width, float roll, int ticks) {
		if (origin == null || direction == null || direction.lengthSqr() < 0.001D) {
			return;
		}
		SLASHES.add(new Slash(origin, direction.normalize(), Math.max(1.0F, range), Math.max(0.1F, width), roll, Math.max(4, ticks)));
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<Slash> iterator = SLASHES.iterator();
		while (iterator.hasNext()) {
			Slash slash = iterator.next();
			slash.ticks--;
			if (slash.ticks <= 0) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void renderSlashes(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || SLASHES.isEmpty()) {
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
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		for (Slash slash : SLASHES) {
			renderSlash(buffer, matrix, camera, slash, partialTick);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static void renderSlash(BufferBuilder buffer, Matrix4f matrix, Vec3 camera, Slash slash, float partialTick) {
		float age = slash.totalTicks - slash.ticks + partialTick;
		float life = Mth.clamp(age / slash.totalTicks, 0.0F, 1.0F);
		float fade = 1.0F - life;
		float travel = Mth.lerp(life, 0.0F, slash.range);
		Basis basis = Basis.fromForward(slash.direction, slash.roll);
		Vec3 origin = slash.origin.add(slash.direction.scale(travel * 0.72F)).subtract(camera);
		int segments = 18;
		for (int i = 0; i < segments; i++) {
			float t0 = i / (float) segments;
			float t1 = (i + 1) / (float) segments;
			Vec3 a = slashPoint(basis, slash, t0);
			Vec3 b = slashPoint(basis, slash, t1);
			Vec3 side0 = slashSide(basis, slash, t0);
			Vec3 side1 = slashSide(basis, slash, t1);
			int alpha = Mth.floor(190.0F * fade * slashStrength(t0));
			int color = (Mth.clamp(alpha, 0, 190) << 24) | 0xF7FFF9;
			addVertex(buffer, matrix, origin.add(a).add(side0), color);
			addVertex(buffer, matrix, origin.add(a).subtract(side0), color);
			addVertex(buffer, matrix, origin.add(b).subtract(side1), color);
			addVertex(buffer, matrix, origin.add(b).add(side1), color);
		}
		renderTrailingSheen(buffer, matrix, origin, basis, slash, fade);
		renderCuttingEdge(buffer, matrix, origin, basis, slash, fade);
	}

	private static Vec3 slashPoint(Basis basis, Slash slash, float t) {
		float centered = t - 0.5F;
		float curve = Mth.sin((t - 0.08F) * Mth.PI) * slash.width * 0.34F;
		float vertical = centered * slash.width * 2.45F;
		float forward = slash.range * 0.08F + centered * slash.width * 0.52F + curve;
		return slash.direction.scale(forward).add(basis.up.scale(vertical));
	}

	private static Vec3 slashSide(Basis basis, Slash slash, float t) {
		return basis.right.scale(slash.width * 0.12F * slashStrength(t));
	}

	private static float slashStrength(float t) {
		float taper = 1.0F - Math.abs(t - 0.5F) * 1.9F;
		return Mth.clamp(taper, 0.08F, 1.0F);
	}

	private static void renderCuttingEdge(BufferBuilder buffer, Matrix4f matrix, Vec3 origin, Basis basis, Slash slash, float fade) {
		int alpha = Mth.floor(230.0F * fade);
		int color = (Mth.clamp(alpha, 0, 230) << 24) | 0xFFFFFF;
		Vec3 start = origin.add(slashPoint(basis, slash, 0.04F));
		Vec3 end = origin.add(slashPoint(basis, slash, 0.96F));
		Vec3 side = basis.right.scale(slash.width * 0.035F);
		addVertex(buffer, matrix, start.add(side), color);
		addVertex(buffer, matrix, start.subtract(side), color);
		addVertex(buffer, matrix, end.subtract(side), color);
		addVertex(buffer, matrix, end.add(side), color);
	}

	private static void renderTrailingSheen(BufferBuilder buffer, Matrix4f matrix, Vec3 origin, Basis basis, Slash slash, float fade) {
		int segments = 12;
		for (int i = 0; i < segments; i++) {
			float t0 = i / (float) segments;
			float t1 = (i + 1) / (float) segments;
			Vec3 a = slashPoint(basis, slash, t0).subtract(slash.direction.scale(slash.width * 0.28F));
			Vec3 b = slashPoint(basis, slash, t1).subtract(slash.direction.scale(slash.width * 0.28F));
			Vec3 side0 = basis.right.scale(slash.width * 0.22F * slashStrength(t0));
			Vec3 side1 = basis.right.scale(slash.width * 0.22F * slashStrength(t1));
			int alpha = Mth.floor(72.0F * fade * slashStrength(t0));
			int color = (Mth.clamp(alpha, 0, 72) << 24) | 0xDFFFF4;
			addVertex(buffer, matrix, origin.add(a).add(side0), color);
			addVertex(buffer, matrix, origin.add(a), color);
			addVertex(buffer, matrix, origin.add(b), color);
			addVertex(buffer, matrix, origin.add(b).add(side1), color);
		}
	}

	private static void addVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 pos, int color) {
		buffer.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).setColor(color);
	}

	private static final class Slash {
		private final Vec3 origin;
		private final Vec3 direction;
		private final float range;
		private final float width;
		private final float roll;
		private final int totalTicks;
		private int ticks;

		private Slash(Vec3 origin, Vec3 direction, float range, float width, float roll, int ticks) {
			this.origin = origin;
			this.direction = direction;
			this.range = range;
			this.width = width;
			this.roll = roll;
			this.ticks = ticks;
			this.totalTicks = ticks;
		}
	}

	private record Basis(Vec3 right, Vec3 up) {
		private static Basis fromForward(Vec3 forward, float roll) {
			Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
			if (right.lengthSqr() < 0.001D) {
				right = new Vec3(1.0D, 0.0D, 0.0D);
			}
			right = right.normalize();
			Vec3 up = forward.cross(right).normalize();
			float cos = Mth.cos(roll);
			float sin = Mth.sin(roll);
			Vec3 rolledRight = right.scale(cos).add(up.scale(sin)).normalize();
			Vec3 rolledUp = up.scale(cos).subtract(right.scale(sin)).normalize();
			return new Basis(rolledRight, rolledUp);
		}
	}
}
