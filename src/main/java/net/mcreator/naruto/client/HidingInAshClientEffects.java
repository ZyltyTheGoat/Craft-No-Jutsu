package net.mcreator.naruto.client;

import org.joml.Matrix4f;

import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import net.mcreator.naruto.client.gui.NarutoWheelShaders;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

@EventBusSubscriber(value = Dist.CLIENT)
public class HidingInAshClientEffects {
	private static final int FIELD_TTL_TICKS = 45;
	private static final Map<UUID, AshField> ACTIVE_FIELDS = new HashMap<>();

	public static void setActive(UUID casterId, boolean active, float radius) {
		if (casterId == null) {
			return;
		}
		if (active) {
			AshField field = ACTIVE_FIELDS.computeIfAbsent(casterId, id -> new AshField(Math.max(1.0F, radius)));
			field.radius = Math.max(1.0F, radius);
			field.ticksLeft = FIELD_TTL_TICKS;
		} else {
			ACTIVE_FIELDS.remove(casterId);
		}
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<Map.Entry<UUID, AshField>> iterator = ACTIVE_FIELDS.entrySet().iterator();
		while (iterator.hasNext()) {
			AshField field = iterator.next().getValue();
			field.ticksLeft--;
			if (field.ticksLeft <= 0) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void onFogColor(ViewportEvent.ComputeFogColor event) {
		Player player = Minecraft.getInstance().player;
		float strength = ashStrength(player);
		if (strength <= 0.0F) {
			return;
		}
		float blend = Mth.clamp(strength * 0.8F, 0.0F, 0.9F);
		event.setRed(Mth.lerp(blend, event.getRed(), 0.42F));
		event.setGreen(Mth.lerp(blend, event.getGreen(), 0.37F));
		event.setBlue(Mth.lerp(blend, event.getBlue(), 0.33F));
	}

	@SubscribeEvent
	public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		Player player = Minecraft.getInstance().player;
		float strength = enemyAshStrength(player);
		if (player == null || strength <= 0.0F) {
			return;
		}
		float time = player.tickCount + (float) event.getPartialTick();
		event.setYaw(event.getYaw() + Mth.sin(time * 0.16F) * 0.75F * strength);
		event.setPitch(event.getPitch() + Mth.cos(time * 0.13F) * 0.45F * strength);
	}

	@SubscribeEvent
	public static void renderAshOverlay(RenderGuiEvent.Post event) {
		Player player = Minecraft.getInstance().player;
		float strength = ashStrength(player);
		if (player == null || strength <= 0.0F) {
			return;
		}
		GuiGraphics guiGraphics = event.getGuiGraphics();
		int width = guiGraphics.guiWidth();
		int height = guiGraphics.guiHeight();
		float time = player.tickCount + event.getPartialTick().getGameTimeDeltaPartialTick(false);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		renderShaderOverlay(guiGraphics, width, height, time, strength);
		RenderSystem.disableBlend();
	}

	private static void renderShaderOverlay(GuiGraphics guiGraphics, int width, int height, float time, float strength) {
		float clampedStrength = Mth.clamp(strength, 0.0F, 1.0F);
		NarutoWheelShaders.prepareAshVeilShader(width, height, time, clampedStrength);
		Matrix4f matrix = guiGraphics.pose().last().pose();
		int alpha = Mth.clamp((int) (235 * clampedStrength), 0, 235);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buffer.addVertex(matrix, 0.0F, height, 0.0F).setColor(255, 255, 255, alpha);
		buffer.addVertex(matrix, width, height, 0.0F).setColor(255, 255, 255, alpha);
		buffer.addVertex(matrix, width, 0.0F, 0.0F).setColor(255, 255, 255, alpha);
		buffer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setColor(255, 255, 255, alpha);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static float ashStrength(Player localPlayer) {
		return computeAshStrength(localPlayer, true);
	}

	private static float enemyAshStrength(Player localPlayer) {
		return computeAshStrength(localPlayer, false);
	}

	private static float computeAshStrength(Player localPlayer, boolean includeOwnAsh) {
		Minecraft minecraft = Minecraft.getInstance();
		if (localPlayer == null || minecraft.level == null || ACTIVE_FIELDS.isEmpty()) {
			return 0.0F;
		}
		float strongest = 0.0F;
		for (Map.Entry<UUID, AshField> entry : ACTIVE_FIELDS.entrySet()) {
			UUID casterId = entry.getKey();
			boolean ownAsh = casterId.equals(localPlayer.getUUID());
			if (ownAsh && !includeOwnAsh) {
				continue;
			}
			Player caster = minecraft.level.getPlayerByUUID(casterId);
			if (caster == null) {
				continue;
			}
			AshField field = entry.getValue();
			float distance = localPlayer.distanceTo(caster);
			if (distance > field.radius) {
				continue;
			}
			boolean previewEnemyView = ownAsh && includeOwnAsh && localPlayer.isShiftKeyDown();
			float normalizedDistance = distance / field.radius;
			float closeAsh = 1.0F - Mth.clamp(normalizedDistance / 0.68F, 0.0F, 1.0F);
			float fieldAsh = Mth.clamp((1.0F - normalizedDistance) * 2.5F, 0.0F, 1.0F);
			float ownClarity = ownAsh && !previewEnemyView ? 0.48F : 1.0F;
			strongest = Math.max(strongest, (Mth.sqrt(closeAsh) * 0.78F + Mth.sqrt(fieldAsh) * 0.72F) * ownClarity);
		}
		return Mth.clamp(strongest, 0.0F, 1.0F);
	}

	private static final class AshField {
		private float radius;
		private int ticksLeft = FIELD_TTL_TICKS;

		private AshField(float radius) {
			this.radius = radius;
		}
	}
}
