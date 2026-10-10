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
import com.mard.pixel.forge.MardPixelForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * 彩色方块扩展主界面（G 键打开），共三页：主页 / 颜色选取（色板大屏）/ 输入色号。
 *
 * 26.3 渲染管线迁移：render/renderBackground→extractRenderState/extractBackground；
 * GuiGraphics→GuiGraphicsExtractor；drawString(Component)→g.text(String)；
 * 矩阵改 JOML Matrix3x2fStack（pushMatrix/translate/scale/popMatrix）；
 * renderTooltip→g.setTooltipForNextFrame；mouseClicked/keyPressed 改事件对象；
 * Window.isFullscreen→isExclusiveFullscreen。
 *
 * 介绍框自适应（9px 整数倍清晰档 + 固定框体内整块垂直居中）为定版方案，逐字保留，只换渲染 API。
 */
public class MardColorScreen extends Screen {

    private static final int PAGE_MAIN = 0;
    private static final int PAGE_PALETTE = 1;
    private static final int PAGE_INPUT = 2;

    // 色板网格几何（Forge 定版，不套用 Fabric 尺寸）
    private static final int SW = 20;
    private static final int GAP = 3;
    private static final int CELL_W = 31;
    private static final int CELL_H = 33;
    private static final int CONTENT_Y = 40;

    private int page = PAGE_MAIN;

    private List<Entry> swatches = new ArrayList<>();
    private double scrollOffset = 0;
    private double scrollProgress = 0;
    private int paletteMaxScroll = 0;

    private EditBox inputBox;
    private Button confirmButton;
    private Button backButton;

    private float ui = 1f;
    private float gs = 1f;
    private boolean fullscreen = true;
    private float infoSc = 1f;

    private String currentMode = "未知";

    // 色板/输入页屏幕内即时状态提示（与 1.21.4 定版、Fabric 端一致）
    private String statusMsg = "";

    private record Entry(String code, int rgb, String target) {}

    public MardColorScreen() {
        super(Component.literal(""));
    }

    @Override
    protected void init() {
        super.init();
        computeUi();
        clearWidgets();
        if (page == PAGE_MAIN) buildMainPage();
        else if (page == PAGE_PALETTE) buildPalettePage();
        else buildInputPage();
    }

    /**
     * 定版窗口参数（与 1.21.4 三态验收版一致，禁止改成"按窗口宽度分档整体缩放"）：
     * ui 恒为 1 —— 框体/面板几何固定，不随窗口做浮点整体缩放（否则位图字体发虚、小窗框高被压缩导致介绍框缩成一团并堆顶）；
     * gs = Minecraft GUI scale（帧缓冲物理像素 / 逻辑像素）；介绍框文字在 renderMainPage 内每帧按框体物理尺寸取 9px 整数倍清晰档并垂直居中。
     */
    private void computeUi() {
        this.ui = 1f;
        this.infoSc = 1f;
        try {
            com.mojang.blaze3d.platform.Window win = Minecraft.getInstance().getWindow();
            this.gs = (float) Math.max(1.0, win.getGuiScale());
            this.fullscreen = win.isExclusiveFullscreen();
        } catch (Throwable t) {
            this.gs = 1f;
            this.fullscreen = true;
        }
    }

    private void scheduleCompute() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(this::computeUi);
    }

    private void loadSwatches() {
        swatches.clear();
        for (MardColor c : MardPalette.COLORS) {
            swatches.add(new Entry(c.code(), c.rgb(), c.code().toLowerCase()));
        }
    }

    // ==================== 页面构建 ====================

    private void buildMainPage() {
        // 与 1.21.4 定版 mainButtonGeometry() 完全一致（26.3 ui 恒为 1，物理放大由 MC guiScale 负责）
        int panelX = 40;
        int panelY = 70;
        int panelW = this.width - 80;
        int panelH = this.height - 130;
        int btnWBase = (int) Math.min(220f, panelW * 0.45f - 40f);
        int bw = Math.max(40, btnWBase);
        int bh = Math.max(16, 30);
        int cx = panelX + (int) (panelW * 0.35f);
        int bx = cx - bw / 2;
        int btnCenterY = panelY + panelH / 2;
        int y1 = btnCenterY - 45;
        int y2 = y1 + 60;

        addRenderableWidget(Button.builder(Component.literal("颜色选取"), b -> {
            loadSwatches();
            page = PAGE_PALETTE;
            scrollOffset = 0; scrollProgress = 0;
            init();
        }).bounds(bx, y1, bw, bh).build());

        addRenderableWidget(Button.builder(Component.literal("输入想用的色号"), b -> {
            if (!isCreative()) {
                return;
            }
            page = PAGE_INPUT;
            init();
        }).bounds(bx, y2, bw, bh).build());
    }

    private void buildPalettePage() {
        // 与 1.21.4 定版 / Fabric 端一致：进色板页按模式给初始状态提示，点击色块后即时更新
        statusMsg = isCreative() ? "" : "生存模式下仅可查看颜色，合成请使用方块染色台（消耗七彩粉末）";
        int bw = 100, bh = 20;
        backButton = addRenderableWidget(Button.builder(Component.literal("◀ 返回"), b -> {
            page = PAGE_MAIN;
            init();
        }).bounds(8, 8, bw, bh).build());
    }

    private void buildInputPage() {
        int bw = 100, bh = 20;
        addRenderableWidget(Button.builder(Component.literal("◀ 返回"), b -> {
            page = PAGE_MAIN;
            init();
        }).bounds(8, 8, bw, bh).build());

        // 定版坐标（与 1.21.4 一致）：输入框 height/2-30、高22；确认按钮与输入框同宽、顶边 boxY+32（底边 height/2+24）；
        // 两行说明在 renderInputPage 中画于 height/2+45、+60（按钮下方），不得与按钮重叠。
        int boxW = Math.min(240, this.width / 3);
        int boxH = 22;
        int boxX = this.width / 2 - boxW / 2;
        int boxY = this.height / 2 - 30;
        inputBox = new EditBox(this.font, boxX, boxY, boxW, boxH, Component.literal(""));
        inputBox.setMaxLength(256);
        inputBox.setFocused(true);
        addRenderableWidget(inputBox);

        confirmButton = addRenderableWidget(Button.builder(Component.literal("确认放入快捷栏"), b -> confirmInput())
                .bounds(boxX, boxY + 32, boxW, 22).build());
    }

    // ==================== 渲染 ====================

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        switch (page) {
            case PAGE_MAIN -> renderMainPage(g);
            case PAGE_PALETTE -> renderPalettePage(g, mouseX, mouseY);
            case PAGE_INPUT -> renderInputPage(g);
            default -> { }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // 26.3 适配：人在世界中时 HUD（快捷栏/血条/聊天）每帧都会被提取，由 Screen 背景负责遮挡
        // （GameRenderer.extract 以 shouldRenderLevel 驱动 Hud.extractRenderState，与是否打开界面无关）。
        // 走官方世界内菜单背景（模糊背景层 + 深色菜单底）彻底遮住 HUD；blur 只作用背景 stratum，
        // 本界面自绘文字/按钮/色块在其后提取，不被模糊、不发虚。旧版 0xC0 半透明清晰背景在新管线下会透出 HUD，故弃用。
        super.extractBackground(g, mouseX, mouseY, partialTick);
    }

    /** 居中绘制（与 1.21.4 定版 drawCenteredAdaptive 一致；ui 恒 1，仅当超过 maxWidth 时等比缩到刚好放下，物理清晰度由 guiScale 整数档保证） */
    private void drawCenteredAdaptive(GuiGraphicsExtractor g, String text, int centerX, float y, int color, int maxWidth) {
        int w = this.font.width(text);
        int yi = (int) y;
        float fit = (maxWidth > 0 && w > maxWidth) ? (float) maxWidth / (float) w : 1f;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(centerX, yi);
        pose.scale(fit, fit);
        g.text(this.font, text, -w / 2, 0, color);
        pose.popMatrix();
    }

    private void renderMainPage(GuiGraphicsExtractor g) {
        currentMode = resolveModeName();
        boolean creative = isCreative();

        // 大面板布局参数（与 1.21.4 定版一致；ui 恒 1）
        int panelX = 40;
        int panelY = 70;
        int panelW = this.width - 80;
        int panelH = this.height - 130;
        int screenPad = 20;
        int maxTextW = this.width - screenPad;

        // 标题 / 当前模式 / 顶部提示（面板上方居中，原生字号自适应，不主动放大）
        drawCenteredAdaptive(g, "彩色方块扩展", this.width / 2, panelY - 28, 0xFFFFFFFF, maxTextW);
        drawCenteredAdaptive(g, "当前模式：" + currentMode, this.width / 2, panelY - 14,
                creative ? 0xFF55FF55 : 0xFFFF5555, maxTextW);

        String topTip = creative
                ? "创造模式：可直接获取方块 | 按钮一：浏览色号点击获取 | 按钮二：输入色号快速获取"
                : "生存模式：仅可查看颜色 | 合成请使用方块染色台 | 七彩粉末可合成任意色块";
        drawCenteredAdaptive(g, topTip, this.width / 2, panelY - 2,
                creative ? 0xFF44FF44 : 0xFFFFAA44, maxTextW);

        // ===== 右侧「mod 使用说明」介绍框（框体固定，文字按物理尺寸取清晰整数档并垂直居中）=====
        int pad = 8;

        int infoPanelX = panelX + (int) (panelW * 0.55);
        int infoPanelW = (int) (panelW * 0.42);
        int infoPanelY = panelY + 10;
        int infoPanelH = panelH - 10;

        g.fill(infoPanelX, infoPanelY, infoPanelX + infoPanelW, infoPanelY + infoPanelH, 0xEE2A2A2A);
        g.fill(infoPanelX, infoPanelY, infoPanelX + infoPanelW, infoPanelY + 1, 0xFF666666);
        g.fill(infoPanelX, infoPanelY, infoPanelX + 1, infoPanelY + infoPanelH, 0xFF666666);
        g.fill(infoPanelX, infoPanelY + infoPanelH - 1, infoPanelX + infoPanelW, infoPanelY + infoPanelH, 0xFF444444);
        g.fill(infoPanelX + infoPanelW - 1, infoPanelY, infoPanelX + infoPanelW, infoPanelY + infoPanelH, 0xFF444444);

        String infoTitle = "mod 使用说明";
        String[] lines = creative ? new String[] {
                "221色像素画（创造模式）",
                "",
                "按钮一：浏览全部色号",
                "  点击色块获取一组方块",
                "",
                "按钮二：输入色号快速获取",
                "  输入色号后放入快捷栏"
        } : new String[] {
                "221色像素画（生存模式）",
                "",
                "按钮一：浏览全部色号",
                "  生存模式仅查看颜色",
                "",
                "按钮二：输入色号",
                "  生存模式下此功能禁用"
        };

        double gsv = gs > 0 ? gs : 1.0;
        int maxW = this.font.width(infoTitle);
        for (String ln : lines) maxW = Math.max(maxW, this.font.width(ln));

        float titleH0 = 9f, titleGap0 = 4f, lineH0 = 10f;
        float blockH0 = titleH0 + titleGap0 + lines.length * lineH0;
        float availW = Math.max(1f, infoPanelW - pad * 2);

        double effByH = 0.78 * infoPanelH * gsv / blockH0;
        double effByW = availW * gsv / Math.max(1f, maxW);
        int eff = Math.max(1, (int) Math.round(effByH));
        int effW = Math.max(1, (int) Math.floor(effByW));
        if (eff > effW) eff = effW;
        eff = Math.min(eff, 8);
        float sc = (float) (eff / gsv);
        infoSc = sc;

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(Math.round(infoPanelX * gsv) / (float) gsv,
                       Math.round(infoPanelY * gsv) / (float) gsv);
        pose.scale(sc, sc);
        float ty = (infoPanelH / sc - blockH0) / 2f;
        if (ty < 2f) ty = 2f;
        g.text(this.font, infoTitle, pad, (int) ty, 0xFFFFFFAA);
        float cy = ty + titleH0 + titleGap0;
        for (String ln : lines) {
            g.text(this.font, ln, pad, (int) cy, 0xFFCCCCCC);
            cy += lineH0;
        }
        pose.popMatrix();

        // 底部版本号（面板下方居中，与 1.21.4 定版位置一致）
        String ver = "彩色方块扩展 v3.0.0";
        drawCenteredAdaptive(g, ver, this.width / 2, panelY + panelH + 10, 0xFF888888, maxTextW);
    }

    private void renderPalettePage(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        boolean creative = isCreative();

        // 返回键固定左上独立；标题/红条右置，不盖返回键
        String title = creative
                ? "颜色选取 — 点击色块获取一组（64个）"
                : "颜色选取 — 仅可查看颜色";
        g.text(this.font, title, this.width - this.font.width(title) - 12, 16, 0xFFFFFFFF);

        if (!creative) {
            String warn = "生存模式：仅可查看颜色，点击不会获取方块，请使用方块染色台合成";
            int barW = this.width - 140 - 20;
            int barX = 140;
            g.fill(barX, 6, barX + barW, 28, 0xAAFF0000);
            g.text(this.font, warn, barX + 10, 12, 0xFFFFFFFF);
        }

        int viewTop = CONTENT_Y;
        int viewBottom = this.height - 36;
        int viewH = viewBottom - viewTop;

        int cols = computeCols();
        int rowH = CELL_H + GAP;
        int totalRows = (int) Math.ceil(swatches.size() / (double) cols);
        int contentH = totalRows * rowH;
        paletteMaxScroll = Math.max(0, contentH - viewH);

        g.enableScissor(0, viewTop, this.width, viewBottom);
        int idx = 0;
        int gridW = cols * CELL_W + (cols - 1) * GAP;
        int startX = this.width / 2 - gridW / 2;
        for (MardColorScreen.Entry e : swatches) {
            int r = idx / cols;
            int c = idx % cols;
            int x = startX + c * (CELL_W + GAP);
            int y = viewTop + r * rowH - (int) scrollOffset;
            if (y + CELL_H >= viewTop && y <= viewBottom) {
                drawSwatch(g, e, x, y, CELL_W, CELL_H);
            }
            idx++;
        }
        g.disableScissor();

        // 滚动条
        if (paletteMaxScroll > 0) {
            int sbX = this.width - 6;
            g.fill(sbX, viewTop, sbX + 4, viewBottom, 0x66FFFFFF);
            int thumbH = Math.max(20, (int) ((double) viewH / contentH * viewH));
            int thumbY = viewTop + (int) ((scrollOffset / paletteMaxScroll) * (viewH - thumbH));
            g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xCCFFFFFF);
        }

        // tooltip
        for (int i = 0; i < swatches.size(); i++) {
            int r = i / cols;
            int c = i % cols;
            int x = startX + c * (CELL_W + GAP);
            int y = viewTop + r * rowH - (int) scrollOffset;
            if (mouseX >= x && mouseX < x + CELL_W && mouseY >= y && mouseY < y + CELL_H
                    && y + CELL_H >= viewTop && y <= viewBottom) {
                ItemStack stack = MardPixelForge.buildStack(swatches.get(i).code());
                if (!stack.isEmpty()) g.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
                break;
            }
        }

        // 屏幕内即时状态提示（与 1.21.4 定版 / Fabric 端一致，左下黄字）
        if (!statusMsg.isEmpty()) {
            g.text(this.font, statusMsg, 10, this.height - 14, 0xFFFFFFAA);
        }
    }

    private void renderInputPage(GuiGraphicsExtractor g) {
        if (!isCreative()) {
            // 生存模式：大的禁用提示（与 1.21.4 定版一致）
            String title = "输入色号功能已禁用";
            drawCenteredAdaptive(g, title, this.width / 2, this.height / 2f - 50, 0xFFFF5555, this.width - 20);

            String warn1 = "生存模式下无法通过输入色号直接获取方块";
            String warn2 = "请使用方块染色台，通过七彩粉末合成对应颜色";
            int boxWidth = Math.min(Math.max(this.font.width(warn1), this.font.width(warn2)) + 40, this.width - 20);
            int boxX = (this.width - boxWidth) / 2;
            int boxY = this.height / 2 - 20;
            g.fill(boxX, boxY, boxX + boxWidth, boxY + 70, 0x88FF3333);
            g.fill(boxX + 2, boxY + 2, boxX + boxWidth - 2, boxY + 68, 0xFFFF5555);
            drawCenteredAdaptive(g, warn1, this.width / 2, boxY + 15, 0xFFFFFFFF, boxWidth - 12);
            drawCenteredAdaptive(g, warn2, this.width / 2, boxY + 35, 0xFFFFEEEE, boxWidth - 12);

            if (statusMsg != null && !statusMsg.isEmpty()) {
                drawCenteredAdaptive(g, statusMsg, this.width / 2, this.height / 2f + 70, 0xFFFFFFAA, this.width - 20);
            }
        } else {
            // 创造模式：标题 + 输入框/按钮（buildInputPage 已按定版坐标创建）+ 按钮下方两行说明
            String title = "输入想用的色号（支持批量输入）";
            drawCenteredAdaptive(g, title, this.width / 2, this.height / 2f - 70, 0xFFFFFFFF, this.width - 20);

            String hint = "输入色号（如 A1、B5、M3），支持批量输入多个色号";
            drawCenteredAdaptive(g, hint, this.width / 2, this.height / 2f + 45, 0xFFAAAAAA, this.width - 20);

            String hint2 = "用空格/逗号/分号分隔，例如：A1 B2 C3 或 A1,B2,C3";
            drawCenteredAdaptive(g, hint2, this.width / 2, this.height / 2f + 60, 0xFF888888, this.width - 20);

            if (statusMsg != null && !statusMsg.isEmpty()) {
                drawCenteredAdaptive(g, statusMsg, this.width / 2, this.height / 2f + 85, 0xFFFFFFAA, this.width - 20);
            }
        }
    }

    private void drawSwatch(GuiGraphicsExtractor g, Entry e, int x, int y, int w, int h) {
        // 底色
        g.fill(x, y, x + w, y + h, 0xFF2A2A2A);
        // 色块（居中方形 SW×SW）
        int s = SW;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x + (w - s) / 2f, y + 2);
        pose.scale(s / 16f, s / 16f);
        g.fill(0, 0, 16, 16, 0xFF000000 | (e.rgb() & 0xFFFFFF));
        pose.popMatrix();
        // 色号标签
        String label = e.code();
        g.text(this.font, label, x + (w - this.font.width(label)) / 2, y + h - 10, 0xFFFFFFFF);
    }

    @SuppressWarnings("unused")
    private void fillGradientVertical(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int top, int bottom) {
        int a1 = (top >>> 24) & 0xFF, r1 = (top >>> 16) & 0xFF, gg1 = (top >>> 8) & 0xFF, b1 = top & 0xFF;
        int a2 = (bottom >>> 24) & 0xFF, r2 = (bottom >>> 16) & 0xFF, gg2 = (bottom >>> 8) & 0xFF, b2 = bottom & 0xFF;
        int hgt = Math.max(1, y2 - y1);
        for (int yy = 0; yy < hgt; yy++) {
            float f = yy / (float) hgt;
            int a = (int) (a1 + (a2 - a1) * f);
            int r = (int) (r1 + (r2 - r1) * f);
            int gg = (int) (gg1 + (gg2 - gg1) * f);
            int b = (int) (b1 + (b2 - b1) * f);
            int col = (a << 24) | (r << 16) | (gg << 8) | b;
            g.fill(x1, y1 + yy, x2, y1 + yy + 1, col);
        }
    }

    // ==================== 输入事件 ====================

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double verticalDelta) {
        if (page == PAGE_PALETTE && paletteMaxScroll > 0) {
            double speed = 32;
            scrollOffset = Math.max(0, Math.min(paletteMaxScroll, scrollOffset - verticalDelta * speed));
            scrollProgress = paletteMaxScroll > 0 ? scrollOffset / paletteMaxScroll : 0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalDelta, verticalDelta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        double mouseX = e.x();
        double mouseY = e.y();
        int button = e.button();
        if (page == PAGE_PALETTE && button == 1) { // 26.3 起输入改 SDL3，左键编号为 1（旧 GLFW 为 0），与原版 isValidClickButton 一致
            int viewTop = CONTENT_Y;
            int viewBottom = this.height - 36;
            int cols = computeCols();
            int rowH = CELL_H + GAP;
            int gridW = cols * CELL_W + (cols - 1) * GAP;
            int startX = this.width / 2 - gridW / 2;
            for (int i = 0; i < swatches.size(); i++) {
                int r = i / cols;
                int c = i % cols;
                int x = startX + c * (CELL_W + GAP);
                int y = viewTop + r * rowH - (int) scrollOffset;
                if (mouseX >= x && mouseX < x + CELL_W && mouseY >= y && mouseY < y + CELL_H
                        && y + CELL_H >= viewTop && y <= viewBottom) {
                    Entry en = swatches.get(i);
                    if (isCreative()) {
                        // 按钮一：点色块 → 服务端 Inventory.add 自动给予一组（提示"已给予一组"）；固定占快捷栏是按钮二专属
                        MardNetwork.CHANNEL.send(new MardNetwork.RequestItemPacket(en.target()),
                                PacketDistributor.SERVER.noArg());
                        statusMsg = "已给予一组 " + en.code();
                    } else {
                        // 生存模式：仅查看，屏幕提示用染色台合成（服务端同样拦截）
                        statusMsg = en.code() + " - 生存模式下请使用方块染色台合成（消耗七彩粉末）";
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        int keyCode = e.key();
        if (page == PAGE_INPUT) {
            if (inputBox != null) {
                inputBox.keyPressed(e);
                if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                    confirmInput();
                    return true;
                }
            }
            if (keyCode == InputConstants.KEY_ESCAPE) {
                page = PAGE_MAIN;
                init();
                return true;
            }
            return true;
        }
        if (keyCode == InputConstants.KEY_G || keyCode == InputConstants.KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(e);
    }

    // ==================== 业务逻辑 ====================

    private int computeCols() {
        int avail = this.width - 24;
        int cols = avail / (CELL_W + GAP);
        return Math.max(8, Math.min(24, cols));
    }

    private boolean isCreative() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;
        return mc.gameMode.getPlayerMode().isCreative();
    }

    private String resolveModeName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return "未知";
        return switch (mc.gameMode.getPlayerMode()) {
            case CREATIVE -> "创造模式";
            case SURVIVAL -> "生存模式";
            case ADVENTURE -> "冒险模式";
            case SPECTATOR -> "旁观模式";
        };
    }

    private void confirmInput() {
        if (!isCreative() || inputBox == null) return;
        String raw = inputBox.getValue().trim().toUpperCase();
        if (raw.isEmpty()) return;
        String[] parts = raw.split("[\\s,，;；、/]+");
        List<String> codes = new ArrayList<>();
        for (String p : parts) {
            String code = p.trim();
            if (!code.isEmpty() && MardPalette.byCode(code) != null) codes.add(code.toLowerCase());
        }
        for (String target : codes) {
            MardNetwork.CHANNEL.send(new MardNetwork.HotbarPacket(target),
                    PacketDistributor.SERVER.noArg());
        }
        page = PAGE_MAIN;
        init();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        scheduleCompute();
    }
}
