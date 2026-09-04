package com.mard.pixel.forge.client;

import com.mard.pixel.forge.MardBlock;
import com.mard.pixel.forge.MardBlockItem;
import com.mard.pixel.forge.MardCustomBlockEntity;
import com.mard.pixel.forge.MardEffectBlock;
import com.mard.pixel.forge.MardPixelForge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * MARD Pixel Mod 客户端主类。
 *
 * 职责分离：
 * 1. 按键注册与处理 - 打开取色器UI
 * 2. 方块/物品颜色注册 - 程序染色（tintindex）
 * 3. Tooltip 清理 - 移除其他模组添加的额外信息行
 *
 * 注意：所有客户端事件必须注册在 FORGE bus 或 MOD bus 上，
 * 由 @Mod.EventBusSubscriber 注解自动处理。
 */
@Mod.EventBusSubscriber(modid = MardPixelForge.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MardPixelForgeClient {

    // ==================== 常量定义 ====================

    /** 打开取色器UI的快捷键（默认G键） */
    public static final KeyMapping OPEN_COLOR_PICKER_KEY = new KeyMapping(
            "key.mard_pixel.open",
            GLFW.GLFW_KEY_G,
            "key.categories.mard_pixel"
    );

    /** Tooltip 中 RGB 值行的标识关键词 */
    private static final String RGB_IDENTIFIER = "RGB";
    /** Tooltip 中效果类型行的标识关键词 */
    private static final String EFFECT_IDENTIFIER = "效果:";

    // ==================== MOD Bus 事件 ====================

    /**
     * 注册快捷键映射。
     * 触发时机：MOD 初始化阶段，客户端专用。
     */
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_COLOR_PICKER_KEY);
    }

    /**
     * 客户端设置：注册半透明渲染层。
     * 果冻(R)和闪粉(T)方块使用半透明渲染。
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var ro : MardPixelForge.MARD_BLOCK_REFS) {
                Block block = ro.get();
                if (block instanceof MardEffectBlock meb && meb.isTransparent()) {
                    ItemBlockRenderTypes.setRenderLayer(block, RenderType.translucent());
                }
            }
        });
    }

    /**
     * 注册方块颜色处理器。
     * 为 MARD 基础色块和自定义色块设置程序染色（tintindex:0）。
     * 所有颜色（包括特殊效果色）都统一使用基础 rgb 值染色，
     * 确保世界中方块颜色与手中物品颜色完全一致。
     * 特殊效果（珠光/温变/光变/夜光等）通过方块属性和材质体现，
     * 不再通过动态颜色变化体现，避免颜色不一致问题。
     *
     * 关键：使用 MARD_BLOCK_REFS（构造函数中已填充）而非 MARD_BLOCKS
     * （onCommonSetup 才填充），否则注册时列表为空导致染色失效。
     */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        Block[] mardBlocks = MardPixelForge.MARD_BLOCK_REFS.stream()
                .map(ro -> ro.get())
                .toArray(Block[]::new);

        // MARD 色块：统一使用方块本身存储的 rgb 值染色
        event.getBlockColors().register((state, level, pos, tint) -> {
            if (level != null && pos != null
                    && level.getBlockEntity(pos) instanceof MardCustomBlockEntity mbe) {
                return mbe.getColor();
            }
            Block block = state.getBlock();
            // 所有 MARD 色块（包括 MardEffectBlock）统一返回基础 rgb 值
            return block instanceof MardBlock mb ? mb.rgb() : 0xFFFFFF;
        }, mardBlocks);

        // 自定义色块：从 BlockEntity 读取颜色
        event.getBlockColors().register((state, level, pos, tint) -> {
            if (level != null && pos != null
                    && level.getBlockEntity(pos) instanceof MardCustomBlockEntity mbe) {
                return mbe.getColor();
            }
            return 0xFFFFFF;
        }, MardPixelForge.CUSTOM_BLOCK.get());
    }

    /**
     * 注册物品颜色处理器。
     * 物品图标使用与方块相同的染色逻辑，确保手持和物品栏中颜色一致。
     */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // MARD 基础色块：使用方块的 rgb 值
        for (var ro : MardPixelForge.MARD_BLOCK_REFS) {
            Block block = ro.get();
            if (block instanceof MardBlock mb) {
                event.getItemColors().register((stack, tint) -> mb.rgb(), mb);
            }
        }

        // 自定义色块：从 NBT 读取 mard_color
        event.getItemColors().register((stack, tint) -> {
            CompoundTag tag = stack.getTag();
            return (tag != null && tag.contains("mard_color"))
                    ? tag.getInt("mard_color") & 0xFFFFFF
                    : 0xFFFFFF;
        }, MardPixelForge.CUSTOM_ITEM.get());
    }

    // ==================== FORGE Bus 事件 ====================

    /**
     * FORGE bus 事件订阅者。
     * 游戏运行时事件（tick、tooltip 等）必须注册在此 bus 上。
     */
    @Mod.EventBusSubscriber(modid = MardPixelForge.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBusEvents {

        /**
         * 客户端 tick 事件：检测快捷键按下。
         * 按下 G 键时打开取色器 UI。
         */
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (OPEN_COLOR_PICKER_KEY.consumeClick() && mc.player != null) {
                mc.setScreen(new MardColorScreen());
            }
        }

        /**
         * 物品 Tooltip 事件：清理其他模组添加的额外信息行。
         *
         * 问题背景：
         * JEI、WTHIT 等信息类模组会在物品 tooltip 中添加模组名称、
         * 创造模式标签页名称等蓝色/紫色文字，干扰 MARD 色块的
         * "色号 + RGB值" 两行显示格式。
         *
         * 清理策略：
         * 1. 只处理本模组的物品（MARD基础色块 + 自定义色块）
         * 2. 保留第一行（物品名称，即色号编号）
         * 3. 保留包含 "RGB" 关键词的行（RGB值）
         * 4. 移除其他所有行（模组名称、标签页名称等）
         *
         * 优先级：LOWEST - 确保在所有其他模组之后执行，
         * 这样能移除其他模组添加的内容。
         */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (stack == null || stack.isEmpty()) return;

            // 只处理本模组的物品
            if (!isMardModItem(stack)) return;

            List<Component> tooltip = event.getToolTip();
            if (tooltip == null || tooltip.size() <= 1) return;

            // 从后往前移除，避免索引偏移
            for (int i = tooltip.size() - 1; i >= 1; i--) {
                Component line = tooltip.get(i);
                String text = line != null ? line.getString() : "";
                // 保留 RGB 值行和效果类型行
                if (!text.contains(RGB_IDENTIFIER) && !text.contains(EFFECT_IDENTIFIER)) {
                    // 特殊效果色的中文名行也保留（通过判断物品是否为效果色）
                    if (stack.getItem() instanceof MardBlockItem mbi
                            && mbi.getBlock() instanceof MardEffectBlock meb
                            && meb.getNameCn() != null && text.equals(meb.getNameCn())) {
                        continue;
                    }
                    tooltip.remove(i);
                }
            }
        }

        /**
         * 判断物品是否属于本模组。
         * @param stack 物品栈
         * @return true 如果是 MARD 基础色块或自定义色块
         */
        private static boolean isMardModItem(ItemStack stack) {
            var item = stack.getItem();
            return item instanceof MardBlockItem
                    || item == MardPixelForge.CUSTOM_ITEM.get();
        }
    }

    // 私有构造函数，防止实例化
    private MardPixelForgeClient() {}
}
