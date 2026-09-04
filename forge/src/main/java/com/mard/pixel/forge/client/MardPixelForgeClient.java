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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
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

        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            // 只处理本模组的物品（MARD色块和自定义色块）
            var item = event.getItemStack().getItem();
            boolean isMardItem = item instanceof com.mard.pixel.forge.MardBlockItem;
            boolean isCustomItem = item == MardPixelForge.CUSTOM_ITEM.get();
            if (!isMardItem && !isCustomItem) return;

            // 移除其他模组添加的额外信息行（模组名称、标签页名称等）
            // 保留第一行（物品名称，包含色号和RGB值）
            var tooltip = event.getToolTip();
            // 从后往前移除，避免索引问题
            for (int i = tooltip.size() - 1; i >= 1; i--) {
                String text = tooltip.get(i).getString();
                // 移除包含"像素色块"、"系列"、"MARD"等关键词的额外信息行
                if (text.contains("像素色块") || text.contains("系列") || text.contains("MARD ")) {
                    tooltip.remove(i);
                }
            }
        }
    }
}
