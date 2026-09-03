package com.mard.pixel.common;

/**
 * 单个 MARD 标准色号。
 * @param code   色号，如 "A1" / "ZG8"
 * @param hex    #RRGGBB
 * @param series 所属系列，如 "A" / "ZG"
 * @param rgb    0xRRGGBB
 */
public record MardColor(String code, String hex, String series, int rgb) {

    /** 便捷构造：仅给 code/hex/series，rgb 自动解析。 */
    public MardColor(String code, String hex, String series) {
        this(code, hex, series, parseRgb(hex));
    }

    private static int parseRgb(String h) {
        String s = h.startsWith("#") ? h.substring(1) : h;
        return (int) Long.parseLong(s, 16);
    }

    public int r() { return (rgb >> 16) & 0xFF; }
    public int g() { return (rgb >> 8) & 0xFF; }
    public int b() { return rgb & 0xFF; }

    /** 物品/方块注册名后缀：色号小写，如 mard_a1 */
    public String blockName() { return "mard_" + code.toLowerCase(); }
}
