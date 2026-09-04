package com.mard.blueprint.blueprint;

import java.util.ArrayList;
import java.util.List;

/**
 * 图纸数据类。
 * 存储导入的拼豆图纸的颜色布局信息。
 */
public class Blueprint {

    private final String name;
    private final int width;
    private final int height;
    private final List<String> colorCodes; // 每个像素对应的MARD色号，null表示透明/跳过
    private final List<int[]> originalColors; // 原始图片颜色（用于预览）

    public Blueprint(String name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.colorCodes = new ArrayList<>(width * height);
        this.originalColors = new ArrayList<>(width * height);
        for (int i = 0; i < width * height; i++) {
            colorCodes.add(null);
            originalColors.add(new int[]{255, 255, 255});
        }
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getColorCode(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        return colorCodes.get(y * width + x);
    }

    public void setColorCode(int x, int y, String code) {
        if (x < 0 || x >= width || y < 0 || y >= height) return;
        colorCodes.set(y * width + x, code);
    }

    public int[] getOriginalColor(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return new int[]{255, 255, 255};
        return originalColors.get(y * width + x);
    }

    public void setOriginalColor(int x, int y, int[] rgb) {
        if (x < 0 || x >= width || y < 0 || y >= height) return;
        originalColors.set(y * width + x, rgb);
    }

    /**
     * 获取非透明像素的数量。
     */
    public int getNonTransparentCount() {
        int count = 0;
        for (String code : colorCodes) {
            if (code != null) count++;
        }
        return count;
    }

    /**
     * 获取使用的颜色种类统计。
     */
    public java.util.Map<String, Integer> getColorStats() {
        java.util.Map<String, Integer> stats = new java.util.LinkedHashMap<>();
        for (String code : colorCodes) {
            if (code != null) {
                stats.merge(code, 1, Integer::sum);
            }
        }
        return stats;
    }
}
