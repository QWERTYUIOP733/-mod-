package com.mard.pixel.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BrandPalette {

    public static final Map<String, List<BrandColor>> BRANDS = new LinkedHashMap<>();
    public static final List<String> BRAND_NAMES = List.of("perler", "hama", "artkal");
    private static boolean loaded = false;

    public static synchronized void load() {
        if (loaded) return;
        try (InputStream in = BrandPalette.class.getResourceAsStream("/assets/mard_pixel/brands/brands.json")) {
            if (in == null) { loaded = true; return; }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject brands = root.getAsJsonObject("brands");
            for (String name : BRAND_NAMES) {
                List<BrandColor> list = new ArrayList<>();
                JsonElement el = brands.get(name);
                if (el != null && el.isJsonArray()) {
                    for (JsonElement e : el.getAsJsonArray()) {
                        JsonObject o = e.getAsJsonObject();
                        String code = o.get("code").getAsString();
                        String hex = o.has("hex") ? o.get("hex").getAsString() : o.get("rgb").getAsString();
                        int rgb = parseHex(hex);
                        list.add(new BrandColor(name, code, rgb));
                    }
                }
                BRANDS.put(name, Collections.unmodifiableList(list));
            }
        } catch (Exception ignored) {
        }
        loaded = true;
    }

    public static List<BrandColor> brandColors(String brand) {
        load();
        return brand == null ? null : BRANDS.get(brand.toLowerCase(Locale.ROOT));
    }

    public static List<String> brandNames() {
        load();
        return new ArrayList<>(BRANDS.keySet());
    }

    public static BrandColor lookup(String brand, String code) {
        List<BrandColor> list = brandColors(brand);
        if (list == null || code == null) return null;
        String c = normalizeCode(code);
        for (BrandColor bc : list) {
            if (bc.code().equalsIgnoreCase(c) || normalizeCode(bc.code()).equals(c)) return bc;
        }
        return null;
    }

    public static String normalizeCode(String code) {
        if (code == null) return null;
        return code.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    public static MardColor nearestMard(int rgb) {
        return MardPalette.nearest(rgb);
    }

    public static int parseHex(String s) {
        if (s == null) return 0xFFFFFF;
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        else if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        if (s.matches("[0-9a-fA-F]{3}")) {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) { sb.append(c).append(c); }
            return (int) Long.parseLong(sb.toString(), 16);
        }
        if (s.matches("[0-9a-fA-F]{6}")) return (int) Long.parseLong(s, 16);
        return 0xFFFFFF;
    }

    private BrandPalette() {}
}
