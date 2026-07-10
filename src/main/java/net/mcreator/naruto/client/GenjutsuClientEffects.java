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
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.NarutoMod;
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
	private static final ResourceLocation SHARINGAN_INDICATOR = ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "textures/screens/icon_sharingan.png");
	private static int visualTicks;
	private static int sharinganNightmareTicks;
	private static int sharinganNightmareTotal;

	public static void triggerSharinganNightmare(int ticks) {
		int duration = Mth.clamp(ticks, 20, 140);
		sharinganNightmareTicks = duration;
		sharinganNightmareTotal = duration;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null && minecraft.player != null) {
			minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.85F, 0.65F, false);
			minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.55F, 0.45F, false);
		}
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		if (hasGenjutsu()) {
			visualTicks = Math.min(FADE_TICKS, visualTicks + 2);
		} else if (visualTicks > 0) {
			visualTicks--;
		}
		if (sharinganNightmareTicks > 0) {
			sharinganNightmareTicks--;
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
		float nightmare = sharinganStrength();
		event.setYaw(event.getYaw() + (Mth.sin(time * 0.74F) * 1.25F + Mth.sin(time * 2.15F) * 0.35F + Mth.sin(time * 5.4F) * nightmare * 0.8F) * strength);
		event.setPitch(event.getPitch() + (Mth.cos(time * 0.61F) * 0.75F + Mth.sin(time * 1.72F) * 0.25F + Mth.cos(time * 4.6F) * nightmare * 0.45F) * strength);
		event.setRoll(event.getRoll() + (Mth.sin(time * 1.27F) * 2.2F + Mth.sin(time * 3.8F) * 0.6F + Mth.sin(time * 6.2F) * nightmare * 2.0F) * strength);
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
		float nightmare = sharinganStrength();
		event.setRed(Mth.lerp((0.18F + nightmare * 0.22F) * strength, event.getRed(), 0.42F + nightmare * 0.18F));
		event.setGreen(Mth.lerp((0.22F + nightmare * 0.32F) * strength, event.getGreen(), 0.08F));
		event.setBlue(Mth.lerp((0.2F + nightmare * 0.28F) * strength, event.getBlue(), 0.22F));
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
		renderSharinganIndicator(guiGraphics, width, height, time);
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
		float nightmare = sharinganStrength();
		NarutoWheelShaders.prepareGenjutsuDistortionShader(width, height, time, Mth.clamp(strength + nightmare * 0.75F, 0.0F, 1.95F));
		Matrix4f matrix = guiGraphics.pose().last().pose();
		int alpha = Mth.clamp((int) ((190 + nightmare * 45) * Mth.clamp(strength, 0.0F, 1.0F)), 0, 240);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buffer.addVertex(matrix, 0.0F, height, 0.0F).setColor(255, 255, 255, alpha);
		buffer.addVertex(matrix, width, height, 0.0F).setColor(255, 255, 255, alpha);
		buffer.addVertex(matrix, width, 0.0F, 0.0F).setColor(255, 255, 255, alpha);
		buffer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setColor(255, 255, 255, alpha);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void renderSharinganIndicator(GuiGraphics guiGraphics, int width, int height, float time) {
		float strength = sharinganStrength();
		if (strength <= 0.0F) {
			return;
		}
		float pulse = 0.84F + 0.16F * Mth.sin(time * 0.36F);
		int size = Mth.floor(Math.min(width, height) * (0.22F + strength * 0.09F) * pulse);
		int x = width / 2 - size / 2;
		int y = height / 2 - size / 2;
		int alpha = Mth.clamp(Mth.floor(230.0F * strength), 0, 230);
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getResourceManager().getResource(SHARINGAN_INDICATOR).isPresent()) {
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha / 255.0F);
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(width / 2.0F, height / 2.0F, 0.0F);
			guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(time * 18.0F));
			guiGraphics.blit(SHARINGAN_INDICATOR, -size / 2, -size / 2, 0.0F, 0.0F, size, size, size, size);
			guiGraphics.pose().popPose();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		} else {
			renderFallbackSharingan(guiGraphics, x, y, size, alpha);
		}
		int vignette = Mth.clamp(Mth.floor(135.0F * strength), 0, 135) << 24;
		guiGraphics.fill(0, 0, width, height, vignette | 0x220000);
	}

	private static void renderFallbackSharingan(GuiGraphics guiGraphics, int x, int y, int size, int alpha) {
		int red = (alpha << 24) | 0xB80000;
		int black = (Mth.clamp(alpha + 20, 0, 255) << 24);
		int cx = x + size / 2;
		int cy = y + size / 2;
		guiGraphics.fill(x + size / 6, cy - size / 8, x + size * 5 / 6, cy + size / 8, red);
		guiGraphics.fill(cx - size / 8, y + size / 6, cx + size / 8, y + size * 5 / 6, red);
		guiGraphics.fill(cx - size / 12, cy - size / 12, cx + size / 12, cy + size / 12, black);
		guiGraphics.fill(cx - size / 3, cy - 2, cx - size / 5, cy + 2, black);
		guiGraphics.fill(cx + size / 5, cy - 2, cx + size / 3, cy + 2, black);
		guiGraphics.fill(cx - 2, cy - size / 3, cx + 2, cy - size / 5, black);
		guiGraphics.fill(cx - 2, cy + size / 5, cx + 2, cy + size / 3, black);
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
		return Mth.clamp((visualTicks / (float) FADE_TICKS) * (1.0F + amplifier * 0.18F) + sharinganStrength() * 0.55F, 0.0F, 1.95F);
	}

	private static float visualTime(Player player, double partialTick) {
		return player.tickCount + (float) partialTick;
	}

	private static float random(float seed, float index, int salt) {
		float value = Mth.sin(seed * 91.345F + index * 37.721F + salt * 17.37F) * 43758.5453F;
		return value - Mth.floor(value);
	}

	private static float sharinganStrength() {
		if (sharinganNightmareTicks <= 0 || sharinganNightmareTotal <= 0) {
			return 0.0F;
		}
		float life = sharinganNightmareTicks / (float) sharinganNightmareTotal;
		return Mth.clamp(Mth.sin(life * Mth.PI) * 1.25F + life * 0.45F, 0.0F, 1.0F);
	}
}
