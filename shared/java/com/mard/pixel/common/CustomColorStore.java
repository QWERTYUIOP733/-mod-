package com.mard.pixel.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class CustomColorStore {

    private static final List<CustomColor> COLORS = new ArrayList<>();

    public static synchronized List<CustomColor> all() {
        return Collections.unmodifiableList(new ArrayList<>(COLORS));
    }

    public static synchronized CustomColor byCode(String code) {
        if (code == null) return null;
        String c = code.trim().toUpperCase();
        for (CustomColor cc : COLORS) if (cc.code().equalsIgnoreCase(c)) return cc;
        return null;
    }

    public static synchronized int size() { return COLORS.size(); }

    /**
     * 统计指定色系（颜色名称）的编号数量。
     */
    public static synchronized int countByColorName(String colorName) {
        int count = 0;
        for (CustomColor cc : COLORS) {
            String dn = cc.name();
            if (dn != null && dn.startsWith(colorName + " ")) {
                count++;
            }
        }
        return count;
    }

    public static synchronized CustomColor add(String name, int rgb) {
        // 先判断颜色名称
        String colorName = ColorMath.colorName(rgb);

        Set<String> used = new TreeSet<>();
        for (CustomColor cc : COLORS) used.add(cc.code().toUpperCase());
        String code = CustomColor.nextCode(used);
        if (code == null) return null;

        // 自动生成"颜色名 编号 #RRGGBB"形式的显示名，如"黄 A1 #FFFF00"
        // 编号循环利用：找最小可用编号（遗失的编号会被回收，可重新分配）
        String serial = nextAvailableSerial(colorName);
        String displayName = colorName + " " + serial + " #" + ColorMath.toHex(rgb);
        CustomColor c = new CustomColor(displayName, code, rgb);
        COLORS.add(c);
        return c;
    }

    /**
     * 为指定颜色名称找最小可用编号（A1, A2, ..., A99, B1, B2...）。
     * 遗失的编号会被回收，可重新分配给新颜色，实现编号循环利用。
     */
    private static String nextAvailableSerial(String colorName) {
        // 收集该色系已使用的编号
        Set<String> usedSerials = new TreeSet<>();
        for (CustomColor cc : COLORS) {
            String dn = cc.name();
            if (dn != null && dn.startsWith(colorName + " ")) {
                String serial = extractSerial(dn, colorName);
                if (serial != null) usedSerials.add(serial);
            }
        }
        // 找最小可用编号：A1-A99, B1-B99, ..., Z1-Z99
        for (char letter = 'A'; letter <= 'Z'; letter++) {
            for (int num = 1; num <= 99; num++) {
                String serial = "" + letter + num;
                if (!usedSerials.contains(serial)) {
                    return serial;
                }
            }
        }
        return "Z99"; // 封顶
    }

    /**
     * 从显示名中提取编号部分（如"黄 A1 #FFFF00" → "A1"）。
     */
    private static String extractSerial(String displayName, String colorName) {
        try {
            String rest = displayName.substring(colorName.length() + 1).trim();
            // 格式："A1 #FFFF00" 或 "A1"
            int spaceIdx = rest.indexOf(' ');
            String serial = spaceIdx > 0 ? rest.substring(0, spaceIdx) : rest;
            if (serial.length() >= 2 && Character.isLetter(serial.charAt(0))) {
                Integer.parseInt(serial.substring(1));
                return serial;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static synchronized boolean remove(String code) {
        if (code == null) return false;
        String c = code.trim().toUpperCase();
        for (int i = COLORS.size() - 1; i >= 0; i--) {
            if (COLORS.get(i).code().equalsIgnoreCase(c)) { COLORS.remove(i); return true; }
        }
        return false;
    }

    public static synchronized void setAll(List<CustomColor> colors) {
        COLORS.clear();
        if (colors != null) COLORS.addAll(colors);
    }

    public static synchronized void loadFrom(List<CustomColor> colors) {
        COLORS.clear();
        Set<String> seen = new TreeSet<>();
        if (colors != null) for (CustomColor cc : colors) {
            if (cc == null || cc.code() == null || cc.code().isBlank()) continue;
            if (!seen.add(cc.code().toUpperCase())) continue;
            COLORS.add(cc);
        }
    }

    private CustomColorStore() {}
}
