package com.mard.blueprint.client;

import com.mard.blueprint.blueprint.Blueprint;
import com.mard.blueprint.blueprint.BlueprintLoader;
import com.mard.blueprint.network.BlueprintNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 图纸导入UI界面。
 * 功能：
 * 1. 选择图纸图片文件
 * 2. 预览映射后的色块布局
 * 3. 调整生成位置和方向
 * 4. 一键在世界中生成图纸
 */
public class BlueprintScreen extends Screen {

    private static final int MAX_BLUEPRINT_SIZE = 64; // 最大图纸尺寸

    private Blueprint currentBlueprint;
    private final List<File> availableBlueprints = new ArrayList<>();
    private int selectedBlueprintIndex = -1;
    private int previewScrollX = 0;
    private int previewScrollY = 0;
    private float previewScale = 1.0f;
    private String statusMessage = "";
    private boolean generateMode = false;

    // 图纸目录
    private static final String BLUEPRINT_DIR = "config/mard_pixel_blueprint/blueprints";

    public BlueprintScreen() {
        super(Component.literal("MARD 图纸导入"));
        loadAvailableBlueprints();
    }

    /**
     * 加载可用的图纸文件。
     */
    private void loadAvailableBlueprints() {
        availableBlueprints.clear();
        File dir = new File(BLUEPRINT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File[] files = BlueprintLoader.listImageFiles(dir);
        for (File f : files) {
            availableBlueprints.add(f);
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = width / 2;
        int centerY = height / 2;

        // 左侧：图纸文件列表
        int listX = 20;
        int listY = 40;
        int listW = 150;
        int listH = height - 100;

        // 右侧：预览区域
        int previewX = listX + listW + 20;
        int previewY = 40;
        int previewW = width - previewX - 20;
        int previewH = height - 120;

        // 底部：操作按钮
        int btnY = height - 50;

        // 刷新列表按钮
        addRenderableWidget(Button.builder(Component.literal("刷新列表"), btn -> {
            loadAvailableBlueprints();
            statusMessage = "已刷新图纸列表";
        }).bounds(listX, listY - 25, 70, 20).build());

        // 打开目录按钮
        addRenderableWidget(Button.builder(Component.literal("打开目录"), btn -> {
            openBlueprintDirectory();
            statusMessage = "请将图纸图片放入 " + BLUEPRINT_DIR;
        }).bounds(listX + 75, listY - 25, 75, 20).build());

        // 生成按钮
        addRenderableWidget(Button.builder(Component.literal("在世界中生成"), btn -> {
            if (currentBlueprint != null) {
                generateBlueprintInWorld();
            } else {
                statusMessage = "请先选择并加载图纸";
            }
        }).bounds(centerX - 100, btnY, 120, 25).build());

        // 取消按钮
        addRenderableWidget(Button.builder(Component.literal("关闭"), btn -> {
            this.onClose();
        }).bounds(centerX + 30, btnY, 70, 25).build());

        // 缩放控制
        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            previewScale = Math.min(4.0f, previewScale * 1.25f);
        }).bounds(previewX + previewW - 50, previewY - 25, 20, 20).build());

        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            previewScale = Math.max(0.25f, previewScale / 1.25f);
        }).bounds(previewX + previewW - 25, previewY - 25, 20, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        // 标题
        String title = "MARD 拼豆图纸导入";
        g.drawString(font, Component.literal(title), (width - font.width(title)) / 2, 12, 0xFFFFFF);

        // 左侧：图纸文件列表
        renderBlueprintList(g, 20, 40, 150, height - 100, mouseX, mouseY);

        // 右侧：预览区域
        int previewX = 190;
        int previewY = 40;
        int previewW = width - previewX - 20;
        int previewH = height - 120;

        renderPreviewArea(g, previewX, previewY, previewW, previewH, mouseX, mouseY);

        // 状态信息
        if (!statusMessage.isEmpty()) {
            g.drawString(font, Component.literal(statusMessage), 20, height - 20, 0xFFFFAA);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    /**
     * 渲染图纸文件列表。
     */
    private void renderBlueprintList(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        // 列表背景
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF1a1a1a);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF2a2a2a);

        g.drawString(font, Component.literal("可用图纸"), x, y - 12, 0xFFFFAA);

        if (availableBlueprints.isEmpty()) {
            g.drawString(font, Component.literal("（暂无图纸）"), x + 5, y + 5, 0x666666);
            g.drawString(font, Component.literal("请将图片放入"), x + 5, y + 20, 0x666666);
            g.drawString(font, Component.literal(BLUEPRINT_DIR), x + 5, y + 32, 0x666666);
            return;
        }

        int itemH = 22;
        for (int i = 0; i < availableBlueprints.size(); i++) {
            int itemY = y + i * itemH;
            if (itemY + itemH > y + h) break;

            File file = availableBlueprints.get(i);
            boolean selected = (i == selectedBlueprintIndex);
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= itemY && mouseY < itemY + itemH;

            // 选中/悬停高亮
            if (selected) {
                g.fill(x, itemY, x + w, itemY + itemH, 0xFF4a4a6a);
            } else if (hovered) {
                g.fill(x, itemY, x + w, itemY + itemH, 0xFF3a3a3a);
            }

            String name = file.getName();
            if (name.length() > 18) name = name.substring(0, 16) + "..";
            g.drawString(font, Component.literal(name), x + 4, itemY + 6, selected ? 0xFFFFFF : 0xCCCCCC);
        }
    }

    /**
     * 渲染预览区域。
     */
    private void renderPreviewArea(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY) {
        // 预览区背景
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF1a1a1a);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF2a2a2a);

        if (currentBlueprint == null) {
            String msg = "选择左侧图纸文件进行预览";
            g.drawString(font, Component.literal(msg), x + (w - font.width(msg)) / 2, y + h / 2 - 10, 0x666666);
            return;
        }

        // 图纸信息
        String info = String.format("%s  %dx%d  %d色块",
                currentBlueprint.getName(),
                currentBlueprint.getWidth(),
                currentBlueprint.getHeight(),
                currentBlueprint.getNonTransparentCount());
        g.drawString(font, Component.literal(info), x, y - 12, 0xFFFFAA);

        // 渲染图纸预览
        int cellSize = (int) (8 * previewScale);
        int bpWidth = currentBlueprint.getWidth() * cellSize;
        int bpHeight = currentBlueprint.getHeight() * cellSize;

        int startX = x + (w - bpWidth) / 2 + previewScrollX;
        int startY = y + (h - bpHeight) / 2 + previewScrollY;

        // 裁剪区域
        int clipX = Math.max(startX, x);
        int clipY = Math.max(startY, y);
        int clipW = Math.min(startX + bpWidth, x + w) - clipX;
        int clipH = Math.min(startY + bpHeight, y + h) - clipY;

        if (clipW > 0 && clipH > 0) {
            // 绘制每个色块
            for (int by = 0; by < currentBlueprint.getHeight(); by++) {
                for (int bx = 0; bx < currentBlueprint.getWidth(); bx++) {
                    String code = currentBlueprint.getColorCode(bx, by);
                    if (code == null) continue;

                    int cellX = startX + bx * cellSize;
                    int cellY = startY + by * cellSize;

                    // 跳过视口外的格子
                    if (cellX + cellSize < x || cellX > x + w || cellY + cellSize < y || cellY > y + h) {
                        continue;
                    }

                    // 获取颜色（从原始颜色或MARD色号映射）
                    int[] rgb = currentBlueprint.getOriginalColor(bx, by);
                    int color = 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];

                    g.fill(cellX, cellY, cellX + cellSize, cellY + cellSize, color);

                    // 格子边框
                    if (cellSize >= 4) {
                        g.fill(cellX, cellY, cellX + cellSize, cellY + 1, 0xFF000000);
                        g.fill(cellX, cellY + cellSize - 1, cellX + cellSize, cellY + cellSize, 0xFF000000);
                        g.fill(cellX, cellY, cellX + 1, cellY + cellSize, 0xFF000000);
                        g.fill(cellX + cellSize - 1, cellY, cellX + cellSize, cellY + cellSize, 0xFF000000);
                    }
                }
            }
        }

        // 颜色统计
        Map<String, Integer> stats = currentBlueprint.getColorStats();
        String statsText = String.format("使用颜色: %d种", stats.size());
        g.drawString(font, Component.literal(statsText), x, y + h + 6, 0x888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 左侧列表点击
        int listX = 20;
        int listY = 40;
        int listW = 150;
        int listH = height - 100;

        if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
            int itemH = 22;
            int index = (int) ((mouseY - listY) / itemH);
            if (index >= 0 && index < availableBlueprints.size()) {
                selectedBlueprintIndex = index;
                loadBlueprint(availableBlueprints.get(index));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 预览区缩放
        int previewX = 190;
        int previewY = 40;
        int previewW = width - previewX - 20;
        int previewH = height - 120;

        if (mouseX >= previewX && mouseX < previewX + previewW && mouseY >= previewY && mouseY < previewY + previewH) {
            if (delta > 0) {
                previewScale = Math.min(4.0f, previewScale * 1.1f);
            } else {
                previewScale = Math.max(0.25f, previewScale / 1.1f);
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 加载图纸文件。
     */
    private void loadBlueprint(File file) {
        try {
            currentBlueprint = BlueprintLoader.loadFromImage(file, MAX_BLUEPRINT_SIZE, MAX_BLUEPRINT_SIZE);
            previewScrollX = 0;
            previewScrollY = 0;
            previewScale = 1.0f;
            statusMessage = String.format("已加载图纸: %s (%dx%d, %d色块)",
                    currentBlueprint.getName(),
                    currentBlueprint.getWidth(),
                    currentBlueprint.getHeight(),
                    currentBlueprint.getNonTransparentCount());
        } catch (Exception e) {
            statusMessage = "加载图纸失败: " + e.getMessage();
            currentBlueprint = null;
        }
    }

    /**
     * 在世界中生成图纸。
     */
    private void generateBlueprintInWorld() {
        if (currentBlueprint == null || minecraft == null || minecraft.player == null) return;

        // 以玩家位置为起点生成
        BlockPos playerPos = minecraft.player.blockPosition();
        int startX = playerPos.getX();
        int startY = playerPos.getY();
        int startZ = playerPos.getZ();

        // 扁平化颜色码数组
        int width = currentBlueprint.getWidth();
        int height = currentBlueprint.getHeight();
        String[] colorCodes = new String[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                colorCodes[y * width + x] = currentBlueprint.getColorCode(x, y);
            }
        }

        // 发送网络包到服务端
        BlueprintNetwork.sendGenerateRequest(startX, startY, startZ, width, height, colorCodes);
        statusMessage = String.format("已请求生成图纸 at (%d, %d, %d)", startX, startY, startZ);
    }

    /**
     * 打开图纸目录。
     */
    private void openBlueprintDirectory() {
        File dir = new File(BLUEPRINT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            java.awt.Desktop.getDesktop().open(dir);
        } catch (Exception e) {
            statusMessage = "无法打开目录，请手动打开: " + BLUEPRINT_DIR;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
