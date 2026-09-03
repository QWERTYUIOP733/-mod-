package com.mard.pixel.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class CustomColorFile {

    public static List<CustomColor> parse(String json) {
        List<CustomColor> out = new ArrayList<>();
        try {
            JsonElement root = JsonParser.parseString(json);
            if (root.isJsonArray()) {
                for (JsonElement e : root.getAsJsonArray()) out.add(readColor(e));
            } else if (root.isJsonObject()) {
                JsonObject o = root.getAsJsonObject();
                if (o.has("colors") && o.get("colors").isJsonArray()) {
                    for (JsonElement e : o.getAsJsonArray("colors")) out.add(readColor(e));
                } else if (o.has("custom")) {
                    JsonElement ce = o.get("custom");
                    if (ce.isJsonArray()) {
                        for (JsonElement e : ce.getAsJsonArray()) out.add(readColor(e));
                    } else if (ce.isJsonObject()) {
                        for (Map.Entry<String, JsonElement> en : ce.getAsJsonObject().entrySet()) {
                            JsonObject v = en.getValue().getAsJsonObject();
                            String code = v.has("code") ? v.get("code").getAsString() : en.getKey();
                            int rgb = BrandPalette.parseHex(v.has("rgb") ? v.get("rgb").getAsString()
                                    : v.has("hex") ? v.get("hex").getAsString() : "#FFFFFF");
                            String name = v.has("name") ? v.get("name").getAsString() : null;
                            out.add(new CustomColor(name, code, rgb));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static CustomColor readColor(JsonElement e) {
        JsonObject o = e.getAsJsonObject();
        String code = o.has("code") ? o.get("code").getAsString()
                : o.has("id") ? o.get("id").getAsString() : null;
        if (code == null) return null;
        int rgb = BrandPalette.parseHex(o.has("rgb") ? o.get("rgb").getAsString()
                : o.has("hex") ? o.get("hex").getAsString() : "#FFFFFF");
        String name = o.has("name") ? o.get("name").getAsString() : null;
        return new CustomColor(name, code, rgb);
    }

    public static String serialize(List<CustomColor> colors) {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonObject custom = new JsonObject();
        for (CustomColor c : colors) {
            JsonObject o = new JsonObject();
            o.addProperty("name", c.name());
            o.addProperty("rgb", ColorMath.toHex(c.rgb()));
            custom.add(c.code(), o);
        }
        root.add("custom", custom);
        return root.toString();
    }

    public static List<CustomColor> load(Path file) {
        try {
            if (file != null && Files.exists(file)) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                return parse(json);
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    public static void save(Path file, List<CustomColor> colors) {
        try {
            if (file != null) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, serialize(colors), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
    }

    public static LinkedHashMap<String, CustomColor> toOrderedMap(List<CustomColor> colors) {
        TreeMap<String, CustomColor> m = new TreeMap<>();
        for (CustomColor c : colors) if (c != null) m.put(c.code(), c);
        return new LinkedHashMap<>(m);
    }

    private CustomColorFile() {}
}
