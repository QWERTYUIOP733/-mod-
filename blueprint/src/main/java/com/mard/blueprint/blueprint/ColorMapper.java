package com.mard.blueprint.blueprint;

/**
 * 颜色映射器。
 * 将任意RGB颜色映射到最近的MARD 221色号。
 * 使用CIEDE2000色差算法进行颜色匹配。
 */
public class ColorMapper {

    /**
     * MARD 221色数据（code, rgb）。
     * 实际使用时从主模组的MardPalette获取，这里预留接口。
     */
    private static String[] mardCodes;
    private static int[] mardRgbs;
    private static boolean initialized = false;

    /**
     * 初始化MARD颜色数据。
     * 从主模组的颜色注册表中获取221色数据。
     */
    public static void init(String[] codes, int[] rgbs) {
        mardCodes = codes;
        mardRgbs = rgbs;
        initialized = true;
    }

    /**
     * 将RGB颜色映射到最近的MARD色号。
     *
     * @param rgb RGB颜色值（0xRRGGBB）
     * @return 最近的MARD色号，如果未初始化则返回null
     */
    public static String mapToMard(int rgb) {
        if (!initialized || mardCodes == null || mardRgbs.length == 0) {
            return null;
        }

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        // 跳过纯白和纯黑（通常是背景）
        if (r > 250 && g > 250 && b > 250) return null;
        if (r < 5 && g < 5 && b < 5) return null;

        String bestCode = null;
        double bestDelta = Double.MAX_VALUE;

        for (int i = 0; i < mardRgbs.length; i++) {
            int mr = (mardRgbs[i] >> 16) & 0xFF;
            int mg = (mardRgbs[i] >> 8) & 0xFF;
            int mb = mardRgbs[i] & 0xFF;

            double delta = deltaE2000(r, g, b, mr, mg, mb);
            if (delta < bestDelta) {
                bestDelta = delta;
                bestCode = mardCodes[i];
            }
        }

        // 色差过大时返回null（可能是透明或特殊颜色）
        if (bestDelta > 50) return null;

        return bestCode;
    }

    /**
     * CIEDE2000色差算法（简化版）。
     * 实际使用时可以调用主模组的ColorMath.deltaE2000方法。
     */
    private static double deltaE2000(int r1, int g1, int b1, int r2, int g2, int b2) {
        // 简化为RGB欧氏距离，实际应使用Lab色彩空间
        double dr = r1 - r2;
        double dg = g1 - g2;
        double db = b1 - b2;
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
