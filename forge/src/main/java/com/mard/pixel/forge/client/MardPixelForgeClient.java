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
        Block[] arr = MardPixelForge.MARD_BLOCKS.toArray(new Block[0]);
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
        for (MardBlock mb : MardPixelForge.MARD_BLOCKS) {
            event.getItemColors().register((stack, tint) -> mb.rgb(), mb);
        }
        event.getItemColors().register((stack, tint) -> {
            CompoundTag tag = stack.getTag();
            return (tag != null && tag.contains("mard_color")) ? tag.getInt("mard_color") & 0xFFFFFF : 0xFFFFFF;
        }, MardPixelForge.CUSTOM_ITEM.get());
    }

    @Mod.EventBusSubscriber(modid = MardPixelForge.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onTick(TickEvent.ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (OPEN_KEY.consumeClick() && mc.player != null) {
                mc.setScreen(new MardColorScreen());
            }
        }
    }
}
