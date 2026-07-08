package net.mcreator.naruto.client;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.client.gui.NarutoWheelShaders;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

@EventBusSubscriber(value = Dist.CLIENT)
public class WaterPrisonClientEffects {
	private static final Map<Integer, BubbleState> ACTIVE = new HashMap<>();
	private static final int LATITUDE_STEPS = 12;
	private static final int LONGITUDE_STEPS = 24;

	public static void setActive(UUID casterId, int targetId, boolean active) {
		if (targetId < 0) {
			return;
		}
		if (active) {
			ACTIVE.put(targetId, new BubbleState(casterId, targetId));
		} else {
			ACTIVE.remove(targetId);
		}
	}

	@SubscribeEvent
	public static void renderWaterPrisons(RenderLevelStageEvent event) {
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
		Iterator<Map.Entry<Integer, BubbleState>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			BubbleState state = iterator.next().getValue();
			Entity entity = minecraft.level.getEntity(state.targetId);
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				iterator.remove();
				continue;
			}
			renderBubble(poseStack, camera, living, state, partialTick);
		}
	}

	private static void renderBubble(PoseStack poseStack, Vec3 camera, LivingEntity target, BubbleState state, float partialTick) {
		double x = Mth.lerp(partialTick, target.xOld, target.getX());
		double y = Mth.lerp(partialTick, target.yOld, target.getY());
		double z = Mth.lerp(partialTick, target.zOld, target.getZ());
		float height = target.getBbHeight();
		float radius = Math.max(1.05F, Math.max(target.getBbWidth() * 1.15F, height * 0.55F) + 0.32F);
		double centerY = y + height * 0.5D;
		float time = (target.tickCount + partialTick + state.seed) * 0.05F;
		poseStack.pushPose();
		poseStack.translate(x - camera.x, centerY - camera.y, z - camera.z);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		NarutoWheelShaders.prepareWaterPrisonSphereShader(time);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		int color = 0x7A45D9FF;
		for (int lat = 0; lat < LATITUDE_STEPS; lat++) {
			float theta1 = (float) Math.PI * lat / LATITUDE_STEPS;
			float theta2 = (float) Math.PI * (lat + 1) / LATITUDE_STEPS;
			for (int lon = 0; lon < LONGITUDE_STEPS; lon++) {
				float phi1 = Mth.TWO_PI * lon / LONGITUDE_STEPS;
				float phi2 = Mth.TWO_PI * (lon + 1) / LONGITUDE_STEPS;
				addSphereVertex(buffer, matrix, theta1, phi1, radius, color);
				addSphereVertex(buffer, matrix, theta2, phi1, radius, color);
				addSphereVertex(buffer, matrix, theta2, phi2, radius, color);
				addSphereVertex(buffer, matrix, theta1, phi1, radius, color);
				addSphereVertex(buffer, matrix, theta2, phi2, radius, color);
				addSphereVertex(buffer, matrix, theta1, phi2, radius, color);
			}
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableBlend();
		poseStack.popPose();
	}

	private static void addSphereVertex(BufferBuilder buffer, Matrix4f matrix, float theta, float phi, float radius, int color) {
		float sinTheta = Mth.sin(theta);
		float x = radius * sinTheta * Mth.cos(phi);
		float y = radius * Mth.cos(theta);
		float z = radius * sinTheta * Mth.sin(phi);
		buffer.addVertex(matrix, x, y, z).setColor(color);
	}

	private static final class BubbleState {
		private final UUID casterId;
		private final int targetId;
		private final int seed;

		private BubbleState(UUID casterId, int targetId) {
			this.casterId = casterId;
			this.targetId = targetId;
			this.seed = Math.abs((casterId == null ? 0 : casterId.hashCode()) ^ targetId) % 5000;
		}
	}
}
