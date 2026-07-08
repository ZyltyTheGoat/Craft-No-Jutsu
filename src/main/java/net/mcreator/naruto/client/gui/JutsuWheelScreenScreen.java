package net.mcreator.naruto.client.gui;

import org.joml.Matrix4f;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.mcreator.naruto.world.inventory.NinjutsuWheelMenu;
import net.mcreator.naruto.world.inventory.NatureReleasesWheelMenu;
import net.mcreator.naruto.world.inventory.KekkeiGenkaiWheelMenu;
import net.mcreator.naruto.world.inventory.JutsuWheelScreenMenu;
import net.mcreator.naruto.world.inventory.DojutsuWheelMenu;
import net.mcreator.naruto.procedures.JutsuWheelOnKeyPressedProcedure;
import net.mcreator.naruto.init.NarutoModScreens;
import net.mcreator.naruto.init.NarutoModKeyMappings;

import java.util.List;
import java.util.Arrays;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.systems.RenderSystem;

public class JutsuWheelScreenScreen extends AbstractContainerScreen<JutsuWheelScreenMenu> implements NarutoModScreens.ScreenAccessor {
	private static final int RADIUS_IN = 42;
	private static final int RADIUS_OUT = 160;
	private static final int BASE_CIRCLE_COLOR = 0x96000000;
	private static final int HOVER_CIRCLE_COLOR = 0x96FFFFFF;
	private static final int PAGE_INDICATOR_COLOR = 0xFFFFFFFF;
	private static final int PAGE_DOT_COLOR = 0x80FFFFFF;
	private static final int PAGE_DOT_ACTIVE_COLOR = 0xFFFFFFFF;
	private static final float GAP_WIDTH_PIXELS = 0F;
	private final Level world;
	private final int x, y, z;
	private final Player entity;

	private static class Page {
		final String name;
		final List<String> jutsus;

		Page(String name, List<String> jutsus) {
			this.name = name;
			this.jutsus = jutsus;
		}
	}

	private final List<Page> pages = Arrays.asList(new Page("Basic Techniques", Arrays.asList("Basic Ninjutsu", "Taijutsu", "Nature Releases", "Kekkei Genkai", "Dojutsu")));
	private int currentPage = 0;
	private boolean menuStateUpdateActive = false;
	private int hovered = -1;
	private final WheelGuiHelper.AnimationState animation = new WheelGuiHelper.AnimationState();

	public JutsuWheelScreenScreen(JutsuWheelScreenMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	public void init() {
		super.init();
		if (this.minecraft == null) {
			this.minecraft = Minecraft.getInstance();
		}
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
		menuStateUpdateActive = true;
		menuStateUpdateActive = false;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		if (this.minecraft == null)
			return;
		WheelGuiHelper.syncMovementKeys();
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		WheelGuiHelper.renderWheelBackdrop(guiGraphics, this.width, this.height);
		int centerX = this.width / 2;
		int centerY = this.height / 2;
		animation.finishIfClosed();
		guiGraphics.pose().pushPose();
		WheelGuiHelper.applyWheelAnimation(guiGraphics, centerX, centerY, animation);
		renderRadialWheel(guiGraphics, centerX, centerY, animation.openness());
		renderJutsuLabels(guiGraphics, centerX, centerY);
		guiGraphics.pose().popPose();
		renderHoveredCategoryInfo(guiGraphics);
		renderPageIndicator(guiGraphics);
	}

	private List<String> getCurrentJutsus() {
		if (currentPage >= 0 && currentPage < pages.size()) {
			return pages.get(currentPage).jutsus;
		}
		return Arrays.asList();
	}

	private String getCurrentPageName() {
		if (currentPage >= 0 && currentPage < pages.size()) {
			return pages.get(currentPage).name;
		}
		return "";
	}

	private void renderRadialWheel(GuiGraphics guiGraphics, int centerX, int centerY, float alphaMultiplier) {
		List<String> jutsus = getCurrentJutsus();
		if (jutsus.isEmpty())
			return;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		NarutoWheelShaders.prepareWheelSliceShader(this.width, this.height);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		guiGraphics.pose().pushPose();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < jutsus.size(); i++) {
			int outerRadius = this.hovered == i ? RADIUS_OUT + 10 : RADIUS_OUT;
			int color = WheelGuiHelper.softCategoryColor(jutsus.get(i), this.hovered == i, alphaMultiplier);
			if (jutsus.size() > 1) {
				float gapAngleIn = GAP_WIDTH_PIXELS / RADIUS_IN;
				float gapAngleOut = GAP_WIDTH_PIXELS / outerRadius;
				float startAngleIn = getAngleFor(i - 0.5F, jutsus.size()) + gapAngleIn / 2;
				float endAngleIn = getAngleFor(i + 0.5F, jutsus.size()) - gapAngleIn / 2;
				float startAngleOut = getAngleFor(i - 0.5F, jutsus.size()) + gapAngleOut / 2;
				float endAngleOut = getAngleFor(i + 0.5F, jutsus.size()) - gapAngleOut / 2;
				drawSliceWithGaps(guiGraphics.pose(), buffer, centerX, centerY, startAngleIn, endAngleIn, startAngleOut, endAngleOut, outerRadius, color);
			} else {
				float startAngle = getAngleFor(i - 0.5F, jutsus.size());
				float endAngle = getAngleFor(i + 0.5F, jutsus.size());
				drawSlice(guiGraphics.pose(), buffer, centerX, centerY, startAngle, endAngle, outerRadius, color);
			}
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.disableBlend();
		guiGraphics.pose().popPose();
	}

	private void renderJutsuLabels(GuiGraphics guiGraphics, int centerX, int centerY) {
		if (font == null)
			return;
		List<String> jutsus = getCurrentJutsus();
		if (jutsus.isEmpty())
			return;
		float radius = (RADIUS_IN + RADIUS_OUT) / 2.0F;
		for (int i = 0; i < jutsus.size(); i++) {
			float startAngle = getAngleFor(i - 0.5F, jutsus.size());
			float endAngle = getAngleFor(i + 0.5F, jutsus.size());
			float middleAngle = (startAngle + endAngle) / 2.0F;
			int posX = (int) (centerX + radius * Math.cos(middleAngle));
			int posY = (int) (centerY + radius * Math.sin(middleAngle));
			WheelGuiHelper.drawCenteredWrappedString(guiGraphics, font, jutsus.get(i), posX, posY, 86, 0xFFFFFFFF);
		}
	}

	private void renderHoveredCategoryInfo(GuiGraphics guiGraphics) {
		List<String> jutsus = getCurrentJutsus();
		if (hovered < 0 || hovered >= jutsus.size()) {
			return;
		}
		String selected = jutsus.get(hovered);
		WheelGuiHelper.renderCategoryInfoPanel(guiGraphics, font, this.width, this.height, selected, getCategoryDescription(selected));
	}

	private String getCategoryDescription(String category) {
		if ("Basic Ninjutsu".equals(category)) {
			return "Core techniques and utility jutsu.";
		}
		if ("Nature Releases".equals(category)) {
			return "Fire, water, earth, lightning, wind, yin, and yang techniques.";
		}
		if ("Kekkei Genkai".equals(category)) {
			return "Bloodline releases unlocked on this character.";
		}
		if ("Dojutsu".equals(category)) {
			return "Eye techniques and their active modes.";
		}
		if ("Taijutsu".equals(category)) {
			return "Coming later.";
		}
		return "";
	}

	private void renderPageIndicator(GuiGraphics guiGraphics) {
		if (font == null || pages.size() < 1)
			return;
		// Render page name at top (larger)
		String pageName = getCurrentPageName();
		guiGraphics.pose().pushPose();
		guiGraphics.pose().scale(1.5F, 1.5F, 1.5F);
		guiGraphics.drawCenteredString(font, pageName, (int) (this.width / 2 / 1.5F), (int) (40 / 1.5F), PAGE_INDICATOR_COLOR);
		guiGraphics.pose().popPose();
		if (pages.size() > 1) {
			// Render page dots
			int dotY = this.height - 40;
			int dotSpacing = 15;
			int totalWidth = (pages.size() - 1) * dotSpacing;
			int startX = (this.width - totalWidth) / 2;
			for (int i = 0; i < pages.size(); i++) {
				int dotX = startX + i * dotSpacing;
				int color = (i == currentPage) ? PAGE_DOT_ACTIVE_COLOR : PAGE_DOT_COLOR;
				int size = (i == currentPage) ? 4 : 3;
				guiGraphics.fill(dotX - size / 2, dotY - size / 2, dotX + size / 2, dotY + size / 2, color);
			}
		}
		WheelGuiHelper.renderCategoryWheelHint(guiGraphics, font, this.width, this.height, pages.size() > 1);
	}

	private void drawSliceWithGaps(PoseStack poseStack, BufferBuilder buffer, float centerX, float centerY, float startAngleIn, float endAngleIn, float startAngleOut, float endAngleOut, int radiusOut, int color) {
		float angleIn = endAngleIn - startAngleIn;
		float angleOut = endAngleOut - startAngleOut;
		float precision = 2.5F / 360.0F;
		int sectionsIn = Math.max(1, Mth.ceil(angleIn / precision));
		int sectionsOut = Math.max(1, Mth.ceil(angleOut / precision));
		int sections = Math.max(sectionsIn, sectionsOut);
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		int a = (color >> 24) & 0xFF;
		float sliceIn = angleIn / sections;
		float sliceOut = angleOut / sections;
		Matrix4f matrix4f = poseStack.last().pose();
		for (int i = 0; i < sections; i++) {
			float angle1In = startAngleIn + i * sliceIn;
			float angle2In = startAngleIn + (i + 1) * sliceIn;
			float angle1Out = startAngleOut + i * sliceOut;
			float angle2Out = startAngleOut + (i + 1) * sliceOut;
			float x1 = centerX + RADIUS_IN * (float) Math.cos(angle1In);
			float y1 = centerY + RADIUS_IN * (float) Math.sin(angle1In);
			float x2 = centerX + radiusOut * (float) Math.cos(angle1Out);
			float y2 = centerY + radiusOut * (float) Math.sin(angle1Out);
			float x3 = centerX + radiusOut * (float) Math.cos(angle2Out);
			float y3 = centerY + radiusOut * (float) Math.sin(angle2Out);
			float x4 = centerX + RADIUS_IN * (float) Math.cos(angle2In);
			float y4 = centerY + RADIUS_IN * (float) Math.sin(angle2In);
			buffer.addVertex(matrix4f, x2, y2, 0.0F).setColor(r, g, b, a);
			buffer.addVertex(matrix4f, x1, y1, 0.0F).setColor(r, g, b, a);
			buffer.addVertex(matrix4f, x4, y4, 0.0F).setColor(r, g, b, a);
			buffer.addVertex(matrix4f, x3, y3, 0.0F).setColor(r, g, b, a);
		}
	}

	private void drawSlice(PoseStack poseStack, BufferBuilder buffer, float centerX, float centerY, float startAngle, float endAngle, int radiusOut, int color) {
		float angle = endAngle - startAngle;
		float precision = 2.5F / 360.0F;
		int sections = Math.max(1, Mth.ceil(angle / precision));
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		int a = (color >> 24) & 0xFF;
		float slice = angle / sections;
		Matrix4f matrix4f = poseStack.last().pose();
		for (int i = 0; i < sections; i++) {
			float angle1 = startAngle + i * slice;
			float angle2 = startAngle + (i + 1) * slice;
			float x1 = centerX + RADIUS_IN * (float) Math.cos(angle1);
			float y1 = centerY + RADIUS_IN * (float) Math.sin(angle1);
			float x2 = centerX + radiusOut * (float) Math.cos(angle1);
			float y2 = centerY + radiusOut * (float) Math.sin(angle1);
			float x3 = centerX + radiusOut * (float) Math.cos(angle2);
			float y3 = centerY + radiusOut * (float) Math.sin(angle2);
			float x4 = centerX + RADIUS_IN * (float) Math.cos(angle2);
			float y4 = centerY + RADIUS_IN * (float) Math.sin(angle2);
			buffer.addVertex(matrix4f, x2, y2, 0.0F).setColor(r, g, b, a);
			buffer.addVertex(matrix4f, x1, y1, 0.0F).setColor(r, g, b, a);
			buffer.addVertex(matrix4f, x4, y4, 0.0F).setColor(r, g, b, a);
			buffer.addVertex(matrix4f, x3, y3, 0.0F).setColor(r, g, b, a);
		}
	}

	private float getAngleFor(float index, int totalItems) {
		if (totalItems == 0)
			return 0;
		return (float) (((index / totalItems) + 0.25D) * Mth.TWO_PI + Math.PI);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		super.mouseMoved(mouseX, mouseY);
		List<String> jutsus = getCurrentJutsus();
		if (jutsus.isEmpty())
			return;
		int centerX = this.width / 2;
		int centerY = this.height / 2;
		double mouseAngle = Math.atan2(mouseY - centerY, mouseX - centerX);
		double mouseDistance = Math.sqrt(Math.pow(mouseX - centerX, 2.0D) + Math.pow(mouseY - centerY, 2.0D));
		float startAngle = getAngleFor(-0.5F, jutsus.size());
		float endAngle = getAngleFor(jutsus.size() - 0.5F, jutsus.size());
		while (mouseAngle < startAngle)
			mouseAngle += Mth.TWO_PI;
		while (mouseAngle >= endAngle)
			mouseAngle -= Mth.TWO_PI;
		this.hovered = -1;
		for (int i = 0; i < jutsus.size(); i++) {
			if (jutsus.size() > 1) {
				// Calculate gap angles for both inner and outer radius
				float gapAngleIn = GAP_WIDTH_PIXELS / RADIUS_IN;
				float gapAngleOut = GAP_WIDTH_PIXELS / RADIUS_OUT;
				// Interpolate gap angle based on mouse distance
				float gapAngle = gapAngleOut + (gapAngleIn - gapAngleOut) * (float) ((mouseDistance - RADIUS_OUT) / (RADIUS_IN - RADIUS_OUT));
				float currentStart = getAngleFor(i - 0.5F, jutsus.size()) + gapAngle / 2;
				float currentEnd = getAngleFor(i + 0.5F, jutsus.size()) - gapAngle / 2;
				if (mouseAngle >= currentStart && mouseAngle < currentEnd && mouseDistance >= RADIUS_IN && mouseDistance < RADIUS_OUT) {
					this.hovered = i;
					break;
				}
			} else {
				float currentStart = getAngleFor(i - 0.5F, jutsus.size());
				float currentEnd = getAngleFor(i + 0.5F, jutsus.size());
				if (mouseAngle >= currentStart && mouseAngle < currentEnd && mouseDistance >= RADIUS_IN && mouseDistance < RADIUS_OUT) {
					this.hovered = i;
					break;
				}
			}
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (pages.size() <= 1)
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		if (scrollY > 0) {
			currentPage--;
			if (currentPage < 0)
				currentPage = pages.size() - 1;
			this.hovered = -1;
			return true;
		} else if (scrollY < 0) {
			currentPage++;
			if (currentPage >= pages.size())
				currentPage = 0;
			this.hovered = -1;
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (animation.isClosing()) {
			return true;
		}
		if (button == 1 && this.minecraft != null && this.minecraft.player != null) {
			animation.startClosing(() -> this.minecraft.player.closeContainer());
			return true;
		}
		List<String> jutsus = getCurrentJutsus();
		if (this.hovered >= 0 && this.hovered < jutsus.size()) {
			if (this.minecraft != null && this.minecraft.player != null) {
				String selectedJutsu = jutsus.get(this.hovered);
				// Check if Nature Releases was selected
				if (selectedJutsu.equals("Nature Releases")) {
					// Store reference to minecraft
					Minecraft mc = this.minecraft;
					Player player = this.minecraft.player;
					int posX = this.x;
					int posY = this.y;
					int posZ = this.z;
					animation.startClosing(() -> mc.tell(() -> {
						FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
						buffer.writeBlockPos(new net.minecraft.core.BlockPos(posX, posY, posZ));
						buffer.resetReaderIndex();
						mc.setScreen(new NatureReleasesWheelScreen(new NatureReleasesWheelMenu(0, player.getInventory(), buffer), player.getInventory(), Component.literal("Nature Releases")));
					}));
				} else if (selectedJutsu.equals("Dojutsu")) {
					// Store reference to minecraft
					Minecraft mc = this.minecraft;
					Player player = this.minecraft.player;
					int posX = this.x;
					int posY = this.y;
					int posZ = this.z;
					animation.startClosing(() -> mc.tell(() -> {
						FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
						buffer.writeBlockPos(new net.minecraft.core.BlockPos(posX, posY, posZ));
						buffer.resetReaderIndex();
						mc.setScreen(new DojutsuWheelScreen(new DojutsuWheelMenu(0, player.getInventory(), buffer), player.getInventory(), Component.literal("Dojutsu")));
					}));
				} else if (selectedJutsu.equals("Kekkei Genkai")) {
					// Store reference to minecraft
					Minecraft mc = this.minecraft;
					Player player = this.minecraft.player;
					int posX = this.x;
					int posY = this.y;
					int posZ = this.z;
					animation.startClosing(() -> mc.tell(() -> {
						FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
						buffer.writeBlockPos(new net.minecraft.core.BlockPos(posX, posY, posZ));
						buffer.resetReaderIndex();
						mc.setScreen(new KekkeiGenkaiWheelScreen(new KekkeiGenkaiWheelMenu(0, player.getInventory(), buffer), player.getInventory(), Component.literal("Kekkei Genkai")));
					}));
				} else if (selectedJutsu.equals("Basic Ninjutsu")) {
					// Store reference to minecraft
					Minecraft mc = this.minecraft;
					Player player = this.minecraft.player;
					int posX = this.x;
					int posY = this.y;
					int posZ = this.z;
					animation.startClosing(() -> mc.tell(() -> {
						FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
						buffer.writeBlockPos(new net.minecraft.core.BlockPos(posX, posY, posZ));
						buffer.resetReaderIndex();
						mc.setScreen(new NinjutsuWheelScreen(new NinjutsuWheelMenu(0, player.getInventory(), buffer), player.getInventory(), Component.literal("Basic Ninjutsu")));
					}));
				} else {
					// Handle other selections
					animation.startClosing(() -> this.minecraft.player.closeContainer());
				}
			}
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (key == 256) {
			if (this.minecraft != null && this.minecraft.player != null) {
				animation.startClosing(() -> this.minecraft.player.closeContainer());
				return true;
			}
		}
		if (WheelGuiHelper.handleMovementKey(key, scanCode, true)) {
			return true;
		}
		return super.keyPressed(key, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int key, int scanCode, int modifiers) {
		if (WheelGuiHelper.handleMovementKey(key, scanCode, false)) {
			return true;
		}
		if (key == NarutoModKeyMappings.JUTSU_WHEEL.getKey().getValue()) {
			animation.startClosing(() -> this.minecraft.player.closeContainer());
			return true;
		}
		return super.keyReleased(key, scanCode, modifiers);
	}

	@Override
	public void removed() {
		if (this.minecraft != null && this.minecraft.player != null) {
			JutsuWheelOnKeyPressedProcedure.recordCloseTime();
			super.removed();
		}
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}
}
