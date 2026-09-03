package com.mard.pixel.common;

/**
 * 品牌/外部色系中的单个色号。
 * @param brand 品牌名（小写），如 perler / hama / artkal / 外部文件名
 * @param code  品牌色号，如 P20 / H45 / S14
 * @param rgb   0xRRGGBB（品牌精确色值）
 */
public record BrandColor(String brand, String code, int rgb) {
    public int r() { return (rgb >> 16) & 0xFF; }
    public int g() { return (rgb >> 8) & 0xFF; }
    public int b() { return rgb & 0xFF; }
}
