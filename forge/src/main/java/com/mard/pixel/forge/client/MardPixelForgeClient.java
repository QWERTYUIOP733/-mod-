package com.mard.pixel.forge.client;

import com.mard.pixel.forge.MardBlock;
import com.mard.pixel.forge.MardCustomBlockEntity;
import com.mard.pixel.forge.MardPixelForge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MardPixelForge.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MardPixelForgeClient {

    public static final KeyMapping OPEN_KEY = new KeyMapping(
            "key.mard_pixel.open", GLFW.GLFW_KEY_G, "key.categories.mard_pixel");

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KEY);
    }

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        // 使用 MARD_BLOCK_REFS（构造函数中已填充）而非 MARD_BLOCKS（onCommonSetup才填充）
        Block[] arr = MardPixelForge.MARD_BLOCK_REFS.stream()
                .map(ro -> ro.get())
                .toArray(Block[]::new);
        event.getBlockColors().register((state, level, pos, tint) -> {
            if (level != null && pos != null && level.getBlockEntity(pos) instanceof MardCustomBlockEntity mbe) {
                return mbe.getColor();
            }
            return state.getBlock() instanceof MardBlock mb ? mb.rgb() : 0xFFFFFF;
        }, arr);
        event.getBlockColors().register((state, level, pos, tint) -> {
            if (level != null && pos != null && level.getBlockEntity(pos) instanceof MardCustomBlockEntity mbe) {
                return mbe.getColor();
            }
            return 0xFFFFFF;
        }, MardPixelForge.CUSTOM_BLOCK.get());
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        // 使用 MARD_BLOCK_REFS 而非 MARD_BLOCKS
        for (var ro : MardPixelForge.MARD_BLOCK_REFS) {
            Block b = ro.get();
            if (b instanceof MardBlock mb) {
                event.getItemColors().register((stack, tint) -> mb.rgb(), mb);
            }
        }
        event.getItemColors().register((stack, tint) -> {
            CompoundTag tag = stack.getTag();
            return (tag != null && tag.contains("mard_color")) ? tag.getInt("mard_color") & 0xFFFFFF : 0xFFFFFF;
        }, MardPixelForge.CUSTOM_ITEM.get());
    }

    @Mod.EventBusSubscriber(modid = MardPixelForge.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onTick(TickEvent.ClientTickEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (OPEN_KEY.consumeClick() && mc.player != null) {
                mc.setScreen(new MardColorScreen());
            }
        }

        /**
         * 使用 RenderTooltipEvent.Pre（渲染前最后一刻）清除其他模组添加的蓝色字符。
         * 比 ItemTooltipEvent 更底层，执行更晚，能彻底清除 JEI/WTHIT 等模组添加的信息。
         */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onRenderTooltipPre(RenderTooltipEvent.Pre event) {
            // 只处理本模组的物品（MARD色块和自定义色块）
            var stack = event.getItemStack();
            var item = stack.getItem();
            boolean isMardItem = item instanceof com.mard.pixel.forge.MardBlockItem;
            boolean isCustomItem = item == MardPixelForge.CUSTOM_ITEM.get();
            if (!isMardItem && !isCustomItem) return;

            // 获取 tooltip 组件列表（可修改）
            var components = event.getComponents();
            if (components == null || components.size() <= 1) return;

            // 从后往前移除，避免索引问题
            // 只保留第一行（物品名称）和包含"RGB"的行（RGB值）
            for (int i = components.size() - 1; i >= 1; i--) {
                String text = components.get(i).getString();
                if (!text.contains("RGB")) {
                    components.remove(i);
                }
            }
        }
    }
}
