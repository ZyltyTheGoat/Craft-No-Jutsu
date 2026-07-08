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

import net.mcreator.naruto.entity.PhoenixFlowerProjectileEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class PhoenixFlowerProjectileRenderer extends EntityRenderer<PhoenixFlowerProjectileEntity> {
	private static final ResourceLocation[] TEXTURES = new ResourceLocation[] {
			ResourceLocation.fromNamespaceAndPath("naruto", "textures/particle/phoenix_flower_particle_1.png"),
			ResourceLocation.fromNamespaceAndPath("naruto", "textures/particle/phoenix_flower_particle_2.png"),
			ResourceLocation.fromNamespaceAndPath("naruto", "textures/particle/phoenix_flower_particle_3.png"),
			ResourceLocation.fromNamespaceAndPath("naruto", "textures/particle/phoenix_flower_particle_4.png") };

	public PhoenixFlowerProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(PhoenixFlowerProjectileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		poseStack.pushPose();
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		float age = entity.tickCount + partialTick;
		float pulse = 1.0F + 0.08F * Mth.sin(age * 0.7F);
		float size = (float) entity.getVisualScale() * pulse;
		poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(age * 8.0F));
		Matrix4f matrix = poseStack.last().pose();
		ResourceLocation texture = TEXTURES[((int) age / 2) & 3];
		VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(texture));
		vertex(buffer, matrix, -size, -size, 0.0F, 1.0F);
		vertex(buffer, matrix, size, -size, 1.0F, 1.0F);
		vertex(buffer, matrix, size, size, 1.0F, 0.0F);
		vertex(buffer, matrix, -size, size, 0.0F, 0.0F);
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}

	private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float u, float v) {
		buffer.addVertex(matrix, x, y, 0.0F).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setUv2(LightTexture.FULL_BRIGHT & 0xFFFF, LightTexture.FULL_BRIGHT >> 16)
				.setNormal(0.0F, 1.0F, 0.0F);
	}

	@Override
	public ResourceLocation getTextureLocation(PhoenixFlowerProjectileEntity entity) {
		return TEXTURES[0];
	}
}
