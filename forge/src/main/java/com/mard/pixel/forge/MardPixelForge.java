package com.mard.pixel.forge;

import com.mard.pixel.common.ColorMath;
import com.mard.pixel.common.MardColor;
import com.mard.pixel.common.MardPalette;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * MARD Pixel Mod 主类。
 *
 * 核心功能：
 * 1. MARD 221 色基础色块（程序染色，色标准确）
 * 2. 物品两行名称（色号编号 + RGB值）
 * 3. 按系列分类的创造模式标签页
 * 4. 快速物品检索（/mardp give / find）
 * 5. 颜色选取UI（快捷键G）
 * 6. 输入色号快速获取（支持批量输入）
 */
@Mod(MardPixelForge.MODID)
public class MardPixelForge {
    public static final String MODID = "mard_pixel";

    // ==================== 注册器 ====================
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    // ==================== 色块引用 ====================
    /** 构造函数中填充的方块引用（用于颜色处理器注册） */
    public static final List<RegistryObject<Block>> MARD_BLOCK_REFS = new ArrayList<>();
    /** 色号 -> 方块 的快速查找缓存 */
    public static final java.util.Map<String, MardBlock> MARD_BLOCK_MAP = new java.util.HashMap<>();
    /** 游戏运行时填充的方块列表（用于标签页显示） */
    public static final List<MardBlock> MARD_BLOCKS = new ArrayList<>();

    // ==================== 通用材料物品 ====================
    /** MARD颜料：通用合成材料，任意染料可合成，用于合成各色块 */
    public static final RegistryObject<Item> MARD_PIGMENT = ITEMS.register("mard_pigment",
            () -> new Item(new Item.Properties()));

    // ==================== MARD 合成台 ====================
    /** MARD合成台方块：功能类似原版工作台，但只能合成模组内物品 */
    public static final RegistryObject<Block> MARD_CRAFTING_TABLE = BLOCKS.register("mard_crafting_table",
            MardCraftingTable::new);
    public static final RegistryObject<Item> MARD_CRAFTING_TABLE_ITEM = ITEMS.register("mard_crafting_table",
            () -> new BlockItem(MARD_CRAFTING_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<MardCraftingTableBlockEntity>> MARD_CRAFTING_TABLE_BE =
            BLOCK_ENTITIES.register("mard_crafting_table",
                    () -> BlockEntityType.Builder.of(MardCraftingTableBlockEntity::new,
                            MARD_CRAFTING_TABLE.get()).build(null));
    public static final RegistryObject<MenuType<MardCraftingMenu>> MARD_CRAFTING_MENU =
            MENUS.register("mard_crafting_menu",
                    () -> new MenuType<>(MardCraftingMenu::new,
                            net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public MardPixelForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册所有注册器
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);

        // 注册 MARD 基础色块
        registerMardBlocks();

        // 注册创造模式标签页（按系列分类）
        registerCreativeTabs();

        // 注册 Forge 事件总线
        MinecraftForge.EVENT_BUS.register(this);
        modBus.addListener(this::onCommonSetup);

        // 初始化网络
        MardNetwork.init();
    }

    // ==================== 注册逻辑 ====================

    /**
     * 注册 MARD 色块（方块 + 物品）。
     * 所有颜色统一使用 MardBlock。
     * 每个色块使用程序染色（tintindex），色值来自 MardPalette。
     */
    private void registerMardBlocks() {
        for (MardColor mc : MardPalette.COLORS) {
            String blockName = mc.blockName();
            RegistryObject<Block> blockRef = BLOCKS.register(blockName,
                    () -> new MardBlock(mc.code(), mc.rgb()));
            ITEMS.register(blockName,
                    () -> new MardBlockItem(blockRef.get(), mc.code(), mc.rgb(), new Item.Properties()));
            MARD_BLOCK_REFS.add(blockRef);
        }
    }

    /**
     * 注册创造模式标签页。
     * 按系列（字母）分类的子标签页，名称仅为字母（A/B/C/.../ZG）。
     */
    private void registerCreativeTabs() {
        // 收集所有系列（去重并排序）
        Set<String> seriesSet = new java.util.LinkedHashSet<>();
        for (MardColor mc : MardPalette.COLORS) {
            seriesSet.add(mc.series());
        }
        List<String> seriesList = new ArrayList<>(seriesSet);
        java.util.Collections.sort(seriesList);

        // 按系列分类的子标签页，名称仅为字母
        for (int i = 0; i < seriesList.size(); i++) {
            final String s = seriesList.get(i);
            final boolean isFirst = (i == 0);
            CREATIVE_TABS.register("mard_pixel_" + s.toLowerCase(), () -> CreativeModeTab.builder()
                    .title(Component.literal(s))
                    .icon(() -> findFirstBlockOfSeries(s))
                    .displayItems((params, output) -> {
                        // 第一个标签页添加MARD颜料（通用合成材料）和合成台
                        if (isFirst) {
                            output.accept(new ItemStack(MARD_PIGMENT.get()));
                            output.accept(new ItemStack(MARD_CRAFTING_TABLE.get()));
                        }
                        for (MardColor mc : MardPalette.COLORS) {
                            if (mc.series().equals(s)) {
                                for (MardBlock mb : MARD_BLOCKS) {
                                    if (mb.code().equalsIgnoreCase(mc.code())) {
                                        output.accept(new ItemStack(mb));
                                        break;
                                    }
                                }
                            }
                        }
                    })
                    .build());
        }
    }

    /**
     * 查找指定系列的第一个色块，用作标签页图标。
     */
    private ItemStack findFirstBlockOfSeries(String series) {
        for (MardColor mc : MardPalette.COLORS) {
            if (mc.series().equals(series)) {
                for (MardBlock mb : MARD_BLOCKS) {
                    if (mb.code().equalsIgnoreCase(mc.code())) {
                        return new ItemStack(mb);
                    }
                }
            }
        }
        // 回退：返回第一个MARD色块
        if (!MARD_BLOCKS.isEmpty()) {
            return new ItemStack(MARD_BLOCKS.get(0));
        }
        return ItemStack.EMPTY;
    }

    // ==================== 生命周期事件 ====================

    private void onCommonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        MARD_BLOCKS.clear();
        MARD_BLOCK_MAP.clear();
        for (RegistryObject<Block> ro : MARD_BLOCK_REFS) {
            Block b = ro.get();
            if (b instanceof MardBlock mb) {
                MARD_BLOCKS.add(mb);
                MARD_BLOCK_MAP.put(mb.code().toUpperCase(), mb);
            }
        }
    }

    // ==================== 命令注册 ====================

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("mardp")
                        // 查找最近的 MARD 色
                        .then(Commands.literal("find")
                                .then(Commands.argument("hex", net.minecraft.commands.arguments.ColorArgument.color())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            ChatFormatting cf = net.minecraft.commands.arguments.ColorArgument.getColor(ctx, "hex");
                                            int rgb = cf.getColor() != null ? cf.getColor() : 0xFFFFFF;
                                            MardColor nearest = MardPalette.nearest(rgb);
                                            p.sendSystemMessage(Component.literal("最近 MARD 色："
                                                    + nearest.code() + " " + ColorMath.toHex(nearest.rgb())));
                                            return 1;
                                        })))
                        // 快速给予物品（MARD:<色号>）
                        .then(Commands.literal("give")
                                .then(Commands.argument("target", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            giveRequestedItem(p, StringArgumentType.getString(ctx, "target"));
                                            return 1;
                                        }))));
    }

    // ==================== 物品生成 ====================

    /**
     * 根据目标字符串生成物品栈。
     * 支持格式：
     * - MARD:<色号> - MARD基础色块
     * - <色号> - 直接按MARD色号查找
     */
    public static ItemStack buildStack(String target) {
        if (target == null) return ItemStack.EMPTY;
        String t = target.trim();
        if (t.isEmpty()) return ItemStack.EMPTY;

        if (t.startsWith("MARD:")) {
            return buildMardStack(t.substring(5).trim());
        }

        // 直接按MARD色号查找
        return buildMardStack(t);
    }

    private static ItemStack buildMardStack(String code) {
        if (code == null || code.isBlank()) return ItemStack.EMPTY;
        String key = code.trim().toUpperCase();
        // 优先使用 Map 缓存（O(1) 查找）
        MardBlock mb = MARD_BLOCK_MAP.get(key);
        if (mb != null) return new ItemStack(mb);
        // 缓存未命中时回退到遍历列表（兼容时序问题）
        MardColor mc = MardPalette.byCode(code);
        if (mc == null) return ItemStack.EMPTY;
        for (MardBlock block : MARD_BLOCKS) {
            if (block.code().equalsIgnoreCase(mc.code())) {
                MARD_BLOCK_MAP.put(block.code().toUpperCase(), block); // 回填缓存
                return new ItemStack(block);
            }
        }
        return ItemStack.EMPTY;
    }

    // ==================== 给予物品 ====================

    /**
     * 快速给予指定物品（/mardp give 命令），给予1个。
     */
    public static void giveRequestedItem(ServerPlayer player, String target) {
        if (target == null || target.isBlank()) {
            player.sendSystemMessage(Component.literal("用法：MARD:<色号> 或直接输入色号").withStyle(ChatFormatting.GRAY));
            return;
        }
        ItemStack stack = buildStack(target);
        if (stack == null || stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("无法生成物品：" + target).withStyle(ChatFormatting.RED));
            return;
        }
        player.getInventory().add(stack);
        player.sendSystemMessage(Component.literal("已给予 ").append(stack.getHoverName()).append(" x" + stack.getCount()));
    }

    /**
     * UI 点击色块时给予一组（64个）对应颜色的方块。
     * 作为快捷标签页的快捷键使用。
     */
    public static void giveRequestedStack(ServerPlayer player, String target) {
        if (target == null || target.isBlank()) return;
        ItemStack stack = buildStack(target);
        if (stack == null || stack.isEmpty()) return;
        stack.setCount(64); // 给予一组（64个）
        player.getInventory().add(stack);
        player.sendSystemMessage(Component.literal("已给予一组 ").append(stack.getHoverName()));
    }

    /**
     * 输入色号后将一组（64个）对应颜色的方块放入快捷栏（0-8格）。
     * 若快捷栏已满则放入背包，背包也满则扔到地面。
     */
    public static void giveToHotbar(ServerPlayer player, String code) {
        if (code == null || code.isBlank()) {
            player.sendSystemMessage(Component.literal("请输入色号").withStyle(ChatFormatting.RED));
            return;
        }
        String target = "MARD:" + code.toUpperCase().trim();
        ItemStack stack = buildStack(target);
        if (stack == null || stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("色号不存在: " + code).withStyle(ChatFormatting.RED));
            return;
        }
        stack.setCount(64);

        // 优先放入快捷栏（0-8格）
        Inventory inv = player.getInventory();
        boolean placed = false;
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, stack);
                placed = true;
                break;
            }
        }
        // 快捷栏满则放入背包
        if (!placed) {
            placed = inv.add(stack);
        }
        // 背包也满则扔到地面
        if (!placed) {
            player.drop(stack, false);
        }
        player.sendSystemMessage(Component.literal("已放入快捷栏一组 ").append(stack.getHoverName()));
    }

    /**
     * 使用七彩粉末合成色块（UI合成模式）。
     * 检查玩家背包中是否有七彩粉末，有则消耗1个，给予64个对应色块。
     */
    public static void craftWithPigment(ServerPlayer player, String code) {
        if (code == null || code.isBlank()) {
            player.sendSystemMessage(Component.literal("色号无效").withStyle(ChatFormatting.RED));
            return;
        }

        Inventory inv = player.getInventory();
        boolean hasPigment = false;
        int pigmentSlot = -1;

        // 检查背包中是否有七彩粉末
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack slotStack = inv.getItem(i);
            if (!slotStack.isEmpty() && slotStack.getItem() == MARD_PIGMENT.get()) {
                hasPigment = true;
                pigmentSlot = i;
                break;
            }
        }

        if (!hasPigment) {
            player.sendSystemMessage(Component.literal("背包中没有七彩粉末，无法合成").withStyle(ChatFormatting.RED));
            return;
        }

        // 生成对应色块
        String target = "MARD:" + code.toUpperCase().trim();
        ItemStack stack = buildStack(target);
        if (stack == null || stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("色号不存在: " + code).withStyle(ChatFormatting.RED));
            return;
        }
        stack.setCount(64);

        // 消耗1个七彩粉末
        ItemStack pigmentStack = inv.getItem(pigmentSlot);
        pigmentStack.shrink(1);
        if (pigmentStack.isEmpty()) {
            inv.setItem(pigmentSlot, ItemStack.EMPTY);
        }

        // 给予色块
        boolean placed = inv.add(stack);
        if (!placed) {
            player.drop(stack, false);
        }

        player.sendSystemMessage(Component.literal("消耗1个七彩粉末，合成一组 ")
                .append(stack.getHoverName()));
    }

    // ==================== 工具方法 ====================

    /**
     * 获取物品的颜色值（MARD基础色块）。
     * @return 颜色RGB值，或-1（非本模组物品）
     */
    public static int colorOf(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        Item item = stack.getItem();
        if (item instanceof MardBlockItem bi && bi.getBlock() instanceof MardBlock mb) return mb.rgb();
        return -1;
    }
}
