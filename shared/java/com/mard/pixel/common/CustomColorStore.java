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

    public static synchronized CustomColor add(String name, int rgb) {
        Set<String> used = new TreeSet<>();
        for (CustomColor cc : COLORS) used.add(cc.code().toUpperCase());
        String code = CustomColor.nextCode(used);
        if (code == null) return null;
        CustomColor c = new CustomColor(name, code, rgb);
        COLORS.add(c);
        return c;
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
