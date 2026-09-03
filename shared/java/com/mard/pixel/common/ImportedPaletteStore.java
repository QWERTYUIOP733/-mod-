package com.mard.pixel.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ImportedPaletteStore {

    public static class Palette {
        public final String name;
        public final List<Integer> colors;
        public final String sourceFile;
        public final long importedAt;

        public Palette(String name, List<Integer> colors, String sourceFile, long importedAt) {
            this.name = name;
            this.colors = Collections.unmodifiableList(new ArrayList<>(colors));
            this.sourceFile = sourceFile;
            this.importedAt = importedAt;
        }

        public int size() { return colors.size(); }
    }

    private static final Map<String, Palette> PALETTES = new LinkedHashMap<>();
    private static Path configFile;
    private static boolean loaded = false;

    public static synchronized void init(Path configDir) {
        configFile = configDir.resolve("mard_pixel_imported_palettes.json");
        load();
    }

    public static synchronized void load() {
        if (loaded) return;
        PALETTES.clear();
        if (configFile != null && Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile, StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                if (root.has("palettes") && root.get("palettes").isJsonArray()) {
                    for (JsonElement e : root.getAsJsonArray("palettes")) {
                        Palette p = parsePalette(e.getAsJsonObject());
                        if (p != null) PALETTES.put(p.name.toLowerCase(Locale.ROOT), p);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        loaded = true;
    }

    private static Palette parsePalette(JsonObject o) {
        try {
            String name = o.get("name").getAsString();
            String sourceFile = o.has("sourceFile") ? o.get("sourceFile").getAsString() : "";
            long importedAt = o.has("importedAt") ? o.get("importedAt").getAsLong() : System.currentTimeMillis();
            List<Integer> colors = new ArrayList<>();
            if (o.has("colors") && o.get("colors").isJsonArray()) {
                for (JsonElement ce : o.getAsJsonArray("colors")) {
                    if (ce.isJsonPrimitive()) {
                        String hex = ce.getAsString();
                        colors.add(parseHex(hex));
                    } else if (ce.isJsonObject()) {
                        JsonObject co = ce.getAsJsonObject();
                        if (co.has("rgb")) colors.add(co.get("rgb").getAsInt() & 0xFFFFFF);
                        else if (co.has("hex")) colors.add(parseHex(co.get("hex").getAsString()));
                    }
                }
            }
            return new Palette(name, colors, sourceFile, importedAt);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static synchronized void save() {
        if (configFile == null) return;
        try {
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (Palette p : PALETTES.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("name", p.name);
                o.addProperty("sourceFile", p.sourceFile);
                o.addProperty("importedAt", p.importedAt);
                JsonArray colors = new JsonArray();
                for (int rgb : p.colors) colors.add(ColorMath.toHex(rgb));
                o.add("colors", colors);
                arr.add(o);
            }
            root.add("palettes", arr);
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, root.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public static synchronized Palette add(String name, List<Integer> colors, String sourceFile) {
        if (name == null || name.isBlank()) name = "Imported_" + System.currentTimeMillis();
        Palette p = new Palette(name.trim(), colors, sourceFile == null ? "" : sourceFile, System.currentTimeMillis());
        PALETTES.put(p.name.toLowerCase(Locale.ROOT), p);
        save();
        return p;
    }

    public static synchronized boolean remove(String name) {
        if (name == null) return false;
        boolean removed = PALETTES.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed) save();
        return removed;
    }

    public static synchronized Palette get(String name) {
        if (name == null) return null;
        return PALETTES.get(name.toLowerCase(Locale.ROOT));
    }

    public static synchronized List<Palette> all() {
        return new ArrayList<>(PALETTES.values());
    }

    public static synchronized List<String> names() {
        List<String> out = new ArrayList<>();
        for (Palette p : PALETTES.values()) out.add(p.name);
        return out;
    }

    public static synchronized int size() { return PALETTES.size(); }

    public static synchronized void clear() {
        PALETTES.clear();
        save();
    }

    private static int parseHex(String s) {
        if (s == null) return 0xFFFFFF;
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        else if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        if (s.matches("[0-9a-fA-F]{3}")) {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) sb.append(c).append(c);
            return (int) Long.parseLong(sb.toString(), 16);
        }
        if (s.matches("[0-9a-fA-F]{6}")) return (int) Long.parseLong(s, 16);
        return 0xFFFFFF;
    }

    private ImportedPaletteStore() {}
}
