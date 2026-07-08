package net.mcreator.naruto.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import net.mcreator.naruto.entity.ShinobiCloneEntity;

public class ShinobiCloneRenderer extends HumanoidMobRenderer<ShinobiCloneEntity, HumanoidModel<ShinobiCloneEntity>> {
	private static final ResourceLocation FALLBACK_TEXTURE = ResourceLocation.fromNamespaceAndPath("naruto", "textures/entities/dummy.png");

	public ShinobiCloneRenderer(EntityRendererProvider.Context context) {
		super(context, new ShinobiCloneModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
		this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
		this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
	}

	@Override
	public ResourceLocation getTextureLocation(ShinobiCloneEntity entity) {
		if (Minecraft.getInstance().level != null && entity.getOwnerUUID() != null) {
			for (AbstractClientPlayer player : Minecraft.getInstance().level.players()) {
				if (player.getUUID().equals(entity.getOwnerUUID())) {
					return player.getSkin().texture();
				}
			}
		}
		return FALLBACK_TEXTURE;
	}

	private static class ShinobiCloneModel extends HumanoidModel<ShinobiCloneEntity> {
		private static final float ATTACK_ANIMATION_LENGTH = 8.0F;

		private ShinobiCloneModel(net.minecraft.client.model.geom.ModelPart root) {
			super(root);
		}

		@Override
		public void setupAnim(ShinobiCloneEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			int attackTicks = entity.getAttackAnimationTicks();
			if (attackTicks <= 0) {
				return;
			}
			float progress = 1.0F - attackTicks / ATTACK_ANIMATION_LENGTH;
			float swing = Mth.sin(progress * Mth.PI);
			this.rightArm.xRot = this.rightArm.xRot * 0.25F - 2.05F * swing;
			this.rightArm.yRot = -0.35F * swing;
			this.rightArm.zRot = 0.08F * swing;
			this.body.yRot = 0.18F * swing;
			this.leftArm.yRot += 0.18F * swing;
		}
	}
}
