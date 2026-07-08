package net.mcreator.naruto.client;

import org.joml.Matrix4f;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.client.gui.NarutoWheelShaders;
import net.mcreator.naruto.init.NarutoModMobEffects;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

@EventBusSubscriber(value = Dist.CLIENT)
public class GenjutsuClientEffects {
	private static final int FADE_TICKS = 24;
	private static final int PHANTOM_COUNT = 7;
	private static int visualTicks;

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		if (hasGenjutsu()) {
			visualTicks = Math.min(FADE_TICKS, visualTicks + 2);
		} else if (visualTicks > 0) {
			visualTicks--;
		}
	}

	@SubscribeEvent
	public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		Player player = Minecraft.getInstance().player;
		float strength = intensity(player);
		if (player == null || strength <= 0.0F) {
			return;
		}
		float time = visualTime(player, event.getPartialTick());
		event.setYaw(event.getYaw() + (Mth.sin(time * 0.74F) * 1.25F + Mth.sin(time * 2.15F) * 0.35F) * strength);
		event.setPitch(event.getPitch() + (Mth.cos(time * 0.61F) * 0.75F + Mth.sin(time * 1.72F) * 0.25F) * strength);
		event.setRoll(event.getRoll() + (Mth.sin(time * 1.27F) * 2.2F + Mth.sin(time * 3.8F) * 0.6F) * strength);
	}

	@SubscribeEvent
	public static void onFov(ViewportEvent.ComputeFov event) {
		Player player = Minecraft.getInstance().player;
		float strength = intensity(player);
		if (player == null || strength <= 0.0F) {
			return;
		}
		float time = visualTime(player, event.getPartialTick());
		double pulse = 1.0D + (Mth.sin(time * 1.6F) * 0.022D + Mth.sin(time * 5.2F) * 0.008D) * strength;
		event.setFOV(event.getFOV() * pulse);
	}

	@SubscribeEvent
	public static void onFogColor(ViewportEvent.ComputeFogColor event) {
		float strength = intensity(Minecraft.getInstance().player);
		if (strength <= 0.0F) {
			return;
		}
		event.setRed(Mth.lerp(0.18F * strength, event.getRed(), 0.42F));
		event.setGreen(Mth.lerp(0.22F * strength, event.getGreen(), 0.08F));
		event.setBlue(Mth.lerp(0.2F * strength, event.getBlue(), 0.22F));
	}

	@SubscribeEvent
	public static void renderWorldPhantoms(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		float strength = intensity(player);
		if (player == null || strength <= 0.0F) {
			return;
		}
		Vec3 camera = event.getCamera().getPosition();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		float time = player.tickCount + partialTick;
		PoseStack poseStack = event.getPoseStack();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < PHANTOM_COUNT; i++) {
			renderPhantom(buffer, poseStack, player, camera, event.getCamera().getYRot(), time, i, strength);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	@SubscribeEvent
	public static void renderScreenDistortion(RenderGuiEvent.Post event) {
		Player player = Minecraft.getInstance().player;
		float strength = intensity(player);
		if (player == null || strength <= 0.0F) {
			return;
		}
		GuiGraphics guiGraphics = event.getGuiGraphics();
		int width = guiGraphics.guiWidth();
		int height = guiGraphics.guiHeight();
		float time = visualTime(player, event.getPartialTick().getGameTimeDeltaPartialTick(false));
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		renderShaderOverlay(guiGraphics, width, height, time, strength);
		RenderSystem.disableBlend();
	}

	private static void renderPhantom(BufferBuilder buffer, PoseStack poseStack, Player player, Vec3 camera, float cameraYaw, float time, int index, float strength) {
		float phase = Mth.floor(time / 34.0F);
		float angle = random(index, phase, 1) * Mth.TWO_PI + time * (0.012F + random(index, phase, 2) * 0.01F);
		float distance = Mth.lerp(random(index, phase, 3), 4.0F, 17.0F);
		float jitter = Mth.sin(time * (0.8F + random(index, phase, 9)) + index) * 0.35F;
		double x = player.getX() + Mth.cos(angle) * distance + jitter;
		double z = player.getZ() + Mth.sin(angle) * distance - jitter;
		double y = player.getY() + Mth.lerp(random(index, phase, 4), -0.8F, 1.1F);
		float height = Mth.lerp(random(index, phase, 5), 1.5F, 3.25F);
		float width = height * Mth.lerp(random(index, phase, 6), 0.18F, 0.36F);
		float alphaPulse = 0.25F + 0.75F * Mth.abs(Mth.sin(time * 0.16F + index * 1.7F));
		int bodyAlpha = Mth.clamp((int) (62 * strength * alphaPulse), 0, 78);
		int eyeAlpha = Mth.clamp((int) (150 * strength * alphaPulse), 0, 165);
		poseStack.pushPose();
		poseStack.translate(x - camera.x, y - camera.y, z - camera.z);
		poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
		Matrix4f matrix = poseStack.last().pose();
		addQuad(buffer, matrix, -width, 0.0F, 0.0F, width, height * 0.72F, 0.0F, 22, 0, 10, bodyAlpha);
		addQuad(buffer, matrix, -width * 0.72F, height * 0.64F, 0.0F, width * 0.72F, height, 0.0F, 6, 0, 4, bodyAlpha + 35);
		addQuad(buffer, matrix, -width * 0.43F, height * 0.8F, -0.01F, -width * 0.12F, height * 0.86F, -0.01F, 255, 18, 32, eyeAlpha);
		addQuad(buffer, matrix, width * 0.12F, height * 0.8F, -0.01F, width * 0.43F, height * 0.86F, -0.01F, 255, 18, 32, eyeAlpha);
		poseStack.popPose();
	}

	private static void renderShaderOverlay(GuiGraphics guiGraphics, int width, int height, float time, float strength) {
		NarutoWheelShaders.prepareGenjutsuDistortionShader(width, height, time, Mth.clamp(strength, 0.0F, 1.65F));
		Matrix4f matrix = guiGraphics.pose().last().pose();
		int alpha = Mth.clamp((int) (190 * Mth.clamp(strength, 0.0F, 1.0F)), 0, 210);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buffer.addVertex(matrix, 0.0F, height, 0.0F).setColor(255, 255, 255, alpha);
		buffer.addVertex(matrix, width, height, 0.0F).setColor(255, 255, 255, alpha);
		buffer.addVertex(matrix, width, 0.0F, 0.0F).setColor(255, 255, 255, alpha);
		buffer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setColor(255, 255, 255, alpha);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void addQuad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
		buffer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a);
		buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
		buffer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a);
		buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
	}

	private static boolean hasGenjutsu() {
		Player player = Minecraft.getInstance().player;
		return player != null && player.hasEffect(NarutoModMobEffects.GENJUTSU);
	}

	private static float intensity(Player player) {
		if (player == null || visualTicks <= 0 || !player.hasEffect(NarutoModMobEffects.GENJUTSU)) {
			return visualTicks > 0 ? visualTicks / (float) FADE_TICKS : 0.0F;
		}
		MobEffectInstance effect = player.getEffect(NarutoModMobEffects.GENJUTSU);
		float amplifier = effect == null ? 0.0F : effect.getAmplifier();
		return Mth.clamp((visualTicks / (float) FADE_TICKS) * (1.0F + amplifier * 0.18F), 0.0F, 1.65F);
	}

	private static float visualTime(Player player, double partialTick) {
		return player.tickCount + (float) partialTick;
	}

	private static float random(float seed, float index, int salt) {
		float value = Mth.sin(seed * 91.345F + index * 37.721F + salt * 17.37F) * 43758.5453F;
		return value - Mth.floor(value);
	}
}
