package net.mcreator.naruto.client.gui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.ChatFormatting;

import com.mojang.blaze3d.platform.InputConstants;

import net.mcreator.naruto.world.inventory.JutsuWheelScreenMenu;
import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.JutsuRegistry;

import java.util.ArrayList;
import java.util.List;

public final class WheelGuiHelper {
	private static final long OPEN_ANIMATION_MS = 130L;
	private static final long CLOSE_ANIMATION_MS = 95L;

	private WheelGuiHelper() {
	}

	public static class AnimationState {
		private final long openedAtMs = System.currentTimeMillis();
		private boolean closing = false;
		private long closingStartedAtMs = 0L;
		private Runnable closingAction;

		public boolean isClosing() {
			return closing;
		}

		public void startClosing(Runnable action) {
			if (closing) {
				return;
			}
			closing = true;
			closingStartedAtMs = System.currentTimeMillis();
			closingAction = action;
		}

		public float openness() {
			if (closing) {
				float progress = clamp01((System.currentTimeMillis() - closingStartedAtMs) / (float) CLOSE_ANIMATION_MS);
				return 1.0F - easeInCubic(progress);
			}
			float progress = clamp01((System.currentTimeMillis() - openedAtMs) / (float) OPEN_ANIMATION_MS);
			return easeOutCubic(progress);
		}

		public float scale() {
			float openness = openness();
			if (closing) {
				return 0.92F + 0.08F * openness;
			}
			float progress = clamp01((System.currentTimeMillis() - openedAtMs) / (float) OPEN_ANIMATION_MS);
			return 0.88F + 0.12F * easeOutBack(progress);
		}

		public void finishIfClosed() {
			if (!closing || closingAction == null) {
				return;
			}
			if (System.currentTimeMillis() - closingStartedAtMs >= CLOSE_ANIMATION_MS) {
				Runnable action = closingAction;
				closingAction = null;
				action.run();
			}
		}
	}

	public static int softJutsuColor(JutsuData jutsu, boolean hovered) {
		return softJutsuColor(jutsu, hovered, 1.0F);
	}

	public static int softJutsuColor(JutsuData jutsu, boolean hovered, float alphaMultiplier) {
		return multiplyAlpha(softColor(jutsu != null ? jutsu.getColor() : 0xFFFFFFFF, hovered), alphaMultiplier);
	}

	public static int softCategoryColor(String category, boolean hovered) {
		return softCategoryColor(category, hovered, 1.0F);
	}

	public static int softCategoryColor(String category, boolean hovered, float alphaMultiplier) {
		int color = switch (category) {
			case "Basic Ninjutsu" -> 0x96E8E8E8;
			case "Taijutsu" -> 0x96D8D0C8;
			case "Nature Releases" -> 0x968FE6D0;
			case "Kekkei Genkai" -> 0x96FFD982;
			case "Dojutsu" -> 0x96E8A0C8;
			default -> 0x96FFFFFF;
		};
		return multiplyAlpha(softColor(color, hovered), alphaMultiplier);
	}

	private static int softColor(int sourceColor, boolean hovered) {
		float pulse = hovered ? (float) ((Math.sin(System.currentTimeMillis() / 130.0D) + 1.0D) * 0.5D) : 0.0F;
		float whiteMix = hovered ? 0.30F - pulse * 0.035F : 0.25F;
		int alpha = hovered ? (int) (0xB0 + pulse * 18) : 0x8C;
		int r = mixChannel((sourceColor >> 16) & 0xFF, whiteMix);
		int g = mixChannel((sourceColor >> 8) & 0xFF, whiteMix);
		int b = mixChannel(sourceColor & 0xFF, whiteMix);
		return (alpha << 24) | (r << 16) | (g << 8) | b;
	}

	private static int mixChannel(int channel, float whiteMix) {
		return Math.min(255, Math.max(0, (int) (channel * (1.0F - whiteMix) + 255 * whiteMix)));
	}

	public static void applyWheelAnimation(GuiGraphics guiGraphics, int centerX, int centerY, AnimationState animation) {
		float scale = animation.scale();
		guiGraphics.pose().translate(centerX, centerY, 0.0F);
		guiGraphics.pose().scale(scale, scale, 1.0F);
		guiGraphics.pose().translate(-centerX, -centerY, 0.0F);
	}

	private static int multiplyAlpha(int color, float multiplier) {
		int alpha = Math.min(255, Math.max(0, (int) (((color >> 24) & 0xFF) * clamp01(multiplier))));
		return (color & 0x00FFFFFF) | (alpha << 24);
	}

	private static float easeOutCubic(float value) {
		float inverted = 1.0F - clamp01(value);
		return 1.0F - inverted * inverted * inverted;
	}

	private static float easeInCubic(float value) {
		float clamped = clamp01(value);
		return clamped * clamped * clamped;
	}

	private static float easeOutBack(float value) {
		float clamped = clamp01(value) - 1.0F;
		return 1.0F + 2.70158F * clamped * clamped * clamped + 1.70158F * clamped * clamped;
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	public static String withFavouriteMarker(String text, boolean favourite) {
		return favourite ? "[★] " + text : text;
	}

	public static String withActiveToggleMarker(String text, boolean active) {
		return active ? "[✔] " + text : text;
	}

	public static void drawCenteredWrappedString(GuiGraphics guiGraphics, Font font, String text, int centerX, int centerY, int maxWidth, int color) {
		List<String> lines = wrap(font, text, maxWidth);
		int totalHeight = lines.size() * font.lineHeight;
		int y = centerY - totalHeight / 2;
		for (String line : lines) {
			guiGraphics.drawCenteredString(font, line, centerX, y, color);
			y += font.lineHeight;
		}
	}

	public static void renderJutsuInfoPanel(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight, String jutsuId, boolean favourite) {
		if (jutsuId == null || jutsuId.isEmpty()) {
			return;
		}
		JutsuData jutsu = JutsuRegistry.getJutsu(jutsuId);
		if (jutsu == null) {
			return;
		}
		List<String> lines = new ArrayList<>();
		lines.add(jutsu.getName());
		lines.add("Chakra: " + jutsu.getChakraCost() + (jutsu.getIsToggle() ? "/tick" : "") + "  Cooldown: " + formatTicks(jutsu.getCooldown()));
		if (jutsu.getChargeTime() > 0) {
			lines.add("Charge: " + formatTicks(jutsu.getChargeTime()));
		}
		lines.add((jutsu.getIsToggle() ? "Left click toggles" : "Left click equips") + "  Right click " + (favourite ? "unfavorites" : "favorites"));
		renderPanel(guiGraphics, font, screenWidth / 2, screenHeight - 118, lines, 250);
	}

	public static void renderCategoryInfoPanel(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight, String title, String detail) {
		if (title == null || title.isEmpty()) {
			return;
		}
		List<String> lines = new ArrayList<>();
		lines.add(title);
		if (detail != null && !detail.isEmpty()) {
			lines.add(detail);
		}
		renderPanel(guiGraphics, font, screenWidth / 2, screenHeight - 80, lines, 230);
	}

	public static void renderWheelHint(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight, boolean hasPages) {
		String hint = hasPages ? "Scroll pages  |  Right click jutsu favorites  |  Right click center back" : "Right click jutsu favorites  |  Right click center back";
		guiGraphics.drawCenteredString(font, hint, screenWidth / 2, 60, 0xA0FFFFFF);
	}

	public static void renderCategoryWheelHint(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight, boolean hasPages) {
		String hint = hasPages ? "Scroll pages  |  Click a category  |  Right click closes" : "Click a category  |  Right click closes";
		guiGraphics.drawCenteredString(font, hint, screenWidth / 2, 60, 0xA0FFFFFF);
	}

	public static void renderWheelBackdrop(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
		guiGraphics.fill(0, 0, screenWidth, screenHeight, 0x26000000);
		guiGraphics.fill(0, 0, screenWidth, 74, 0x30000000);
	}

	public static boolean handleMovementKey(int key, int scanCode, boolean pressed) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.options == null) {
			return false;
		}
		KeyMapping[] movementKeys = {minecraft.options.keyUp, minecraft.options.keyDown, minecraft.options.keyLeft, minecraft.options.keyRight, minecraft.options.keyJump, minecraft.options.keyShift, minecraft.options.keySprint};
		for (KeyMapping movementKey : movementKeys) {
			if (movementKey.matches(key, scanCode)) {
				movementKey.setDown(pressed);
				return true;
			}
		}
		return false;
	}

	public static void syncMovementKeys() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.options == null || minecraft.getWindow() == null) {
			return;
		}
		long window = minecraft.getWindow().getWindow();
		KeyMapping[] movementKeys = {minecraft.options.keyUp, minecraft.options.keyDown, minecraft.options.keyLeft, minecraft.options.keyRight, minecraft.options.keyJump, minecraft.options.keyShift, minecraft.options.keySprint};
		for (KeyMapping movementKey : movementKeys) {
			if (movementKey.getKey().getType() == InputConstants.Type.KEYSYM) {
				movementKey.setDown(InputConstants.isKeyDown(window, movementKey.getKey().getValue()));
			}
		}
	}

	public static void openRootWheel(Minecraft minecraft, Player player, int x, int y, int z) {
		if (minecraft == null || player == null) {
			return;
		}
		minecraft.tell(() -> {
			FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
			buffer.writeBlockPos(new net.minecraft.core.BlockPos(x, y, z));
			buffer.resetReaderIndex();
			minecraft.setScreen(new JutsuWheelScreenScreen(new JutsuWheelScreenMenu(0, player.getInventory(), buffer), player.getInventory(), Component.literal("Jutsu Wheel")));
		});
	}

	public static void announceEquipped(Player player, JutsuData jutsu) {
	}

	public static List<String> wrap(Font font, String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		if (text == null || text.isEmpty()) {
			return lines;
		}
		for (String paragraph : text.split("\\n")) {
			String[] words = paragraph.split(" ");
			String current = "";
			for (String word : words) {
				String candidate = current.isEmpty() ? word : current + " " + word;
				if (font.width(candidate) <= maxWidth) {
					current = candidate;
				} else {
					if (!current.isEmpty()) {
						lines.add(current);
					}
					current = word;
				}
			}
			if (!current.isEmpty()) {
				lines.add(current);
			}
		}
		return lines;
	}

	private static void renderPanel(GuiGraphics guiGraphics, Font font, int centerX, int topY, List<String> lines, int maxWidth) {
		List<String> wrapped = new ArrayList<>();
		for (String line : lines) {
			wrapped.addAll(wrap(font, line, maxWidth - 16));
		}
		if (wrapped.isEmpty()) {
			return;
		}
		int width = 0;
		for (String line : wrapped) {
			width = Math.max(width, font.width(line));
		}
		width = Math.min(maxWidth, width + 20);
		int height = wrapped.size() * font.lineHeight + 14;
		int left = centerX - width / 2;
		int top = topY;
		guiGraphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, 0xAAFFFFFF);
		guiGraphics.fill(left, top, left + width, top + height, 0xCC050505);
		int y = top + 7;
		for (int i = 0; i < wrapped.size(); i++) {
			int color = i == 0 ? 0xFFFFFFFF : 0xFFD8D8D8;
			guiGraphics.drawCenteredString(font, wrapped.get(i), centerX, y, color);
			y += font.lineHeight;
		}
	}

	private static String formatTicks(int ticks) {
		if (ticks <= 0) {
			return "none";
		}
		if (ticks % 20 == 0) {
			return (ticks / 20) + "s";
		}
		return String.format("%.1fs", ticks / 20.0F);
	}

}
