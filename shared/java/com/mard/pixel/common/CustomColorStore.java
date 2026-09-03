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

        // 检测该色系编号是否已达64上限（每个色系独立计数）
        if (countByColorName(colorName) >= 64) {
            return null; // 该色系编号已满，返回null
        }

        Set<String> used = new TreeSet<>();
        for (CustomColor cc : COLORS) used.add(cc.code().toUpperCase());
        String code = CustomColor.nextCode(used);
        if (code == null) return null;
        // 自动生成"颜色名 编号"形式的显示名，如"黄 A1"
        String serial = nextSerialForColor(colorName);
        String displayName = colorName + " " + serial;
        CustomColor c = new CustomColor(displayName, code, rgb);
        COLORS.add(c);
        return c;
    }

    /**
     * 为指定颜色名称生成下一个编号（A1, A2, ..., A99, B1, B2...）。
     */
    private static String nextSerialForColor(String colorName) {
        int maxNum = 0;
        char maxLetter = 'A';
        for (CustomColor cc : COLORS) {
            String dn = cc.name();
            if (dn != null && dn.startsWith(colorName + " ")) {
                String serial = dn.substring(colorName.length() + 1).trim();
                if (serial.length() >= 2) {
                    char letter = serial.charAt(0);
                    try {
                        int num = Integer.parseInt(serial.substring(1));
                        if (letter > maxLetter || (letter == maxLetter && num > maxNum)) {
                            maxLetter = letter;
                            maxNum = num;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        int nextNum = maxNum + 1;
        char nextLetter = maxLetter;
        if (nextNum > 99) {
            nextNum = 1;
            nextLetter = (char) (maxLetter + 1);
            if (nextLetter > 'Z') nextLetter = 'Z'; // 封顶
        }
        return "" + nextLetter + nextNum;
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
