package com.mard.pixel.forge;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.ChatFormatting;

import javax.annotation.Nullable;
import java.util.List;

/**
 * MARD 色块物品类，支持两行显示：
 * 第一行：色号（如"A1"）
 * 第二行：RGB值（如"#FF0000"）
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // 第二行：RGB值
        String hex = String.format("#%06X", rgb & 0xFFFFFF);
        tooltip.add(Component.literal(hex).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Component getName(ItemStack stack) {
        // 第一行：色号
        return Component.literal(code);
    }
}
