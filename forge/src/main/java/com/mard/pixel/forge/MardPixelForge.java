package com.mard.pixel.forge;

import com.mard.pixel.common.ColorMath;
import com.mard.pixel.common.CustomColor;
import com.mard.pixel.common.CustomColorFile;
import com.mard.pixel.common.CustomColorStore;
import com.mard.pixel.common.ImportedPaletteStore;
import com.mard.pixel.common.MardColor;
import com.mard.pixel.common.MardPalette;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * MARD Pixel Mod 主类。
 *
 * 核心功能：
 * 1. MARD 295 色基础色块（程序染色，色标准确）
 * 2. 自定义色号（PS风格取色器，增删管理）
 * 3. 物品两行名称（色号编号 + RGB值）
 * 4. 按系列分类的创造模式标签页
 * 5. PNG色卡导入功能
 * 6. 编号循环回收机制（遗失编号自动回收重用）
 * 7. 快速物品检索（/mardp give）
 *
 * 已移除：品牌色号转换功能（Perler/Hama/Artkal等），改为PNG导入
 */
@Mod(MardPixelForge.MODID)
public class MardPixelForge {
    public static final String MODID = "mard_pixel";

    // ==================== 注册器 ====================
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // ==================== 色块引用 ====================
    /** 构造函数中填充的方块引用（用于颜色处理器注册） */
    public static final List<RegistryObject<Block>> MARD_BLOCK_REFS = new ArrayList<>();
    /** 游戏运行时填充的方块列表（用于标签页显示） */
    public static final List<MardBlock> MARD_BLOCKS = new ArrayList<>();

    // ==================== 自定义方块 ====================
    public static final RegistryObject<MardCustomBlock> CUSTOM_BLOCK =
            BLOCKS.register("mard_custom", MardCustomBlock::new);
    public static final RegistryObject<BlockItem> CUSTOM_ITEM =
            ITEMS.register("mard_custom", () -> new BlockItem(CUSTOM_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<MardCustomBlockEntity>> CUSTOM_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("mard_custom_be",
                    () -> BlockEntityType.Builder.of(MardCustomBlockEntity::new, CUSTOM_BLOCK.get()).build(null));

    // ==================== 配置文件路径 ====================
    public static final Path CUSTOM_COLORS_FILE = FMLPaths.CONFIGDIR.get().resolve("mard_pixel_custom.json");
    public static final Path IMPORTED_PALETTES_DIR = FMLPaths.CONFIGDIR.get().resolve("mard_pixel");

    public MardPixelForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册所有注册器
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        CREATIVE_TABS.register(modBus);

        // 注册 295 个 MARD 基础色块
        registerMardBlocks();

        // 注册创造模式标签页（主标签页 + 按系列分类）
        registerCreativeTabs();

        // 注册 Forge 事件总线
        MinecraftForge.EVENT_BUS.register(this);
        modBus.addListener(this::onCommonSetup);

        // 初始化网络和存储
        MardNetwork.init();
        ImportedPaletteStore.init(IMPORTED_PALETTES_DIR);
    }

    // ==================== 注册逻辑 ====================

    /**
     * 注册 295 个 MARD 基础色块（方块 + 物品）。
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
     * 只保留按系列（字母）分类的子标签页，名称仅为字母（A/B/C/.../ZG）。
     * 自定义色块放在最后一个系列标签页中。
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
            final boolean isLast = (i == seriesList.size() - 1);
            CREATIVE_TABS.register("mard_pixel_" + s.toLowerCase(), () -> CreativeModeTab.builder()
                    .title(Component.literal(s))
                    .icon(() -> findFirstBlockOfSeries(s))
                    .displayItems((params, output) -> {
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
                        // 最后一个标签页添加自定义色块
                        if (isLast) {
                            output.accept(new ItemStack(CUSTOM_ITEM.get()));
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
        return new ItemStack(CUSTOM_ITEM.get());
    }

    // ==================== 生命周期事件 ====================

    private void onCommonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        MARD_BLOCKS.clear();
        for (RegistryObject<Block> ro : MARD_BLOCK_REFS) {
            Block b = ro.get();
            if (b instanceof MardBlock mb) MARD_BLOCKS.add(mb);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            loadCustomColorsFromFile();
            MardNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new MardNetwork.SyncCustomPacket(CustomColorFile.serialize(CustomColorStore.all())));
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
                        // 快速给予物品（MARD:<色号> / CUSTOM:<编号> / IMPORTED:<名称>:<索引> / CUSTOM_RAW:<rgb>）
                        .then(Commands.literal("give")
                                .then(Commands.argument("target", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            giveRequestedItem(p, StringArgumentType.getString(ctx, "target"));
                                            return 1;
                                        })))
                        // 扫描遗失的自定义颜色编号并回收
                        .then(Commands.literal("scan")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    scanCustomItems(p);
                                    return 1;
                                }))
                        // 补全遗失的自定义颜色物品
                        .then(Commands.literal("restore")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    restoreCustomItems(p);
                                    return 1;
                                })));
    }

    // ==================== 自定义色管理 ====================

    public static void loadCustomColorsFromFile() {
        CustomColorStore.loadFrom(CustomColorFile.load(CUSTOM_COLORS_FILE));
    }

    /**
     * 新增自定义颜色。
     * 检测：背包已满（36格全满）则阻止新增并提示。
     * 新增后自动给玩家对应的物品（快捷栏→背包→扔地面）。
     */
    public static void addCustom(ServerPlayer player, String name, int rgb) {
        if (isInventoryFull(player)) {
            player.sendSystemMessage(Component.literal("背包已满，无法新增自定义色块。").withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("请丢弃或移走部分物品后再试，也可将物品存入箱子腾出空间。").withStyle(ChatFormatting.GRAY));
            return;
        }

        CustomColor c = CustomColorStore.add(name, rgb);
        if (c == null) {
            player.sendSystemMessage(Component.literal("自定义色编号已用尽，无法继续新增。").withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("可使用 /mardp scan 扫描并回收遗失编号，或删除不需要的颜色后再试。").withStyle(ChatFormatting.GRAY));
            return;
        }

        CustomColorFile.save(CUSTOM_COLORS_FILE, CustomColorStore.all());
        broadcastCustomColors();
        player.sendSystemMessage(Component.literal("已新增自定义色 " + c.code() + " " + ColorMath.toHex(rgb)));

        // 自动给玩家对应的自定义方块物品
        ItemStack stack = createCustomItemStack(c, rgb, c.displayName());
        giveItemSmart(player, stack);
    }

    public static void removeCustom(ServerPlayer player, String code) {
        boolean ok = CustomColorStore.remove(code);
        if (ok) {
            CustomColorFile.save(CUSTOM_COLORS_FILE, CustomColorStore.all());
            broadcastCustomColors();
            player.sendSystemMessage(Component.literal("已删除自定义色 " + code));
        } else {
            player.sendSystemMessage(Component.literal("未找到自定义色 " + code).withStyle(ChatFormatting.RED));
        }
    }

    /**
     * 向所有在线玩家广播自定义色更新。
     */
    public static void broadcastCustomColors() {
        String json = CustomColorFile.serialize(CustomColorStore.all());
        for (ServerPlayer sp : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            MardNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new MardNetwork.SyncCustomPacket(json));
        }
    }

    // ==================== 物品生成 ====================

    /**
     * 根据目标字符串生成物品栈。
     * 支持格式：
     * - MARD:<色号> - MARD基础色块
     * - CUSTOM:<编号> - 已保存的自定义色
     * - CUSTOM_RAW:<rgb> - 临时自定义色（不保存）
     * - IMPORTED:<名称>:<索引> - 导入的色卡颜色
     * - <色号> - 直接按MARD色号查找
     */
    public static ItemStack buildStack(String target) {
        if (target == null) return ItemStack.EMPTY;
        String t = target.trim();
        if (t.isEmpty()) return ItemStack.EMPTY;

        if (t.startsWith("MARD:")) {
            return buildMardStack(t.substring(5).trim());
        }
        if (t.startsWith("IMPORTED:")) {
            return buildImportedStack(t.substring(9));
        }
        if (t.startsWith("CUSTOM_RAW:")) {
            try {
                int rgb = Integer.parseInt(t.substring(11).trim());
                return createCustomItemStack(null, rgb, null);
            } catch (NumberFormatException e) {
                return ItemStack.EMPTY;
            }
        }
        if (t.startsWith("CUSTOM:")) {
            CustomColor cc = CustomColorStore.byCode(t.substring(7).trim());
            return cc != null ? createCustomItemStack(cc, cc.rgb(), null) : ItemStack.EMPTY;
        }

        // 直接按MARD色号查找
        return buildMardStack(t);
    }

    private static ItemStack buildMardStack(String code) {
        MardColor mc = MardPalette.byCode(code);
        if (mc == null) return ItemStack.EMPTY;
        for (MardBlock mb : MARD_BLOCKS) {
            if (mb.code().equalsIgnoreCase(mc.code())) return new ItemStack(mb);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack buildImportedStack(String param) {
        String[] parts = param.split(":", 2);
        if (parts.length != 2) return ItemStack.EMPTY;
        ImportedPaletteStore.Palette p = ImportedPaletteStore.get(parts[0]);
        if (p == null) return ItemStack.EMPTY;
        int idx;
        try {
            idx = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            return ItemStack.EMPTY;
        }
        if (idx < 1 || idx > p.colors.size()) return ItemStack.EMPTY;
        int rgb = p.colors.get(idx - 1);
        return createCustomItemStack(null, rgb, p.name + " #" + idx);
    }

    /**
     * 创建自定义色块物品栈。
     * 物品名称两行显示：第一行色号编号（白色），第二行RGB值（灰色）。
     */
    public static ItemStack createCustomItemStack(CustomColor cc, int rgb, String displayName) {
        ItemStack stack = new ItemStack(CUSTOM_ITEM.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("mard_color", rgb & 0xFFFFFF);

        // 写入色号编号到NBT，用于扫描检测遗失编号
        if (cc != null && cc.code() != null) {
            tag.putString("mard_code", cc.code());
        }

        // 第一行（hoverName）：颜色编号（白色）
        String name = displayName != null ? displayName
                : cc != null ? cc.displayName()
                : "MARD Custom";
        stack.setHoverName(Component.literal(name));

        // 第二行（lore）：RGB值（灰色），格式 "RGB #FF0000"
        String hex = ColorMath.toHex(rgb);
        CompoundTag display = tag.getCompound("display");
        net.minecraft.nbt.ListTag lore = new net.minecraft.nbt.ListTag();
        lore.add(net.minecraft.nbt.StringTag.valueOf(
                net.minecraft.network.chat.Component.Serializer.toJson(
                        Component.literal("RGB " + hex).withStyle(ChatFormatting.GRAY))));
        display.put("Lore", lore);
        tag.put("display", display);

        return stack;
    }

    // ==================== 智能给予物品 ====================

    /**
     * 检测背包是否已满（快捷栏9格 + 主物品栏27格 = 36格全部被占用）。
     */
    private static boolean isInventoryFull(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    /**
     * 智能给予物品：先放快捷栏，再放主物品栏，都满则扔到地面。
     */
    private static void giveItemSmart(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        Inventory inv = player.getInventory();

        // 1. 先尝试放快捷栏（0-8槽位）
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, stack.copy());
                player.sendSystemMessage(Component.literal("已放入快捷栏").withStyle(ChatFormatting.GRAY));
                return;
            }
        }

        // 2. 再尝试放主物品栏（9-35槽位）
        for (int i = 9; i < 36; i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, stack.copy());
                player.sendSystemMessage(Component.literal("已放入背包").withStyle(ChatFormatting.GRAY));
                return;
            }
        }

        // 3. 都满了，扔到地面
        player.drop(stack.copy(), false);
        player.sendSystemMessage(Component.literal("背包已满，已扔到地面").withStyle(ChatFormatting.YELLOW));
    }

    /**
     * 快速给予指定物品（/mardp give 命令），给予1个。
     */
    public static void giveRequestedItem(ServerPlayer player, String target) {
        if (target == null || target.isBlank()) {
            player.sendSystemMessage(Component.literal("用法：MARD:<色号> / CUSTOM:<编号> / IMPORTED:<名称>:<索引> / CUSTOM_RAW:<rgb>").withStyle(ChatFormatting.GRAY));
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

    // ==================== 编号循环回收 ====================

    /**
     * 扫描玩家背包和附近5格内的箱子，检测遗失的自定义颜色编号并回收。
     * 已保存但物品不存在于世界的编号 → 从保存列表移除，编号可循环利用。
     */
    public static void scanCustomItems(ServerPlayer player) {
        Set<String> foundCodes = new TreeSet<>();
        int chestCount = 0;
        int itemCount = 0;
        int range = 5;

        // 1. 扫描玩家背包（36格）
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == CUSTOM_ITEM.get()) {
                String code = readCustomCodeFromStack(stack);
                if (code != null) { foundCodes.add(code); itemCount++; }
            }
        }

        // 2. 扫描附近5格内的箱子（箱子/陷阱箱/木桶/潜影盒等容器）
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        net.minecraft.core.BlockPos pp = player.blockPosition();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    net.minecraft.core.BlockPos pos = pp.offset(dx, dy, dz);
                    net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof net.minecraft.world.Container container) {
                        chestCount++;
                        for (int slot = 0; slot < container.getContainerSize(); slot++) {
                            ItemStack stack = container.getItem(slot);
                            if (!stack.isEmpty() && stack.getItem() == CUSTOM_ITEM.get()) {
                                String code = readCustomCodeFromStack(stack);
                                if (code != null) { foundCodes.add(code); itemCount++; }
                            }
                        }
                    }
                }
            }
        }

        // 3. 对比已保存的自定义颜色，找出遗失的编号
        List<CustomColor> all = CustomColorStore.all();
        List<String> lostCodes = new ArrayList<>();
        for (CustomColor cc : all) {
            if (!foundCodes.contains(cc.code())) {
                lostCodes.add(cc.code());
            }
        }

        // 4. 回收遗失编号（从保存列表移除，编号可循环利用）
        int recycled = 0;
        for (String code : lostCodes) {
            if (CustomColorStore.remove(code)) recycled++;
        }
        if (recycled > 0) {
            CustomColorFile.save(CUSTOM_COLORS_FILE, CustomColorStore.all());
            broadcastCustomColors();
        }

        // 5. 输出检测报告
        player.sendSystemMessage(Component.literal("===== 自定义颜色物品检测报告 =====").withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal("扫描范围：背包 + 附近 " + range + " 格内 " + chestCount + " 个容器").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("找到自定义物品：" + itemCount + " 个，涉及 " + foundCodes.size() + " 个编号").withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal("已保存编号：" + all.size() + " 个").withStyle(ChatFormatting.GRAY));
        if (lostCodes.isEmpty()) {
            player.sendSystemMessage(Component.literal("未发现遗失编号，所有编号均有对应物品。").withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.literal("发现遗失编号：" + lostCodes.size() + " 个，已回收（编号可重新分配）").withStyle(ChatFormatting.YELLOW));
            StringBuilder sb = new StringBuilder("回收编号：");
            for (int i = 0; i < Math.min(lostCodes.size(), 10); i++) {
                CustomColor cc = null;
                for (CustomColor x : all) if (x.code().equals(lostCodes.get(i))) { cc = x; break; }
                if (cc != null) sb.append(cc.name()).append(", ");
            }
            if (lostCodes.size() > 10) sb.append("等共 ").append(lostCodes.size()).append(" 个");
            player.sendSystemMessage(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
        }
        player.sendSystemMessage(Component.literal("提示：遗失编号已回收，新增颜色时将优先复用这些编号。").withStyle(ChatFormatting.GRAY));
    }

    /**
     * 补全遗失的自定义颜色物品：扫描后为遗失编号生成对应物品放入背包。
     */
    public static void restoreCustomItems(ServerPlayer player) {
        Set<String> foundCodes = new TreeSet<>();
        int range = 5;

        // 1. 扫描玩家背包
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == CUSTOM_ITEM.get()) {
                String code = readCustomCodeFromStack(stack);
                if (code != null) foundCodes.add(code);
            }
        }

        // 2. 扫描附近5格内的箱子
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        net.minecraft.core.BlockPos pp = player.blockPosition();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    net.minecraft.core.BlockPos pos = pp.offset(dx, dy, dz);
                    net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof net.minecraft.world.Container container) {
                        for (int slot = 0; slot < container.getContainerSize(); slot++) {
                            ItemStack stack = container.getItem(slot);
                            if (!stack.isEmpty() && stack.getItem() == CUSTOM_ITEM.get()) {
                                String code = readCustomCodeFromStack(stack);
                                if (code != null) foundCodes.add(code);
                            }
                        }
                    }
                }
            }
        }

        // 3. 找出遗失的编号并补全
        List<CustomColor> all = CustomColorStore.all();
        int restored = 0;
        for (CustomColor cc : all) {
            if (!foundCodes.contains(cc.code())) {
                ItemStack stack = createCustomItemStack(cc, cc.rgb(), cc.name());
                giveItemSmart(player, stack);
                restored++;
            }
        }

        if (restored == 0) {
            player.sendSystemMessage(Component.literal("未发现遗失编号，无需补全。").withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.literal("已补全 " + restored + " 个遗失编号的物品。").withStyle(ChatFormatting.GREEN));
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 从自定义物品的NBT中读取色号编号（code）。
     */
    private static String readCustomCodeFromStack(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != CUSTOM_ITEM.get()) return null;
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains("mard_code")) ? tag.getString("mard_code") : null;
    }

    /**
     * 获取物品的颜色值（MARD基础色块或自定义色块）。
     * @return 颜色RGB值，或-1（非本模组物品）
     */
    public static int colorOf(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        Item item = stack.getItem();
        if (item instanceof BlockItem bi && bi.getBlock() instanceof MardBlock mb) return mb.rgb();
        if (item == CUSTOM_ITEM.get()) {
            CompoundTag tag = stack.getTag();
            return (tag != null && tag.contains("mard_color")) ? tag.getInt("mard_color") & 0xFFFFFF : -1;
        }
        return -1;
    }
}
