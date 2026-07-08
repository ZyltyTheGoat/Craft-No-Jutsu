package net.mcreator.naruto.client.screens;

import org.joml.Matrix4f;

import org.checkerframework.checker.units.qual.h;
import org.checkerframework.checker.units.qual.g;

import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;

import net.mcreator.naruto.procedures.ReturnCurrentJutsuProcedure;
import net.mcreator.naruto.procedures.ReturnCurrentChakraProcedure;
import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.JutsuRegistry;
import net.mcreator.naruto.JutsuData;

import java.util.Map;
import java.util.HashMap;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;

@EventBusSubscriber(Dist.CLIENT)
public class DisplayChakraOverlay {
	private static String lastActiveJutsu = "";
	private static int equipPulseTicks = 0;
	private static final Map<String, Long> LOCAL_COOLDOWNS = new HashMap<>();
	private static final long MIN_FEEDBACK_MESSAGE_MILLIS = 2400L;
	private static String cooldownMessage = "";
	private static long cooldownMessageUntilMillis = 0L;
	private static int suppressChargeTicks = 0;
	private static int cooldownReadyPulseTicks = 0;

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
		int w = event.getGuiGraphics().guiWidth();
		int h = event.getGuiGraphics().guiHeight();
		Level world = null;
		double x = 0;
		double y = 0;
		double z = 0;
		Player entity = Minecraft.getInstance().player;
		if (entity != null) {
			world = entity.level();
			x = entity.getX();
			y = entity.getY();
			z = entity.getZ();
		}
		if (entity != null) {
			NarutoModVariables.PlayerVariables vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
			GuiGraphics guiGraphics = event.getGuiGraphics();
			Font font = Minecraft.getInstance().font;
			int left = 14;
			int top = h - 64;
			int width = 136;
			int barHeight = 8;
			double maxChakra = Math.max(1.0D, vars.maxChakra);
			double chakraRatio = Math.max(0.0D, Math.min(1.0D, vars.currentChakra / maxChakra));
			int fillWidth = (int) ((width - 2) * chakraRatio);
			JutsuRegistry.initializeJutsus();
			JutsuData activeJutsu = JutsuRegistry.getJutsu(vars.activeJutsu);
			updateEquipPulse(vars.activeJutsu);
			int accentColor = activeJutsu != null ? forceOpaque(activeJutsu.getColor()) : 0xFF72D6FF;
			int pulseAlpha = equipPulseTicks > 0 ? Math.min(0x80, 0x20 + equipPulseTicks * 3) : 0;
			guiGraphics.fill(left - 3, top - 15, left + width + 43, top + 39, 0x99000000);
			guiGraphics.fill(left - 3, top - 15, left + 1, top + 39, accentColor);
			if (pulseAlpha > 0) {
				guiGraphics.fill(left - 3, top - 15, left + width + 43, top + 39, (pulseAlpha << 24) | (accentColor & 0x00FFFFFF));
			}
			guiGraphics.fill(left - 2, top - 14, left + width + 42, top - 13, 0x66FFFFFF);
			guiGraphics.drawString(font, "Chakra", left + 5, top - 11, 0xFF72D6FF, false);
			guiGraphics.drawString(font, ReturnCurrentChakraProcedure.execute(entity), left + 56, top - 11, 0xFFFFFFFF, false);
			guiGraphics.fill(left + 5, top, left + 5 + width, top + barHeight, 0xFF151515);
			guiGraphics.fill(left + 6, top + 1, left + 6 + fillWidth, top + barHeight - 1, 0xFF1E9DFF);
			guiGraphics.fill(left + 5, top, left + 5 + width, top + 1, 0xAAFFFFFF);
			renderEquippedJutsu(guiGraphics, font, left + 5, top + 13, width + 32, vars.activeJutsu, activeJutsu, accentColor, pulseAlpha);
			renderCooldownMessage(guiGraphics, font, left + 5, top + 40);
			renderChargeWheel(guiGraphics, font, entity, activeJutsu, accentColor, event.getGuiGraphics().guiWidth() / 2, event.getGuiGraphics().guiHeight() / 2);
		}
	}

	private static void renderEquippedJutsu(GuiGraphics guiGraphics, Font font, int left, int top, int width, String activeJutsuId, JutsuData activeJutsu, int accentColor, int pulseAlpha) {
		long cooldownRemaining = getCooldownRemainingTicks(activeJutsuId);
		float cooldownRatio = activeJutsu != null && activeJutsu.getCooldown() > 0 ? Mth.clamp(cooldownRemaining / (float) activeJutsu.getCooldown(), 0.0F, 1.0F) : 0.0F;
		boolean readyPulse = cooldownRemaining <= 0L && cooldownReadyPulseTicks > 0 && activeJutsuId != null && activeJutsuId.equals(lastActiveJutsu);
		guiGraphics.fill(left, top, left + 24, top + 24, 0xDD070707);
		if (pulseAlpha > 0) {
			guiGraphics.fill(left, top, left + 24, top + 24, (Math.min(0x90, pulseAlpha + 0x20) << 24) | (accentColor & 0x00FFFFFF));
		}
		if (readyPulse) {
			int alpha = Math.min(0x90, 0x20 + cooldownReadyPulseTicks * 5);
			guiGraphics.fill(left - 1, top - 1, left + 25, top + 25, (alpha << 24) | 0x41FF91);
		}
		guiGraphics.fill(left, top, left + 24, top + 1, 0x88FFFFFF);
		guiGraphics.fill(left, top + 23, left + 24, top + 24, 0x66000000);
		guiGraphics.fill(left, top, left + 1, top + 24, accentColor);
		if (activeJutsu != null) {
			ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("naruto", "textures/screens/" + activeJutsu.getIconName() + ".png");
			guiGraphics.blit(icon, left + 4, top + 4, 16, 16, 0.0F, 0.0F, 64, 64, 64, 64);
		} else {
			guiGraphics.drawCenteredString(font, "-", left + 12, top + 8, 0xFF888888);
		}
		if (cooldownRemaining > 0L) {
			int wipeTop = top + Mth.floor(24.0F * (1.0F - cooldownRatio));
			guiGraphics.fill(left, top, left + 24, top + 24, 0x66000000);
			guiGraphics.fill(left, wipeTop, left + 24, top + 24, 0xAA000000);
			guiGraphics.fill(left, wipeTop, left + 24, Math.min(top + 24, wipeTop + 1), 0xFFE07A22);
			String remainingText = formatCooldown(cooldownRemaining);
			guiGraphics.drawCenteredString(font, remainingText, left + 12, top + 8, 0xFFFFD884);
		}
		String label = activeJutsu != null ? activeJutsu.getName() : ReturnCurrentJutsuProcedure.execute(Minecraft.getInstance().player);
		if (label == null || label.isEmpty()) {
			label = "No Jutsu Equipped";
		}
		String trimmed = font.plainSubstrByWidth(label, width - 32);
		int textColor = cooldownRemaining > 0L ? 0xFFFFD884 : readyPulse ? 0xFF96FFBA : activeJutsuId != null && !activeJutsuId.isEmpty() ? 0xFFE8E8E8 : 0xFF9A9A9A;
		String status = cooldownRemaining > 0L ? "Cooldown" : readyPulse ? "Ready" : "Equipped";
		int statusColor = cooldownRemaining > 0L ? 0xFFFFA942 : readyPulse ? 0xFF71FF99 : 0xFFA8DFFF;
		guiGraphics.drawString(font, status, left + 30, top + 1, statusColor, false);
		guiGraphics.drawString(font, trimmed, left + 30, top + 12, textColor, false);
		if (readyPulse) {
			cooldownReadyPulseTicks--;
		}
	}

	private static int forceOpaque(int color) {
		return 0xFF000000 | (color & 0x00FFFFFF);
	}

	private static void updateEquipPulse(String activeJutsu) {
		String current = activeJutsu == null ? "" : activeJutsu;
		if (!current.equals(lastActiveJutsu)) {
			lastActiveJutsu = current;
			equipPulseTicks = current.isEmpty() ? 0 : 24;
		} else if (equipPulseTicks > 0) {
			equipPulseTicks--;
		}
	}

	public static boolean tryUseLocalCooldown(String key, int cooldownTicks, String displayName) {
		Player player = Minecraft.getInstance().player;
		if (player == null || key == null || key.isEmpty()) {
			return true;
		}
		long now = player.level().getGameTime();
		long readyAt = LOCAL_COOLDOWNS.getOrDefault(key, 0L);
		if (readyAt - now > cooldownTicks) {
			LOCAL_COOLDOWNS.remove(key);
			readyAt = 0L;
		}
		if (now < readyAt) {
			showCooldown(displayName + " on cooldown: " + formatCooldown(readyAt - now));
			return false;
		}
		startCooldown(key, cooldownTicks);
		return true;
	}

	public static void startCooldown(String key, int cooldownTicks) {
		Player player = Minecraft.getInstance().player;
		if (player == null || key == null || key.isEmpty() || cooldownTicks <= 0) {
			return;
		}
		LOCAL_COOLDOWNS.put(key, player.level().getGameTime() + Math.max(1, cooldownTicks));
	}

	public static void showCooldown(String message) {
		showCooldown(message, 28);
	}

	public static void showCooldown(String message, int ticks) {
		cooldownMessage = message == null ? "" : message;
		if (cooldownMessage.toLowerCase(java.util.Locale.ROOT).contains("cooldown")) {
			cooldownMessage = "";
			cooldownMessageUntilMillis = 0L;
			suppressChargeTicks = Math.max(suppressChargeTicks, 8);
		} else {
			cooldownMessageUntilMillis = System.currentTimeMillis() + Math.max(MIN_FEEDBACK_MESSAGE_MILLIS, ticks * 50L);
		}
	}

	private static void renderCooldownMessage(GuiGraphics guiGraphics, Font font, int left, int top) {
		long remainingMillis = cooldownMessageUntilMillis - System.currentTimeMillis();
		if (remainingMillis <= 0L || cooldownMessage.isEmpty()) {
			return;
		}
		int alpha = remainingMillis < 650L ? Mth.clamp((int) (remainingMillis * 0xE8 / 650L), 0x40, 0xE8) : 0xE8;
		int width = Math.min(170, font.width(cooldownMessage) + 12);
		String message = font.plainSubstrByWidth(cooldownMessage, width - 12);
		guiGraphics.fill(left - 3, top - 3, left + width, top + 10, (alpha << 24) | 0x120806);
		guiGraphics.fill(left - 3, top - 3, left, top + 10, 0xFFE07A22);
		guiGraphics.drawString(font, message, left + 4, top, 0xFFFFC46A, false);
	}

	private static void renderChargeWheel(GuiGraphics guiGraphics, Font font, Player player, JutsuData activeJutsu, int accentColor, int centerX, int centerY) {
		if (player == null || activeJutsu == null) {
			return;
		}
		if (suppressChargeTicks > 0) {
			suppressChargeTicks--;
			return;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		if (!vars.chargingAbility || activeJutsu.getChargeTime() <= 0) {
			return;
		}
		float progress = Mth.clamp((float) (vars.chargeTime / Math.max(1.0D, activeJutsu.getChargeTime())), 0.0F, 1.0F);
		int wheelY = centerY + 46;
		int radius = 20;
		int thickness = 3;
		drawRing(guiGraphics.pose(), centerX, wheelY, radius, thickness, 1.0F, 0x44000000);
		drawRing(guiGraphics.pose(), centerX, wheelY, radius, thickness, progress, (0xC8 << 24) | (accentColor & 0x00FFFFFF));
		guiGraphics.drawCenteredString(font, progress >= 1.0F ? "READY" : Mth.floor(progress * 100.0F) + "%", centerX, wheelY - 4, progress >= 1.0F ? 0xFFFFFFFF : 0xDDE8F6FF);
	}

	private static void drawRing(PoseStack poseStack, int centerX, int centerY, int radius, int thickness, float progress, int color) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		int sections = Math.max(1, (int) (72 * Mth.clamp(progress, 0.0F, 1.0F)));
		float start = (float) (-Math.PI / 2.0D);
		float end = start + Mth.TWO_PI * Mth.clamp(progress, 0.0F, 1.0F);
		int a = (color >> 24) & 0xFF;
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		for (int i = 0; i < sections; i++) {
			float angle1 = Mth.lerp(i / (float) sections, start, end);
			float angle2 = Mth.lerp((i + 1) / (float) sections, start, end);
			float x1 = centerX + (radius - thickness) * Mth.cos(angle1);
			float y1 = centerY + (radius - thickness) * Mth.sin(angle1);
			float x2 = centerX + radius * Mth.cos(angle1);
			float y2 = centerY + radius * Mth.sin(angle1);
			float x3 = centerX + radius * Mth.cos(angle2);
			float y3 = centerY + radius * Mth.sin(angle2);
			float x4 = centerX + (radius - thickness) * Mth.cos(angle2);
			float y4 = centerY + (radius - thickness) * Mth.sin(angle2);
			buffer.addVertex(matrix, x2, y2, 0.0F).setColor(r, g, b, a);
			buffer.addVertex(matrix, x1, y1, 0.0F).setColor(r, g, b, a);
			buffer.addVertex(matrix, x4, y4, 0.0F).setColor(r, g, b, a);
			buffer.addVertex(matrix, x3, y3, 0.0F).setColor(r, g, b, a);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.disableBlend();
	}

	private static String formatCooldown(long ticks) {
		return String.format("%.1fs", ticks / 20.0D);
	}

	private static long getCooldownRemainingTicks(String key) {
		Player player = Minecraft.getInstance().player;
		if (player == null || key == null || key.isEmpty()) {
			return 0L;
		}
		long readyAt = LOCAL_COOLDOWNS.getOrDefault(key, 0L);
		long remaining = readyAt - player.level().getGameTime();
		if (remaining <= 0L) {
			LOCAL_COOLDOWNS.remove(key);
			if (readyAt > 0L && key.equals(player.getData(NarutoModVariables.PLAYER_VARIABLES).activeJutsu)) {
				cooldownReadyPulseTicks = Math.max(cooldownReadyPulseTicks, 28);
			}
			return 0L;
		}
		return remaining;
	}
}
