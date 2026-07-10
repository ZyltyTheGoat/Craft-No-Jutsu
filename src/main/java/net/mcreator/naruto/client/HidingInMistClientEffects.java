package net.mcreator.naruto.client;

import org.joml.Matrix4f;

import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.mcreator.naruto.client.gui.NarutoWheelShaders;

import java.util.UUID;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.systems.RenderSystem;

@EventBusSubscriber(value = Dist.CLIENT)
public class HidingInMistClientEffects {
	private static final int FIELD_TTL_TICKS = 45;
	private static final Map<UUID, MistField> ACTIVE_FIELDS = new HashMap<>();

	public static void setActive(UUID casterId, boolean active, float radius) {
		if (casterId == null) {
			return;
		}
		if (active) {
			MistField field = ACTIVE_FIELDS.computeIfAbsent(casterId, id -> new MistField(Math.max(1.0F, radius)));
			field.radius = Math.max(1.0F, radius);
			field.ticksLeft = FIELD_TTL_TICKS;
		} else {
			ACTIVE_FIELDS.remove(casterId);
		}
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Iterator<Map.Entry<UUID, MistField>> iterator = ACTIVE_FIELDS.entrySet().iterator();
		while (iterator.hasNext()) {
			MistField field = iterator.next().getValue();
			field.ticksLeft--;
			if (field.ticksLeft <= 0) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void onFogColor(ViewportEvent.ComputeFogColor event) {
		Player player = Minecraft.getInstance().player;
		float strength = mistStrength(player);
		if (strength <= 0.0F) {
			return;
		}
		float blend = Mth.clamp(strength * 0.88F, 0.0F, 0.92F);
		event.setRed(Mth.lerp(blend, event.getRed(), 0.68F));
		event.setGreen(Mth.lerp(blend, event.getGreen(), 0.86F));
		event.setBlue(Mth.lerp(blend, event.getBlue(), 1.0F));
	}

	@SubscribeEvent
	public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		Player player = Minecraft.getInstance().player;
		float strength = enemyMistStrength(player);
		if (player == null || strength <= 0.0F) {
			return;
		}
		float time = player.tickCount + (float) event.getPartialTick();
		event.setYaw(event.getYaw() + Mth.sin(time * 0.12F) * 1.4F * strength);
		event.setPitch(event.getPitch() + Mth.cos(time * 0.09F) * 0.75F * strength);
		event.setRoll(event.getRoll() + Mth.sin(time * 0.18F) * 1.0F * strength);
	}

	@SubscribeEvent
	public static void renderMistOverlay(RenderGuiEvent.Post event) {
		Player player = Minecraft.getInstance().player;
		float strength = mistStrength(player);
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
		NarutoWheelShaders.prepareMistVeilShader(width, height, time, clampedStrength);
		Matrix4f matrix = guiGraphics.pose().last().pose();
		int alpha = Mth.clamp((int) (245 * clampedStrength), 0, 245);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buffer.addVertex(matrix, 0.0F, height, 0.0F).setColor(255, 255, 255, alpha);
		buffer.addVertex(matrix, width, height, 0.0F).setColor(255, 255, 255, alpha);
		buffer.addVertex(matrix, width, 0.0F, 0.0F).setColor(255, 255, 255, alpha);
		buffer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setColor(255, 255, 255, alpha);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static float mistStrength(Player localPlayer) {
		return computeMistStrength(localPlayer, true);
	}

	private static float enemyMistStrength(Player localPlayer) {
		return computeMistStrength(localPlayer, false);
	}

	private static float computeMistStrength(Player localPlayer, boolean includeOwnMist) {
		Minecraft minecraft = Minecraft.getInstance();
		if (localPlayer == null || minecraft.level == null || ACTIVE_FIELDS.isEmpty()) {
			return 0.0F;
		}
		float strongest = 0.0F;
		for (Map.Entry<UUID, MistField> entry : ACTIVE_FIELDS.entrySet()) {
			UUID casterId = entry.getKey();
			boolean ownMist = casterId.equals(localPlayer.getUUID());
			if (ownMist && !includeOwnMist) {
				continue;
			}
			Player caster = minecraft.level.getPlayerByUUID(casterId);
			if (caster == null) {
				continue;
			}
			MistField field = entry.getValue();
			float distance = localPlayer.distanceTo(caster);
			if (distance > field.radius) {
				continue;
			}
			boolean previewEnemyView = ownMist && includeOwnMist && localPlayer.isShiftKeyDown();
			float normalizedDistance = distance / field.radius;
			float inside = 1.0F - normalizedDistance;
			float closeFog = 1.0F - Mth.clamp(normalizedDistance / 0.75F, 0.0F, 1.0F);
			closeFog = Mth.sqrt(closeFog);
			float fieldFog = Mth.clamp(inside * 2.8F, 0.0F, 1.0F);
			fieldFog = Mth.sqrt(fieldFog);
			float combinedFog = Mth.clamp(fieldFog * 1.05F + closeFog * 0.95F, 0.0F, 1.0F);
			float casterClarity = ownMist && !previewEnemyView ? 0.62F : 1.0F;
			strongest = Math.max(strongest, combinedFog * casterClarity);
		}
		return Mth.clamp(strongest, 0.0F, 1.0F);
	}

	private static final class MistField {
		private float radius;
		private int ticksLeft = FIELD_TTL_TICKS;

		private MistField(float radius) {
			this.radius = radius;
		}
	}
}
