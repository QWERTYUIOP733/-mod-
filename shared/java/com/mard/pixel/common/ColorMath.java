package com.mard.pixel.common;

/**
 * 颜色数学工具：CIEDE2000 色差、RGB/HSV 互转。
 */
public final class ColorMath {

    /** 两个 0xRRGGBB 颜色之间的 CIEDE2000 色差（越小越接近）。 */
    public static double deltaE2000(int rgb1, int rgb2) {
        double[] lab1 = rgbToLab(rgb1);
        double[] lab2 = rgbToLab(rgb2);
        return deltaE2000Lab(lab1[0], lab1[1], lab1[2], lab2[0], lab2[1], lab2[2]);
    }

    public static double[] rgbToLab(int rgb) {
        return xyzToLab(rgbToXyz(rgb));
    }

    /** sRGB(0-255) -> XYZ (D65)。 */
    public static double[] rgbToXyz(int rgb) {
        double r = ((rgb >> 16) & 0xFF) / 255.0;
        double g = ((rgb >> 8) & 0xFF) / 255.0;
        double b = (rgb & 0xFF) / 255.0;
        r = (r > 0.04045) ? Math.pow((r + 0.055) / 1.055, 2.4) : r / 12.92;
        g = (g > 0.04045) ? Math.pow((g + 0.055) / 1.055, 2.4) : g / 12.92;
        b = (b > 0.04045) ? Math.pow((b + 0.055) / 1.055, 2.4) : b / 12.92;
        double x = r * 0.4124564 + g * 0.3575761 + b * 0.1804375;
        double y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750;
        double z = r * 0.0193339 + g * 0.1191920 + b * 0.9503041;
        return new double[]{x, y, z};
    }

    public static double[] xyzToLab(double[] xyz) {
        double fx = f(xyz[0] / 0.95047);
        double fy = f(xyz[1] / 1.0);
        double fz = f(xyz[2] / 1.08883);
        return new double[]{116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz)};
    }

    private static double f(double t) {
        return t > 216.0 / 24389.0 ? Math.cbrt(t) : (24389.0 / 27.0 * t + 16.0) / 116.0;
    }

    public static double deltaE2000Lab(double l1, double a1, double b1, double l2, double a2, double b2) {
        double c1 = Math.sqrt(a1 * a1 + b1 * b1);
        double c2 = Math.sqrt(a2 * a2 + b2 * b2);
        double cbar = (c1 + c2) / 2.0;
        double g = 0.5 * (1 - Math.sqrt(Math.pow(cbar, 7) / (Math.pow(cbar, 7) + Math.pow(25, 7))));
        double a1p = (1 + g) * a1;
        double a2p = (1 + g) * a2;
        double c1p = Math.sqrt(a1p * a1p + b1 * b1);
        double c2p = Math.sqrt(a2p * a2p + b2 * b2);
        double h1p = Math.toDegrees(Math.atan2(b1, a1p)); if (h1p < 0) h1p += 360;
        double h2p = Math.toDegrees(Math.atan2(b2, a2p)); if (h2p < 0) h2p += 360;
        double dlp = l2 - l1;
        double dcp = c2p - c1p;
        double dhp;
        if (c1p * c2p == 0) {
            dhp = 0;
        } else {
            double diff = h2p - h1p;
            if (diff <= 180 && diff >= -180) dhp = diff;
            else if (diff > 180) dhp = diff - 360;
            else dhp = diff + 360;
        }
        double dHp = 2 * Math.sqrt(c1p * c2p) * Math.sin(Math.toRadians(dhp) / 2.0);
        double lbarp = (l1 + l2) / 2.0;
        double cbarp = (c1p + c2p) / 2.0;
        double hbarp;
        if (c1p * c2p == 0) hbarp = h1p + h2p;
        else {
            double s = Math.abs(h1p - h2p);
            if (s <= 180) hbarp = (h1p + h2p) / 2.0;
            else if (s > 180 && (h1p + h2p) < 360) hbarp = (h1p + h2p + 360) / 2.0;
            else hbarp = (h1p + h2p - 360) / 2.0;
        }
        double t = 1 - 0.17 * Math.cos(Math.toRadians(hbarp - 30))
                + 0.24 * Math.cos(Math.toRadians(2 * hbarp))
                + 0.32 * Math.cos(Math.toRadians(3 * hbarp + 6))
                - 0.20 * Math.cos(Math.toRadians(4 * hbarp - 63));
        double sl = 1 + (0.015 * Math.pow(lbarp - 50, 2)) / Math.sqrt(20 + Math.pow(lbarp - 50, 2));
        double sc = 1 + 0.045 * cbarp;
        double sh = 1 + 0.015 * cbarp * t;
        double rt = -2 * Math.sqrt(Math.pow(cbarp, 7) / (Math.pow(cbarp, 7) + Math.pow(25, 7)))
                * Math.sin(Math.toRadians(60 * Math.exp(-Math.pow((hbarp - 275) / 25, 2))));
        double dlpKlSl = dlp / sl;
        double dcpKcSc = dcp / sc;
        double dHpKhSh = dHp / sh;
        return Math.sqrt(dlpKlSl * dlpKlSl + dcpKcSc * dcpKcSc + dHpKhSh * dHpKhSh
                + rt * dcpKcSc * dHpKhSh);
    }

    /** HSV(h0-360, s0-1, v0-1) -> 0xRRGGBB */
    public static int hsvToRgb(double h, double s, double v) {
        double c = v * s;
        double hp = h / 60.0;
        double x = c * (1 - Math.abs(hp % 2 - 1));
        double r = 0, g = 0, b = 0;
        if (hp < 1) { r = c; g = x; }
        else if (hp < 2) { r = x; g = c; }
        else if (hp < 3) { g = c; b = x; }
        else if (hp < 4) { g = x; b = c; }
        else if (hp < 5) { r = x; b = c; }
        else { r = c; b = x; }
        double m = v - c;
        return ((int) Math.round((r + m) * 255) << 16)
             | ((int) Math.round((g + m) * 255) << 8)
             | (int) Math.round((b + m) * 255);
    }

    /** 0xRRGGBB -> {h, s, v} */
    public static double[] rgbToHsv(int rgb) {
        double r = ((rgb >> 16) & 0xFF) / 255.0;
        double g = ((rgb >> 8) & 0xFF) / 255.0;
        double b = (rgb & 0xFF) / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double d = max - min;
        double h;
        if (d == 0) h = 0;
        else if (max == r) h = 60 * (((g - b) / d) % 6);
        else if (max == g) h = 60 * ((b - r) / d + 2);
        else h = 60 * ((r - g) / d + 4);
        if (h < 0) h += 360;
        double s = max == 0 ? 0 : d / max;
        return new double[]{h, s, max};
    }

    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    /**
     * 根据 RGB 判断中文颜色名称。
     * 返回：红/橙/黄/绿/青/蓝/紫/粉/棕/灰/黑/白
     */
    public static String colorName(int rgb) {
        double[] hsv = rgbToHsv(rgb);
        double h = hsv[0], s = hsv[1], v = hsv[2];

        // 黑色：明度极低
        if (v < 0.12) return "黑";
        // 白色：明度极高且饱和度极低
        if (v > 0.88 && s < 0.12) return "白";
        // 灰色：饱和度极低（中等明度）
        if (s < 0.12) return "灰";

        // 棕色：橙/红色系且明度偏低
        if ((h >= 15 && h < 45) && v < 0.55) return "棕";
        // 深红/暗红色也算棕色
        if ((h >= 345 || h < 15) && v < 0.45 && s > 0.5) return "棕";

        // 按色相判断
        if (h >= 345 || h < 15) return "红";
        if (h >= 15 && h < 45) return "橙";
        if (h >= 45 && h < 75) return "黄";
        if (h >= 75 && h < 165) return "绿";
        if (h >= 165 && h < 195) return "青";
        if (h >= 195 && h < 255) return "蓝";
        if (h >= 255 && h < 285) return "紫";
        if (h >= 285 && h < 345) return "粉";

        return "灰";
    }

    private ColorMath() {}
}
