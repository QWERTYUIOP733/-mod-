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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MardColorScreen extends Screen {

    private record Entry(String code, int rgb, String target) {}
    private record Rect(int x, int y, int w, int h) {
        boolean hit(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private String currentSystem = "MARD";
    private final Map<String, Button> systemButtons = new LinkedHashMap<>();
    private List<Entry> swatches = new ArrayList<>();
    private final List<Rect> swatchRects = new ArrayList<>();
    private List<CustomColor> customList = new ArrayList<>();

    private boolean picking = false;
    private int curHue = 200, curSat = 80, curVal = 90;
    private int curColor() { return ColorMath.hsvToRgb(curHue, curSat / 100.0, curVal / 100.0); }
    private String curName = "";
    private EditBox nameBox;
    private String statusMsg = "";

    // 拖动状态
    private boolean draggingPicker = false;  // 拖动取色方块
    private boolean draggingHue = false;     // 拖动色相条

    private static final int SW = 18, GAP = 4, COLS = 16;
    private static final int LABEL_H = 8; // 色号标签高度
    private static final Path IMPORT_DIR = FMLPaths.CONFIGDIR.get().resolve("mard_pixel").resolve("import");

    // PS风格取色器布局常量
    private static final int PICKER_X = 140, PICKER_Y = 100, PICKER_SIZE = 170;
    private static final int HUE_X = 325, HUE_Y = 100, HUE_W = 20, HUE_H = 170;

    public MardColorScreen() {
        super(Component.translatable("screen.mard_pixel.title"));
        ImportedPaletteStore.init(FMLPaths.CONFIGDIR.get().resolve("mard_pixel"));
        rebuildSystems();
        rebuildSwatches();
    }

    private void rebuildSystems() {
        systemButtons.clear();
        List<String> list = new ArrayList<>();
        list.add("MARD");
        for (String n : ImportedPaletteStore.names()) {
            if (!list.contains(n)) list.add(n);
        }
        list.add("CUSTOM");
        for (String n : list) systemButtons.put(n, null);
        if (!list.contains(currentSystem)) currentSystem = "MARD";
    }

    private void rebuildSwatches() {
        swatches.clear();
        String sys = currentSystem;
        if (sys.equals("MARD")) {
            for (MardColor mc : MardPalette.COLORS) swatches.add(new Entry(mc.code(), mc.rgb(), "MARD:" + mc.code()));
        } else if (sys.equals("CUSTOM")) {
            customList = new ArrayList<>(CustomColorStore.all());
            for (CustomColor cc : customList) swatches.add(new Entry(cc.code(), cc.rgb(), "CUSTOM:" + cc.code()));
        } else {
            ImportedPaletteStore.Palette p = ImportedPaletteStore.get(sys);
            if (p != null) {
                int idx = 1;
                for (int rgb : p.colors) {
                    String code = sys.substring(0, Math.min(3, sys.length())).toUpperCase() + idx;
                    swatches.add(new Entry(code, rgb, "IMPORTED:" + sys + ":" + idx));
                    idx++;
                }
            }
        }
        swatchRects.clear();
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int bw = 66, bh = 18, startX = 10, startY = 30;
        int x = startX, y = startY;
        int maxPerRow = Math.max(4, Math.min(8, (this.width - 20) / bw));
        int col = 0;
        List<String> names = new ArrayList<>(systemButtons.keySet());
        for (String name : names) {
            if (col >= maxPerRow) { col = 0; y += bh + 2; }
            String label = name.equals("MARD") ? "MARD" : name.equals("CUSTOM") ? "CUSTOM" : name.substring(0, Math.min(5, name.length()));
            Button b = Button.builder(Component.literal(label), btn -> {
                currentSystem = name;
                picking = false;
                rebuildSwatches();
                init();
            }).bounds(x + col * bw, y, bw, bh).build();
            addRenderableWidget(b);
            systemButtons.put(name, b);
            col++;
        }
        int sysBottom = y + bh + 4;

        addRenderableWidget(Button.builder(Component.literal("导入PNG"), btn -> {
            importPngPalettes();
        }).bounds(width - 100, 6, 90, 18).build());

        if (!currentSystem.equals("MARD") && !currentSystem.equals("CUSTOM")
                && ImportedPaletteStore.get(currentSystem) != null) {
            addRenderableWidget(Button.builder(Component.literal("删除该色系"), btn -> {
                ImportedPaletteStore.remove(currentSystem);
                statusMsg = "已删除色系: " + currentSystem;
                currentSystem = "MARD";
                rebuildSystems();
                rebuildSwatches();
                init();
            }).bounds(width - 220, height - 28, 90, 20).build());
        }

        if (currentSystem.equals("CUSTOM")) {
            addRenderableWidget(Button.builder(Component.translatable("screen.mard_pixel.pick"), btn -> {
                picking = !picking;
                statusMsg = picking ? "吸取模式：点击任意色块取色" : "";
            }).bounds(10, height - 28, 90, 20).build());

            nameBox = new EditBox(font, 110, sysBottom + 2, 140, 16, Component.literal(""));
            nameBox.setMaxLength(32);
            nameBox.setValue(curName);
            addRenderableWidget(nameBox);

            addRenderableWidget(Button.builder(Component.translatable("screen.mard_pixel.add"), btn -> {
                curName = nameBox.getValue();
                if (curName.isBlank()) curName = "Custom " + ColorMath.toHex(curColor());
                MardNetwork.CHANNEL.sendToServer(new MardNetwork.AddCustomPacket(curName, curColor()));
                statusMsg = "已请求新增自定义色";
            }).bounds(110, sysBottom + 20, 70, 18).build());

            addRenderableWidget(Button.builder(Component.translatable("screen.mard_pixel.remove"), btn -> {
                if (!customList.isEmpty()) {
                    CustomColor cc = customList.get(customList.size() - 1);
                    MardNetwork.CHANNEL.sendToServer(new MardNetwork.RemoveCustomPacket(cc.code()));
                    statusMsg = "已请求删除 " + cc.code();
                }
            }).bounds(190, sysBottom + 20, 70, 18).build());
        }
    }

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
            statusMsg = "目录中没有 PNG 文件，请把色卡放到: " + IMPORT_DIR;
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
        rebuildSystems();
        rebuildSwatches();
        init();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g);
        g.drawString(font, Component.translatable("screen.mard_pixel.title"), 10, 12, 0xFFFFFF);
        g.drawString(font, Component.literal("当前色系: " + currentSystem), 10, height - 48, 0xAAAAAA);
        if (!statusMsg.isEmpty()) g.drawString(font, Component.literal(statusMsg), 10, height - 36, 0xFFFFAA);

        if (currentSystem.equals("CUSTOM")) {
            renderCustomEditor(g, mx, my);
        } else {
            renderSwatches(g, mx, my);
        }
        super.render(g, mx, my, partialTick);
    }

    private void renderSwatches(GuiGraphics g, int mx, int my) {
        swatchRects.clear();
        int y = 60;
        int cellH = SW + LABEL_H + GAP;
        for (int i = 0; i < swatches.size(); i++) {
            int col = i % COLS, row = i / COLS;
            int x = 14 + col * (SW + GAP);
            int yy = y + row * cellH;
            if (yy + cellH > height - 60) break;
            Entry e = swatches.get(i);
            drawSwatchWithLabel(g, x, yy, e.rgb(), e.code());
            swatchRects.add(new Rect(x, yy, SW, SW + LABEL_H));
        }
    }

    private void drawSwatchWithLabel(GuiGraphics g, int x, int y, int rgb, String label) {
        // 色块边框
        g.fill(x - 1, y - 1, x + SW + 1, y + SW + 1, 0xFF333333);
        // 色块本体
        g.fill(x, y, x + SW, y + SW, 0xFF000000 | rgb);
        // 色号标签（色块下方）
        int textColor = isLightColor(rgb) ? 0xFF000000 : 0xFFFFFFFF;
        // 在色块右下角显示色号（小字体）
        String shortLabel = label.length() > 4 ? label.substring(0, 4) : label;
        g.drawString(font, Component.literal(shortLabel), x + 1, y + SW + 1, 0xCCCCCC, false);
    }

    private boolean isLightColor(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 > 160;
    }

    private void drawSwatch(GuiGraphics g, int x, int y, int rgb) {
        g.fill(x - 1, y - 1, x + SW + 1, y + SW + 1, 0xFF222222);
        g.fill(x, y, x + SW, y + SW, 0xFF000000 | rgb);
    }

    private void renderCustomEditor(GuiGraphics g, int mx, int my) {
        // === 左侧：当前颜色预览 ===
        int cur = curColor();
        g.fill(10, 100, 80, 200, 0xFF000000 | cur);
        g.drawString(font, Component.literal(String.format("R %d", (cur >> 16) & 0xFF)), 12, 206, 0xFFFFFF);
        g.drawString(font, Component.literal(String.format("G %d", (cur >> 8) & 0xFF)), 12, 218, 0xFFFFFF);
        g.drawString(font, Component.literal(String.format("B %d", cur & 0xFF)), 12, 230, 0xFFFFFF);
        g.drawString(font, Component.literal("HEX " + ColorMath.toHex(cur)), 12, 244, 0xFFFFAA);

        // === 中间：PS风格取色方块（饱和度×明度） ===
        renderPsPicker(g, PICKER_X, PICKER_Y, PICKER_SIZE, mx, my);
        g.drawString(font, Component.literal("取色区"), PICKER_X, PICKER_Y - 12, 0xAAAAAA);

        // === 右侧：垂直色相条 ===
        renderHueSlider(g, HUE_X, HUE_Y, HUE_W, HUE_H, mx, my);
        g.drawString(font, Component.literal("色相"), HUE_X - 2, HUE_Y - 12, 0xAAAAAA);

        // HSV数值显示
        g.drawString(font, Component.literal("H " + curHue + "°"), PICKER_X, PICKER_Y + PICKER_SIZE + 6, 0xCCCCCC);
        g.drawString(font, Component.literal("S " + curSat + "%"), PICKER_X + 50, PICKER_Y + PICKER_SIZE + 6, 0xCCCCCC);
        g.drawString(font, Component.literal("V " + curVal + "%"), PICKER_X + 100, PICKER_Y + PICKER_SIZE + 6, 0xCCCCCC);

        // === 最右侧：已保存的自定义色列表 ===
        int ly = 60;
        for (CustomColor cc : customList) {
            drawSwatch(g, 430, ly, cc.rgb());
            g.drawString(font, Component.literal(cc.displayName()), 452, ly + 5, 0xFFFFFF);
            ly += SW + 4;
            if (ly > height - 90) break;
        }
    }

    /**
     * PS风格取色方块：X轴饱和度（左灰右纯），Y轴明度（上白下黑）。
     * 支持多角度、多轴取色，拖动指示器实时选色。
     */
    private void renderPsPicker(GuiGraphics g, int x, int y, int size, int mx, int my) {
        // 逐像素渲染取色方块
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                double sat = (double) px / (size - 1);  // 0=左(灰), 1=右(纯)
                double val = 1.0 - (double) py / (size - 1); // 1=上(白), 0=下(黑)
                int c = ColorMath.hsvToRgb(curHue, sat, val);
                g.fill(x + px, y + py, x + px + 1, y + py + 1, 0xFF000000 | c);
            }
        }
        // 边框
        g.fill(x - 1, y - 1, x + size + 1, y, 0xFF888888);
        g.fill(x - 1, y + size, x + size + 1, y + size + 1, 0xFF888888);
        g.fill(x - 1, y - 1, x, y + size + 1, 0xFF888888);
        g.fill(x + size, y - 1, x + size + 1, y + size + 1, 0xFF888888);

        // 取色指示器（白色圆圈+黑色描边，PS风格）
        int ix = x + (int) ((curSat / 100.0) * (size - 1));
        int iy = y + (int) ((1 - curVal / 100.0) * (size - 1));
        // 外白圈
        for (int dy = -5; dy <= 5; dy++) {
            for (int dx = -5; dx <= 5; dx++) {
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d <= 5 && d >= 3) {
                    g.fill(ix + dx, iy + dy, ix + dx + 1, iy + dy + 1, 0xFFFFFFFF);
                }
            }
        }
        // 内黑圈
        for (int dy = -3; dy <= 3; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d <= 3) {
                    g.fill(ix + dx, iy + dy, ix + dx + 1, iy + dy + 1, 0xFF000000);
                }
            }
        }
        // 中心点（当前颜色）
        g.fill(ix - 1, iy - 1, ix + 2, iy + 2, 0xFF000000 | curColor());
    }

    /**
     * 垂直色相条：从上到下红→黄→绿→青→蓝→紫→红。
     */
    private void renderHueSlider(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        // 逐行渲染色相条
        for (int py = 0; py < h; py++) {
            double hue = (double) py / (h - 1) * 360.0; // 0=顶(红), 360=底(红)
            int c = ColorMath.hsvToRgb(hue, 1, 1);
            g.fill(x, y + py, x + w, y + py + 1, 0xFF000000 | c);
        }
        // 边框
        g.fill(x - 1, y - 1, x + w + 1, y, 0xFF888888);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFF888888);
        g.fill(x - 1, y - 1, x, y + h + 1, 0xFF888888);
        g.fill(x + w, y - 1, x + w + 1, y + h + 1, 0xFF888888);

        // 色相指示器（白色横线+黑色描边）
        int sy = y + (int) ((curHue / 360.0) * (h - 1));
        g.fill(x - 4, sy - 2, x + w + 4, sy + 3, 0xFFFFFFFF);
        g.fill(x - 3, sy - 1, x + w + 3, sy + 2, 0xFF000000);
        g.fill(x - 2, sy, x + w + 2, sy + 1, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (currentSystem.equals("CUSTOM")) {
                // PS取色方块点击（X轴饱和度，Y轴明度）
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
            }
            if (picking) {
                for (int i = 0; i < swatchRects.size() && i < swatches.size(); i++) {
                    Rect r = swatchRects.get(i);
                    if (r.hit(mx, my)) {
                        int c = swatches.get(i).rgb();
                        double[] hsv = ColorMath.rgbToHsv(c);
                        curHue = (int) hsv[0];
                        curSat = (int) Math.round(hsv[1] * 100);
                        curVal = (int) Math.round(hsv[2] * 100);
                        statusMsg = "已吸取 " + swatches.get(i).code + " (" + ColorMath.toHex(c) + ")";
                        picking = false;
                        return true;
                    }
                }
            }
            for (int i = 0; i < swatchRects.size() && i < swatches.size(); i++) {
                Rect r = swatchRects.get(i);
                if (r.hit(mx, my)) {
                    Entry e = swatches.get(i);
                    MardNetwork.CHANNEL.sendToServer(new MardNetwork.RequestItemPacket(e.target()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (button == 0 && currentSystem.equals("CUSTOM")) {
            if (draggingPicker) {
                // 拖动取色方块：X轴饱和度，Y轴明度（支持多角度、多轴取色）
                curSat = (int) Math.round(Math.max(0, Math.min(1, (mx - PICKER_X) / (double) (PICKER_SIZE - 1))) * 100);
                curVal = (int) Math.round(Math.max(0, Math.min(1, 1 - (my - PICKER_Y) / (double) (PICKER_SIZE - 1))) * 100);
                return true;
            }
            if (draggingHue) {
                // 拖动色相条
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox != null && nameBox.isFocused()) {
            return nameBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (nameBox != null && nameBox.isFocused()) {
            return nameBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
