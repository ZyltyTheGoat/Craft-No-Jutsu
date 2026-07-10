package net.mcreator.naruto.client;

import org.joml.Matrix4f;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.util.Mth;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.PlayerModel;

import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.DojutsuEyeConfigStore;
import net.mcreator.naruto.NarutoMod;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

@EventBusSubscriber(modid = NarutoMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class DojutsuEyeClientRegistration {
	@SubscribeEvent
	public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
		for (PlayerSkin.Model skin : event.getSkins()) {
			PlayerRenderer renderer = event.getSkin(skin);
			if (renderer != null) {
				renderer.addLayer(new DojutsuEyeLayer(renderer));
			}
		}
	}

	private static class DojutsuEyeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
		private DojutsuEyeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
			super(parent);
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
			NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
			int color = getActiveEyeColor(vars);
			if (color == 0 || player.isInvisible()) {
				return;
			}
			DojutsuEyeConfigStore.Config config = DojutsuEyeConfigStore.get(player.getId());
			float eyeWidth = (float) Mth.clamp(config.width(), 1.0D, 3.0D);
			float eyeHeight = (float) Mth.clamp(config.height(), 1.0D, 3.0D);
			float spacing = (float) Mth.clamp(config.spacing(), 1.0D, 7.0D);
			float yOffset = (float) Mth.clamp(config.yOffset(), -7.0D, -1.0D);
			float zOffset = (float) Mth.clamp(config.zOffset(), -4.35D, -3.75D);
			poseStack.pushPose();
			this.getParentModel().head.translateAndRotate(poseStack);
			poseStack.scale(0.0625F, 0.0625F, 0.0625F);
			Matrix4f matrix = poseStack.last().pose();
			VertexConsumer buffer = bufferSource.getBuffer(RenderType.lightning());
			renderEye(buffer, matrix, -spacing * 0.5F, yOffset, zOffset, eyeWidth, eyeHeight, color);
			renderEye(buffer, matrix, spacing * 0.5F, yOffset, zOffset, eyeWidth, eyeHeight, color);
			poseStack.popPose();
		}

		private void renderEye(VertexConsumer buffer, Matrix4f matrix, float centerX, float centerY, float z, float width, float height, int color) {
			float x0 = centerX - width * 0.5F;
			float x1 = centerX + width * 0.5F;
			float y0 = centerY - height * 0.5F;
			float y1 = centerY + height * 0.5F;
			addVertex(buffer, matrix, x0, y0, z, color);
			addVertex(buffer, matrix, x0, y1, z, color);
			addVertex(buffer, matrix, x1, y1, z, color);
			addVertex(buffer, matrix, x1, y0, z, color);
			int highlight = (0xB8 << 24) | 0xFFFFFF;
			float inset = Math.min(width, height) * 0.2F;
			addVertex(buffer, matrix, x0 + inset, y0 + inset, z - 0.01F, highlight);
			addVertex(buffer, matrix, x0 + inset, y0 + inset * 1.8F, z - 0.01F, highlight);
			addVertex(buffer, matrix, x1 - inset, y0 + inset * 1.8F, z - 0.01F, highlight);
			addVertex(buffer, matrix, x1 - inset, y0 + inset, z - 0.01F, highlight);
		}

		private void addVertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, int color) {
			buffer.addVertex(matrix, x, y, z).setColor(color);
		}

		private int getActiveEyeColor(NarutoModVariables.PlayerVariables vars) {
			String toggles = (vars.activeToggleJutsu == null ? "" : vars.activeToggleJutsu).toLowerCase();
			if (!hasActiveDojutsu(toggles)) {
				return 0;
			}
			String active = (toggles + "," + (vars.activeDojutsu == null ? "" : vars.activeDojutsu)).toLowerCase();
			if (active.contains("byakugan")) {
				return 0xEAEAF7FF;
			}
			if (active.contains("rinnegan")) {
				return 0xEABF7CFF;
			}
			if (active.contains("tenseigan")) {
				return 0xEA70E8FF;
			}
			if (active.contains("sharingan")) {
				return 0xEAE01818;
			}
			return 0xEAF0F0F0;
		}

		private boolean hasActiveDojutsu(String active) {
			return active.contains("sharingan") || active.contains("byakugan") || active.contains("rinnegan") || active.contains("tenseigan");
		}
	}
}
