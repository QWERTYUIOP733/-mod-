package com.mard.pixel.forge.client;

import com.mard.pixel.common.MardColor;
import com.mard.pixel.common.MardPalette;
import com.mard.pixel.forge.MardCraftingMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * MARD 合成台屏幕。
 * 布局类似原版工作台，右侧添加颜色选择列表（七彩粉末模式）。
 * - 左上：3x3 合成网格（9格）
 * - 右侧：结果槽（1格）
 * - 下方：玩家背包（27格）
 * - 最下方：玩家快捷栏（9格）
 * - 最右侧：颜色选择列表（仅当合成网格中有七彩粉末时显示）
 */
public class MardCraftingScreen extends AbstractContainerScreen<MardCraftingMenu> {

    // 基础界面尺寸
    private static final int BASE_WIDTH = 176;
    private static final int BASE_HEIGHT = 166;

    // 颜色选择列表面板尺寸
    private static final int COLOR_PANEL_WIDTH = 120;
    private static final int COLOR_PANEL_HEIGHT = 140;
    private static final int COLOR_ITEM_HEIGHT = 20;
    private static final int COLOR_SWATCH_SIZE = 14;
    private static final int MAX_VISIBLE_COLORS = COLOR_PANEL_HEIGHT / COLOR_ITEM_HEIGHT;

    // 颜色选择列表滚动偏移
    private int scrollOffset = 0;
    private String selectedColor = "";

    public MardCraftingScreen(MardCraftingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BASE_WIDTH;
        this.imageHeight = BASE_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        // 标题位置
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        // 玩家背包标签位置
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // 如果有七彩粉末，渲染颜色选择列表
        if (menu.hasPigment()) {
            renderColorPanel(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = this.leftPos;
        int y = this.topPos;

        // 绘制主背景（浅灰色）
        graphics.fill(x, y, x + BASE_WIDTH, y + BASE_HEIGHT, 0xFFC6C6C6);

        // 绘制标题栏背景
        graphics.fill(x, y, x + BASE_WIDTH, y + 16, 0xFF8B8B8B);

        // 绘制每个槽位的背景（带立体边框）
        // 3x3 合成网格
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = x + MardCraftingMenu.GRID_START_X + col * MardCraftingMenu.SLOT_SIZE;
                int slotY = y + MardCraftingMenu.GRID_START_Y + row * MardCraftingMenu.SLOT_SIZE;
                drawSlotBackground(graphics, slotX, slotY);
            }
        }

        // 结果槽
        int resultX = x + MardCraftingMenu.RESULT_X;
        int resultY = y + MardCraftingMenu.RESULT_Y;
        drawSlotBackground(graphics, resultX, resultY);

        // 绘制箭头（合成网格 -> 结果槽）
        int arrowX = x + 90;
        int arrowY = y + 38;
        graphics.fill(arrowX, arrowY + 3, arrowX + 22, arrowY + 7, 0xFF555555);
        graphics.fill(arrowX + 18, arrowY, arrowX + 22, arrowY + 10, 0xFF555555);

        // 玩家背包（27格）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + MardCraftingMenu.PLAYER_INV_X + col * MardCraftingMenu.SLOT_SIZE;
                int slotY = y + MardCraftingMenu.PLAYER_INV_Y + row * MardCraftingMenu.SLOT_SIZE;
                drawSlotBackground(graphics, slotX, slotY);
            }
        }

        // 玩家快捷栏（9格）
        for (int col = 0; col < 9; col++) {
            int slotX = x + MardCraftingMenu.PLAYER_INV_X + col * MardCraftingMenu.SLOT_SIZE;
            int slotY = y + MardCraftingMenu.HOTBAR_Y;
            drawSlotBackground(graphics, slotX, slotY);
        }
    }

    /**
     * 绘制单个槽位的背景（带立体边框效果）。
     */
    private void drawSlotBackground(GuiGraphics graphics, int x, int y) {
        int size = MardCraftingMenu.SLOT_SIZE;
        // 槽位内部（深灰色）
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF8B8B8B);
        // 上/左边框（浅色，凸起效果）
        graphics.fill(x, y, x + size, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + size, 0xFFFFFFFF);
        // 下/右边框（深色，凹陷效果）
        graphics.fill(x, y + size - 1, x + size, y + size, 0xFF373737);
        graphics.fill(x + size - 1, y, x + size, y + size, 0xFF373737);
    }

    /**
     * 渲染颜色选择列表面板。
     */
    private void renderColorPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int panelX = this.leftPos + BASE_WIDTH + 4;
        int panelY = this.topPos + 10;

        // 绘制面板背景
        graphics.fill(panelX, panelY, panelX + COLOR_PANEL_WIDTH, panelY + COLOR_PANEL_HEIGHT, 0xFFC6C6C6);
        // 面板边框
        graphics.fill(panelX, panelY, panelX + COLOR_PANEL_WIDTH, panelY + 1, 0xFFFFFFFF);
        graphics.fill(panelX, panelY, panelX + 1, panelY + COLOR_PANEL_HEIGHT, 0xFFFFFFFF);
        graphics.fill(panelX, panelY + COLOR_PANEL_HEIGHT - 1, panelX + COLOR_PANEL_WIDTH, panelY + COLOR_PANEL_HEIGHT, 0xFF373737);
        graphics.fill(panelX + COLOR_PANEL_WIDTH - 1, panelY, panelX + COLOR_PANEL_WIDTH, panelY + COLOR_PANEL_HEIGHT, 0xFF373737);

        // 绘制标题
        graphics.drawString(this.font, net.minecraft.network.chat.Component.translatable("screen.mard_pixel.crafting.select_color"), panelX + 4, panelY + 4, 0x404040, false);

        // 计算可见的颜色范围
        int totalColors = MardPalette.COLORS.size();
        int startIndex = Math.max(0, Math.min(scrollOffset, totalColors - 1));
        int endIndex = Math.min(startIndex + MAX_VISIBLE_COLORS, totalColors);

        // 绘制颜色列表项
        for (int i = startIndex; i < endIndex; i++) {
            MardColor color = MardPalette.COLORS.get(i);
            int itemY = panelY + 16 + (i - startIndex) * COLOR_ITEM_HEIGHT;

            // 选中项高亮背景
            if (color.code().equals(selectedColor)) {
                graphics.fill(panelX + 2, itemY, panelX + COLOR_PANEL_WIDTH - 2, itemY + COLOR_ITEM_HEIGHT - 2, 0xFF90EE90);
            }

            // 绘制颜色方块
            int swatchX = panelX + 4;
            int swatchY = itemY + 3;
            graphics.fill(swatchX, swatchY, swatchX + COLOR_SWATCH_SIZE, swatchY + COLOR_SWATCH_SIZE, 0xFF000000);
            graphics.fill(swatchX + 1, swatchY + 1, swatchX + COLOR_SWATCH_SIZE - 1, swatchY + COLOR_SWATCH_SIZE - 1, 0xFF000000 | color.rgb());

            // 绘制颜色介绍（色号 + RGB）
            String codeText = color.code();
            String rgbText = String.format("RGB:%d,%d,%d", color.r(), color.g(), color.b());
            graphics.drawString(this.font, codeText, swatchX + COLOR_SWATCH_SIZE + 4, itemY + 2, 0x404040, false);
            graphics.drawString(this.font, rgbText, swatchX + COLOR_SWATCH_SIZE + 4, itemY + 11, 0x606060, false);
        }

        // 绘制滚动条
        if (totalColors > MAX_VISIBLE_COLORS) {
            int scrollbarX = panelX + COLOR_PANEL_WIDTH - 6;
            int scrollbarY = panelY + 16;
            int scrollbarHeight = COLOR_PANEL_HEIGHT - 20;
            int thumbHeight = Math.max(20, (int) ((float) MAX_VISIBLE_COLORS / totalColors * scrollbarHeight));
            int thumbY = scrollbarY + (int) ((float) scrollOffset / (totalColors - MAX_VISIBLE_COLORS) * (scrollbarHeight - thumbHeight));

            // 滚动条背景
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0xFF8B8B8B);
            // 滚动条滑块
            graphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0xFF555555);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 颜色选择列表滚动
        if (menu.hasPigment()) {
            int panelX = this.leftPos + BASE_WIDTH + 4;
            int panelY = this.topPos + 10;

            if (mouseX >= panelX && mouseX <= panelX + COLOR_PANEL_WIDTH &&
                mouseY >= panelY && mouseY <= panelY + COLOR_PANEL_HEIGHT) {
                int totalColors = MardPalette.COLORS.size();
                int maxOffset = Math.max(0, totalColors - MAX_VISIBLE_COLORS);
                scrollOffset = Math.max(0, Math.min(scrollOffset - (int) delta, maxOffset));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 颜色选择列表点击
        if (menu.hasPigment() && button == 0) {
            int panelX = this.leftPos + BASE_WIDTH + 4;
            int panelY = this.topPos + 10;

            if (mouseX >= panelX && mouseX <= panelX + COLOR_PANEL_WIDTH &&
                mouseY >= panelY + 16 && mouseY <= panelY + COLOR_PANEL_HEIGHT) {

                int itemIndex = (int) ((mouseY - panelY - 16) / COLOR_ITEM_HEIGHT);
                int colorIndex = scrollOffset + itemIndex;

                if (colorIndex >= 0 && colorIndex < MardPalette.COLORS.size()) {
                    MardColor color = MardPalette.COLORS.get(colorIndex);
                    selectedColor = color.code();
                    menu.selectColor(color.code());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);

        // 颜色列表项的tooltip
        if (menu.hasPigment()) {
            int panelX = this.leftPos + BASE_WIDTH + 4;
            int panelY = this.topPos + 10;

            if (mouseX >= panelX && mouseX <= panelX + COLOR_PANEL_WIDTH &&
                mouseY >= panelY + 16 && mouseY <= panelY + COLOR_PANEL_HEIGHT) {

                int itemIndex = (int) ((mouseY - panelY - 16) / COLOR_ITEM_HEIGHT);
                int colorIndex = scrollOffset + itemIndex;

                if (colorIndex >= 0 && colorIndex < MardPalette.COLORS.size()) {
                    MardColor color = MardPalette.COLORS.get(colorIndex);
                    ItemStack stack = com.mard.pixel.forge.MardPixelForge.buildStack(color.code());
                    if (!stack.isEmpty()) {
                        graphics.renderTooltip(this.font, stack, mouseX, mouseY);
                    }
                }
            }
        }
    }
}
