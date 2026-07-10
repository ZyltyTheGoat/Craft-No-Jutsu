package net.mcreator.naruto.client.renderer;

import org.joml.Matrix4f;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.entity.ElementalBulletEntity;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;

public class ElementalBulletRenderer extends EntityRenderer<ElementalBulletEntity> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("naruto", "textures/particle/particle_great_fireball.png");

	public ElementalBulletRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(ElementalBulletEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		Vec3 velocity = entity.getDeltaMovement();
		if (velocity.lengthSqr() < 0.0001D) {
			return;
		}
		Vec3 forward = velocity.normalize();
		Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
		if (right.lengthSqr() < 0.001D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		}
		right = right.normalize();
		Vec3 up = forward.cross(right).normalize();
		float age = entity.tickCount + partialTick;
		float scale = entity.getVisualScale();
		poseStack.pushPose();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		if (entity.getElement() == ElementalBulletEntity.ELEMENT_WATER) {
			renderWater(buffer, matrix, forward, right, up, age, scale);
		} else {
			renderWind(buffer, matrix, forward, right, up, age, scale);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}

	private static void renderWater(BufferBuilder buffer, Matrix4f matrix, Vec3 forward, Vec3 right, Vec3 up, float age, float scale) {
		float length = scale * 1.65F;
		float half = scale * (0.16F + 0.025F * Mth.sin(age * 0.8F));
		int core = 0x8A63B7D6;
		int shell = 0x4A2F7F99;
		Vec3 back = forward.scale(-length * 0.55F);
		Vec3 front = forward.scale(length * 0.45F);
		addQuad(buffer, matrix, back.add(right.scale(half)).add(up.scale(half)), back.subtract(right.scale(half)).add(up.scale(half)), front.subtract(right.scale(half * 0.62F)).add(up.scale(half * 0.62F)),
				front.add(right.scale(half * 0.62F)).add(up.scale(half * 0.62F)), core);
		addQuad(buffer, matrix, back.add(right.scale(half)).subtract(up.scale(half)), back.subtract(right.scale(half)).subtract(up.scale(half)), front.subtract(right.scale(half * 0.62F)).subtract(up.scale(half * 0.62F)),
				front.add(right.scale(half * 0.62F)).subtract(up.scale(half * 0.62F)), core);
		for (int i = 0; i < 2; i++) {
			float sign = i == 0 ? 1.0F : -1.0F;
			Vec3 side = right.scale(sign * half * 1.45F);
			addQuad(buffer, matrix, back.add(side).add(up.scale(half * 0.4F)), back.add(side).subtract(up.scale(half * 0.4F)), front.add(side.scale(0.4D)).subtract(up.scale(half * 0.22F)),
					front.add(side.scale(0.4D)).add(up.scale(half * 0.22F)), shell);
		}
	}

	private static void renderWind(BufferBuilder buffer, Matrix4f matrix, Vec3 forward, Vec3 right, Vec3 up, float age, float scale) {
		int ribbons = 3;
		float length = scale * 2.0F;
		for (int ribbon = 0; ribbon < ribbons; ribbon++) {
			float phase = ribbon * Mth.TWO_PI / ribbons + age * 0.36F;
			int color = ribbon == 0 ? 0x8EEFFFF8 : 0x6EDFFFF1;
			for (int i = 0; i < 7; i++) {
				float t0 = i / 7.0F;
				float t1 = (i + 1) / 7.0F;
				Vec3 p0 = windPoint(forward, right, up, phase, t0, length, scale);
				Vec3 p1 = windPoint(forward, right, up, phase, t1, length, scale);
				Vec3 side = up.scale(scale * 0.055F);
				addQuad(buffer, matrix, p0.add(side), p0.subtract(side), p1.subtract(side), p1.add(side), color);
			}
		}
	}

	private static Vec3 windPoint(Vec3 forward, Vec3 right, Vec3 up, float phase, float t, float length, float scale) {
		float distance = Mth.lerp(t, -length * 0.62F, length * 0.42F);
		float radius = scale * (0.16F + t * 0.18F);
		float angle = phase + t * 5.2F;
		return forward.scale(distance).add(right.scale(Mth.cos(angle) * radius)).add(up.scale(Mth.sin(angle) * radius));
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

	@Override
	public ResourceLocation getTextureLocation(ElementalBulletEntity entity) {
		return TEXTURE;
	}
}
