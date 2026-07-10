package net.mcreator.naruto.client;

import org.joml.Matrix4f;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;

@EventBusSubscriber(value = Dist.CLIENT)
public class EarthSpikeClientEffects {
	private static final List<Spike> SPIKES = new ArrayList<>();

	public static void addSpike(Vec3 position, int blockStateId, float chargePercent, float scale, float rotation, int ticks) {
		BlockState state = Block.stateById(blockStateId);
		if (state == null || state.isAir()) {
			state = Blocks.DIRT.defaultBlockState();
		}
		SPIKES.add(new Spike(position, state, Mth.clamp(chargePercent, 0.0F, 1.0F), Math.max(0.15F, scale), rotation, Math.max(6, ticks)));
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<Spike> iterator = SPIKES.iterator();
		while (iterator.hasNext()) {
			Spike spike = iterator.next();
			spike.ticks--;
			if (spike.ticks <= 0) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void renderSpikes(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || SPIKES.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}
		Vec3 camera = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		// Force a known-good state before drawing so a leftover tint/blend from whatever rendered
		// just before us (particles, other jutsu effects, etc.) can't bleed into these quads.
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		// Use a lightmapped textured shader for custom geometry. This keeps the block texture,
		// but lets the world light darken the spike so it does not read as emissive.
		RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
		RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
		Matrix4f matrix = poseStack.last().pose();
		for (Spike spike : SPIKES) {
			renderSpike(minecraft, buffer, matrix, camera, spike, partialTick);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.enableCull();
	}

	private static void renderSpike(Minecraft minecraft, BufferBuilder buffer, Matrix4f matrix, Vec3 camera, Spike spike, float partialTick) {
		float age = spike.totalTicks - spike.ticks + partialTick;
		float life = Mth.clamp(age / spike.totalTicks, 0.0F, 1.0F);
		float rise = Mth.clamp(life * 2.4F, 0.0F, 1.0F);
		float sink = life > 0.72F ? Mth.clamp((life - 0.72F) / 0.28F, 0.0F, 1.0F) * 0.35F : 0.0F;
		float height = Mth.lerp(spike.chargePercent, 1.35F, 2.35F) * spike.scale * rise;
		float radius = Mth.lerp(spike.chargePercent, 0.42F, 0.68F) * spike.scale;
		int shade = Mth.floor(Mth.lerp(spike.scale, 138.0F, 205.0F));
		// Sample the world's actual light at the spike's block so it matches its surroundings
		// (dim in shade/at night, bright in daylight) instead of always looking lit.
		BlockPos lightPos = BlockPos.containing(spike.position.x, spike.position.y + 1.0D, spike.position.z);
		BlockState lightState = minecraft.level.getBlockState(lightPos);
		int packedLight = LevelRenderer.getLightColor(minecraft.level, lightState, lightPos);
		float blockLight = LightTexture.block(packedLight) / 15.0F;
		float skyBrightness = Mth.clamp(minecraft.level.getSkyDarken(partialTick), 0.18F, 1.0F);
		float worldBrightness = Math.max(blockLight, 0.25F + skyBrightness * 0.75F);
		shade = Mth.clamp(Mth.floor(shade * worldBrightness), 28, 255);
		TextureAtlasSprite sprite = minecraft.getBlockRenderer().getBlockModel(spike.state).getParticleIcon();
		float u0 = sprite.getU0();
		float u1 = sprite.getU1();
		float v0 = sprite.getV0();
		float v1 = sprite.getV1();
		double x = spike.position.x - camera.x;
		double y = spike.position.y - camera.y - 0.08D - sink;
		double z = spike.position.z - camera.z;
		Vec3 tip = new Vec3(x, y + height, z);
		float cos = Mth.cos(spike.rotation);
		float sin = Mth.sin(spike.rotation);
		Vec3 north = rotatedBase(x, y, z, 0.0F, -radius, cos, sin);
		Vec3 east = rotatedBase(x, y, z, radius, 0.0F, cos, sin);
		Vec3 south = rotatedBase(x, y, z, 0.0F, radius, cos, sin);
		Vec3 west = rotatedBase(x, y, z, -radius, 0.0F, cos, sin);
		addFace(buffer, matrix, tip, north, east, u0, u1, v0, v1, shade, packedLight);
		addFace(buffer, matrix, tip, east, south, u0, u1, v0, v1, shade, packedLight);
		addFace(buffer, matrix, tip, south, west, u0, u1, v0, v1, shade, packedLight);
		addFace(buffer, matrix, tip, west, north, u0, u1, v0, v1, shade, packedLight);
	}

	private static Vec3 rotatedBase(double x, double y, double z, float localX, float localZ, float cos, float sin) {
		return new Vec3(x + localX * cos - localZ * sin, y, z + localX * sin + localZ * cos);
	}

	private static void addFace(BufferBuilder buffer, Matrix4f matrix, Vec3 tip, Vec3 a, Vec3 b, float u0, float u1, float v0, float v1, int shade, int packedLight) {
		vertex(buffer, matrix, tip, (u0 + u1) * 0.5F, v0, shade, packedLight);
		vertex(buffer, matrix, a, u0, v1, shade, packedLight);
		vertex(buffer, matrix, b, u1, v1, shade, packedLight);
		vertex(buffer, matrix, tip, (u0 + u1) * 0.5F, v0, shade, packedLight);
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 pos, float u, float v, int shade, int packedLight) {
		buffer.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).setColor(shade, shade, shade, 255).setUv(u, v).setLight(packedLight);
	}

	private static final class Spike {
		private final Vec3 position;
		private final BlockState state;
		private final float chargePercent;
		private final float scale;
		private final float rotation;
		private final int totalTicks;
		private int ticks;

		private Spike(Vec3 position, BlockState state, float chargePercent, float scale, float rotation, int ticks) {
			this.position = position;
			this.state = state;
			this.chargePercent = chargePercent;
			this.scale = scale;
			this.rotation = rotation;
			this.ticks = ticks;
			this.totalTicks = ticks;
		}
	}
}
