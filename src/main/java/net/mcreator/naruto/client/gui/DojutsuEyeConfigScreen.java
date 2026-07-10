package net.mcreator.naruto.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.mcreator.naruto.network.SetDojutsuEyeConfigPacket;
import net.mcreator.naruto.DojutsuEyeConfigStore;

public class DojutsuEyeConfigScreen extends Screen {
	private static final int PANEL_WIDTH = 260;
	private static final int PANEL_HEIGHT = 190;
	private final Screen parent;
	private double eyeWidth;
	private double eyeHeight;
	private double eyeSpacing;
	private double eyeYOffset;
	private double eyeZOffset;

	public DojutsuEyeConfigScreen(Screen parent) {
		super(Component.literal("Dojutsu Eye Config"));
		this.parent = parent;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			DojutsuEyeConfigStore.Config config = DojutsuEyeConfigStore.get(minecraft.player.getId());
			this.eyeWidth = config.width();
			this.eyeHeight = config.height();
			this.eyeSpacing = config.spacing();
			this.eyeYOffset = config.yOffset();
			this.eyeZOffset = config.zOffset();
		} else {
			this.eyeWidth = 2.0D;
			this.eyeHeight = 2.0D;
			this.eyeSpacing = 4.0D;
			this.eyeYOffset = -3.2D;
			this.eyeZOffset = -4.08D;
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		guiGraphics.fill(0, 0, this.width, this.height, 0x66000000);
		guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xD0101010);
		guiGraphics.fill(left, top, left + PANEL_WIDTH, top + 1, 0x80FFFFFF);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, 0xFFFFFF);
		renderPreview(guiGraphics, left + 18, top + 34);
		int controlsX = left + 104;
		guiGraphics.drawString(this.font, "Eye shape", controlsX, top + 34, 0xE8E8E8, false);
		renderButton(guiGraphics, controlsX, top + 48, 34, 18, "1x1");
		renderButton(guiGraphics, controlsX + 38, top + 48, 34, 18, "2x1");
		renderButton(guiGraphics, controlsX + 76, top + 48, 34, 18, "2x2");
		renderButton(guiGraphics, controlsX + 114, top + 48, 34, 18, "2x3");
		renderStepper(guiGraphics, controlsX, top + 76, "Spacing", eyeSpacing);
		renderStepper(guiGraphics, controlsX, top + 100, "Height", -eyeYOffset);
		renderStepper(guiGraphics, controlsX, top + 124, "Depth", -eyeZOffset);
		renderButton(guiGraphics, left + 68, top + 160, 56, 18, "Apply");
		renderButton(guiGraphics, left + 136, top + 160, 56, 18, "Done");
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
	}

	private void renderPreview(GuiGraphics guiGraphics, int x, int y) {
		guiGraphics.drawString(this.font, "Preview", x, y - 12, 0xE8E8E8, false);
		int scale = 8;
		int headX = x + 8;
		int headY = y + 8;
		guiGraphics.fill(headX, headY, headX + 64, headY + 64, 0xFF8D6A50);
		guiGraphics.fill(headX, headY, headX + 64, headY + 10, 0xFF3A271D);
		drawPreviewEye(guiGraphics, headX, headY, -eyeSpacing * 0.5D, eyeYOffset, scale);
		drawPreviewEye(guiGraphics, headX, headY, eyeSpacing * 0.5D, eyeYOffset, scale);
		guiGraphics.drawString(this.font, formatSize(), x + 8, y + 78, 0xBBBBBB, false);
	}

	private void drawPreviewEye(GuiGraphics guiGraphics, int headX, int headY, double centerX, double centerY, int scale) {
		int eyeX0 = headX + Mth.floor((4.0D + centerX - eyeWidth * 0.5D) * scale);
		int eyeY0 = headY + Mth.floor((8.0D + centerY - eyeHeight * 0.5D) * scale);
		int eyeX1 = headX + Mth.ceil((4.0D + centerX + eyeWidth * 0.5D) * scale);
		int eyeY1 = headY + Mth.ceil((8.0D + centerY + eyeHeight * 0.5D) * scale);
		guiGraphics.fill(eyeX0, eyeY0, eyeX1, eyeY1, 0xFFE02020);
		guiGraphics.fill(eyeX0, eyeY0, eyeX1, eyeY0 + 1, 0xFFFFC8C8);
	}

	private void renderStepper(GuiGraphics guiGraphics, int x, int y, String label, double value) {
		guiGraphics.drawString(this.font, label + ": " + String.format("%.1f", value), x, y + 5, 0xE8E8E8, false);
		renderButton(guiGraphics, x + 92, y, 18, 18, "-");
		renderButton(guiGraphics, x + 114, y, 18, 18, "+");
	}

	private void renderButton(GuiGraphics guiGraphics, int x, int y, int w, int h, String label) {
		guiGraphics.fill(x, y, x + w, y + h, 0xAA242424);
		guiGraphics.fill(x, y, x + w, y + 1, 0x80FFFFFF);
		guiGraphics.drawCenteredString(this.font, label, x + w / 2, y + 5, 0xFFFFFF);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		int controlsX = left + 104;
		if (button == 0) {
			if (hit(mouseX, mouseY, controlsX, top + 48, 34, 18)) {
				setShape(1.0D, 1.0D);
				return true;
			}
			if (hit(mouseX, mouseY, controlsX + 38, top + 48, 34, 18)) {
				setShape(2.0D, 1.0D);
				return true;
			}
			if (hit(mouseX, mouseY, controlsX + 76, top + 48, 34, 18)) {
				setShape(2.0D, 2.0D);
				return true;
			}
			if (hit(mouseX, mouseY, controlsX + 114, top + 48, 34, 18)) {
				setShape(2.0D, 3.0D);
				return true;
			}
			if (handleStepper(mouseX, mouseY, controlsX, top + 76, 0)) {
				return true;
			}
			if (handleStepper(mouseX, mouseY, controlsX, top + 100, 1)) {
				return true;
			}
			if (handleStepper(mouseX, mouseY, controlsX, top + 124, 2)) {
				return true;
			}
			if (hit(mouseX, mouseY, left + 68, top + 160, 56, 18)) {
				apply();
				return true;
			}
			if (hit(mouseX, mouseY, left + 136, top + 160, 56, 18)) {
				apply();
				this.minecraft.setScreen(parent);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean handleStepper(double mouseX, double mouseY, int x, int y, int index) {
		if (hit(mouseX, mouseY, x + 92, y, 18, 18)) {
			adjust(index, -0.25D);
			return true;
		}
		if (hit(mouseX, mouseY, x + 114, y, 18, 18)) {
			adjust(index, 0.25D);
			return true;
		}
		return false;
	}

	private void setShape(double width, double height) {
		this.eyeWidth = width;
		this.eyeHeight = height;
	}

	private void adjust(int index, double amount) {
		if (index == 0) {
			eyeSpacing = Mth.clamp(eyeSpacing + amount, 1.0D, 7.0D);
		} else if (index == 1) {
			eyeYOffset = Mth.clamp(eyeYOffset - amount, -7.0D, -1.0D);
		} else {
			eyeZOffset = Mth.clamp(eyeZOffset - amount * 0.1D, -4.35D, -3.75D);
		}
	}

	private void apply() {
		if (this.minecraft != null && this.minecraft.player != null) {
			DojutsuEyeConfigStore.set(this.minecraft.player.getId(), eyeWidth, eyeHeight, eyeSpacing, eyeYOffset, eyeZOffset);
			PacketDistributor.sendToServer(new SetDojutsuEyeConfigPacket(eyeWidth, eyeHeight, eyeSpacing, eyeYOffset, eyeZOffset));
		}
	}

	private String formatSize() {
		return String.format("%.0fx%.0f px", eyeWidth, eyeHeight);
	}

	private boolean hit(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
