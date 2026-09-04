package com.mard.pixel.forge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * MARD 色块物品类，支持两行显示：
 * 第一行：白色色号编号（如"A1"）
 * 第二行：RGB值（如"#FF0000"，灰色）
 * 两行直接相邻，中间无空行
 * 在背包和物品栏中均显示两行
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
        // 两行都作为物品名称：第一行白色色号，第二行灰色RGB值
        // 使用换行符分隔，中间无空行，背包和物品栏均显示
        String hex = String.format("#%06X", rgb & 0xFFFFFF);
        return Component.literal(code)
                .append(Component.literal("\n"))
                .append(Component.literal(hex).withStyle(ChatFormatting.GRAY));
    }
}
