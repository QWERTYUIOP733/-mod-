/*
 * Copyright (c) 2026 Color Blocks Extension
 * SPDX-License-Identifier: MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.mard.pixel.forge.client;

import com.mard.pixel.common.MardColor;
import com.mard.pixel.common.MardPalette;
import com.mard.pixel.forge.MardCraftingMenu;
import com.mard.pixel.forge.MardPixelForge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 方块染色台屏幕。
 * 布局类似原版工作台，左侧添加颜色选择列表（七彩粉末模式）。
 *
 * 26.3 渲染管线迁移：render/renderBg/renderTooltip 三件套改为
 * extractBackground（super 背景 + 自定义面板）与 extractTooltip（g.setTooltipForNextFrame）；
 * GuiGraphics→GuiGraphicsExtractor、drawString→g.text；mouseClicked 改 MouseButtonEvent。
 */
public class MardCraftingScreen extends AbstractContainerScreen<MardCraftingMenu> {

    // 基础界面尺寸
    private static final int BASE_WIDTH = 176;
    private static final int BASE_HEIGHT = 166;

    // 颜色选择列表面板尺寸
    private static final int COLOR_PANEL_DEFAULT_WIDTH = 130;
    private static final int COLOR_PANEL_MIN_WIDTH = 80;
    private static final int COLOR_PANEL_HEIGHT = 166;
    private static final int COLOR_PANEL_GAP = 4;
    private static final int COLOR_PANEL_SCREEN_LEFT_MARGIN = 2;
    private static final int COLOR_ITEM_HEIGHT = 22;
    private static final int COLOR_SWATCH_SIZE = 16;
    private static final int COLOR_TEXT_PADDING = 4;

    // 运行时计算的面板宽度（根据窗口大小自适应）
    private int panelWidth = COLOR_PANEL_DEFAULT_WIDTH;
    private int maxVisibleColors = (COLOR_PANEL_HEIGHT - 20) / COLOR_ITEM_HEIGHT;

    // 颜色选择列表滚动偏移
    private int scrollOffset = 0;
    private String selectedColor = "";

    public MardCraftingScreen(MardCraftingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, BASE_WIDTH, BASE_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
        updatePanelWidth();
    }

    /**
     * 根据窗口大小计算颜色面板宽度。面板固定放在主界面左侧，宽度随左侧可用空间动态调整。
     */
    private void updatePanelWidth() {
        int availableWidth = this.leftPos - COLOR_PANEL_SCREEN_LEFT_MARGIN - COLOR_PANEL_GAP;
        panelWidth = Math.max(COLOR_PANEL_MIN_WIDTH, Math.min(COLOR_PANEL_DEFAULT_WIDTH, availableWidth));
        maxVisibleColors = Math.max(3, (COLOR_PANEL_HEIGHT - 20) / COLOR_ITEM_HEIGHT);
        int totalColors = MardPalette.COLORS.size();
        int maxOffset = Math.max(0, totalColors - maxVisibleColors);
        scrollOffset = Math.min(scrollOffset, maxOffset);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        drawWorkbenchPanel(g);
        if (menu.hasPigment()) {
            renderColorPanel(g, mouseX, mouseY);
        }
    }

    /**
     * 绘制工作台主面板（浅灰底 + 标题栏 + 槽位立体边框 + 箭头）。
     */
    private void drawWorkbenchPanel(GuiGraphicsExtractor g) {
        int x = this.leftPos;
        int y = this.topPos;

        g.fill(x, y, x + BASE_WIDTH, y + BASE_HEIGHT, 0xFFC6C6C6);
        g.fill(x, y, x + BASE_WIDTH, y + 16, 0xFF8B8B8B);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = x + MardCraftingMenu.GRID_START_X + col * MardCraftingMenu.SLOT_SIZE;
                int slotY = y + MardCraftingMenu.GRID_START_Y + row * MardCraftingMenu.SLOT_SIZE;
                drawSlotBackground(g, slotX, slotY);
            }
        }

        int resultX = x + MardCraftingMenu.RESULT_X;
        int resultY = y + MardCraftingMenu.RESULT_Y;
        drawSlotBackground(g, resultX, resultY);

        int arrowX = x + 90;
        int arrowY = y + 38;
        g.fill(arrowX, arrowY + 3, arrowX + 22, arrowY + 7, 0xFF555555);
        g.fill(arrowX + 18, arrowY, arrowX + 22, arrowY + 10, 0xFF555555);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + MardCraftingMenu.PLAYER_INV_X + col * MardCraftingMenu.SLOT_SIZE;
                int slotY = y + MardCraftingMenu.PLAYER_INV_Y + row * MardCraftingMenu.SLOT_SIZE;
                drawSlotBackground(g, slotX, slotY);
            }
        }
        for (int col = 0; col < 9; col++) {
            int slotX = x + MardCraftingMenu.PLAYER_INV_X + col * MardCraftingMenu.SLOT_SIZE;
            int slotY = y + MardCraftingMenu.HOTBAR_Y;
            drawSlotBackground(g, slotX, slotY);
        }
    }

    private void drawSlotBackground(GuiGraphicsExtractor g, int x, int y) {
        int size = MardCraftingMenu.SLOT_SIZE;
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF8B8B8B);
        g.fill(x, y, x + size, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + size, 0xFFFFFFFF);
        g.fill(x, y + size - 1, x + size, y + size, 0xFF373737);
        g.fill(x + size - 1, y, x + size, y + size, 0xFF373737);
    }

    /**
     * 渲染颜色选择列表面板（固定放在主界面左侧）。
     */
    private void renderColorPanel(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        updatePanelWidth();

        int panelX = this.leftPos - panelWidth - COLOR_PANEL_GAP;
        panelX = Math.max(COLOR_PANEL_SCREEN_LEFT_MARGIN, panelX);
        int panelY = this.topPos;

        g.fill(panelX, panelY, panelX + panelWidth, panelY + COLOR_PANEL_HEIGHT, 0xFFC6C6C6);
        g.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFFFFFFFF);
        g.fill(panelX, panelY, panelX + 1, panelY + COLOR_PANEL_HEIGHT, 0xFFFFFFFF);
        g.fill(panelX, panelY + COLOR_PANEL_HEIGHT - 1, panelX + panelWidth, panelY + COLOR_PANEL_HEIGHT, 0xFF373737);
        g.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + COLOR_PANEL_HEIGHT, 0xFF373737);

        g.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 14, 0xFF8B8B8B);
        g.text(this.font, "选择颜色", panelX + 4, panelY + 4, 0xFFFFFFFF, false);

        int totalColors = MardPalette.COLORS.size();
        int startIndex = Math.max(0, Math.min(scrollOffset, totalColors - 1));
        int endIndex = Math.min(startIndex + maxVisibleColors, totalColors);
        int listStartY = panelY + 18;

        for (int i = startIndex; i < endIndex; i++) {
            MardColor color = MardPalette.COLORS.get(i);
            int itemY = listStartY + (i - startIndex) * COLOR_ITEM_HEIGHT;

            if (color.code().equals(selectedColor)) {
                g.fill(panelX + 2, itemY, panelX + panelWidth - 2, itemY + COLOR_ITEM_HEIGHT - 1, 0xFF90EE90);
            }

            int swatchX = panelX + 4;
            int swatchY = itemY + 3;
            g.fill(swatchX, swatchY, swatchX + COLOR_SWATCH_SIZE, swatchY + COLOR_SWATCH_SIZE, 0xFF000000);
            g.fill(swatchX + 1, swatchY + 1, swatchX + COLOR_SWATCH_SIZE - 1, swatchY + COLOR_SWATCH_SIZE - 1, 0xFF000000 | color.rgb());

            int textX = swatchX + COLOR_SWATCH_SIZE + COLOR_TEXT_PADDING;
            int availableTextWidth = panelWidth - (textX - panelX) - 8;

            String codeText = color.code();
            String rgbText = String.format("RGB:%d,%d,%d", color.r(), color.g(), color.b());

            g.text(this.font, codeText, textX, itemY + 3, 0xFF404040, false);
            if (availableTextWidth > 40) {
                g.text(this.font, rgbText, textX, itemY + 13, 0xFF606060, false);
            }
        }

        if (totalColors > maxVisibleColors) {
            int scrollbarX = panelX + panelWidth - 6;
            int scrollbarY = listStartY;
            int scrollbarHeight = COLOR_PANEL_HEIGHT - 22;
            int thumbHeight = Math.max(20, (int) ((float) maxVisibleColors / totalColors * scrollbarHeight));
            int maxOffset = Math.max(0, totalColors - maxVisibleColors);
            int thumbY = scrollbarY + (maxOffset > 0 ? (int) ((float) scrollOffset / maxOffset * (scrollbarHeight - thumbHeight)) : 0);

            g.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0xFF8B8B8B);
            g.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0xFF555555);
        }
    }

    private int[] getColorPanelBounds() {
        updatePanelWidth();
        int panelX = this.leftPos - panelWidth - COLOR_PANEL_GAP;
        panelX = Math.max(COLOR_PANEL_SCREEN_LEFT_MARGIN, panelX);
        int panelY = this.topPos;
        return new int[]{panelX, panelY};
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double verticalDelta) {
        if (menu.hasPigment()) {
            int[] bounds = getColorPanelBounds();
            int panelX = bounds[0];
            int panelY = bounds[1];
            int listStartY = panelY + 18;

            if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
                mouseY >= listStartY && mouseY <= panelY + COLOR_PANEL_HEIGHT) {
                int totalColors = MardPalette.COLORS.size();
                int maxOffset = Math.max(0, totalColors - maxVisibleColors);
                scrollOffset = Math.max(0, Math.min(scrollOffset - (int) verticalDelta, maxOffset));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalDelta, verticalDelta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        double mouseX = e.x();
        double mouseY = e.y();
        int button = e.button();
        if (menu.hasPigment() && button == 1) { // 26.3 SDL3 左键=1（旧 GLFW 为 0）
            int[] bounds = getColorPanelBounds();
            int panelX = bounds[0];
            int panelY = bounds[1];
            int listStartY = panelY + 18;

            if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
                mouseY >= listStartY && mouseY <= panelY + COLOR_PANEL_HEIGHT) {

                int itemIndex = (int) ((mouseY - listStartY) / COLOR_ITEM_HEIGHT);
                int colorIndex = scrollOffset + itemIndex;

                if (colorIndex >= 0 && colorIndex < MardPalette.COLORS.size()) {
                    MardColor color = MardPalette.COLORS.get(colorIndex);
                    selectedColor = color.code();
                    menu.selectColor(color.code());
                    return true;
                }
            }
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);

        if (menu.hasPigment()) {
            int[] bounds = getColorPanelBounds();
            int panelX = bounds[0];
            int panelY = bounds[1];
            int listStartY = panelY + 18;

            if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
                mouseY >= listStartY && mouseY <= panelY + COLOR_PANEL_HEIGHT) {

                int itemIndex = (int) ((mouseY - listStartY) / COLOR_ITEM_HEIGHT);
                int colorIndex = scrollOffset + itemIndex;

                if (colorIndex >= 0 && colorIndex < MardPalette.COLORS.size()) {
                    MardColor color = MardPalette.COLORS.get(colorIndex);
                    ItemStack stack = MardPixelForge.buildStack(color.code());
                    if (!stack.isEmpty()) {
                        g.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
                    }
                }
            }
        }
    }
}
