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
    private boolean draggingWheel = false;
    private boolean draggingSat = false;
    private boolean draggingVal = false;

    private static final int SW = 18, GAP = 4, COLS = 16;
    private static final int LABEL_H = 8; // 色号标签高度
    private static final Path IMPORT_DIR = FMLPaths.CONFIGDIR.get().resolve("mard_pixel").resolve("import");

    // 自定义编辑器布局常量
    private static final int WHEEL_CX = 340, WHEEL_CY = 170;
    private static final int WHEEL_R1 = 18, WHEEL_R2 = 62;
    private static final int SLIDER_X = 200, SLIDER_W = 16, SLIDER_H = 120;
    private static final int SAT_SLIDER_Y = 110;
    private static final int VAL_SLIDER_Y = 110;
    private static final int VAL_SLIDER_X = 230;

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

        addRenderableWidget(Button.builder(Component.translatable("screen.mard_pixel.switchbag"), btn -> {
            MardNetwork.CHANNEL.sendToServer(new MardNetwork.SwitchBagSystemPacket(currentSystem));
            statusMsg = "已请求切换到 " + currentSystem + " 色系";
        }).bounds(width - 120, height - 28, 110, 20).build());

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

        // === 中间：饱和度 + 明度 垂直调节条 ===
        renderSatSlider(g, SLIDER_X, SAT_SLIDER_Y, SLIDER_W, SLIDER_H, mx, my);
        renderValSlider(g, VAL_SLIDER_X, VAL_SLIDER_Y, SLIDER_W, SLIDER_H, mx, my);

        // 标签
        g.drawString(font, Component.literal("饱和度"), SLIDER_X - 2, SAT_SLIDER_Y - 12, 0xAAAAAA);
        g.drawString(font, Component.literal("明度"), VAL_SLIDER_X - 2, VAL_SLIDER_Y - 12, 0xAAAAAA);
        g.drawString(font, Component.literal(curSat + "%"), SLIDER_X, SAT_SLIDER_Y + SLIDER_H + 4, 0xCCCCCC);
        g.drawString(font, Component.literal(curVal + "%"), VAL_SLIDER_X, VAL_SLIDER_Y + SLIDER_H + 4, 0xCCCCCC);

        // === 右侧：圆润色环 ===
        renderSmoothWheel(g, WHEEL_CX, WHEEL_CY, WHEEL_R1, WHEEL_R2, mx, my);
        g.drawString(font, Component.literal("色相环"), WHEEL_CX - 16, WHEEL_CY - WHEEL_R2 - 14, 0xAAAAAA);

        // === 最右侧：已保存的自定义色列表 ===
        int ly = 60;
        for (CustomColor cc : customList) {
            drawSwatch(g, 430, ly, cc.rgb());
            g.drawString(font, Component.literal(cc.code()), 452, ly + 5, 0xFFFFFF);
            ly += SW + 4;
            if (ly > height - 90) break;
        }
    }

    /**
     * 渲染饱和度垂直调节条：顶部灰色（0%），底部当前色相纯色（100%）
     */
    private void renderSatSlider(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        int pure = ColorMath.hsvToRgb(curHue, 1, curVal / 100.0);
        int pr = (pure >> 16) & 0xFF, pg = (pure >> 8) & 0xFF, pb = pure & 0xFF;
        // 逐行渐变：从灰到纯色
        for (int py = 0; py < h; py++) {
            double t = (double) py / (h - 1); // 0=顶(灰), 1=底(纯色)
            int gray = (int) (curVal / 100.0 * 255);
            int r = (int) (gray + t * (pr - gray));
            int gg = (int) (gray + t * (pg - gray));
            int b = (int) (gray + t * (pb - gray));
            g.fill(x, y + py, x + w, y + py + 1, 0xFF000000 | ((r & 0xFF) << 16) | ((gg & 0xFF) << 8) | (b & 0xFF));
        }
        // 边框
        g.fill(x - 1, y - 1, x + w + 1, y, 0xFF888888);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFF888888);
        g.fill(x - 1, y - 1, x, y + h + 1, 0xFF888888);
        g.fill(x + w, y - 1, x + w + 1, y + h + 1, 0xFF888888);
        // 滑块指示器
        int sy = y + (int) ((curSat / 100.0) * (h - 1));
        g.fill(x - 3, sy - 2, x + w + 3, sy + 3, 0xFFFFFFFF);
        g.fill(x - 2, sy - 1, x + w + 2, sy + 2, 0xFF000000);
    }

    /**
     * 渲染明度垂直调节条：顶部黑色（0%），底部白色（100%），中间当前色相
     */
    private void renderValSlider(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        int pure = ColorMath.hsvToRgb(curHue, curSat / 100.0, 1);
        int pr = (pure >> 16) & 0xFF, pg = (pure >> 8) & 0xFF, pb = pure & 0xFF;
        // 逐行渐变：从黑到纯色到白
        for (int py = 0; py < h; py++) {
            double t = (double) py / (h - 1); // 0=顶(黑), 0.5=纯色, 1=底(白)
            int r, gg, b;
            if (t < 0.5) {
                double k = t * 2;
                r = (int) (k * pr);
                gg = (int) (k * pg);
                b = (int) (k * pb);
            } else {
                double k = (t - 0.5) * 2;
                r = (int) (pr + k * (255 - pr));
                gg = (int) (pg + k * (255 - pg));
                b = (int) (pb + k * (255 - pb));
            }
            g.fill(x, y + py, x + w, y + py + 1, 0xFF000000 | ((r & 0xFF) << 16) | ((gg & 0xFF) << 8) | (b & 0xFF));
        }
        // 边框
        g.fill(x - 1, y - 1, x + w + 1, y, 0xFF888888);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFF888888);
        g.fill(x - 1, y - 1, x, y + h + 1, 0xFF888888);
        g.fill(x + w, y - 1, x + w + 1, y + h + 1, 0xFF888888);
        // 滑块指示器
        int sy = y + (int) ((curVal / 100.0) * (h - 1));
        g.fill(x - 3, sy - 2, x + w + 3, sy + 3, 0xFFFFFFFF);
        g.fill(x - 2, sy - 1, x + w + 2, sy + 2, 0xFF000000);
    }

    /**
     * 渲染光滑圆润的色相环，支持抗锯齿边缘
     */
    private void renderSmoothWheel(GuiGraphics g, int cx, int cy, int r1, int r2, int mx, int my) {
        // 外发光背景
        for (int py = -r2 - 2; py <= r2 + 2; py++) {
            for (int px = -r2 - 2; px <= r2 + 2; px++) {
                double d = Math.sqrt(px * px + py * py);
                if (d > r2 && d <= r2 + 2) {
                    double alpha = 1 - (d - r2) / 2.0;
                    int a = (int) (alpha * 60);
                    g.fill(cx + px, cy + py, cx + px + 1, cy + py + 1, (a & 0xFF) << 24 | 0x888888);
                }
            }
        }
        // 色环主体（逐像素，更细腻）
        for (int py = -r2; py <= r2; py++) {
            for (int px = -r2; px <= r2; px++) {
                double d = Math.sqrt(px * px + py * py);
                if (d >= r1 && d <= r2) {
                    // 边缘抗锯齿
                    double edgeAlpha = 1.0;
                    if (d < r1 + 1) edgeAlpha = d - r1;
                    if (d > r2 - 1) edgeAlpha = r2 - d;
                    if (edgeAlpha > 0) {
                        double hue = (Math.toDegrees(Math.atan2(py, px)) + 360) % 360;
                        int c = ColorMath.hsvToRgb(hue, 1, 1);
                        int a = (int) (Math.min(1, edgeAlpha) * 255);
                        g.fill(cx + px, cy + py, cx + px + 1, cy + py + 1, (a << 24) | (c & 0xFFFFFF));
                    }
                }
            }
        }
        // 内圆背景（深色）
        for (int py = -r1 + 1; py < r1; py++) {
            for (int px = -r1 + 1; px < r1; px++) {
                double d = Math.sqrt(px * px + py * py);
                if (d < r1) {
                    g.fill(cx + px, cy + py, cx + px + 1, cy + py + 1, 0xFF1A1A2E);
                }
            }
        }
        // 内圆显示当前颜色
        int cur = curColor();
        for (int py = -r1 + 3; py < r1 - 3; py++) {
            for (int px = -r1 + 3; px < r1 - 3; px++) {
                double d = Math.sqrt(px * px + py * py);
                if (d < r1 - 4) {
                    g.fill(cx + px, cy + py, cx + px + 1, cy + py + 1, 0xFF000000 | cur);
                }
            }
        }
        // 色相指示器（白色圆环 + 黑色描边）
        double ang = Math.toRadians(curHue);
        int indicatorR = (r1 + r2) / 2;
        int ix = (int) (cx + Math.cos(ang) * indicatorR);
        int iy = (int) (cy + Math.sin(ang) * indicatorR);
        // 外白圈
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                double dd = Math.sqrt(dx * dx + dy * dy);
                if (dd <= 4 && dd >= 2.5) {
                    g.fill(ix + dx, iy + dy, ix + dx + 1, iy + dy + 1, 0xFFFFFFFF);
                }
            }
        }
        // 内黑圈
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                double dd = Math.sqrt(dx * dx + dy * dy);
                if (dd <= 2) {
                    g.fill(ix + dx, iy + dy, ix + dx + 1, iy + dy + 1, 0xFF000000);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (currentSystem.equals("CUSTOM")) {
                // 色环点击
                double dx = mx - WHEEL_CX, dy = my - WHEEL_CY;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d >= WHEEL_R1 - 2 && d <= WHEEL_R2 + 2) {
                    curHue = (int) ((Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360);
                    draggingWheel = true;
                    return true;
                }
                // 饱和度条点击
                if (mx >= SLIDER_X - 3 && mx <= SLIDER_X + SLIDER_W + 3
                        && my >= SAT_SLIDER_Y && my <= SAT_SLIDER_Y + SLIDER_H) {
                    curSat = (int) Math.round(Math.max(0, Math.min(1, (my - SAT_SLIDER_Y) / (double) SLIDER_H)) * 100);
                    draggingSat = true;
                    return true;
                }
                // 明度条点击
                if (mx >= VAL_SLIDER_X - 3 && mx <= VAL_SLIDER_X + SLIDER_W + 3
                        && my >= VAL_SLIDER_Y && my <= VAL_SLIDER_Y + SLIDER_H) {
                    curVal = (int) Math.round(Math.max(0, Math.min(1, (my - VAL_SLIDER_Y) / (double) SLIDER_H)) * 100);
                    draggingVal = true;
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
            if (draggingWheel) {
                double dx = mx - WHEEL_CX, dy = my - WHEEL_CY;
                curHue = (int) ((Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360);
                return true;
            }
            if (draggingSat) {
                curSat = (int) Math.round(Math.max(0, Math.min(1, (my - SAT_SLIDER_Y) / (double) SLIDER_H)) * 100);
                return true;
            }
            if (draggingVal) {
                curVal = (int) Math.round(Math.max(0, Math.min(1, (my - VAL_SLIDER_Y) / (double) SLIDER_H)) * 100);
                return true;
            }
        }
        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingWheel = false;
        draggingSat = false;
        draggingVal = false;
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
