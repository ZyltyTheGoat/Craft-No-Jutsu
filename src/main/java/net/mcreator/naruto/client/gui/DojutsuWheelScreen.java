package net.mcreator.naruto.client.gui;

import org.joml.Matrix4f;

import org.checkerframework.checker.units.qual.s;
import org.checkerframework.checker.units.qual.g;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.mcreator.naruto.world.inventory.DojutsuWheelMenu;
import net.mcreator.naruto.procedures.JutsuWheelOnKeyPressedProcedure;
import net.mcreator.naruto.network.SetToggleJutsuPacket;
import net.mcreator.naruto.network.SetFavouriteJutsuPacket;
import net.mcreator.naruto.network.SetActiveJutsuPacket;
import net.mcreator.naruto.network.SetActiveDojutsuPacket;
import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.init.NarutoModScreens;
import net.mcreator.naruto.init.NarutoModKeyMappings;
import net.mcreator.naruto.JutsuRegistry;
import net.mcreator.naruto.JutsuIdList;
import net.mcreator.naruto.JutsuData;

import java.util.stream.Collectors;
import java.util.*;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;

import com.jcraft.jogg.Page;

public class DojutsuWheelScreen extends AbstractContainerScreen<DojutsuWheelMenu> implements NarutoModScreens.ScreenAccessor {
	private static final int RADIUS_IN = 42;
	private static final int RADIUS_OUT = 160;
	private static final int RADIUS_COLOR_OUT = 165; // Outer radius for color ring (extends from RADIUS_OUT)
	private static final int BASE_CIRCLE_COLOR = 0x96000000;
	private static final int HOVER_CIRCLE_COLOR = 0x96FFFFFF;
	private static final int PAGE_INDICATOR_COLOR = 0xFFFFFFFF;
	private static final int PAGE_DOT_COLOR = 0x80FFFFFF;
	private static final int PAGE_DOT_ACTIVE_COLOR = 0xFFFFFFFF;
	private static final float GAP_WIDTH_PIXELS = 0F;
	private static final int ICON_SIZE = 64; // Size of the nature icon in pixels
	private final Level world;
	private final int x, y, z;
	private final Player entity;

	private static class Page {
		final String name;
		final String nature; // Store the nature type for icon lookup
		final List<String> jutsuIds;
		final List<String> jutsuNames;

		Page(String name, String nature, List<String> jutsuIds, List<String> jutsuNames) {
			this.name = name;
			this.nature = nature;
			this.jutsuIds = jutsuIds;
			this.jutsuNames = jutsuNames;
		}
	}

	private final List<Page> pages = new ArrayList<>();
	private int currentPage = 0;
	private boolean menuStateUpdateActive = false;
	private int hovered = -1;
	private final WheelGuiHelper.AnimationState animation = new WheelGuiHelper.AnimationState();

	public DojutsuWheelScreen(DojutsuWheelMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 166;
		// Ensure jutsus are initialized
		JutsuRegistry.initializeJutsus();
		// Initialize pages dynamically from player's unlocked jutsus and natures
		initializePages();
	}

	private void initializePages() {
		// Get player's unlocked dojutsu string
		String unlockedDojutsuString = entity.getData(NarutoModVariables.PLAYER_VARIABLES).unlockedDojutsu;
		if (unlockedDojutsuString == null)
			unlockedDojutsuString = "";
		// Parse unlocked dojutsu into a set
		Set<String> unlockedDojutsu = new HashSet<>();
		if (!unlockedDojutsuString.isEmpty()) {
			String[] dojutsuArray = unlockedDojutsuString.split(",");
			for (String dojutsu : dojutsuArray) {
				unlockedDojutsu.add(dojutsu.trim().toUpperCase());
			}
		}
		// Define readable names for each dojutsu
		Map<String, String> dojutsuDisplayNames = new HashMap<>();
		dojutsuDisplayNames.put("SHARINGAN", "Sharingan");
		dojutsuDisplayNames.put("SHARINGAN_ITACHI", "Mangekyō Sharingan - Itachi");
		dojutsuDisplayNames.put("SHARINGAN_KAMUI", "Mangekyō Sharingan - Kamui");
		dojutsuDisplayNames.put("SHARINGAN_SASUKE", "Mangekyō Sharingan - Sasuke");
		dojutsuDisplayNames.put("SHARINGAN_KOTOAMATSUKAMI", "Mangekyō Sharingan - Kotoamatsukami");
		dojutsuDisplayNames.put("SHARINGAN_OHIRUME", "Mangekyō Sharingan - Ōhirume");
		dojutsuDisplayNames.put("BYAKUGAN", "Byakugan");
		dojutsuDisplayNames.put("RINNEGAN", "Rinnegan");
		dojutsuDisplayNames.put("TENSEIGAN", "Tenseigan");
		// Predefined page order
		List<String> pageOrder = Arrays.asList("SHARINGAN", "SHARINGAN_ITACHI", "SHARINGAN_KAMUI", "SHARINGAN_SASUKE", "SHARINGAN_KOTOAMATSUKAMI", "SHARINGAN_OHIRUME", "BYAKUGAN", "RINNEGAN", "TENSEIGAN");
		// Create a temporary map of pages
		Map<String, Page> tempPages = new HashMap<>();
		for (String dojutsu : unlockedDojutsu) {
			// Get jutsus belonging to this dojutsu
			Map<String, JutsuData> dojutsuJutsus = JutsuRegistry.getJutsusByNature(dojutsu);
			List<String> jutsuIds = new ArrayList<>();
			List<String> jutsuNames = new ArrayList<>();
			for (Map.Entry<String, JutsuData> entry : dojutsuJutsus.entrySet()) {
				JutsuData jutsu = entry.getValue();
				String jutsuId = entry.getKey();
				if (jutsu.getType().equalsIgnoreCase("DOJUTSU")) {
					jutsuIds.add(jutsuId);
					jutsuNames.add(jutsu.getName());
				}
			}
			// Get display name
			String displayName = dojutsuDisplayNames.getOrDefault(dojutsu, dojutsu.substring(0, 1) + dojutsu.substring(1).toLowerCase());
			if (!jutsuIds.isEmpty()) {
				tempPages.put(dojutsu, new Page(displayName, dojutsu, jutsuIds, jutsuNames));
			}
		}
		// Clear pages and add them in predefined order
		pages.clear();
		for (String dojutsu : pageOrder) {
			if (tempPages.containsKey(dojutsu)) {
				pages.add(tempPages.get(dojutsu));
			}
		}
		// Fallback if player has no unlocked dojutsu
		if (pages.isEmpty()) {
			pages.add(new Page("No Unlocked Dojutsu", "", new ArrayList<>(), new ArrayList<>()));
		}
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
		WheelGuiHelper.renderWheelBackdrop(guiGraphics, this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight());
		int windowWidth = this.minecraft.getWindow().getGuiScaledWidth();
		int windowHeight = this.minecraft.getWindow().getGuiScaledHeight();
		int centerX = windowWidth / 2;
		int centerY = windowHeight / 2;
		animation.finishIfClosed();
		guiGraphics.pose().pushPose();
		WheelGuiHelper.applyWheelAnimation(guiGraphics, centerX, centerY, animation);
		renderRadialWheel(guiGraphics, centerX, centerY, animation.openness());
		renderDojutsuIcon(guiGraphics, centerX, centerY); // Render the nature icon in the center
		renderJutsuLabels(guiGraphics, centerX, centerY);
		guiGraphics.pose().popPose();
		if (getCurrentJutsuIds().isEmpty()) {
			WheelGuiHelper.renderCategoryInfoPanel(guiGraphics, font, windowWidth, windowHeight, "No Unlocked Dojutsu", "Unlock a dojutsu to view its techniques.");
		} else {
			renderHoveredJutsuInfo(guiGraphics, windowWidth, windowHeight);
		}
		renderPageIndicator(guiGraphics, windowWidth, windowHeight);
	}

	private List<String> getCurrentJutsuNames() {
		if (currentPage >= 0 && currentPage < pages.size()) {
			return pages.get(currentPage).jutsuNames;
		}
		return new ArrayList<>();
	}

	private List<String> getCurrentJutsuIds() {
		if (currentPage >= 0 && currentPage < pages.size()) {
			return pages.get(currentPage).jutsuIds;
		}
		return new ArrayList<>();
	}

	private String getCurrentPageName() {
		if (currentPage >= 0 && currentPage < pages.size()) {
			return pages.get(currentPage).name;
		}
		return "";
	}

	private String getCurrentNature() {
		if (currentPage >= 0 && currentPage < pages.size()) {
			return pages.get(currentPage).nature;
		}
		return "";
	}

	private Set<String> getActiveToggleJutsus() {
		Set<String> activeToggles = new HashSet<>();
		String activeToggleString = entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeToggleJutsu;
		if (activeToggleString != null && !activeToggleString.isEmpty()) {
			String[] toggleArray = activeToggleString.split(",");
			for (String toggle : toggleArray) {
				activeToggles.add(toggle.trim());
			}
		}
		return activeToggles;
	}

	private boolean isJutsuToggled(String jutsuId) {
		return getActiveToggleJutsus().contains(jutsuId);
	}

	private void renderDojutsuIcon(GuiGraphics guiGraphics, int centerX, int centerY) {
		String dojutsu = getCurrentNature(); // The "nature" field now stores dojutsu name
		if (dojutsu.isEmpty())
			return;
		// Format the icon name: "<dojutsu>_icon.png"
		String iconName = "icon_" + dojutsu.toLowerCase();
		ResourceLocation iconLocation = ResourceLocation.fromNamespaceAndPath("naruto", "textures/screens/" + iconName + ".png");
		int iconX = centerX - ICON_SIZE / 2;
		int iconY = centerY - ICON_SIZE / 2;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(iconLocation, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
		RenderSystem.disableBlend();
	}

	private void renderRadialWheel(GuiGraphics guiGraphics, int centerX, int centerY, float alphaMultiplier) {
		List<String> jutsuNames = getCurrentJutsuNames();
		List<String> jutsuIds = getCurrentJutsuIds();
		if (jutsuNames.isEmpty())
			return;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		NarutoWheelShaders.prepareDojutsuWheelSliceShader(this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight());
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		guiGraphics.pose().pushPose();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < jutsuNames.size(); i++) {
			// Check if this jutsu is toggled
			boolean isToggled = false;
			if (i < jutsuIds.size()) {
				String jutsuId = jutsuIds.get(i);
				isToggled = isJutsuToggled(jutsuId);
			}
			// Determine color: if toggled, use hover color; if hovered and toggled, use base color; otherwise normal
			JutsuData sliceJutsuData = i < jutsuIds.size() ? JutsuRegistry.getJutsu(jutsuIds.get(i)) : null;
			int outerRadius = this.hovered == i ? RADIUS_OUT + 10 : RADIUS_OUT;
			int color = WheelGuiHelper.softJutsuColor(sliceJutsuData, this.hovered == i, alphaMultiplier);
			if (jutsuNames.size() > 1) {
				float gapAngleIn = GAP_WIDTH_PIXELS / RADIUS_IN;
				float gapAngleOut = GAP_WIDTH_PIXELS / outerRadius;
				float startAngleIn = getAngleFor(i - 0.5F, jutsuNames.size()) + gapAngleIn / 2;
				float endAngleIn = getAngleFor(i + 0.5F, jutsuNames.size()) - gapAngleIn / 2;
				float startAngleOut = getAngleFor(i - 0.5F, jutsuNames.size()) + gapAngleOut / 2;
				float endAngleOut = getAngleFor(i + 0.5F, jutsuNames.size()) - gapAngleOut / 2;
				drawSliceWithGaps(guiGraphics.pose(), buffer, centerX, centerY, startAngleIn, endAngleIn, startAngleOut, endAngleOut, RADIUS_IN, outerRadius, color);
			} else {
				float startAngle = getAngleFor(i - 0.5F, jutsuNames.size());
				float endAngle = getAngleFor(i + 0.5F, jutsuNames.size());
				drawSlice(guiGraphics.pose(), buffer, centerX, centerY, startAngle, endAngle, RADIUS_IN, outerRadius, color);
			}
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.disableBlend();
		guiGraphics.pose().popPose();
	}

	private void renderColorRing(GuiGraphics guiGraphics, int centerX, int centerY) {
		List<String> jutsuNames = getCurrentJutsuNames();
		List<String> jutsuIds = getCurrentJutsuIds();
		// Add safety check - both lists must have the same size and not be empty
		if (jutsuNames.isEmpty() || jutsuIds.isEmpty() || jutsuNames.size() != jutsuIds.size()) {
			return;
		}
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		guiGraphics.pose().pushPose();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < jutsuNames.size(); i++) {
			// Get the jutsu color from JutsuData
			String jutsuId = jutsuIds.get(i);
			JutsuData jutsuData = JutsuRegistry.getJutsu(jutsuId);
			int color = jutsuData != null ? jutsuData.getColor() : 0x96FFFFFF; // Default to white if not found
			if (jutsuNames.size() > 1) {
				// Use same gap angles as outer radius to create seamless continuation
				float gapAngleOut = GAP_WIDTH_PIXELS / RADIUS_OUT;
				float gapAngleColorOut = GAP_WIDTH_PIXELS / RADIUS_COLOR_OUT;
				float startAngleOut = getAngleFor(i - 0.5F, jutsuNames.size()) + gapAngleOut / 2;
				float endAngleOut = getAngleFor(i + 0.5F, jutsuNames.size()) - gapAngleOut / 2;
				float startAngleColorOut = getAngleFor(i - 0.5F, jutsuNames.size()) + gapAngleColorOut / 2;
				float endAngleColorOut = getAngleFor(i + 0.5F, jutsuNames.size()) - gapAngleColorOut / 2;
				drawSliceWithGaps(guiGraphics.pose(), buffer, centerX, centerY, startAngleOut, endAngleOut, startAngleColorOut, endAngleColorOut, RADIUS_OUT, RADIUS_COLOR_OUT, color);
			} else {
				float startAngle = getAngleFor(i - 0.5F, jutsuNames.size());
				float endAngle = getAngleFor(i + 0.5F, jutsuNames.size());
				drawSlice(guiGraphics.pose(), buffer, centerX, centerY, startAngle, endAngle, RADIUS_OUT, RADIUS_COLOR_OUT, color);
			}
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.disableBlend();
		guiGraphics.pose().popPose();
	}

	private void renderJutsuLabels(GuiGraphics guiGraphics, int centerX, int centerY) {
		if (font == null)
			return;
		List<String> jutsuNames = getCurrentJutsuNames();
		List<String> jutsuIds = getCurrentJutsuIds();
		if (jutsuNames.isEmpty())
			return;
		Set<String> favouriteJutsus = JutsuIdList.toSet(entity.getData(NarutoModVariables.PLAYER_VARIABLES).favouriteJutsu);
		Set<String> activeToggleJutsus = JutsuIdList.toSet(entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeToggleJutsu);
		float radius = (RADIUS_IN + RADIUS_OUT) / 2.0F;
		for (int i = 0; i < jutsuNames.size(); i++) {
			float startAngle = getAngleFor(i - 0.5F, jutsuNames.size());
			float endAngle = getAngleFor(i + 0.5F, jutsuNames.size());
			float middleAngle = (startAngle + endAngle) / 2.0F;
			int posX = (int) (centerX + radius * Math.cos(middleAngle));
			int posY = (int) (centerY + radius * Math.sin(middleAngle));
			String displayName = jutsuNames.get(i);
			if (i < jutsuIds.size() && activeToggleJutsus.contains(jutsuIds.get(i))) {
				displayName = WheelGuiHelper.withActiveToggleMarker(displayName, true);
			}
			if (i < jutsuIds.size() && favouriteJutsus.contains(jutsuIds.get(i))) {
				displayName = WheelGuiHelper.withFavouriteMarker(displayName, true);
			}
			WheelGuiHelper.drawCenteredWrappedString(guiGraphics, font, displayName, posX, posY, 76, 0xFFFFFFFF);
		}
	}

	private void renderHoveredJutsuInfo(GuiGraphics guiGraphics, int windowWidth, int windowHeight) {
		List<String> jutsuIds = getCurrentJutsuIds();
		if (hovered < 0 || hovered >= jutsuIds.size()) {
			return;
		}
		Set<String> favouriteJutsus = JutsuIdList.toSet(entity.getData(NarutoModVariables.PLAYER_VARIABLES).favouriteJutsu);
		String jutsuId = jutsuIds.get(hovered);
		WheelGuiHelper.renderJutsuInfoPanel(guiGraphics, font, windowWidth, windowHeight, jutsuId, favouriteJutsus.contains(jutsuId));
	}

	private void renderPageIndicator(GuiGraphics guiGraphics, int windowWidth, int windowHeight) {
		if (font == null || pages.size() < 1)
			return;
		// Render page name at top (larger)
		String pageName = getCurrentPageName();
		guiGraphics.pose().pushPose();
		guiGraphics.pose().scale(1.5F, 1.5F, 1.5F);
		guiGraphics.drawCenteredString(font, pageName, (int) (windowWidth / 2 / 1.5F), (int) (40 / 1.5F), PAGE_INDICATOR_COLOR);
		guiGraphics.pose().popPose();
		if (pages.size() > 1) {
			// Render page dots
			int dotY = windowHeight - 40;
			int dotSpacing = 15;
			int totalWidth = (pages.size() - 1) * dotSpacing;
			int startX = (windowWidth - totalWidth) / 2;
			for (int i = 0; i < pages.size(); i++) {
				int dotX = startX + i * dotSpacing;
				int color = (i == currentPage) ? PAGE_DOT_ACTIVE_COLOR : PAGE_DOT_COLOR;
				int size = (i == currentPage) ? 4 : 3;
				guiGraphics.fill(dotX - size / 2, dotY - size / 2, dotX + size / 2, dotY + size / 2, color);
			}
		}
		WheelGuiHelper.renderWheelHint(guiGraphics, font, windowWidth, windowHeight, pages.size() > 1);
	}

	private void drawSliceWithGaps(PoseStack poseStack, BufferBuilder buffer, float centerX, float centerY, float startAngleIn, float endAngleIn, float startAngleOut, float endAngleOut, int radiusIn, int radiusOut, int color) {
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
			float x1 = centerX + radiusIn * (float) Math.cos(angle1In);
			float y1 = centerY + radiusIn * (float) Math.sin(angle1In);
			float x2 = centerX + radiusOut * (float) Math.cos(angle1Out);
			float y2 = centerY + radiusOut * (float) Math.sin(angle1Out);
			float x3 = centerX + radiusOut * (float) Math.cos(angle2Out);
			float y3 = centerY + radiusOut * (float) Math.sin(angle2Out);
			float x4 = centerX + radiusIn * (float) Math.cos(angle2In);
			float y4 = centerY + radiusIn * (float) Math.sin(angle2In);
			buffer.addVertex(matrix4f, x2, y2, 0.0F).setColor(r, g, b, a);
			buffer.addVertex(matrix4f, x1, y1, 0.0F).setColor(r, g, b, a);
			buffer.addVertex(matrix4f, x4, y4, 0.0F).setColor(r, g, b, a);
			buffer.addVertex(matrix4f, x3, y3, 0.0F).setColor(r, g, b, a);
		}
	}

	private void drawSlice(PoseStack poseStack, BufferBuilder buffer, float centerX, float centerY, float startAngle, float endAngle, int radiusIn, int radiusOut, int color) {
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
			float x1 = centerX + radiusIn * (float) Math.cos(angle1);
			float y1 = centerY + radiusIn * (float) Math.sin(angle1);
			float x2 = centerX + radiusOut * (float) Math.cos(angle1);
			float y2 = centerY + radiusOut * (float) Math.sin(angle1);
			float x3 = centerX + radiusOut * (float) Math.cos(angle2);
			float y3 = centerY + radiusOut * (float) Math.sin(angle2);
			float x4 = centerX + radiusIn * (float) Math.cos(angle2);
			float y4 = centerY + radiusIn * (float) Math.sin(angle2);
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
		List<String> jutsuNames = getCurrentJutsuNames();
		if (jutsuNames.isEmpty())
			return;
		int windowWidth = this.minecraft.getWindow().getGuiScaledWidth();
		int windowHeight = this.minecraft.getWindow().getGuiScaledHeight();
		int centerX = windowWidth / 2;
		int centerY = windowHeight / 2;
		double mouseAngle = Math.atan2(mouseY - centerY, mouseX - centerX);
		double mouseDistance = Math.sqrt(Math.pow(mouseX - centerX, 2.0D) + Math.pow(mouseY - centerY, 2.0D));
		float startAngle = getAngleFor(-0.5F, jutsuNames.size());
		float endAngle = getAngleFor(jutsuNames.size() - 0.5F, jutsuNames.size());
		while (mouseAngle < startAngle)
			mouseAngle += Mth.TWO_PI;
		while (mouseAngle >= endAngle)
			mouseAngle -= Mth.TWO_PI;
		this.hovered = -1;
		for (int i = 0; i < jutsuNames.size(); i++) {
			if (jutsuNames.size() > 1) {
				// Calculate gap angles for both inner and outer radius
				float gapAngleIn = GAP_WIDTH_PIXELS / RADIUS_IN;
				float gapAngleOut = GAP_WIDTH_PIXELS / RADIUS_OUT;
				// Interpolate gap angle based on mouse distance
				float gapAngle = gapAngleOut + (gapAngleIn - gapAngleOut) * (float) ((mouseDistance - RADIUS_OUT) / (RADIUS_IN - RADIUS_OUT));
				float currentStart = getAngleFor(i - 0.5F, jutsuNames.size()) + gapAngle / 2;
				float currentEnd = getAngleFor(i + 0.5F, jutsuNames.size()) - gapAngle / 2;
				if (mouseAngle >= currentStart && mouseAngle < currentEnd && mouseDistance >= RADIUS_IN && mouseDistance < RADIUS_OUT) {
					this.hovered = i;
					break;
				}
			} else {
				float currentStart = getAngleFor(i - 0.5F, jutsuNames.size());
				float currentEnd = getAngleFor(i + 0.5F, jutsuNames.size());
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
		List<String> jutsuIds = getCurrentJutsuIds();
		List<String> jutsuNames = getCurrentJutsuNames();
		if (jutsuIds.isEmpty() || this.hovered < 0 || this.hovered >= jutsuIds.size() || this.minecraft == null || this.minecraft.player == null) {
			if (button == 1 && this.minecraft != null && this.minecraft.player != null) {
				Minecraft mc = this.minecraft;
				Player player = this.minecraft.player;
				animation.startClosing(() -> WheelGuiHelper.openRootWheel(mc, player, this.x, this.y, this.z));
				return true;
			}
			return super.mouseClicked(mouseX, mouseY, button);
		}
		String selectedJutsuId = jutsuIds.get(this.hovered);
		String selectedJutsuName = jutsuNames.get(this.hovered);
		JutsuData jutsuData = JutsuRegistry.getJutsu(selectedJutsuId);
		// Handle favorite toggle (right click)
		if (button == 1) {
			String currentFavorites = entity.getData(NarutoModVariables.PLAYER_VARIABLES).favouriteJutsu;
			String updatedFavorites = JutsuIdList.has(currentFavorites, selectedJutsuId) ? JutsuIdList.remove(currentFavorites, selectedJutsuId) : JutsuIdList.add(currentFavorites, selectedJutsuId);
			entity.getData(NarutoModVariables.PLAYER_VARIABLES).favouriteJutsu = updatedFavorites;
			PacketDistributor.sendToServer(new SetFavouriteJutsuPacket(updatedFavorites));
			return true;
		}
		// Handle left click - new logic
		if (jutsuData != null && jutsuData.getType().equalsIgnoreCase("DOJUTSU")) {
			// Check if this is an activation jutsu (ends with "_activate")
			boolean isActivationJutsu = selectedJutsuId.toLowerCase().endsWith("_activate");
			// Get the dojutsu nature (remove "_activate" suffix if present)
			String dojutsuNature = jutsuData.getNature();
			if (isActivationJutsu) {
				// For activation jutsus, just toggle them in activeToggleJutsu
				String currentToggles = entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeToggleJutsu;
				Set<String> toggleSet = currentToggles != null && !currentToggles.isEmpty() ? Arrays.stream(currentToggles.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet()) : new HashSet<>();
				// Toggle this activation jutsu
				if (!toggleSet.remove(selectedJutsuId)) {
					toggleSet.add(selectedJutsuId);
				}
				String updatedToggles = String.join(",", toggleSet);
				entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeToggleJutsu = updatedToggles;
				PacketDistributor.sendToServer(new SetToggleJutsuPacket(updatedToggles));
				// Update activeDojutsu to this dojutsu nature
				entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeDojutsu = dojutsuNature;
				PacketDistributor.sendToServer(new SetActiveDojutsuPacket(dojutsuNature));
				// Close the wheel if this is not a toggle jutsu
				if (!jutsuData.getIsToggle()) {
					animation.startClosing(() -> this.minecraft.player.closeContainer());
				}
			} else {
				// For regular dojutsu jutsus, set them as activeJutsu and also update activeDojutsu
				entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeJutsu = selectedJutsuId;
				entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeDojutsu = dojutsuNature;
				PacketDistributor.sendToServer(new SetActiveJutsuPacket(selectedJutsuId));
				PacketDistributor.sendToServer(new SetActiveDojutsuPacket(dojutsuNature));
				WheelGuiHelper.announceEquipped(this.minecraft.player, jutsuData);
				// Handle toggle logic for non-activation dojutsu jutsus
				if (jutsuData.getIsToggle()) {
					String currentToggles = entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeToggleJutsu;
					Set<String> toggleSet = currentToggles != null && !currentToggles.isEmpty() ? Arrays.stream(currentToggles.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet()) : new HashSet<>();
					// Remove other dojutsu jutsus from different natures
					Set<String> toRemove = new HashSet<>();
					for (String toggledJutsuId : toggleSet) {
						JutsuData toggledJutsuData = JutsuRegistry.getJutsu(toggledJutsuId);
						if (toggledJutsuData != null && toggledJutsuData.getType().equalsIgnoreCase("DOJUTSU")) {
							String toggledNature = toggledJutsuData.getNature();
							if (!toggledNature.equalsIgnoreCase(dojutsuNature)) {
								toRemove.add(toggledJutsuId);
							}
						}
					}
					toggleSet.removeAll(toRemove);
					// Add the corresponding activation jutsu if it exists
					String activationJutsuId = dojutsuNature.toLowerCase() + "_activate";
					JutsuData activationJutsu = JutsuRegistry.getJutsu(activationJutsuId);
					if (activationJutsu != null && activationJutsu.getIsToggle()) {
						toggleSet.add(activationJutsuId);
					}
					// Toggle this jutsu
					if (!toggleSet.remove(selectedJutsuId)) {
						toggleSet.add(selectedJutsuId);
					}
					String updatedToggles = String.join(",", toggleSet);
					entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeToggleJutsu = updatedToggles;
					PacketDistributor.sendToServer(new SetToggleJutsuPacket(updatedToggles));
				} else {
					// For non-toggle jutsus, close the wheel
					animation.startClosing(() -> this.minecraft.player.closeContainer());
				}
			}
		} else if (jutsuData != null && jutsuData.getIsToggle()) {
			// Handle non-dojutsu toggle jutsus (existing logic)
			String currentToggles = entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeToggleJutsu;
			Set<String> toggleSet = currentToggles != null && !currentToggles.isEmpty() ? Arrays.stream(currentToggles.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet()) : new HashSet<>();
			if (!toggleSet.remove(selectedJutsuId)) {
				toggleSet.add(selectedJutsuId);
			}
			String updatedToggles = String.join(",", toggleSet);
			entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeToggleJutsu = updatedToggles;
			PacketDistributor.sendToServer(new SetToggleJutsuPacket(updatedToggles));
		} else {
			// Handle regular non-dojutsu, non-toggle jutsus
			entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeJutsu = selectedJutsuId;
			PacketDistributor.sendToServer(new SetActiveJutsuPacket(selectedJutsuId));
			WheelGuiHelper.announceEquipped(this.minecraft.player, jutsuData);
			animation.startClosing(() -> this.minecraft.player.closeContainer());
		}
		return true;
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