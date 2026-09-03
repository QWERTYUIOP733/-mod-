package com.mard.pixel.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ExternalBrandStore {

    public static List<BrandColor> parseBrandFile(String fileName, String json) {
        List<BrandColor> out = new ArrayList<>();
        try {
            JsonElement root = JsonParser.parseString(json);
            String fallbackBrand = brandKey(fileName);
            if (root.isJsonArray()) {
                for (JsonElement e : root.getAsJsonArray()) {
                    BrandColor c = readColor(fallbackBrand, e);
                    if (c != null) out.add(c);
                }
            } else if (root.isJsonObject()) {
                JsonObject o = root.getAsJsonObject();
                String name = o.has("name") && o.get("name").isJsonPrimitive()
                        ? o.get("name").getAsString() : fallbackBrand;
                String brand = name.trim().toUpperCase(Locale.ROOT);
                if (o.has("colors") && o.get("colors").isJsonArray()) {
                    for (JsonElement e : o.getAsJsonArray("colors")) {
                        BrandColor c = readColor(brand, e);
                        if (c != null) out.add(c);
                    }
                } else {
                    for (Map.Entry<String, JsonElement> en : o.entrySet()) {
                        if (en.getKey().equalsIgnoreCase("name")) continue;
                        String hex = en.getValue().isJsonPrimitive()
                                ? en.getValue().getAsString() : null;
                        if (hex == null) continue;
                        int rgb = BrandPalette.parseHex(hex);
                        out.add(new BrandColor(brand, en.getKey(), rgb));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static BrandColor readColor(String brand, JsonElement e) {
        try {
            if (e.isJsonPrimitive()) {
                String hex = e.getAsString();
                return new BrandColor(brand, "C", BrandPalette.parseHex(hex));
            }
            JsonObject o = e.getAsJsonObject();
            String code = o.has("code") ? o.get("code").getAsString() : null;
            if (code == null && o.has("id")) code = o.get("id").getAsString();
            if (code == null) return null;
            int rgb;
            if (o.has("rgb")) {
                rgb = parseRgbField(o.get("rgb"));
            } else if (o.has("hex")) {
                rgb = BrandPalette.parseHex(o.get("hex").getAsString());
            } else if (o.has("r") && o.has("g") && o.has("b")) {
                int r = o.get("r").getAsInt(), g = o.get("g").getAsInt(), b = o.get("b").getAsInt();
                rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
            } else {
                return null;
            }
            return new BrandColor(brand, code.trim(), rgb);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int parseRgbField(JsonElement el) {
        if (el.isJsonPrimitive()) {
            if (el.getAsJsonPrimitive().isNumber()) return el.getAsInt() & 0xFFFFFF;
            String s = el.getAsString().trim();
            if (s.startsWith("#")) return BrandPalette.parseHex(s);
            String[] parts = s.split("[,\\s]+");
            if (parts.length == 3) {
                try {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                } catch (NumberFormatException ignored) {
                }
            }
            return BrandPalette.parseHex(s);
        }
        if (el.isJsonArray() && el.getAsJsonArray().size() == 3) {
            JsonArray a = el.getAsJsonArray();
            int r = a.get(0).getAsInt(), g = a.get(1).getAsInt(), b = a.get(2).getAsInt();
            return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        }
        return 0xFFFFFF;
    }

    public static String brandKey(String fileName) {
        if (fileName == null) return "EXTERNAL";
        String n = fileName;
        int idx = n.lastIndexOf('.');
        if (idx > 0) n = n.substring(0, idx);
        return n.trim().toUpperCase(Locale.ROOT);
    }

    public static String serialize(String brandName, List<BrandColor> colors) {
        JsonObject root = new JsonObject();
        root.addProperty("name", brandName == null ? "EXTERNAL" : brandName);
        JsonArray arr = new JsonArray();
        if (colors != null) {
            for (BrandColor bc : colors) {
                JsonObject o = new JsonObject();
                o.addProperty("code", bc.code());
                o.addProperty("rgb", ColorMath.toHex(bc.rgb()));
                arr.add(o);
            }
        }
        root.add("colors", arr);
        return root.toString();
    }

    private ExternalBrandStore() {}
}
