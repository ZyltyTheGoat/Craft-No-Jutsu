package net.mcreator.naruto.client.gui;

import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.GameRenderer;

import net.mcreator.naruto.NarutoMod;

import java.io.IOException;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;

@EventBusSubscriber(modid = NarutoMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NarutoWheelShaders {
	private static ShaderInstance wheelSliceShader;
	private static ShaderInstance dojutsuWheelSliceShader;
	private static ShaderInstance genjutsuDistortionShader;
	private static ShaderInstance waterPrisonSphereShader;
	private static ShaderInstance windGustShader;
	private static ShaderInstance mistVeilShader;

	private NarutoWheelShaders() {
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "wheel_slice"), DefaultVertexFormat.POSITION_COLOR), shader -> wheelSliceShader = shader);
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "wheel_slice_dojutsu"), DefaultVertexFormat.POSITION_COLOR), shader -> dojutsuWheelSliceShader = shader);
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "genjutsu_distortion"), DefaultVertexFormat.POSITION_COLOR), shader -> genjutsuDistortionShader = shader);
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "water_prison_sphere"), DefaultVertexFormat.POSITION_COLOR), shader -> waterPrisonSphereShader = shader);
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "wind_gust"), DefaultVertexFormat.POSITION_COLOR), shader -> windGustShader = shader);
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "mist_veil"), DefaultVertexFormat.POSITION_COLOR), shader -> mistVeilShader = shader);
	}

	public static void prepareWheelSliceShader(int screenWidth, int screenHeight) {
		RenderSystem.setShader(() -> wheelSliceShader != null ? wheelSliceShader : GameRenderer.getPositionColorShader());
		ShaderInstance shader = RenderSystem.getShader();
		if (shader == wheelSliceShader) {
			setWheelUniforms(shader, screenWidth, screenHeight);
		}
	}

	public static void prepareDojutsuWheelSliceShader(int screenWidth, int screenHeight) {
		RenderSystem.setShader(() -> dojutsuWheelSliceShader != null ? dojutsuWheelSliceShader : GameRenderer.getPositionColorShader());
		ShaderInstance shader = RenderSystem.getShader();
		if (shader == dojutsuWheelSliceShader) {
			setWheelUniforms(shader, screenWidth, screenHeight);
		}
	}

	public static void prepareGenjutsuDistortionShader(int screenWidth, int screenHeight, float time, float intensity) {
		RenderSystem.setShader(() -> genjutsuDistortionShader != null ? genjutsuDistortionShader : GameRenderer.getPositionColorShader());
		ShaderInstance shader = RenderSystem.getShader();
		if (shader == genjutsuDistortionShader) {
			shader.safeGetUniform("GenjutsuTime").set(time);
			shader.safeGetUniform("GenjutsuIntensity").set(intensity);
			shader.safeGetUniform("GenjutsuScreenSize").set((float) screenWidth, (float) screenHeight);
		}
	}

	public static void prepareWaterPrisonSphereShader(float time) {
		RenderSystem.setShader(() -> waterPrisonSphereShader != null ? waterPrisonSphereShader : GameRenderer.getPositionColorShader());
		ShaderInstance shader = RenderSystem.getShader();
		if (shader == waterPrisonSphereShader) {
			shader.safeGetUniform("WaterPrisonTime").set(time);
		}
	}

	public static void prepareWindGustShader(float time) {
		RenderSystem.setShader(() -> windGustShader != null ? windGustShader : GameRenderer.getPositionColorShader());
		ShaderInstance shader = RenderSystem.getShader();
		if (shader == windGustShader) {
			shader.safeGetUniform("WindTime").set(time);
		}
	}

	public static void prepareMistVeilShader(int screenWidth, int screenHeight, float time, float strength) {
		RenderSystem.setShader(() -> mistVeilShader != null ? mistVeilShader : GameRenderer.getPositionColorShader());
		ShaderInstance shader = RenderSystem.getShader();
		if (shader == mistVeilShader) {
			shader.safeGetUniform("MistTime").set(time);
			shader.safeGetUniform("MistStrength").set(strength);
			shader.safeGetUniform("MistScreenSize").set((float) screenWidth, (float) screenHeight);
		}
	}

	private static void setWheelUniforms(ShaderInstance shader, int screenWidth, int screenHeight) {
		float time = (System.currentTimeMillis() % 100000L) / 1000.0F;
		shader.safeGetUniform("WheelTime").set(time);
		shader.safeGetUniform("WheelScreenSize").set((float) screenWidth, (float) screenHeight);
	}
}
