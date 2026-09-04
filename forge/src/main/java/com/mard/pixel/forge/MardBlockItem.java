package com.mard.pixel.forge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * MARD 色块物品类，两行显示：
 * 第一行（物品名称）：色号编号（如"A1"），白色
 * 第二行（物品名称）：RGB值（如"#FF0000"），灰色
 * 两行都在物品名称中通过换行符分隔，背包和物品栏均显示
 */
public class MardBlockItem extends BlockItem {
    private final int rgb;
    private final String code;

    public MardBlockItem(Block block, String code, int rgb, Properties properties) {
        super(block, properties);
        this.code = code;
        this.rgb = rgb;
    }

    public int rgb() { return rgb; }
    public String code() { return code; }

    @Override
    public Component getName(ItemStack stack) {
        // 两行都在物品名称中：第一行白色色号（居中），第二行灰色RGB值
        // 使用换行符分隔，背包和物品栏均显示两行
        String hex = String.format("#%06X", rgb & 0xFFFFFF);
        // 编号居中：根据编号长度动态添加前导空格
        String centered = centerCode(code);
        return Component.literal(centered)
                .append(Component.literal("\n"))
                .append(Component.literal("RGB " + hex).withStyle(ChatFormatting.GRAY));
    }

    /**
     * 根据编号长度动态添加前导空格，实现居中效果。
     * Minecraft字体中，数字和字母宽度约为6像素，空格宽度为4像素。
     */
    private static String centerCode(String code) {
        if (code == null || code.isEmpty()) return code;
        // 假设最大编号宽度为4个字符（如"ZG12"），短编号前面加空格居中
        int targetWidth = 4;
        int len = code.length();
        if (len >= targetWidth) return code;
        int spaces = (targetWidth - len) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spaces; i++) sb.append(' ');
        sb.append(code);
        return sb.toString();
    }
}
