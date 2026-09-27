/*
 * Copyright (c) 2026 Color Blocks Extension
 * SPDX-License-Identifier: MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.mard.pixel.forge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 色块物品类，两行显示：
 * 第一行（物品名称 hoverName）：色号编号（如"A1"），白色
 * 第二行（lore）：RGB值（如"RGB #FF0000"），灰色
 * 使用 Minecraft 标准的 hoverName + lore 方式，避免换行符导致乱码
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
        // 第一行：色号编号（白色）
        return Component.literal(code);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // 不调用 super，避免添加额外信息
        // 第二行：RGB值（灰色），格式 "RGB #FF0000"
        String hex = String.format("#%06X", rgb & 0xFFFFFF);
        tooltip.add(Component.literal("RGB " + hex).withStyle(ChatFormatting.GRAY));
    }
}
