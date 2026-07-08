package net.mcreator.naruto.client;

import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

@EventBusSubscriber(Dist.CLIENT)
public class BodyFlickerAfterimageRenderer {
	private static final Map<UUID, Afterimage> AFTERIMAGES = new HashMap<>();
	private static final int LIFE_TICKS = 25;

	public static void record(Player player) {
		if (player == null) {
			return;
		}
		AFTERIMAGES.put(player.getUUID(), new Afterimage(player.getYRot(), player.getXRot(), player.tickCount));
	}

	@SubscribeEvent
	public static void renderAfterimage(RenderPlayerEvent.Pre event) {
		if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
			return;
		}
		Afterimage afterimage = AFTERIMAGES.get(player.getUUID());
		if (afterimage == null) {
			return;
		}
		int age = player.tickCount - afterimage.createdAt;
		if (age < 0 || age > LIFE_TICKS) {
			AFTERIMAGES.remove(player.getUUID());
			return;
		}
		float baseAlpha = 0.58F * (1.0F - age / (float) LIFE_TICKS);
		PlayerRenderer renderer = event.getRenderer();
		PoseStack poseStack = event.getPoseStack();
		renderGhost(player, renderer, poseStack, event, afterimage, 1.35D, baseAlpha);
		renderGhost(player, renderer, poseStack, event, afterimage, 2.35D, baseAlpha * 0.48F);
	}

	private static void renderGhost(AbstractClientPlayer player, PlayerRenderer renderer, PoseStack poseStack, RenderPlayerEvent event, Afterimage afterimage, double distanceBehind, float alpha) {
		poseStack.pushPose();
		Vec3 facing = Vec3.directionFromRotation(0.0F, player.getYRot()).multiply(1.0D, 0.0D, 1.0D);
		if (facing.lengthSqr() < 0.001D) {
			facing = Vec3.directionFromRotation(0.0F, afterimage.yRot()).multiply(1.0D, 0.0D, 1.0D);
		}
		facing = facing.normalize();
		poseStack.translate(-facing.x * distanceBehind, 0.0D, -facing.z * distanceBehind);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - afterimage.yRot));
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		renderer.getModel().prepareMobModel(player, 0.0F, 0.0F, event.getPartialTick());
		renderer.getModel().setupAnim(player, 0.0F, 0.0F, player.tickCount + event.getPartialTick(), 0.0F, afterimage.xRot);
		ResourceLocation texture = player.getSkin().texture();
		VertexConsumer vertexConsumer = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(texture));
		int color = ((int) (alpha * 255.0F) << 24) | 0xB8E8FF;
		renderer.getModel().renderToBuffer(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
		poseStack.popPose();
	}

	private record Afterimage(float yRot, float xRot, int createdAt) {
	}
}
