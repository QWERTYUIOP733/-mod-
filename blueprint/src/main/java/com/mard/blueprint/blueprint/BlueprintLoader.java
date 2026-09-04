package com.mard.blueprint.blueprint;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 图纸加载器。
 * 从图片文件（PNG/JPG）加载拼豆图纸，自动映射到MARD颜色。
 */
public class BlueprintLoader {

    /**
     * 从图片文件加载图纸。
     *
     * @param imageFile 图片文件
     * @param maxWidth  最大宽度（超过则缩放）
     * @param maxHeight 最大高度（超过则缩放）
     * @return 加载后的图纸对象
     * @throws IOException 读取图片失败时抛出
     */
    public static Blueprint loadFromImage(File imageFile, int maxWidth, int maxHeight) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("无法读取图片文件: " + imageFile.getName());
        }

        // 缩放图片到最大尺寸
        int width = image.getWidth();
        int height = image.getHeight();

        if (width > maxWidth || height > maxHeight) {
            double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
            int newWidth = (int) (width * scale);
            int newHeight = (int) (height * scale);
            image = resizeImage(image, newWidth, newHeight);
            width = newWidth;
            height = newHeight;
        }

        String name = imageFile.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }

        Blueprint blueprint = new Blueprint(name, width, height);

        // 逐像素映射颜色
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                int rgb = argb & 0xFFFFFF;

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                blueprint.setOriginalColor(x, y, new int[]{r, g, b});

                // 跳过透明像素
                if (alpha < 128) {
                    blueprint.setColorCode(x, y, null);
                    continue;
                }

                // 映射到MARD颜色
                String code = ColorMapper.mapToMard(rgb);
                blueprint.setColorCode(x, y, code);
            }
        }

        return blueprint;
    }

    /**
     * 缩放图片。
     */
    private static BufferedImage resizeImage(BufferedImage original, int newWidth, int newHeight) {
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return resized;
    }

    /**
     * 获取指定目录下的所有图片文件。
     */
    public static File[] listImageFiles(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return new File[0];
        }
        return directory.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
        });
    }
}
