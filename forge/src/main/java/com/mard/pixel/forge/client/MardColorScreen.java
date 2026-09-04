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
 * 结构：主 UI + 子标签页
 * 1. 色块浏览器 - MARD 295色色块网格，点击给一组方块
 * 2. 自定义色 - PS风格取色器 + 自定义色管理
 * 3. 导入 - PNG色卡导入
 */
public class MardColorScreen extends Screen {

    // ==================== 子标签页枚举 ====================
    private enum Tab {
        SWATCHES("色块"),
        CUSTOM("自定义"),
        IMPORT("导入");

        final String label;
        Tab(String label) { this.label = label; }
    }

    // ==================== 色块数据 ====================
    private record Entry(String code, int rgb, String target) {}
    private record Rect(int x, int y, int w, int h) {
        boolean hit(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    // ==================== 布局常量 ====================
    private static final int SW = 20, GAP = 3, COLS = 18;
    private static final int TAB_BAR_H = 28;
    private static final int CONTENT_Y = 40;
    private static final Path IMPORT_DIR = FMLPaths.CONFIGDIR.get().resolve("mard_pixel").resolve("import");

    // PS风格取色器布局
    private static final int PICKER_X = 120, PICKER_Y = 60, PICKER_SIZE = 150;
    private static final int HUE_X = 290, HUE_Y = 60, HUE_W = 18, HUE_H = 150;

    // ==================== 状态 ====================
    private Tab currentTab = Tab.SWATCHES;
    private final List<Entry> swatches = new ArrayList<>();
    private final List<Rect> swatchRects = new ArrayList<>();
    private int scrollOffset = 0; // 色块浏览器滚动偏移

    // 自定义色状态
    private List<CustomColor> customList = new ArrayList<>();
    private boolean picking = false;
    private int curHue = 200, curSat = 80, curVal = 90;
    private int curColor() { return ColorMath.hsvToRgb(curHue, curSat / 100.0, curVal / 100.0); }
    private String curName = "";
    private EditBox nameBox;
    private String statusMsg = "";

    // 拖动状态
    private boolean draggingPicker = false;
    private boolean draggingHue = false;

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

        // 子标签页按钮
        int tabX = 10, tabY = 8, tabW = 70, tabH = 20;
        for (Tab tab : Tab.values()) {
            boolean selected = (tab == currentTab);
            Button b = Button.builder(Component.literal(tab.label), btn -> {
                currentTab = tab;
                picking = false;
                if (tab == Tab.CUSTOM) {
                    customList = new ArrayList<>(CustomColorStore.all());
                }
                init();
            }).bounds(tabX, tabY, tabW, tabH).build();
            addRenderableWidget(b);
            tabX += tabW + 4;
        }

        // 导入标签页：导入按钮
        if (currentTab == Tab.IMPORT) {
            addRenderableWidget(Button.builder(Component.literal("导入PNG色卡"), btn -> {
                importPngPalettes();
            }).bounds(width / 2 - 80, 80, 160, 24).build());
        }

        // 自定义标签页：取色器相关控件
        if (currentTab == Tab.CUSTOM) {
            addRenderableWidget(Button.builder(Component.literal(picking ? "取消吸取" : "吸取模式"), btn -> {
                picking = !picking;
                statusMsg = picking ? "吸取模式：点击任意色块取色" : "";
                init();
            }).bounds(10, height - 32, 90, 20).build());

            nameBox = new EditBox(font, 10, 230, 100, 16, Component.literal(""));
            nameBox.setMaxLength(32);
            nameBox.setValue(curName);
            addRenderableWidget(nameBox);

            addRenderableWidget(Button.builder(Component.literal("新增"), btn -> {
                curName = nameBox.getValue();
                if (curName.isBlank()) curName = "Custom " + ColorMath.toHex(curColor());
                MardNetwork.CHANNEL.sendToServer(new MardNetwork.AddCustomPacket(curName, curColor()));
                statusMsg = "已请求新增自定义色";
            }).bounds(10, 250, 50, 18).build());

            addRenderableWidget(Button.builder(Component.literal("删除末位"), btn -> {
                if (!customList.isEmpty()) {
                    CustomColor cc = customList.get(customList.size() - 1);
                    MardNetwork.CHANNEL.sendToServer(new MardNetwork.RemoveCustomPacket(cc.code()));
                    statusMsg = "已请求删除 " + cc.code();
                }
            }).bounds(65, 250, 60, 18).build());
        }
    }

    // ==================== 渲染 ====================

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g);

        // 标题
        g.drawString(font, Component.literal("MARD 色板"), width - 80, 12, 0xFFFFFF);

        // 状态信息
        if (!statusMsg.isEmpty()) {
            g.drawString(font, Component.literal(statusMsg), 10, height - 14, 0xFFFFAA);
        }

        // 渲染当前子标签页内容
        switch (currentTab) {
            case SWATCHES -> renderSwatchesTab(g, mx, my);
            case CUSTOM -> renderCustomTab(g, mx, my);
            case IMPORT -> renderImportTab(g);
        }

        super.render(g, mx, my, partialTick);
    }

    /**
     * 色块浏览器标签页：显示全部295色，支持滚动，点击给一组方块。
     */
    private void renderSwatchesTab(GuiGraphics g, int mx, int my) {
        swatchRects.clear();
        int contentH = height - CONTENT_Y - 20;
        int cellH = SW + GAP;
        int visibleRows = contentH / cellH;
        int totalRows = (int) Math.ceil((double) swatches.size() / COLS);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        int startIdx = scrollOffset * COLS;
        int y = CONTENT_Y;

        for (int i = startIdx; i < swatches.size(); i++) {
            int col = (i - startIdx) % COLS;
            int row = (i - startIdx) / COLS;
            if (row >= visibleRows) break;

            int x = 10 + col * (SW + GAP);
            int yy = y + row * cellH;
            Entry e = swatches.get(i);
            drawSwatch(g, x, yy, e.rgb(), e.code());
            swatchRects.add(new Rect(x, yy, SW, SW));
        }

        // 滚动条
        if (totalRows > visibleRows) {
            int barX = width - 8;
            int barY = CONTENT_Y;
            int barH = contentH;
            int thumbH = Math.max(20, barH * visibleRows / totalRows);
            int thumbY = barY + (barH - thumbH) * scrollOffset / maxScroll;
            g.fill(barX, barY, barX + 4, barY + barH, 0xFF333333);
            g.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xFF888888);
        }

        // 提示
        g.drawString(font, Component.literal("点击色块获取一组（64个） | 共" + swatches.size() + "色"),
                10, height - 14, 0xAAAAAA);
    }

    /**
     * 自定义色标签页：PS风格取色器 + 自定义色列表。
     */
    private void renderCustomTab(GuiGraphics g, int mx, int my) {
        // 左侧：当前颜色预览
        int cur = curColor();
        g.fill(10, 60, 100, 200, 0xFF000000 | cur);
        g.drawString(font, Component.literal(String.format("R %d", (cur >> 16) & 0xFF)), 12, 206, 0xFFFFFF);
        g.drawString(font, Component.literal(String.format("G %d", (cur >> 8) & 0xFF)), 12, 218, 0xFFFFFF);
        g.drawString(font, Component.literal(String.format("B %d", cur & 0xFF)), 12, 230, 0xFFFFFF);
        g.drawString(font, Component.literal("HEX " + ColorMath.toHex(cur)), 12, 244, 0xFFFFAA);

        // 中间：PS风格取色方块
        renderPsPicker(g, PICKER_X, PICKER_Y, PICKER_SIZE, mx, my);
        g.drawString(font, Component.literal("取色区"), PICKER_X, PICKER_Y - 12, 0xAAAAAA);

        // 右侧：垂直色相条
        renderHueSlider(g, HUE_X, HUE_Y, HUE_W, HUE_H, mx, my);
        g.drawString(font, Component.literal("色相"), HUE_X - 2, HUE_Y - 12, 0xAAAAAA);

        // HSV数值
        g.drawString(font, Component.literal("H " + curHue + "°"), PICKER_X, PICKER_Y + PICKER_SIZE + 6, 0xCCCCCC);
        g.drawString(font, Component.literal("S " + curSat + "%"), PICKER_X + 50, PICKER_Y + PICKER_SIZE + 6, 0xCCCCCC);
        g.drawString(font, Component.literal("V " + curVal + "%"), PICKER_X + 100, PICKER_Y + PICKER_SIZE + 6, 0xCCCCCC);

        // 最右侧：已保存的自定义色列表
        int ly = CONTENT_Y;
        swatchRects.clear();
        for (CustomColor cc : customList) {
            drawSwatch(g, 340, ly, cc.rgb(), cc.displayName());
            swatchRects.add(new Rect(340, ly, SW, SW));
            ly += SW + GAP;
            if (ly > height - 60) break;
        }
    }

    /**
     * 导入标签页：显示已导入的色系列表。
     */
    private void renderImportTab(GuiGraphics g) {
        g.drawString(font, Component.literal("将PNG色卡放入以下目录后点击导入："),
                width / 2 - 150, 120, 0xAAAAAA);
        g.drawString(font, Component.literal(IMPORT_DIR.toString()),
                width / 2 - 150, 136, 0xFFFFAA);

        // 已导入的色系列表
        List<String> names = ImportedPaletteStore.names();
        if (names.isEmpty()) {
            g.drawString(font, Component.literal("暂无导入的色系"),
                    width / 2 - 60, 180, 0x666666);
        } else {
            g.drawString(font, Component.literal("已导入的色系（" + names.size() + "个）："),
                    width / 2 - 150, 170, 0xFFFFFF);
            int y = 190;
            for (String name : names) {
                ImportedPaletteStore.Palette p = ImportedPaletteStore.get(name);
                int count = p != null ? p.colors.size() : 0;
                g.drawString(font, Component.literal("• " + name + " (" + count + "色)"),
                        width / 2 - 140, y, 0xCCCCCC);
                y += 14;
                if (y > height - 60) break;
            }
        }
    }

    // ==================== 绘制辅助 ====================

    private void drawSwatch(GuiGraphics g, int x, int y, int rgb, String label) {
        // 边框
        g.fill(x - 1, y - 1, x + SW + 1, y + SW + 1, 0xFF333333);
        // 色块本体
        g.fill(x, y, x + SW, y + SW, 0xFF000000 | rgb);
        // 色号标签（色块右下角，小字体）
        String shortLabel = label.length() > 4 ? label.substring(0, 4) : label;
        g.drawString(font, Component.literal(shortLabel), x + 1, y + SW + 1, 0x999999, false);
    }

    /**
     * PS风格取色方块：X轴饱和度，Y轴明度。
     */
    private void renderPsPicker(GuiGraphics g, int x, int y, int size, int mx, int my) {
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                double sat = (double) px / (size - 1);
                double val = 1.0 - (double) py / (size - 1);
                int c = ColorMath.hsvToRgb(curHue, sat, val);
                g.fill(x + px, y + py, x + px + 1, y + py + 1, 0xFF000000 | c);
            }
        }
        // 边框
        g.fill(x - 1, y - 1, x + size + 1, y, 0xFF888888);
        g.fill(x - 1, y + size, x + size + 1, y + size + 1, 0xFF888888);
        g.fill(x - 1, y - 1, x, y + size + 1, 0xFF888888);
        g.fill(x + size, y - 1, x + size + 1, y + size + 1, 0xFF888888);

        // 取色指示器
        int ix = x + (int) ((curSat / 100.0) * (size - 1));
        int iy = y + (int) ((1 - curVal / 100.0) * (size - 1));
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d <= 4 && d >= 2) g.fill(ix + dx, iy + dy, ix + dx + 1, iy + dy + 1, 0xFFFFFFFF);
                if (d <= 2) g.fill(ix + dx, iy + dy, ix + dx + 1, iy + dy + 1, 0xFF000000);
            }
        }
    }

    /**
     * 垂直色相条。
     */
    private void renderHueSlider(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        for (int py = 0; py < h; py++) {
            double hue = (double) py / (h - 1) * 360.0;
            int c = ColorMath.hsvToRgb(hue, 1, 1);
            g.fill(x, y + py, x + w, y + py + 1, 0xFF000000 | c);
        }
        g.fill(x - 1, y - 1, x + w + 1, y, 0xFF888888);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFF888888);
        g.fill(x - 1, y - 1, x, y + h + 1, 0xFF888888);
        g.fill(x + w, y - 1, x + w + 1, y + h + 1, 0xFF888888);

        // 色相指示器
        int sy = y + (int) ((curHue / 360.0) * (h - 1));
        g.fill(x - 3, sy - 2, x + w + 3, sy + 3, 0xFFFFFFFF);
        g.fill(x - 2, sy - 1, x + w + 2, sy + 2, 0xFF000000);
    }

    // ==================== 鼠标交互 ====================

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            // 色块浏览器：点击色块给一组方块
            if (currentTab == Tab.SWATCHES) {
                for (int i = 0; i < swatchRects.size(); i++) {
                    Rect r = swatchRects.get(i);
                    if (r.hit(mx, my)) {
                        int idx = scrollOffset * COLS + i;
                        if (idx < swatches.size()) {
                            Entry e = swatches.get(idx);
                            MardNetwork.CHANNEL.sendToServer(new MardNetwork.RequestItemPacket(e.target()));
                            statusMsg = "已给予一组 " + e.code();
                        }
                        return true;
                    }
                }
            }

            // 自定义色标签页
            if (currentTab == Tab.CUSTOM) {
                // PS取色方块点击
                if (mx >= PICKER_X && mx < PICKER_X + PICKER_SIZE
                        && my >= PICKER_Y && my < PICKER_Y + PICKER_SIZE) {
                    curSat = (int) Math.round(Math.max(0, Math.min(1, (mx - PICKER_X) / (double) (PICKER_SIZE - 1))) * 100);
                    curVal = (int) Math.round(Math.max(0, Math.min(1, 1 - (my - PICKER_Y) / (double) (PICKER_SIZE - 1))) * 100);
                    draggingPicker = true;
                    return true;
                }
                // 色相条点击
                if (mx >= HUE_X - 2 && mx < HUE_X + HUE_W + 2
                        && my >= HUE_Y && my < HUE_Y + HUE_H) {
                    curHue = (int) Math.round(Math.max(0, Math.min(360, (my - HUE_Y) / (double) (HUE_H - 1) * 360)));
                    if (curHue >= 360) curHue = 0;
                    draggingHue = true;
                    return true;
                }
                // 吸取模式：点击自定义色列表取色
                if (picking) {
                    for (int i = 0; i < swatchRects.size() && i < customList.size(); i++) {
                        Rect r = swatchRects.get(i);
                        if (r.hit(mx, my)) {
                            CustomColor cc = customList.get(i);
                            double[] hsv = ColorMath.rgbToHsv(cc.rgb());
                            curHue = (int) hsv[0];
                            curSat = (int) Math.round(hsv[1] * 100);
                            curVal = (int) Math.round(hsv[2] * 100);
                            statusMsg = "已吸取 " + cc.displayName();
                            picking = false;
                            init();
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (button == 0 && currentTab == Tab.CUSTOM) {
            if (draggingPicker) {
                curSat = (int) Math.round(Math.max(0, Math.min(1, (mx - PICKER_X) / (double) (PICKER_SIZE - 1))) * 100);
                curVal = (int) Math.round(Math.max(0, Math.min(1, 1 - (my - PICKER_Y) / (double) (PICKER_SIZE - 1))) * 100);
                return true;
            }
            if (draggingHue) {
                curHue = (int) Math.round(Math.max(0, Math.min(360, (my - HUE_Y) / (double) (HUE_H - 1) * 360)));
                if (curHue >= 360) curHue = 0;
                return true;
            }
        }
        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingPicker = false;
        draggingHue = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        // 色块浏览器：鼠标滚轮滚动
        if (currentTab == Tab.SWATCHES) {
            scrollOffset -= (int) Math.signum(delta);
            if (scrollOffset < 0) scrollOffset = 0;
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox != null && nameBox.isFocused()) {
            return nameBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ==================== PNG导入 ====================

    private void importPngPalettes() {
        try {
            java.nio.file.Files.createDirectories(IMPORT_DIR);
        } catch (Exception ignored) {}

        File dir = IMPORT_DIR.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            statusMsg = "导入目录不存在，已创建: " + IMPORT_DIR;
            return;
        }

        File[] pngFiles = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
        if (pngFiles == null || pngFiles.length == 0) {
            statusMsg = "目录中没有PNG文件: " + IMPORT_DIR;
            return;
        }

        int imported = 0, skipped = 0;
        for (File png : pngFiles) {
            try {
                String paletteName = PngImporter.paletteNameFromFile(png);
                if (ImportedPaletteStore.get(paletteName) != null) {
                    skipped++;
                    continue;
                }
                PngImporter.Result result = PngImporter.importFromPng(
                        png, paletteName, 500, true, true, true);
                if (result.colors.isEmpty()) {
                    skipped++;
                    continue;
                }
                ImportedPaletteStore.add(paletteName, result.colors, png.getName());
                imported++;
            } catch (Exception e) {
                skipped++;
            }
        }

        statusMsg = String.format("导入完成: 新增 %d 个色系, 跳过 %d 个", imported, skipped);
    }
}
