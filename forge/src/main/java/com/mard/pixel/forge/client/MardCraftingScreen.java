package com.mard.pixel.forge.client;

import com.mard.pixel.forge.MardCraftingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * MARD 合成台屏幕。
 * 布局类似原版工作台，使用纯色绘制背景。
 */
public class MardCraftingScreen extends AbstractContainerScreen<MardCraftingMenu> {

    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 166;

    // 颜色定义
    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int SLOT_BG_COLOR = 0xFF8B8B8B;
    private static final int BORDER_COLOR = 0xFF373737;
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

        // 绘制边框
        g.fill(x, y, x + this.imageWidth, y + 1, BORDER_COLOR);
        g.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, BORDER_COLOR);
        g.fill(x, y, x + 1, y + this.imageHeight, BORDER_COLOR);
        g.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, BORDER_COLOR);

        // 绘制合成网格区域背景
        int gridX = x + MardCraftingMenu.GRID_START_X - 1;
        int gridY = y + MardCraftingMenu.GRID_START_Y - 1;
        int gridW = 3 * MardCraftingMenu.SLOT_SIZE + 2;
        int gridH = 3 * MardCraftingMenu.SLOT_SIZE + 2;
        g.fill(gridX, gridY, gridX + gridW, gridY + gridH, SLOT_BG_COLOR);

        // 绘制结果槽背景
        int resultX = x + MardCraftingMenu.RESULT_X - 1;
        int resultY = y + MardCraftingMenu.RESULT_Y - 1;
        g.fill(resultX, resultY, resultX + 20, resultY + 20, SLOT_BG_COLOR);

        // 绘制合成箭头
        int arrowX = x + 95;
        int arrowY = y + 35;
        g.fill(arrowX, arrowY + 3, arrowX + 18, arrowY + 7, ARROW_COLOR);
        g.fill(arrowX + 16, arrowY, arrowX + 22, arrowY + 10, ARROW_COLOR);

        // 绘制玩家背包区域背景
        int invX = x + MardCraftingMenu.PLAYER_INV_X - 1;
        int invY = y + MardCraftingMenu.PLAYER_INV_Y - 1;
        int invW = 9 * MardCraftingMenu.SLOT_SIZE + 2;
        int invH = 3 * MardCraftingMenu.SLOT_SIZE + 2;
        g.fill(invX, invY, invX + invW, invY + invH, SLOT_BG_COLOR);

        // 绘制快捷栏区域背景
        int hotbarY = y + MardCraftingMenu.HOTBAR_Y - 1;
        g.fill(invX, hotbarY, invX + invW, hotbarY + MardCraftingMenu.SLOT_SIZE + 2, SLOT_BG_COLOR);

        // 绘制标题
        g.drawString(this.font, this.title, x + 8, y + 6, TITLE_COLOR, false);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        // 不渲染默认标签，已在renderBg中绘制标题
    }
}
