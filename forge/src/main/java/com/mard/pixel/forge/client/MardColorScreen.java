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
    private void initMainPage() {
        // 按钮区域：左侧 30% 宽度，按钮偏左且较窄
        int btnAreaW = (int) (width * 0.30);
        int btnW = Math.min(180, btnAreaW - 20);
        int btnH = 30;
        int btnX = Math.max(25, (btnAreaW - btnW) / 2 + 8);
        // 按钮区域居中偏下，确保不与顶部警告条重叠
        int centerY = Math.max(height / 2 + 20, 160);
        int gapY = 50;

        // 按钮一：颜色选取
        addRenderableWidget(Button.builder(Component.literal("颜色选取"), btn -> {
            currentPage = Page.SWATCHES;
            scrollOffset = 0;
            init();
        }).bounds(btnX, centerY - gapY / 2 - btnH / 2, btnW, btnH).build());

        // 按钮二：输入想用的色号
        addRenderableWidget(Button.builder(Component.literal("输入想用的色号"), btn -> {
            currentPage = Page.INPUT;
            init();
        }).bounds(btnX, centerY + gapY / 2 - btnH / 2, btnW, btnH).build());
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
                MardNetwork.CHANNEL.sendToServer(new MardNetwork.HotbarPacket(trimmed));
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
        String title = "彩色方块扩展";
        g.drawString(font, Component.literal(title), (width - font.width(title)) / 2, 40, 0xFFFFFF);

        // 在标题下方显示当前游戏模式（生存模式用红色警告）
        String modeText = "当前模式：" + getGameModeName();
        int modeColor = isSurvivalMode() ? 0xFF5555 : 0x55FF55;
        g.drawString(font, Component.literal(modeText), (width - font.width(modeText)) / 2, 58, modeColor);

        // 生存模式下显示红色警告条
        if (isSurvivalMode()) {
            String warnText = "⚠ 生存模式：仅可查看颜色，合成请使用方块染色台";
            int warnWidth = Math.min(font.width(warnText) + 24, width - 40);
            int warnX = (width - warnWidth) / 2;
            int warnY = 72;
            int warnH = 22;
            // 警告条背景（半透明外框 + 实心内框）
            g.fill(warnX, warnY, warnX + warnWidth, warnY + warnH, 0x99CC0000);
            g.fill(warnX + 1, warnY + 1, warnX + warnWidth - 1, warnY + warnH - 1, 0xFFFF4444);
            // 文字居中显示
            int textX = warnX + (warnWidth - font.width(warnText)) / 2;
            g.drawString(font, Component.literal(warnText), textX, warnY + 7, 0xFFFFFF);
        }

        // 右侧：mod 使用说明（右侧 40% 宽度）
        int infoAreaX = (int) (width * 0.55);
        int infoAreaW = (int) (width * 0.35);
        int infoY = Math.max(110, height / 2 - 90);
        int infoH = Math.min(200, height - 180);

        // 说明框背景
        g.fill(infoAreaX - 6, infoY - 6, infoAreaX + infoAreaW + 6, infoY + infoH + 6, 0xFF1a1a1a);
        g.fill(infoAreaX - 5, infoY - 5, infoAreaX + infoAreaW + 5, infoY + infoH + 5, 0xFF2a2a2a);

        g.drawString(font, Component.literal("mod 使用说明"), infoAreaX, infoY, 0xFFFFAA);

        String[] lines;
        if (isSurvivalMode()) {
            lines = new String[]{
                "",
                "221 色像素画模组（生存模式）",
                "",
                "按钮一：浏览全部色号",
                "  生存模式：仅查看颜色",
                "  合成请使用方块染色台",
                "",
                "按钮二：输入色号（仅创造模式）",
                "  生存模式下此功能禁用",
                "",
                "合成表：任意染料→七彩粉末→色块",
                "方块染色台：放入粉末后选择颜色",
                "",
                "按 G 键打开/关闭本界面"
            };
        } else {
            lines = new String[]{
                "",
                "221 色像素画模组（创造模式）",
                "",
                "按钮一：浏览全部色号",
                "  点击色块直接获取一组方块",
                "  支持连续选择",
                "",
                "按钮二：输入色号快速获取",
                "  输入色号后放入快捷栏",
                "  支持批量输入（空格/逗号分隔）",
                "",
                "合成表：任意染料→七彩粉末→色块",
                "方块染色台：放入粉末后选择颜色",
                "",
                "按 G 键打开/关闭本界面"
            };
        }

        int y = infoY + 15;
        int lineH = Math.max(10, (infoH - 20) / lines.length);
        for (String line : lines) {
            if (y + 10 < infoY + infoH) {
                g.drawString(font, Component.literal(line), infoAreaX, y, 0xCCCCCC);
            }
            y += lineH;
        }

        // 底部提示
        String bottomText = "彩色方块扩展 v1.2.0";
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
        // 检测当前游戏模式
        boolean isSurvivalRender = isSurvivalMode();

        // 生存模式下显示红色警告条（更醒目）
        if (isSurvivalRender) {
            String warnText = "⚠ 生存模式：仅可查看颜色，点击不会获取方块，请使用方块染色台合成";
            int warnWidth = font.width(warnText) + 20;
            g.fill(5, 5, Math.min(width - 10, warnWidth), 28, 0x88FF3333);
            g.fill(6, 6, Math.min(width - 11, warnWidth - 1), 27, 0xFFFF5555);
            g.drawString(font, Component.literal(warnText), 15, 12, 0xFFFFFF);
        } else {
            // 创造模式：正常标题
            String title = "颜色选取 - 点击色块获取一组（64个）";
            g.drawString(font, Component.literal(title), 80, 12, 0xFFFFFF);
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
            g.drawString(font, Component.literal(statusMsg), 10, height - 14, 0xFFFFAA);
        }
    }

    /**
     * 输入色号页面渲染。
     */
    private void renderInputPage(GuiGraphics g) {
        if (isSurvivalMode()) {
            // 生存模式：显示大的禁用提示
            String title = "输入色号功能已禁用";
            g.drawString(font, Component.literal(title),
                    (width - font.width(title)) / 2, height / 2 - 50, 0xFF5555);

            // 红色警告框
            String warn1 = "生存模式下无法通过输入色号直接获取方块";
            String warn2 = "请使用方块染色台，通过七彩粉末合成对应颜色";
            int boxWidth = Math.max(font.width(warn1), font.width(warn2)) + 40;
            int boxX = (width - boxWidth) / 2;
            int boxY = height / 2 - 20;
            g.fill(boxX, boxY, boxX + boxWidth, boxY + 70, 0x88FF3333);
            g.fill(boxX + 2, boxY + 2, boxX + boxWidth - 2, boxY + 68, 0xFFFF5555);
            g.drawString(font, Component.literal(warn1), (width - font.width(warn1)) / 2, boxY + 15, 0xFFFFFF);
            g.drawString(font, Component.literal(warn2), (width - font.width(warn2)) / 2, boxY + 35, 0xFFFFEE);

            if (!statusMsg.isEmpty()) {
                g.drawString(font, Component.literal(statusMsg),
                        (width - font.width(statusMsg)) / 2, height / 2 + 70, 0xFFFFAA);
            }
        } else {
            // 创造模式：正常显示
            String title = "输入想用的色号（支持批量输入）";
            g.drawString(font, Component.literal(title),
                    (width - font.width(title)) / 2, height / 2 - 70, 0xFFFFFF);

            String hint = "输入色号（如 A1、B5、M3），支持批量输入多个色号";
            g.drawString(font, Component.literal(hint),
                    (width - font.width(hint)) / 2, height / 2 + 45, 0xAAAAAA);

            String hint2 = "用空格/逗号/分号分隔，例如：A1 B2 C3 或 A1,B2,C3";
            g.drawString(font, Component.literal(hint2),
                    (width - font.width(hint2)) / 2, height / 2 + 60, 0x888888);

            if (!statusMsg.isEmpty()) {
                g.drawString(font, Component.literal(statusMsg),
                        (width - font.width(statusMsg)) / 2, height / 2 + 85, 0xFFFFAA);
            }
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
