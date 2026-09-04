package com.mard.pixel.forge.client;

import com.mard.pixel.common.MardColor;
import com.mard.pixel.common.MardPalette;
import com.mard.pixel.forge.MardNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * MARD Pixel Mod 主 UI 界面。
 *
 * 布局根据游戏窗口大小动态调整。
 * 主菜单：左侧3个按钮 + 右侧说明 + 底部提示
 * 按钮一：颜色选取 - 全部色号网格，点击给一组，支持连续选择
 * 按钮二：输入色号 - 输入框，输入后放快捷栏一组
 * 按钮三：导入外部图纸 - 附属模组，开发中
 */
public class MardColorScreen extends Screen {

    // ==================== 页面枚举 ====================
    private enum Page { MAIN, SWATCHES, INPUT }

    // ==================== 色块数据 ====================
    private record Entry(String code, int rgb, String target) {}
    private record Rect(int x, int y, int w, int h) {
        boolean hit(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    // ==================== 布局常量 ====================
    private static final int SW = 20, GAP = 3;

    // ==================== 状态 ====================
    private Page currentPage = Page.MAIN;
    private final List<Entry> swatches = new ArrayList<>();
    private final List<Rect> swatchRects = new ArrayList<>();
    private int scrollOffset = 0;
    private int lastScrollOffset = -1; // 用于检测滚动变化，避免每帧重算Rect
    private int lastWindowWidth = -1;
    private int lastWindowHeight = -1;

    // 输入色号页面
    private EditBox inputBox;
    private String statusMsg = "";

    public MardColorScreen() {
        super(Component.literal("MARD 色板"));
        rebuildSwatches();
    }

    private void rebuildSwatches() {
        swatches.clear();
        for (MardColor mc : MardPalette.COLORS) {
            swatches.add(new Entry(mc.code(), mc.rgb(), "MARD:" + mc.code()));
        }
    }

    // ==================== 初始化 ====================

    @Override
    protected void init() {
        this.clearWidgets();
        // 切换页面时重置输入框
        if (currentPage != Page.INPUT) inputBox = null;
        // 重置Rect缓存，强制重算
        lastScrollOffset = -1;
        lastWindowWidth = -1;
        lastWindowHeight = -1;

        switch (currentPage) {
            case MAIN -> initMainPage();
            case SWATCHES -> initSwatchesPage();
            case INPUT -> initInputPage();
        }
    }

    /**
     * 主菜单页面：左侧3个按钮 + 右侧说明。
     * 所有位置根据窗口大小动态计算。
     */
    private void initMainPage() {
        // 按钮区域：左侧 45% 宽度
        int btnAreaW = (int) (width * 0.45);
        int btnW = Math.min(300, btnAreaW - 40);
        int btnH = 36;
        int btnX = (btnAreaW - btnW) / 2 + 20;
        int centerY = height / 2;
        int gapY = 55;

        // 按钮一：MARD 颜色选取
        addRenderableWidget(Button.builder(Component.literal("MARD 颜色选取"), btn -> {
            currentPage = Page.SWATCHES;
            scrollOffset = 0;
            init();
        }).bounds(btnX, centerY - gapY - btnH / 2, btnW, btnH).build());

        // 按钮二：输入想用的色号
        addRenderableWidget(Button.builder(Component.literal("输入想用的色号"), btn -> {
            currentPage = Page.INPUT;
            init();
        }).bounds(btnX, centerY - btnH / 2, btnW, btnH).build());

        // 按钮三：导入外部图纸（占位）
        addRenderableWidget(Button.builder(Component.literal("导入外部图纸"), btn -> {
            statusMsg = "按钮三在开发中，敬请期待";
        }).bounds(btnX, centerY + gapY - btnH / 2, btnW, btnH).build());
    }

    /**
     * 颜色选取页面。
     */
    private void initSwatchesPage() {
        addRenderableWidget(Button.builder(Component.literal("← 返回"), btn -> {
            currentPage = Page.MAIN;
            statusMsg = "";
            init();
        }).bounds(10, 6, 60, 20).build());
    }

    /**
     * 输入色号页面。
     */
    private void initInputPage() {
        addRenderableWidget(Button.builder(Component.literal("← 返回"), btn -> {
            currentPage = Page.MAIN;
            statusMsg = "";
            init();
        }).bounds(10, 6, 60, 20).build());

        int boxW = Math.min(240, width / 3);
        int boxH = 22;
        int boxX = (width - boxW) / 2;
        int boxY = height / 2 - 30;

        inputBox = new EditBox(font, boxX, boxY, boxW, boxH, Component.literal(""));
        inputBox.setMaxLength(16);
        inputBox.setFocused(true);
        addRenderableWidget(inputBox);

        addRenderableWidget(Button.builder(Component.literal("确认放入快捷栏"), btn -> {
            submitCode();
        }).bounds(boxX, boxY + 32, boxW, 22).build());
    }

    private void submitCode() {
        if (inputBox == null) return;
        String code = inputBox.getValue().trim().toUpperCase();
        if (!code.isEmpty()) {
            MardNetwork.CHANNEL.sendToServer(new MardNetwork.HotbarPacket(code));
            statusMsg = "已请求放入快捷栏: " + code;
            inputBox.setValue("");
        } else {
            statusMsg = "请输入色号";
        }
    }

    // ==================== 渲染 ====================

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g);

        switch (currentPage) {
            case MAIN -> renderMainPage(g);
            case SWATCHES -> renderSwatchesPage(g);
            case INPUT -> renderInputPage(g);
        }

        super.render(g, mx, my, partialTick);
    }

    /**
     * 主菜单页面渲染。
     */
    private void renderMainPage(GuiGraphics g) {
        // 标题（居中偏上）
        String title = "MARD 像素色块 Mod";
        g.drawString(font, Component.literal(title), (width - font.width(title)) / 2, 40, 0xFFFFFF);

        // 右侧：mod 使用说明（右侧 40% 宽度）
        int infoAreaX = (int) (width * 0.55);
        int infoAreaW = (int) (width * 0.35);
        int infoY = Math.max(80, height / 2 - 120);
        int infoH = Math.min(240, height - 160);

        // 说明框背景
        g.fill(infoAreaX - 6, infoY - 6, infoAreaX + infoAreaW + 6, infoY + infoH + 6, 0xFF1a1a1a);
        g.fill(infoAreaX - 5, infoY - 5, infoAreaX + infoAreaW + 5, infoY + infoH + 5, 0xFF2a2a2a);

        g.drawString(font, Component.literal("mod 使用说明"), infoAreaX, infoY, 0xFFFFAA);

        String[] lines = {
            "",
            "MARD 295 色像素画模组",
            "",
            "按钮一：浏览全部色号",
            "  点击色块获取一组方块",
            "  支持连续选择",
            "",
            "按钮二：输入色号快速获取",
            "  输入色号后放入快捷栏",
            "",
            "按钮三：导入外部图纸",
            "  附属模组开发中",
            "",
            "按 G 键打开/关闭本界面"
        };

        int y = infoY + 15;
        int lineH = Math.max(10, (infoH - 20) / lines.length);
        for (String line : lines) {
            if (y + 10 < infoY + infoH) {
                g.drawString(font, Component.literal(line), infoAreaX, y, 0xCCCCCC);
            }
            y += lineH;
        }

        // 底部最后一行：按钮三在开发中，敬请期待
        String bottomText = "按钮三在开发中，敬请期待";
        g.drawString(font, Component.literal(bottomText),
                (width - font.width(bottomText)) / 2, height - 35, 0x888888);

        // 状态信息
        if (!statusMsg.isEmpty()) {
            g.drawString(font, Component.literal(statusMsg), 10, height - 14, 0xFFFFAA);
        }
    }

    /**
     * 颜色选取页面渲染：全部色号网格，支持滚动。
     * 优化：只在滚动或窗口大小变化时重算Rect，避免每帧卡顿。
     */
    private void renderSwatchesPage(GuiGraphics g) {
        // 标题
        String title = "MARD 颜色选取 - 点击色块获取一组（64个）";
        g.drawString(font, Component.literal(title), 80, 12, 0xFFFFFF);

        int contentY = 40;
        int contentH = height - contentY - 20;
        int cellW = SW + GAP + 8;
        int cellH = SW + GAP + 10;
        int cols = Math.max(8, Math.min(24, (width - 20) / cellW));
        int visibleRows = Math.max(1, contentH / cellH);
        int totalRows = (int) Math.ceil((double) swatches.size() / cols);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        // 只在滚动偏移或窗口大小变化时重算Rect（性能优化）
        if (lastScrollOffset != scrollOffset || lastWindowWidth != width || lastWindowHeight != height) {
            swatchRects.clear();
            int startIdx = scrollOffset * cols;
            for (int i = startIdx; i < swatches.size(); i++) {
                int col = (i - startIdx) % cols;
                int row = (i - startIdx) / cols;
                if (row >= visibleRows) break;
                int x = 10 + col * cellW;
                int y = contentY + row * cellH;
                swatchRects.add(new Rect(x, y, SW, SW + 10));
            }
            lastScrollOffset = scrollOffset;
            lastWindowWidth = width;
            lastWindowHeight = height;
        }

        // 渲染色块（使用已缓存的Rect）
        int startIdx = scrollOffset * cols;
        for (int i = 0; i < swatchRects.size() && startIdx + i < swatches.size(); i++) {
            Rect r = swatchRects.get(i);
            Entry e = swatches.get(startIdx + i);
            drawSwatchWithLabel(g, r.x, r.y, e.rgb(), e.code());
        }

        // 滚动条
        if (totalRows > visibleRows) {
            int barX = width - 8;
            int barY = contentY;
            int barH = contentH;
            int thumbH = Math.max(20, barH * visibleRows / totalRows);
            int thumbY = barY + (barH - thumbH) * scrollOffset / Math.max(1, maxScroll);
            g.fill(barX, barY, barX + 4, barY + barH, 0xFF333333);
            g.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xFF888888);
        }

        // 状态信息
        if (!statusMsg.isEmpty()) {
            g.drawString(font, Component.literal(statusMsg), 10, height - 14, 0xFFFFAA);
        }
    }

    /**
     * 输入色号页面渲染。
     */
    private void renderInputPage(GuiGraphics g) {
        String title = "输入想用的色号";
        g.drawString(font, Component.literal(title),
                (width - font.width(title)) / 2, height / 2 - 70, 0xFFFFFF);

        String hint = "输入色号（如 A1、B5、Y7）后点击确认，自动放入快捷栏一组（64个）";
        g.drawString(font, Component.literal(hint),
                (width - font.width(hint)) / 2, height / 2 + 50, 0xAAAAAA);

        if (!statusMsg.isEmpty()) {
            g.drawString(font, Component.literal(statusMsg),
                    (width - font.width(statusMsg)) / 2, height / 2 + 75, 0xFFFFAA);
        }
    }

    // ==================== 绘制辅助 ====================

    private void drawSwatchWithLabel(GuiGraphics g, int x, int y, int rgb, String label) {
        g.fill(x - 1, y - 1, x + SW + 1, y + SW + 1, 0xFF333333);
        g.fill(x, y, x + SW, y + SW, 0xFF000000 | rgb);
        g.drawString(font, Component.literal(label), x, y + SW + 2, 0x999999, false);
    }

    // ==================== 鼠标交互 ====================

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && currentPage == Page.SWATCHES) {
            // 使用已缓存的Rect进行点击检测
            int cols = Math.max(8, Math.min(24, (width - 31) / 31));
            int startIdx = scrollOffset * cols;
            for (int i = 0; i < swatchRects.size() && startIdx + i < swatches.size(); i++) {
                Rect r = swatchRects.get(i);
                if (r.hit(mx, my)) {
                    Entry e = swatches.get(startIdx + i);
                    MardNetwork.CHANNEL.sendToServer(new MardNetwork.RequestItemPacket(e.target()));
                    statusMsg = "已给予一组 " + e.code();
                    return true; // 不关闭页面，支持连续选择
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (currentPage == Page.SWATCHES) {
            scrollOffset -= (int) Math.signum(delta);
            if (scrollOffset < 0) scrollOffset = 0;
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    // ==================== 键盘交互 ====================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC 键优先处理：关闭界面（修复ESC无法退出）
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        // 输入框聚焦时处理按键
        if (inputBox != null && inputBox.isFocused() && currentPage == Page.INPUT) {
            // 回车键确认
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                submitCode();
                return true;
            }
            return inputBox.keyPressed(keyCode, scanCode, modifiers);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏
    }
}
