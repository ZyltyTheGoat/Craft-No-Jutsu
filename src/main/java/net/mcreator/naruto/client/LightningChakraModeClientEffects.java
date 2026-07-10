package net.mcreator.naruto.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;

@EventBusSubscriber(value = Dist.CLIENT)
public class LightningChakraModeClientEffects {
	private static final Map<UUID, ClientState> ACTIVE = new HashMap<>();

	public static void setActive(UUID playerId, boolean active, int ticks) {
		if (playerId == null) {
			return;
		}
		if (active) {
			ACTIVE.put(playerId, new ClientState(Math.max(4, ticks)));
		} else {
			ACTIVE.remove(playerId);
		}
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<Map.Entry<UUID, ClientState>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			ClientState state = iterator.next().getValue();
			state.ticks--;
			if (state.ticks <= 0) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void renderFirstPerson(RenderHandEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (player == null || event.getHand() != InteractionHand.MAIN_HAND || !minecraft.options.getCameraType().isFirstPerson() || !ACTIVE.containsKey(player.getUUID())) {
			return;
		}
		PoseStack poseStack = event.getPoseStack();
		float partialTick = event.getPartialTick();
		float age = player.tickCount + partialTick;
		poseStack.pushPose();
		poseStack.translate(0.38D, -0.42D, -0.78D);
		poseStack.mulPose(Axis.XP.rotationDegrees(-22.0F + Mth.sin(age * 0.35F) * 3.0F));
		poseStack.mulPose(Axis.YP.rotationDegrees(8.0F));
		poseStack.scale(1.28F, 1.28F, 1.28F);
		ChidoriClientEffects.renderCloneLightning(poseStack, event.getMultiBufferSource(), age);
		poseStack.popPose();
	}

	@SubscribeEvent
	public static void renderWorldAura(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}
		Vec3 camera = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		for (Player player : minecraft.level.players()) {
			if (!ACTIVE.containsKey(player.getUUID())) {
				continue;
			}
			boolean firstPersonSelf = player == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
			if (firstPersonSelf) {
				continue;
			}
			float age = player.tickCount + partialTick;
			Vec3 position = player.getPosition(partialTick);
			renderAuraLayer(poseStack, bufferSource, camera, position.add(0.0D, player.getBbHeight() * 0.55D, 0.0D), age, 1.55F, 1.2F);
			renderAuraLayer(poseStack, bufferSource, camera, position.add(0.0D, player.getBbHeight() * 0.86D, 0.0D), age + 4.0F, 1.05F, 0.82F);
			renderAuraLayer(poseStack, bufferSource, camera, position.add(0.0D, player.getBbHeight() * 0.22D, 0.0D), age + 8.0F, 0.95F, 0.78F);
			if (player.isSprinting()) {
				Vec3 back = player.getLookAngle();
				if (back.lengthSqr() < 0.001D) {
					back = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
				}
				back = back.normalize().scale(-0.42D);
				renderAuraLayer(poseStack, bufferSource, camera, position.add(back).add(0.0D, 0.62D, 0.0D), age + 12.0F, 0.9F, 0.7F);
			}
		}
		bufferSource.endBatch(RenderType.lightning());
	}

	private static void renderAuraLayer(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camera, Vec3 position, float age, float yScale, float scale) {
		poseStack.pushPose();
		poseStack.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
		poseStack.scale(scale, yScale, scale);
		ChidoriClientEffects.renderCloneLightning(poseStack, bufferSource, age);
		poseStack.popPose();
	}

	private static final class ClientState {
		private int ticks;

		private ClientState(int ticks) {
			this.ticks = ticks;
		}
	}
}
