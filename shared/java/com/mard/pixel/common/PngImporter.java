package com.mard.pixel.common;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PngImporter {

    public static class Result {
        public final String paletteName;
        public final List<Integer> colors;
        public final int totalPixels;
        public final int uniqueColors;

        public Result(String paletteName, List<Integer> colors, int totalPixels, int uniqueColors) {
            this.paletteName = paletteName;
            this.colors = colors;
            this.totalPixels = totalPixels;
            this.uniqueColors = uniqueColors;
        }
    }

    public static Result importFromPng(File pngFile, String paletteName, int maxColors,
                                        boolean skipWhite, boolean skipBlack, boolean skipTransparent)
            throws IOException {
        BufferedImage image = ImageIO.read(pngFile);
        if (image == null) {
            throw new IOException("Not a valid PNG image: " + pngFile.getName());
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        Set<Integer> uniqueSet = new LinkedHashSet<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                int rgb = argb & 0xFFFFFF;

                if (skipTransparent && alpha < 128) continue;
                if (skipWhite && rgb == 0xFFFFFF) continue;
                if (skipBlack && rgb == 0x000000) continue;

                uniqueSet.add(rgb);
            }
        }

        List<Integer> colors = new ArrayList<>(uniqueSet);
        int uniqueColors = colors.size();

        if (maxColors > 0 && colors.size() > maxColors) {
            colors = clusterColors(colors, maxColors);
        }

        return new Result(paletteName, colors, totalPixels, uniqueColors);
    }

    private static List<Integer> clusterColors(List<Integer> colors, int targetCount) {
        if (colors.size() <= targetCount) return new ArrayList<>(colors);

        List<Integer> remaining = new ArrayList<>(colors);
        while (remaining.size() > targetCount) {
            int bestI = 0, bestJ = 1;
            double bestDelta = Double.MAX_VALUE;
            for (int i = 0; i < remaining.size(); i++) {
                for (int j = i + 1; j < remaining.size(); j++) {
                    double d = ColorMath.deltaE2000(remaining.get(i), remaining.get(j));
                    if (d < bestDelta) {
                        bestDelta = d;
                        bestI = i;
                        bestJ = j;
                    }
                }
            }
            int c1 = remaining.get(bestI);
            int c2 = remaining.get(bestJ);
            int r = (((c1 >> 16) & 0xFF) + ((c2 >> 16) & 0xFF)) / 2;
            int g = (((c1 >> 8) & 0xFF) + ((c2 >> 8) & 0xFF)) / 2;
            int b = ((c1 & 0xFF) + (c2 & 0xFF)) / 2;
            int merged = (r << 16) | (g << 8) | b;

            remaining.set(bestI, merged);
            remaining.remove(bestJ);
        }
        return remaining;
    }

    public static String paletteNameFromFile(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        if (name.isEmpty()) return "Imported";
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private PngImporter() {}
}
