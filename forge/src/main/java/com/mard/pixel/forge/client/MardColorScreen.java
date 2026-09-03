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

    private static final int SW = 16, GAP = 2, COLS = 20;
    private static final Path IMPORT_DIR = FMLPaths.CONFIGDIR.get().resolve("mard_pixel").resolve("import");

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
        for (int i = 0; i < swatches.size(); i++) {
            int col = i % COLS, row = i / COLS;
            int x = 14 + col * (SW + GAP);
            int yy = y + row * (SW + GAP);
            if (yy + SW > height - 60) break;
            drawSwatch(g, x, yy, swatches.get(i).rgb());
            if (mx >= x && mx < x + SW && my >= yy && my < yy + SW) {
                g.drawString(font, Component.literal(swatches.get(i).code), mx + 8, my - 14, 0xFFFFFF);
            }
            swatchRects.add(new Rect(x, yy, SW, SW));
        }
    }

    private void drawSwatch(GuiGraphics g, int x, int y, int rgb) {
        g.fill(x - 1, y - 1, x + SW + 1, y + SW + 1, 0xFF222222);
        g.fill(x, y, x + SW, y + SW, 0xFF000000 | rgb);
    }

    private void renderCustomEditor(GuiGraphics g, int mx, int my) {
        int tx = 150, ty = 120, size = 110;
        int pure = ColorMath.hsvToRgb(curHue, 1, 1);
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                double[] bc = bary(tx + px, ty + py, tx + size / 2, ty, tx, ty + size, tx + size, ty + size);
                if (bc != null) {
                    int r = (int) (bc[0] * 255 + bc[1] * ((pure >> 16) & 0xFF) + bc[2] * 0);
                    int gg = (int) (bc[0] * 255 + bc[1] * ((pure >> 8) & 0xFF) + bc[2] * 0);
                    int b = (int) (bc[0] * 255 + bc[1] * (pure & 0xFF) + bc[2] * 0);
                    g.fill(tx + px, ty + py, tx + px + 1, ty + py + 1, 0xFF000000 | ((r & 0xFF) << 16) | ((gg & 0xFF) << 8) | (b & 0xFF));
                }
            }
        }
        g.fill(tx - 1, ty - 1, tx + size + 1, ty + 1, 0xFFFFFFFF);
        g.fill(tx - 1, ty + size - 1, tx + size + 1, ty + size + 1, 0xFFFFFFFF);

        int cx = 330, cy = 175, r1 = 12, r2 = 48;
        for (int py = -r2; py <= r2; py++) {
            for (int px = -r2; px <= r2; px++) {
                double d = Math.sqrt(px * px + py * py);
                if (d >= r1 && d <= r2) {
                    double hue = (Math.toDegrees(Math.atan2(py, px)) + 360) % 360;
                    int c = ColorMath.hsvToRgb(hue, 1, 1);
                    g.fill(cx + px, cy + py, cx + px + 1, cy + py + 1, 0xFF000000 | c);
                }
            }
        }
        double ang = Math.toRadians(curHue);
        int mx2 = (int) (cx + Math.cos(ang) * (r1 + (r2 - r1) / 2.0));
        int my2 = (int) (cy + Math.sin(ang) * (r1 + (r2 - r1) / 2.0));
        g.fill(mx2 - 2, my2 - 2, mx2 + 3, my2 + 3, 0xFFFFFFFF);

        int cur = curColor();
        g.fill(10, 120, 70, 180, 0xFF000000 | cur);
        g.drawString(font, Component.literal(String.format("R %d  G %d  B %d", (cur >> 16) & 0xFF, (cur >> 8) & 0xFF, cur & 0xFF)), 10, 190, 0xFFFFFF);
        g.drawString(font, Component.literal("HEX " + ColorMath.toHex(cur)), 10, 204, 0xFFFFFF);

        int ly = 60;
        for (CustomColor cc : customList) {
            drawSwatch(g, 410, ly, cc.rgb());
            g.drawString(font, Component.literal(cc.code()), 428, ly + 4, 0xFFFFFF);
            ly += SW + 2;
            if (ly > height - 90) break;
        }

        g.drawString(font, Component.translatable("screen.mard_pixel.tri"), 150, 240, 0x888888);
        g.drawString(font, Component.translatable("screen.mard_pixel.wheel"), 300, 240, 0x888888);
    }

    private double[] bary(double px, double py, double ax, double ay, double bx, double by, double cx, double cy) {
        double v0x = cx - ax, v0y = cy - ay, v1x = bx - ax, v1y = by - ay, v2x = px - ax, v2y = py - ay;
        double d00 = v0x * v0x + v0y * v0y, d01 = v0x * v1x + v0y * v1y, d11 = v1x * v1x + v1y * v1y;
        double d20 = v2x * v0x + v2y * v0y, d21 = v2x * v1x + v2y * v1y;
        double denom = d00 * d11 - d01 * d01;
        if (denom == 0) return null;
        double v = (d11 * d20 - d01 * d21) / denom;
        double w = (d00 * d21 - d01 * d20) / denom;
        double u = 1 - v - w;
        if (u < -0.01 || v < -0.01 || w < -0.01) return null;
        return new double[]{Math.max(0, u), Math.max(0, v), Math.max(0, w)};
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (currentSystem.equals("CUSTOM")) {
                int cx = 330, cy = 175, r1 = 12, r2 = 48;
                double dx = mx - cx, dy = my - cy;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d >= r1 && d <= r2) {
                    curHue = (int) ((Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360);
                    return true;
                }
                int tx = 150, ty = 120, size = 110;
                double[] bc = bary(mx, my, tx + size / 2, ty, tx, ty + size, tx + size, ty + size);
                if (bc != null) {
                    double s = (bc[1] + bc[2]) > 0 ? bc[1] / (bc[1] + bc[2]) : 0;
                    double vv = bc[0] + bc[1];
                    curSat = (int) Math.round(Math.max(0, Math.min(1, s)) * 100);
                    curVal = (int) Math.round(Math.max(0, Math.min(1, vv)) * 100);
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
