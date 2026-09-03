package com.mard.pixel.common;

import java.util.Set;

/**
 * 自定义色号（玩家自定义 RGB 色）。
 * @param name 显示名（可为空 → 用 code）
 * @param code 唯一编号，形如 C0001（C + 4 位）
 * @param rgb  0xRRGGBB
 */
public record CustomColor(String name, String code, int rgb) {
    public String displayName() {
        return (name == null || name.isBlank()) ? code : name;
    }

    /** 生成下一个未占用编号：C0001..C9999，封顶 9999。 */
    public static String nextCode(Set<String> used) {
        for (int i = 1; i <= 9999; i++) {
            String c = String.format("C%04d", i);
            if (!used.contains(c)) return c;
        }
        return null;
    }
}
