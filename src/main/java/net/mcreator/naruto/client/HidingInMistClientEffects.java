package net.mcreator.naruto.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
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

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

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
		float blend = Mth.clamp(strength * 0.72F, 0.0F, 0.82F);
		event.setRed(Mth.lerp(blend, event.getRed(), 0.72F));
		event.setGreen(Mth.lerp(blend, event.getGreen(), 0.88F));
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
		NarutoWheelShaders.prepareMistVeilShader(width, height, time, Mth.clamp(strength, 0.0F, 1.0F));
		Matrix4f matrix = guiGraphics.pose().last().pose();
		int alpha = Mth.clamp((int) (205 * Mth.clamp(strength, 0.0F, 1.0F)), 0, 215);
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
			float inside = 1.0F - distance / field.radius;
			float edgeFade = Mth.clamp(inside * 1.45F, 0.0F, 1.0F);
			float casterClarity = ownMist ? 0.38F : 1.0F;
			strongest = Math.max(strongest, edgeFade * casterClarity);
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
