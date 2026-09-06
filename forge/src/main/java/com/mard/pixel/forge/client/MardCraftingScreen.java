package com.mard.pixel.forge.client;

import com.mard.pixel.forge.MardCraftingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * MARD 合成台屏幕。
 * 布局参考原版工作台，每个槽位单独绘制背景。
 */
public class MardCraftingScreen extends AbstractContainerScreen<MardCraftingMenu> {

    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 166;

    // 颜色定义
    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int SLOT_BG_COLOR = 0xFF8B8B8B;
    private static final int SLOT_BORDER_DARK = 0xFF373737;
    private static final int SLOT_BORDER_LIGHT = 0xFFFFFFFF;
    private static final int TITLE_COLOR = 0x404040;
    private static final int ARROW_COLOR = 0xFF555555;

    public MardCraftingScreen(MardCraftingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 绘制主背景
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, BG_COLOR);

        // 绘制3x3合成网格的每个槽位背景
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = x + MardCraftingMenu.GRID_START_X + col * MardCraftingMenu.SLOT_SIZE;
                int slotY = y + MardCraftingMenu.GRID_START_Y + row * MardCraftingMenu.SLOT_SIZE;
                drawSlotBackground(g, slotX, slotY);
            }
        }

        // 绘制结果槽背景（稍大）
        int resultX = x + MardCraftingMenu.RESULT_X;
        int resultY = y + MardCraftingMenu.RESULT_Y;
        drawSlotBackground(g, resultX, resultY, 20, 20);

        // 绘制合成箭头
        int arrowX = x + 95;
        int arrowY = y + 35;
        g.fill(arrowX, arrowY + 3, arrowX + 18, arrowY + 7, ARROW_COLOR);
        g.fill(arrowX + 16, arrowY, arrowX + 22, arrowY + 10, ARROW_COLOR);

        // 绘制玩家背包的每个槽位背景
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + MardCraftingMenu.PLAYER_INV_X + col * MardCraftingMenu.SLOT_SIZE;
                int slotY = y + MardCraftingMenu.PLAYER_INV_Y + row * MardCraftingMenu.SLOT_SIZE;
                drawSlotBackground(g, slotX, slotY);
            }
        }

        // 绘制快捷栏的每个槽位背景
        for (int col = 0; col < 9; col++) {
            int slotX = x + MardCraftingMenu.PLAYER_INV_X + col * MardCraftingMenu.SLOT_SIZE;
            int slotY = y + MardCraftingMenu.HOTBAR_Y;
            drawSlotBackground(g, slotX, slotY);
        }

        // 绘制标题
        g.drawString(this.font, this.title, x + 8, y + 6, TITLE_COLOR, false);

        // 绘制玩家背包标签
        g.drawString(this.font, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY, TITLE_COLOR, false);
    }

    /**
     * 绘制标准槽位背景（18x18），带立体边框效果。
     */
    private void drawSlotBackground(GuiGraphics g, int x, int y) {
        drawSlotBackground(g, x, y, 16, 16);
    }

    /**
     * 绘制槽位背景，带立体边框效果。
     * 槽位实际内容区域为 width x height，外围有1像素边框。
     */
    private void drawSlotBackground(GuiGraphics g, int x, int y, int width, int height) {
        // 槽位内部背景
        g.fill(x + 1, y + 1, x + 1 + width, y + 1 + height, SLOT_BG_COLOR);

        // 上边框（浅色）
        g.fill(x, y, x + width + 2, y + 1, SLOT_BORDER_LIGHT);
        // 左边框（浅色）
        g.fill(x, y, x + 1, y + height + 2, SLOT_BORDER_LIGHT);

        // 下边框（深色）
        g.fill(x, y + height + 1, x + width + 2, y + height + 2, SLOT_BORDER_DARK);
        // 右边框（深色）
        g.fill(x + width + 1, y, x + width + 2, y + height + 2, SLOT_BORDER_DARK);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        // 不渲染默认标签，已在renderBg中绘制
    }
}
