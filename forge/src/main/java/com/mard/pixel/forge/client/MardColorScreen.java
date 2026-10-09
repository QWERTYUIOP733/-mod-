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
import com.mard.pixel.forge.MardNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 彩色方块扩展 主 UI 界面。
 *
 * 布局根据游戏窗口大小动态调整。
 * 主菜单：左侧2个按钮 + 右侧说明 + 底部提示
 * 按钮一：颜色选取 - 全部色号网格，点击给一组，支持连续选择
 * 按钮二：输入色号 - 输入框，输入后放快捷栏一组
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

    /** 全局布局系数：恒为 1，页面元素按标准像素清晰绘制（不做浮点缩放，避免位图字体发虚） */
    private float ui = 1f;
    /** 当前 Minecraft GUI scale（帧缓冲/逻辑分辨率反推） */
    private double gs = 1.0;
    /** 是否全屏或最大化 */
    private boolean fullscreen = true;
    /** 模组介绍框文字系数：全屏=1（标准），窗口化=降一个整数 GUI 档（整数比，清晰变小） */
    private float infoSc = 1f;

    /**
     * 回退到清晰基准：全局不缩放（ui=1）。
     * 仅判定“全屏/窗口化”，并为模组介绍框文字计算一个整数 GUI 档之比 infoSc，
     * 位图字体按整数档缩放可保持像素对齐、清晰不糊。
     */
    private void computeUi() {
        ui = 1f;
        gs = 1.0;
        fullscreen = true;
        infoSc = 1f;
        try {
            long win = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
            if (win != 0L) {
                int fbW, fbH, sw, sh, mw = 0, mh = 0;
                try (org.lwjgl.system.MemoryStack st = org.lwjgl.system.MemoryStack.stackPush()) {
                    java.nio.IntBuffer wb = st.mallocInt(1), hb = st.mallocInt(1);
                    org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(win, wb, hb);
                    fbW = wb.get(0);
                    fbH = hb.get(0);
                    java.nio.IntBuffer swb = st.mallocInt(1), shb = st.mallocInt(1);
                    org.lwjgl.glfw.GLFW.glfwGetWindowSize(win, swb, shb);
                    sw = swb.get(0);
                    sh = shb.get(0);
                    long mon = org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor();
                    if (mon != 0L) {
                        java.nio.IntBuffer xb = st.mallocInt(1), yb = st.mallocInt(1), wwb = st.mallocInt(1), hhb = st.mallocInt(1);
                        org.lwjgl.glfw.GLFW.glfwGetMonitorWorkarea(mon, xb, yb, wwb, hhb);
                        mw = wwb.get(0);
                        mh = hhb.get(0);
                    }
                }
                int logW = Math.max(1, this.width);
                double cur = Math.max(1.0, (double) fbW / logW);
                gs = cur;
                boolean fs = org.lwjgl.glfw.GLFW.glfwGetWindowMonitor(win) != 0L;
                if (!fs && mw > 0 && mh > 0) fs = sw >= mw * 0.95 && sh >= mh * 0.95;
                fullscreen = fs;
                // 介绍框字号 infoSc 在 renderMainPage 内按框体实际尺寸逐帧计算（自动取清晰整数档并垂直居中）
                infoSc = 1f;
            }
        } catch (Throwable ignored) {}
    }

    public MardColorScreen() {
        super(Component.literal("色板"));
        rebuildSwatches();
    }

    private void rebuildSwatches() {
        swatches.clear();
        for (MardColor mc : MardPalette.COLORS) {
            swatches.add(new Entry(mc.code(), mc.rgb(), mc.code()));
        }
    }

    // ==================== 初始化 ====================

    @Override
    protected void init() {
        computeUi();
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
     * 主菜单页面：左侧2个按钮 + 右侧说明。
     * 按钮区偏左且较窄，说明区保持不变。
     * 按钮一：颜色选取
     * 按钮二：输入想用的色号
     */
    /** 主页两按钮的几何（真实像素，随 ui 等比缩放）：[btnX, btn1Y, btnW, btnH, btn2Y, centerX] */
    private int[] mainButtonGeometry() {
        int panelX = (int) (40 * ui);
        int panelY = (int) (70 * ui);
        int panelW = (int) (width - 80 * ui);
        int panelH = (int) (height - 130 * ui);
        int btnWBase = (int) Math.min(220f, panelW * 0.45f / ui - 40f);
        int btnW = Math.max(40, (int) (btnWBase * ui));
        int btnH = Math.max(16, (int) (30 * ui));
        int centerX = panelX + (int) (panelW * 0.35f);
        int btnX = centerX - btnW / 2;
        int btnCenterY = panelY + panelH / 2;
        int btn1Y = btnCenterY - (int) (45 * ui);
        int btn2Y = btn1Y + (int) (60 * ui);
        return new int[]{btnX, btn1Y, btnW, btnH, btn2Y, centerX};
    }

    /** 在按钮 widget 之上绘制两个主页按钮的文字（与按钮共用 ui 缩放系数） */
    private void renderMainButtonLabels(GuiGraphics g) {
        int textH = Math.max(1, (int) (9 * ui));
        int[] geo = mainButtonGeometry();
        int cx = geo[5], b1 = geo[1], b2 = geo[4], bw = geo[2], bh = geo[3];
        drawCenteredAdaptive(g, "颜色选取", cx, b1 + (bh - textH) / 2, 0xFFFFFFFF, bw - (int) (6 * ui));
        drawCenteredAdaptive(g, "输入想用的色号", cx, b2 + (bh - textH) / 2, 0xFFFFFFFF, bw - (int) (6 * ui));
    }

    private void initMainPage() {
        int[] geo = mainButtonGeometry();
        int btnX = geo[0], btn1Y = geo[1], btnW = geo[2], btnH = geo[3], btn2Y = geo[4];

        // 按钮一：颜色选取（文字自绘以随 ui 缩放）
        addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            currentPage = Page.SWATCHES;
            scrollOffset = 0;
            init();
        }).bounds(btnX, btn1Y, btnW, btnH).build());

        // 按钮二：输入想用的色号
        addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            currentPage = Page.INPUT;
            init();
        }).bounds(btnX, btn2Y, btnW, btnH).build());
    }

    /**
     * 颜色选取页面。
     * 创造模式：点击色块直接获取一组方块。
     * 生存模式：仅查看颜色，合成请使用方块染色台。
     */
    private void initSwatchesPage() {
        addRenderableWidget(Button.builder(Component.literal("← 返回"), btn -> {
            currentPage = Page.MAIN;
            statusMsg = "";
            init();
        }).bounds(10, 6, 60, 20).build());

        // 生存模式下显示提示
        if (isSurvivalMode()) {
            statusMsg = "生存模式下仅可查看颜色，合成请使用方块染色台（消耗七彩粉末）";
        }
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

        if (isSurvivalMode()) {
            // 生存模式：显示禁用提示，不显示输入框
            statusMsg = "生存模式下输入色号功能已禁用，请使用方块染色台或七彩粉末合成";
        } else {
            // 创造模式：正常显示输入框
            inputBox = new EditBox(font, boxX, boxY, boxW, boxH, Component.literal(""));
            inputBox.setMaxLength(16);
            inputBox.setFocused(true);
            addRenderableWidget(inputBox);

            addRenderableWidget(Button.builder(Component.literal("确认放入快捷栏"), btn -> {
                submitCode();
            }).bounds(boxX, boxY + 32, boxW, 22).build());
        }
    }

    private void submitCode() {
        if (inputBox == null) return;
        String input = inputBox.getValue().trim();
        if (input.isEmpty()) {
            statusMsg = "请输入色号";
            return;
        }

        // 支持多种分隔符：空格、逗号、分号、顿号、斜杠
        String[] codes = input.split("[\\s,;、/]+");
        int validCount = 0;
        StringBuilder successMsg = new StringBuilder();

        for (String code : codes) {
            String trimmed = code.trim().toUpperCase();
            if (!trimmed.isEmpty()) {
                MardNetwork.CHANNEL.send(new MardNetwork.HotbarPacket(trimmed),
                        net.minecraftforge.network.PacketDistributor.SERVER.noArg());
                if (validCount > 0) successMsg.append(", ");
                successMsg.append(trimmed);
                validCount++;
            }
        }

        if (validCount > 0) {
            if (validCount == 1) {
                statusMsg = "已请求放入快捷栏: " + successMsg;
            } else {
                statusMsg = "已批量请求 " + validCount + " 个色号: " + successMsg;
            }
            inputBox.setValue("");
        } else {
            statusMsg = "未识别到有效色号";
        }
    }

    // ==================== 渲染 ====================

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 手动绘制和 1.20.1 一致的深色半透明背景，避免 1.21 默认模糊背景导致文字重影
        g.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        computeUi();
        // super.render 会调用上面的 renderBackground 绘制纯色半透明背景，再绘制按钮
        super.render(g, mx, my, partialTick);

        // 文字内容绘制在最上层，保证清晰
        switch (currentPage) {
            case MAIN -> renderMainPage(g);
            case SWATCHES -> renderSwatchesPage(g);
            case INPUT -> renderInputPage(g);
        }

        // 主页按钮文字在最上层绘制，保证随 ui 缩放且不被按钮背景遮挡
        if (currentPage == Page.MAIN) renderMainButtonLabels(g);
    }

    /**
     * 主菜单页面渲染。
     */
    private void renderMainPage(GuiGraphics g) {
        // 大面板布局参数（随 ui 等比缩放）
        int panelX = (int) (40 * ui);
        int panelY = (int) (70 * ui);
        int panelW = (int) (width - 80 * ui);
        int panelH = (int) (height - 130 * ui);
        int screenPad = (int) (20 * ui);
        int textH = Math.max(1, (int) (9 * ui));

        // 标题（面板上方居中）
        String title = "彩色方块扩展";
        drawCenteredAdaptive(g, title, width / 2, panelY - (int) (28 * ui), 0xFFFFFF, width - screenPad);

        // 当前模式（标题下方）
        String modeText = "当前模式：" + getGameModeName();
        int modeColor = isSurvivalMode() ? 0xFF5555 : 0x55FF55;
        drawCenteredAdaptive(g, modeText, width / 2, panelY - (int) (14 * ui), modeColor, width - screenPad);

        // 提示内容（模式文本下方，精简版，根据模式改变）
        String tipText;
        if (isSurvivalMode()) {
            tipText = "生存模式：仅可查看颜色 | 合成请使用方块染色台 | 七彩粉末可合成任意色块";
        } else {
            tipText = "创造模式：可直接获取方块 | 按钮一：浏览色号点击获取 | 按钮二：输入色号快速获取";
        }
        int tipColor = isSurvivalMode() ? 0xFFAA44 : 0x44FF44;
        drawCenteredAdaptive(g, tipText, width / 2, panelY - (int) (2 * ui), tipColor, width - screenPad);

        // 大面板背景已移除（透明背景）

        // 右侧说明面板
        int pad = (int) (8 * ui);
        int infoPanelX = panelX + (int) (panelW * 0.55);
        int infoPanelW = (int) (panelW * 0.42);
        int infoPanelY = panelY + (int) (10 * ui);
        int infoPanelH = panelH - (int) (10 * ui);

        // 说明面板背景
        g.fill(infoPanelX, infoPanelY, infoPanelX + infoPanelW, infoPanelY + infoPanelH, 0xEE2a2a2a);
        // 说明面板边框
        g.fill(infoPanelX, infoPanelY, infoPanelX + infoPanelW, infoPanelY + 1, 0xFF666666);
        g.fill(infoPanelX, infoPanelY + infoPanelH - 1, infoPanelX + infoPanelW, infoPanelY + infoPanelH, 0xFF444444);
        g.fill(infoPanelX, infoPanelY, infoPanelX + 1, infoPanelY + infoPanelH, 0xFF666666);
        g.fill(infoPanelX + infoPanelW - 1, infoPanelY, infoPanelX + infoPanelW, infoPanelY + infoPanelH, 0xFF444444);

        String infoTitle = "mod 使用说明";
        String[] lines = isSurvivalMode() ? new String[]{
            "221色像素画（生存模式）",
            "",
            "按钮一：浏览全部色号",
            "  生存模式仅查看颜色",
            "",
            "按钮二：输入色号",
            "  生存模式下此功能禁用"
        } : new String[]{
            "221色像素画（创造模式）",
            "",
            "按钮一：浏览全部色号",
            "  点击色块获取一组方块",
            "",
            "按钮二：输入色号快速获取",
            "  输入色号后放入快捷栏"
        };

        // —— 介绍文字：按框体当前物理尺寸自动取“最大清晰整数档”，并在框内垂直居中 ——
        double gsv = gs > 0 ? gs : 1.0;
        float maxW = font.width(infoTitle);
        for (String ln : lines) maxW = Math.max(maxW, font.width(ln));
        float titleH0 = 9f, titleGap0 = 4f, lineH0 = 10f;
        float blockH0 = titleH0 + titleGap0 + lines.length * lineH0; // GUI1 档下文字块逻辑高
        float availW = Math.max(1f, infoPanelW - pad * 2);
        double effByH = 0.78 * infoPanelH * gsv / blockH0;           // 高度期望档（块约占框 78%）
        double effByW = availW * gsv / Math.max(1f, maxW);           // 宽度允许档（不超出框）
        int eff = Math.max(1, (int) Math.round(effByH));
        int effW = Math.max(1, (int) Math.floor(effByW));
        if (eff > effW) eff = effW;
        eff = Math.min(eff, 8);
        float sc = (float) (eff / gsv);                              // 相对当前 GUI 档的缩放（整数档→清晰）
        infoSc = sc;

        var pose = g.pose();
        pose.pushPose();
        pose.translate(Math.round(infoPanelX * gsv) / (float) gsv,
                       Math.round(infoPanelY * gsv) / (float) gsv, 0);
        pose.scale(sc, sc, 1f);
        float ty = (infoPanelH / sc - blockH0) / 2f;                 // 文字块在框内垂直居中
        if (ty < 2f) ty = 2f;
        g.drawString(font, Component.literal(infoTitle), pad, (int) ty, 0xFFFFAA);
        float cy = ty + titleH0 + titleGap0;
        for (String ln : lines) {
            g.drawString(font, Component.literal(ln), pad, (int) cy, 0xCCCCCC);
            cy += lineH0;
        }
        pose.popPose();

        // 版本号（面板下方居中）
        String bottomText = "彩色方块扩展 v2.1.0";
        drawCenteredAdaptive(g, bottomText, width / 2, panelY + panelH + (int) (10 * ui), 0x888888, width - screenPad);

        // 状态信息
        if (!statusMsg.isEmpty()) {
            drawLeftAdaptive(g, statusMsg, (int) (10 * ui), height - (int) (14 * ui), 0xFFFFAA, width - screenPad);
        }
    }

    /**
     * 颜色选取页面渲染：全部色号网格，支持滚动。
     * 优化：只在滚动或窗口大小变化时重算Rect，避免每帧卡顿。
     */
    private void renderSwatchesPage(GuiGraphics g) {
        // 检测当前游戏模式
        boolean isSurvivalRender = isSurvivalMode();

        // 生存模式下显示红色警告条（右对齐到右上角，避开左上"返回"按钮）
        if (isSurvivalRender) {
            String warnText = "生存模式：仅可查看颜色，点击不会获取方块，请使用方块染色台合成";
            int warnY = 6, warnH = 20, pad = 12;
            int warnWidth = Math.min(font.width(warnText) + pad * 2, width - 78 - 6);
            int warnX = Math.max(78, width - 6 - warnWidth);
            g.fill(warnX, warnY, warnX + warnWidth, warnY + warnH, 0xFF8B0000);
            g.fill(warnX + 1, warnY + 1, warnX + warnWidth - 1, warnY + warnH - 1, 0xFFB22222);
            drawLeftAdaptive(g, warnText, warnX + pad, warnY + (warnH - 8f) / 2f + 1f, 0xFFFFE4B5, warnWidth - pad * 2);
        } else {
            // 创造模式：标题右对齐到右上角，避免压住"返回"按钮；窄屏等比缩小
            String title = "颜色选取 - 点击色块获取一组（64个）";
            drawRightAdaptive(g, title, width - 10, 12, 0xFFFFFF, width - 78 - 6);
        }

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
            drawLeftAdaptive(g, statusMsg, 10, height - 14, 0xFFFFAA, width - 20);
        }
    }

    /**
     * 输入色号页面渲染。
     */
    private void renderInputPage(GuiGraphics g) {
        if (isSurvivalMode()) {
            // 生存模式：显示大的禁用提示
            String title = "输入色号功能已禁用";
            drawCenteredAdaptive(g, title, width / 2, height / 2f - 50, 0xFF5555, width - 20);

            // 红色警告框
            String warn1 = "生存模式下无法通过输入色号直接获取方块";
            String warn2 = "请使用方块染色台，通过七彩粉末合成对应颜色";
            int boxWidth = Math.min(Math.max(font.width(warn1), font.width(warn2)) + 40, width - 20);
            int boxX = (width - boxWidth) / 2;
            int boxY = height / 2 - 20;
            g.fill(boxX, boxY, boxX + boxWidth, boxY + 70, 0x88FF3333);
            g.fill(boxX + 2, boxY + 2, boxX + boxWidth - 2, boxY + 68, 0xFFFF5555);
            drawCenteredAdaptive(g, warn1, width / 2, boxY + 15, 0xFFFFFF, boxWidth - 12);
            drawCenteredAdaptive(g, warn2, width / 2, boxY + 35, 0xFFFFEE, boxWidth - 12);

            if (!statusMsg.isEmpty()) {
                drawCenteredAdaptive(g, statusMsg, width / 2, height / 2f + 70, 0xFFFFAA, width - 20);
            }
        } else {
            // 创造模式：正常显示
            String title = "输入想用的色号（支持批量输入）";
            drawCenteredAdaptive(g, title, width / 2, height / 2f - 70, 0xFFFFFF, width - 20);

            String hint = "输入色号（如 A1、B5、M3），支持批量输入多个色号";
            drawCenteredAdaptive(g, hint, width / 2, height / 2f + 45, 0xAAAAAA, width - 20);

            String hint2 = "用空格/逗号/分号分隔，例如：A1 B2 C3 或 A1,B2,C3";
            drawCenteredAdaptive(g, hint2, width / 2, height / 2f + 60, 0x888888, width - 20);

            if (!statusMsg.isEmpty()) {
                drawCenteredAdaptive(g, statusMsg, width / 2, height / 2f + 85, 0xFFFFAA, width - 20);
            }
        }
    }

    // ==================== 绘制辅助 ====================

    // ==================== 文字自适应（随窗口宽度等比缩放，不溢出/不重叠） ====================

    /** 居中绘制：整体随 ui 等比缩放；若缩放后仍超过 maxWidth 再额外缩到刚好放下 */
    private void drawCenteredAdaptive(GuiGraphics g, String text, int centerX, float y, int color, int maxWidth) {
        int w = font.width(text);
        int yi = (int) y;
        float fit = (maxWidth > 0 && w * ui > maxWidth) ? (float) maxWidth / (w * ui) : 1f;
        float sc = ui * fit;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(centerX, yi, 0);
        pose.scale(sc, sc, 1f);
        g.drawString(font, Component.literal(text), -w / 2, 0, color);
        pose.popPose();
    }

    /** 左对齐绘制：整体随 ui 等比缩放；若缩放后仍超过 maxWidth 再额外缩到刚好放下 */
    private void drawLeftAdaptive(GuiGraphics g, String text, float x, float y, int color, int maxWidth) {
        if (text == null || text.isEmpty()) return;
        int w = font.width(text);
        int xi = (int) x, yi = (int) y;
        float fit = (maxWidth > 0 && w * ui > maxWidth) ? (float) maxWidth / (w * ui) : 1f;
        float sc = ui * fit;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(xi, yi, 0);
        pose.scale(sc, sc, 1f);
        g.drawString(font, Component.literal(text), 0, 0, color);
        pose.popPose();
    }

    /** 右对齐绘制：整体随 ui 等比缩放；若缩放后仍超过 maxWidth 再额外缩到刚好放下 */
    private void drawRightAdaptive(GuiGraphics g, String text, int rightX, float y, int color, int maxWidth) {
        int w = font.width(text);
        int yi = (int) y;
        float fit = (maxWidth > 0 && w * ui > maxWidth) ? (float) maxWidth / (w * ui) : 1f;
        float sc = ui * fit;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(rightX, yi, 0);
        pose.scale(sc, sc, 1f);
        g.drawString(font, Component.literal(text), -w, 0, color);
        pose.popPose();
    }

    private void drawSwatchWithLabel(GuiGraphics g, int x, int y, int rgb, String label) {
        g.fill(x - 1, y - 1, x + SW + 1, y + SW + 1, 0xFF333333);
        g.fill(x, y, x + SW, y + SW, 0xFF000000 | rgb);
        g.drawString(font, Component.literal(label), x, y + SW + 2, 0x999999, false);
    }

    // ==================== 鼠标交互 ====================

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && currentPage == Page.SWATCHES) {
            // 检测当前游戏模式
            boolean isSurvivalClick = isSurvivalMode();

            // 使用已缓存的Rect进行点击检测
            int cols = Math.max(8, Math.min(24, (width - 31) / 31));
            int startIdx = scrollOffset * cols;
            for (int i = 0; i < swatchRects.size() && startIdx + i < swatches.size(); i++) {
                Rect r = swatchRects.get(i);
                if (r.hit(mx, my)) {
                    Entry e = swatches.get(startIdx + i);
                    if (isSurvivalClick) {
                        // 生存模式：仅查看，提示使用方块染色台
                        statusMsg = e.code() + " - 生存模式下请使用方块染色台合成（消耗七彩粉末）";
                    } else {
                        // 创造模式：直接给予一组
                        MardNetwork.CHANNEL.send(new MardNetwork.RequestItemPacket(e.target()),
                                net.minecraftforge.network.PacketDistributor.SERVER.noArg());
                        statusMsg = "已给予一组 " + e.code();
                    }
                    return true; // 不关闭页面，支持连续选择
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontalDelta, double verticalDelta) {
        if (currentPage == Page.SWATCHES) {
            scrollOffset -= (int) Math.signum(verticalDelta);
            if (scrollOffset < 0) scrollOffset = 0;
            return true;
        }
        return super.mouseScrolled(mx, my, horizontalDelta, verticalDelta);
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

    /**
     * 检测当前是否为生存模式（非创造、非旁观）。
     * 使用多种方式检测，确保准确性。
     */
    private boolean isSurvivalMode() {
        if (Minecraft.getInstance().player == null) return false;
        var player = Minecraft.getInstance().player;
        // 非创造且非旁观即为生存/冒险模式
        return !player.isCreative() && !player.isSpectator();
    }

    /**
     * 获取当前游戏模式的显示名称。
     */
    private String getGameModeName() {
        if (isSurvivalMode()) {
            return "生存模式";
        } else if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative()) {
            return "创造模式";
        } else if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.isSpectator()) {
            return "旁观模式";
        }
        return "未知模式";
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏
    }
}
