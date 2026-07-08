package net.mcreator.naruto.client.renderer;

import org.joml.Matrix4f;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import net.mcreator.naruto.entity.GreatFireballEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class GreatFireballRenderer extends EntityRenderer<GreatFireballEntity> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("naruto", "textures/particle/particle_great_fireball.png");

	public GreatFireballRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(GreatFireballEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		poseStack.pushPose();
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		float age = entity.tickCount + partialTick;
		float pulse = 1.0F + 0.06F * Mth.sin(age * 0.45F);
		float size = entity.getVisualScale() * pulse;
		poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(age * 5.5F));
		Matrix4f matrix = poseStack.last().pose();
		VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
		int color = 0xFFFFFFFF;
		int frame = ((int) age / 2) & 3;
		float v0 = frame * 0.25F;
		float v1 = v0 + 0.25F;
		vertex(buffer, matrix, -size, -size, 0.0F, v1, color);
		vertex(buffer, matrix, size, -size, 1.0F, v1, color);
		vertex(buffer, matrix, size, size, 1.0F, v0, color);
		vertex(buffer, matrix, -size, size, 0.0F, v0, color);
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}

	private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float u, float v, int color) {
		buffer.addVertex(matrix, x, y, 0.0F).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setUv2(LightTexture.FULL_BRIGHT & 0xFFFF, LightTexture.FULL_BRIGHT >> 16).setNormal(0.0F, 1.0F, 0.0F);
	}

	@Override
	public ResourceLocation getTextureLocation(GreatFireballEntity entity) {
		return TEXTURE;
	}
}
