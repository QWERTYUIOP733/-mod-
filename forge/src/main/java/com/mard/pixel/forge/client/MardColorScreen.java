package com.mard.pixel.forge.client;

import com.mard.pixel.common.ColorMath;
import com.mard.pixel.common.CustomColor;
import com.mard.pixel.common.CustomColorStore;
import com.mard.pixel.common.ImportedPaletteStore;
import com.mard.pixel.common.MardColor;
import com.mard.pixel.common.MardPalette;
import com.mard.pixel.common.PngImporter;
import com.mard.pixel.forge.MardNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * MARD Pixel Mod 主 UI 界面。
 *
 * 按照设计模板布局：
 * 主菜单：左侧3个按钮 + 右侧mod说明 + 底部提示
 * 按钮一：MARD颜色选取 - 展开全部色号，点击给一组，支持连续选择
 * 按钮二：输入想用的色号 - 输入框，输入后放快捷栏一组
 * 按钮三：导入外部图纸 - 附属模组，开发中
 */
public class MardColorScreen extends Screen {

    // ==================== 页面枚举 ====================
    private enum Page {
        MAIN,       // 主菜单
        SWATCHES,   // 颜色选取（按钮一）
        INPUT       // 输入色号（按钮二）
    }

    // ==================== 色块数据 ====================
    private record Entry(String code, int rgb, String target) {}
    private record Rect(int x, int y, int w, int h) {
        boolean hit(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    // ==================== 布局常量 ====================
    private static final int SW = 20, GAP = 3, COLS = 18;
    private static final Path IMPORT_DIR = FMLPaths.CONFIGDIR.get().resolve("mard_pixel").resolve("import");

    // ==================== 状态 ====================
    private Page currentPage = Page.MAIN;
    private final List<Entry> swatches = new ArrayList<>();
    private final List<Rect> swatchRects = new ArrayList<>();
    private int scrollOffset = 0;

    // 输入色号页面
    private EditBox inputBox;
    private String statusMsg = "";

    public MardColorScreen() {
        super(Component.literal("MARD 色板"));
        ImportedPaletteStore.init(FMLPaths.CONFIGDIR.get().resolve("mard_pixel"));
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

        switch (currentPage) {
            case MAIN -> initMainPage();
            case SWATCHES -> initSwatchesPage();
            case INPUT -> initInputPage();
        }
    }

    /**
     * 主菜单页面：左侧3个按钮 + 右侧说明 + 底部提示。
     */
    private void initMainPage() {
        int btnW = 280, btnH = 36;
        int btnX = (width - btnW) / 2 - 80; // 偏左
        int startY = height / 2 - 80;
        int gapY = 50;

        // 按钮一：MARD 颜色选取
        addRenderableWidget(Button.builder(Component.literal("MARD 颜色选取"), btn -> {
            currentPage = Page.SWATCHES;
            scrollOffset = 0;
            init();
        }).bounds(btnX, startY, btnW, btnH).build());

        // 按钮二：输入想用的色号
        addRenderableWidget(Button.builder(Component.literal("输入想用的色号"), btn -> {
            currentPage = Page.INPUT;
            init();
        }).bounds(btnX, startY + gapY, btnW, btnH).build());

        // 按钮三：导入外部图纸（占位，开发中）
        addRenderableWidget(Button.builder(Component.literal("导入外部图纸"), btn -> {
            statusMsg = "按钮三在开发中，敬请期待";
        }).bounds(btnX, startY + gapY * 2, btnW, btnH).build());
    }

    /**
     * 颜色选取页面：全部色号网格，点击给一组，支持连续选择。
     */
    private void initSwatchesPage() {
        // 返回按钮
        addRenderableWidget(Button.builder(Component.literal("← 返回"), btn -> {
            currentPage = Page.MAIN;
            statusMsg = "";
            init();
        }).bounds(10, 6, 60, 20).build());
    }

    /**
     * 输入色号页面：输入框 + 确认按钮。
     */
    private void initInputPage() {
        // 返回按钮
        addRenderableWidget(Button.builder(Component.literal("← 返回"), btn -> {
            currentPage = Page.MAIN;
            statusMsg = "";
            init();
        }).bounds(10, 6, 60, 20).build());

        // 输入框
        int boxW = 200, boxH = 20;
        int boxX = (width - boxW) / 2;
        int boxY = height / 2 - 20;

        inputBox = new EditBox(font, boxX, boxY, boxW, boxH, Component.literal(""));
        inputBox.setMaxLength(16);
        inputBox.setFocused(true);
        addRenderableWidget(inputBox);

        // 确认按钮
        addRenderableWidget(Button.builder(Component.literal("确认放入快捷栏"), btn -> {
            String code = inputBox.getValue().trim().toUpperCase();
            if (!code.isEmpty()) {
                MardNetwork.CHANNEL.sendToServer(new MardNetwork.HotbarPacket(code));
                statusMsg = "已请求放入快捷栏: " + code;
                inputBox.setValue("");
            } else {
                statusMsg = "请输入色号";
            }
        }).bounds(boxX, boxY + 30, boxW, 20).build());
    }

    // ==================== 渲染 ====================

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g);

        switch (currentPage) {
            case MAIN -> renderMainPage(g);
            case SWATCHES -> renderSwatchesPage(g, mx, my);
            case INPUT -> renderInputPage(g);
        }

        super.render(g, mx, my, partialTick);
    }

    /**
     * 主菜单页面渲染。
     */
    private void renderMainPage(GuiGraphics g) {
        // 标题
        g.drawString(font, Component.literal("MARD 像素色块 Mod"), width / 2 - 60, 30, 0xFFFFFF);

        // 右侧：mod 使用说明
        int infoX = width / 2 + 100;
        int infoY = height / 2 - 100;
        int infoW = 200;

        // 说明框背景
        g.fill(infoX - 5, infoY - 5, infoX + infoW + 5, infoY + 180, 0xFF1a1a1a);
        g.fill(infoX - 4, infoY - 4, infoX + infoW + 4, infoY + 179, 0xFF2a2a2a);

        g.drawString(font, Component.literal("mod 使用说明"), infoX, infoY, 0xFFFFAA);

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
        for (String line : lines) {
            g.drawString(font, Component.literal(line), infoX, y, 0xCCCCCC);
            y += 11;
        }

        // 底部最后一行：按钮三在开发中，敬请期待
        g.drawString(font, Component.literal("按钮三在开发中，敬请期待"),
                width / 2 - 80, height - 30, 0x888888);

        // 状态信息
        if (!statusMsg.isEmpty()) {
            g.drawString(font, Component.literal(statusMsg), 10, height - 14, 0xFFFFAA);
        }
    }

    /**
     * 颜色选取页面渲染：全部色号网格，支持滚动。
     */
    private void renderSwatchesPage(GuiGraphics g, int mx, int my) {
        // 标题
        g.drawString(font, Component.literal("MARD 颜色选取 - 点击色块获取一组（64个）"),
                80, 12, 0xFFFFFF);

        swatchRects.clear();
        int contentY = 40;
        int contentH = height - contentY - 20;
        int cellH = SW + GAP + 10; // 色块+标签
        int visibleRows = contentH / cellH;
        int totalRows = (int) Math.ceil((double) swatches.size() / COLS);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        int startIdx = scrollOffset * COLS;

        for (int i = startIdx; i < swatches.size(); i++) {
            int col = (i - startIdx) % COLS;
            int row = (i - startIdx) / COLS;
            if (row >= visibleRows) break;

            int x = 10 + col * (SW + GAP + 8);
            int y = contentY + row * cellH;
            Entry e = swatches.get(i);
            drawSwatchWithLabel(g, x, y, e.rgb(), e.code());
            swatchRects.add(new Rect(x, y, SW, SW + 10));
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
        // 标题
        g.drawString(font, Component.literal("输入想用的色号"),
                width / 2 - 50, height / 2 - 60, 0xFFFFFF);

        // 提示
        g.drawString(font, Component.literal("输入色号（如 A1、B5、Y7）后点击确认，自动放入快捷栏一组（64个）"),
                width / 2 - 200, height / 2 + 70, 0xAAAAAA);

        // 状态信息
        if (!statusMsg.isEmpty()) {
            g.drawString(font, Component.literal(statusMsg),
                    width / 2 - 100, height / 2 + 90, 0xFFFFAA);
        }
    }

    // ==================== 绘制辅助 ====================

    private void drawSwatchWithLabel(GuiGraphics g, int x, int y, int rgb, String label) {
        // 边框
        g.fill(x - 1, y - 1, x + SW + 1, y + SW + 1, 0xFF333333);
        // 色块本体
        g.fill(x, y, x + SW, y + SW, 0xFF000000 | rgb);
        // 色号标签（色块下方）
        g.drawString(font, Component.literal(label), x, y + SW + 2, 0x999999, false);
    }

    // ==================== 鼠标交互 ====================

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && currentPage == Page.SWATCHES) {
            // 点击色块给一组方块，支持连续选择（不关闭页面）
            for (int i = 0; i < swatchRects.size(); i++) {
                Rect r = swatchRects.get(i);
                if (r.hit(mx, my)) {
                    int idx = scrollOffset * COLS + i;
                    if (idx < swatches.size()) {
                        Entry e = swatches.get(idx);
                        MardNetwork.CHANNEL.sendToServer(new MardNetwork.RequestItemPacket(e.target()));
                        statusMsg = "已给予一组 " + e.code();
                    }
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inputBox != null && inputBox.isFocused()) {
            // 回车键确认
            if (keyCode == 257 || keyCode == 335) { // Enter / Numpad Enter
                String code = inputBox.getValue().trim().toUpperCase();
                if (!code.isEmpty()) {
                    MardNetwork.CHANNEL.sendToServer(new MardNetwork.HotbarPacket(code));
                    statusMsg = "已请求放入快捷栏: " + code;
                    inputBox.setValue("");
                }
                return true;
            }
            return inputBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
