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

package com.mard.pixel.forge.client;

import com.mard.pixel.forge.MardBlock;
import com.mard.pixel.forge.MardBlockItem;
import com.mard.pixel.forge.MardPixelForge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

/**
 * Color Blocks Mod 客户端主类。
 *
 * 职责分离：
 * 1. 按键注册与处理 - 打开取色器UI
 * 2. 方块/物品颜色注册 - 程序染色（tintindex）
 * 3. Tooltip 清理 - 移除其他模组添加的额外信息行
 *
 * 26.3：@SubscribeEvent/Priority 迁到 eventbus.api.listener 包；KeyMapping 第三参改 Category；
 * 方块颜色注册改 event.register(List<BlockTintSource>, Block...)；屏幕注册在 enqueueWork 内；
 * setScreen 改 setScreenAndShow。
 */
@Mod.EventBusSubscriber(modid = MardPixelForge.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MardPixelForgeClient {

    // ==================== 常量定义 ====================

    /** 打开取色器UI的快捷键（默认G键） */
    public static final KeyMapping OPEN_COLOR_PICKER_KEY = new KeyMapping(
            "key.mard_pixel.open",
            InputConstants.KEY_G,
            KeyMapping.Category.MISC
    );

    /** Tooltip 中 RGB 值行的标识关键词 */
    private static final String RGB_IDENTIFIER = "RGB";

    // ==================== 客户端游戏侧事件订阅 ====================
    // 26.3：RegisterKeyMappingsEvent / RegisterColorHandlersEvent.Block 均不实现 IModBusEvent，
    // 不能放进 bus=MOD 的 @EventBusSubscriber 自动扫描（否则启动期 BusGroup 抛 IllegalArgumentException）。
    // 二者各自带静态 BUS，由主类构造器经 DistExecutor 在物理客户端调用本方法、于事件触发前订阅。

    /**
     * 物理客户端一次性订阅按键注册与方块颜色注册（由主类构造器 DistExecutor 调用）。
     */
    public static void registerClientEvents() {
        RegisterKeyMappingsEvent.BUS.addListener(MardPixelForgeClient::registerKeyMappings);
        RegisterColorHandlersEvent.Block.BUS.addListener(MardPixelForgeClient::registerBlockColors);
    }

    /**
     * 注册快捷键映射。
     */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_COLOR_PICKER_KEY);
    }

    /**
     * 客户端设置：注册合成台屏幕（需在 enqueueWork 内执行）。
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(MardPixelForge._CRAFTING_MENU.get(), MardCraftingScreen::new);
            // 26.3：ClientTickEvent 拆为 Pre/Post 两个 record，需显式向 Post.BUS 订阅
            TickEvent.ClientTickEvent.Post.BUS.addListener(ForgeBusEvents::onClientTick);
        });
    }

    /**
     * 注册方块颜色处理器（tintindex:0）。
     * 26.3：签名为 register(List<BlockTintSource>, Block...)；经静态 BUS 订阅。
     */
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        Block[] mardBlocks = MardPixelForge._BLOCK_REFS.stream()
                .map(ro -> ro.get())
                .toArray(Block[]::new);

        BlockTintSource tint = state ->
                state.getBlock() instanceof MardBlock mb ? mb.rgb() : 0xFFFFFF;
        event.register(List.of(tint), mardBlocks);
    }

    // ==================== FORGE Bus 事件 ====================

    /**
     * FORGE bus 事件订阅者：游戏运行时事件（tick、tooltip 等）。
     */
    @Mod.EventBusSubscriber(modid = MardPixelForge.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBusEvents {

        /**
         * 客户端 tick（Post）：检测快捷键按下，按下 G 键打开取色器 UI。
         * 26.3 起由 onClientSetup 显式向 TickEvent.ClientTickEvent.Post.BUS 订阅。
         */
        public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (OPEN_COLOR_PICKER_KEY.consumeClick() && mc.player != null && mc.gui.screen() == null) {
                mc.setScreenAndShow(new MardColorScreen());
            }
        }

        /**
         * 物品 Tooltip 事件：清理其他模组添加的额外信息行，只保留名称与 RGB 行。
         */
        @SubscribeEvent(priority = Priority.LOWEST)
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (stack == null || stack.isEmpty()) return;

            if (!isMardModItem(stack)) return;

            List<Component> tooltip = event.getToolTip();
            if (tooltip == null || tooltip.size() <= 1) return;

            // 从后往前移除，避免索引偏移
            for (int i = tooltip.size() - 1; i >= 1; i--) {
                Component line = tooltip.get(i);
                String text = line != null ? line.getString() : "";
                if (!text.contains(RGB_IDENTIFIER)) {
                    tooltip.remove(i);
                }
            }
        }

        private static boolean isMardModItem(ItemStack stack) {
            return stack.getItem() instanceof MardBlockItem;
        }
    }

    private MardPixelForgeClient() {}
}
