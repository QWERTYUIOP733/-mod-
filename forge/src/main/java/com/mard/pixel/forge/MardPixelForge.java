package com.mard.pixel.forge;

import com.mard.pixel.common.BrandColor;
import com.mard.pixel.common.BrandPalette;
import com.mard.pixel.common.ColorMath;
import com.mard.pixel.common.CustomColor;
import com.mard.pixel.common.CustomColorFile;
import com.mard.pixel.common.CustomColorStore;
import com.mard.pixel.common.ExternalBrandStore;
import com.mard.pixel.common.ImportedPaletteStore;
import com.mard.pixel.common.MardColor;
import com.mard.pixel.common.MardPalette;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mod(MardPixelForge.MODID)
public class MardPixelForge {
    public static final String MODID = "mard_pixel";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BE = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final List<RegistryObject<Block>> MARD_BLOCK_REFS = new ArrayList<>();
    public static final List<MardBlock> MARD_BLOCKS = new ArrayList<>();

    public static final RegistryObject<MardCustomBlock> CUSTOM_BLOCK =
            BLOCKS.register("mard_custom", MardCustomBlock::new);
    public static final RegistryObject<BlockItem> CUSTOM_ITEM =
            ITEMS.register("mard_custom", () -> new BlockItem(CUSTOM_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<MardCustomBlockEntity>> CUSTOM_BE_TYPE =
            BE.register("mard_custom_be",
                    () -> BlockEntityType.Builder.of(MardCustomBlockEntity::new, CUSTOM_BLOCK.get()).build(null));

    public static final Path CUSTOM_FILE = FMLPaths.CONFIGDIR.get().resolve("mard_pixel_custom.json");
    public static final Path EXTERNAL_DIR = FMLPaths.CONFIGDIR.get().resolve("mard_pixel_brands");

    public static Map<String, List<BrandColor>> externalBrands = new LinkedHashMap<>();
    public static Map<String, String> externalBrandsJson = new LinkedHashMap<>();
    public static Map<String, String> clientExternalBrands = new LinkedHashMap<>();

    public MardPixelForge() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BE.register(bus);
        TABS.register(bus);

        for (MardColor mc : MardPalette.COLORS) {
            String name = mc.blockName();
            RegistryObject<Block> ro = BLOCKS.register(name, () -> new MardBlock(mc.code(), mc.rgb()));
            ITEMS.register(name, () -> new BlockItem(ro.get(), new Item.Properties()));
            MARD_BLOCK_REFS.add(ro);
        }

        // 按系列（字母）分类的创造模式标签页
        java.util.Set<String> seriesSet = new java.util.LinkedHashSet<>();
        for (MardColor mc : MardPalette.COLORS) {
            seriesSet.add(mc.series());
        }
        List<String> seriesList = new ArrayList<>(seriesSet);
        java.util.Collections.sort(seriesList);

        // 主标签页：全部色块 + 自定义色块
        TABS.register("mard_pixel", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mard_pixel"))
                .icon(() -> new ItemStack(CUSTOM_ITEM.get()))
                .displayItems((params, output) -> {
                    for (MardBlock mb : MARD_BLOCKS) output.accept(new ItemStack(mb));
                    output.accept(new ItemStack(CUSTOM_ITEM.get()));
                })
                .build());

        // 按系列分类的标签页
        for (String series : seriesList) {
            final String s = series;
            TABS.register("mard_pixel_" + series.toLowerCase(), () -> CreativeModeTab.builder()
                    .title(Component.literal("MARD " + series + " 系列"))
                    .icon(() -> {
                        // 用该系列第一个色块作为图标，使用MARD_BLOCKS（游戏运行时onCommonSetup已填充）
                        for (MardColor mc : MardPalette.COLORS) {
                            if (mc.series().equals(s)) {
                                for (MardBlock mb : MARD_BLOCKS) {
                                    if (mb.code().equalsIgnoreCase(mc.code())) {
                                        return new ItemStack(mb);
                                    }
                                }
                            }
                        }
                        return new ItemStack(CUSTOM_ITEM.get());
                    })
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
                    })
                    .build());
        }

        MinecraftForge.EVENT_BUS.register(this);
        bus.addListener(this::onCommonSetup);
        MardNetwork.init();
        BrandPalette.load();
        ImportedPaletteStore.init(FMLPaths.CONFIGDIR.get().resolve("mard_pixel"));
    }

    private void onCommonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        MARD_BLOCKS.clear();
        for (RegistryObject<Block> ro : MARD_BLOCK_REFS) {
            Block b = ro.get();
            if (b instanceof MardBlock mb) MARD_BLOCKS.add(mb);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var root = event.getDispatcher().register(
                Commands.literal("mardp")
                        .then(Commands.literal("brands")
                                .executes(ctx -> { listBrands(ctx.getSource().getPlayerOrException()); return 1; }))
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
                        .then(Commands.literal("convert")
                                .then(Commands.argument("brand", StringArgumentType.word())
                                        .then(Commands.argument("code", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                                    String brand = StringArgumentType.getString(ctx, "brand");
                                                    String code = StringArgumentType.getString(ctx, "code");
                                                    BrandColor bc = lookupBrand(brand, code);
                                                    if (bc == null) {
                                                        p.sendSystemMessage(Component.literal("未找到品牌色 " + brand + " " + code).withStyle(ChatFormatting.RED));
                                                    } else {
                                                        MardColor nearest = MardPalette.nearest(bc.rgb());
                                                        p.sendSystemMessage(Component.literal(brandDisplay(brand) + " " + bc.code()
                                                                + " (" + ColorMath.toHex(bc.rgb()) + ") → MARD "
                                                                + nearest.code() + " " + ColorMath.toHex(nearest.rgb())));
                                                    }
                                                    return 1;
                                                }))))
                        .then(Commands.literal("give")
                                .then(Commands.argument("target", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            String t = StringArgumentType.getString(ctx, "target");
                                            giveRequestedItem(p, t);
                                            return 1;
                                        })))
                        .then(Commands.literal("switch")
                                .then(Commands.argument("system", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            switchBagSystem(p, StringArgumentType.getString(ctx, "system"));
                                            return 1;
                                        })))
                        .then(Commands.literal("reload")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    scanExternalBrands();
                                    p.sendSystemMessage(Component.literal("已重新扫描外部色系：" + externalBrands.size() + " 个"));
                                    return 1;
                                })));
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            loadCustomFromFile();
            MardNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new MardNetwork.SyncCustomPacket(CustomColorFile.serialize(CustomColorStore.all())));
            sendExternalBrands(sp);
        }
    }

    public static void loadCustomFromFile() {
        CustomColorStore.loadFrom(CustomColorFile.load(CUSTOM_FILE));
    }

    public static void addCustom(ServerPlayer player, String name, int rgb) {
        // 检测背包中自定义颜色物品数量上限（64个）
        int customCount = countCustomItemsInInventory(player);
        if (customCount >= 64) {
            player.sendSystemMessage(Component.literal("背包中自定义颜色物品已达上限（64个），无法新增。请清空背包中部分物品后继续。").withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("也可以将物品移至箱子或其他地方以腾出背包空间。").withStyle(ChatFormatting.GRAY));
            return;
        }

        CustomColor c = CustomColorStore.add(name, rgb);
        if (c == null) {
            player.sendSystemMessage(Component.literal("自定义色已满（9999）或参数非法").withStyle(ChatFormatting.RED));
            return;
        }
        CustomColorFile.save(CUSTOM_FILE, CustomColorStore.all());
        broadcastCustom();
        player.sendSystemMessage(Component.literal("已新增自定义色 " + c.code() + " " + ColorMath.toHex(rgb)));

        // 自动给玩家对应的自定义方块物品：快捷栏 → 主物品栏 → 扔地面
        ItemStack stack = customStack(c, rgb, c.displayName());
        giveItemSmart(player, stack);
    }

    /**
     * 统计背包中自定义颜色物品的总数量。
     */
    private static int countCustomItemsInInventory(ServerPlayer player) {
        int count = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == CUSTOM_ITEM.get()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * 智能给予物品：先放快捷栏，再放主物品栏，都满则扔到地面
     */
    private static void giveItemSmart(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        Inventory inv = player.getInventory();

        // 1. 先尝试放快捷栏（0-8槽位）
        for (int i = 0; i < 9; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot.isEmpty()) {
                inv.setItem(i, stack.copy());
                player.sendSystemMessage(Component.literal("已放入快捷栏").withStyle(ChatFormatting.GRAY));
                return;
            }
        }

        // 2. 再尝试放主物品栏（9-35槽位）
        for (int i = 9; i < 36; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot.isEmpty()) {
                inv.setItem(i, stack.copy());
                player.sendSystemMessage(Component.literal("已放入背包").withStyle(ChatFormatting.GRAY));
                return;
            }
        }

        // 3. 都满了，扔到地面
        player.drop(stack.copy(), false);
        player.sendSystemMessage(Component.literal("背包已满，已扔到地面").withStyle(ChatFormatting.YELLOW));
    }

    public static void removeCustom(ServerPlayer player, String code) {
        boolean ok = CustomColorStore.remove(code);
        if (ok) {
            CustomColorFile.save(CUSTOM_FILE, CustomColorStore.all());
            broadcastCustom();
            player.sendSystemMessage(Component.literal("已删除自定义色 " + code));
        } else {
            player.sendSystemMessage(Component.literal("未找到自定义色 " + code).withStyle(ChatFormatting.RED));
        }
    }

    public static void broadcastCustom() {
        String json = CustomColorFile.serialize(CustomColorStore.all());
        for (ServerPlayer sp : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            MardNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new MardNetwork.SyncCustomPacket(json));
        }
    }

    public static void scanExternalBrands() {
        Map<String, List<BrandColor>> colors = new LinkedHashMap<>();
        Map<String, String> jsons = new LinkedHashMap<>();
        try {
            if (Files.exists(EXTERNAL_DIR)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(EXTERNAL_DIR, "*.json")) {
                    for (Path p : ds) {
                        String fileName = p.getFileName().toString();
                        String json = Files.readString(p, StandardCharsets.UTF_8);
                        List<BrandColor> list = ExternalBrandStore.parseBrandFile(fileName, json);
                        if (!list.isEmpty()) {
                            String key = ExternalBrandStore.brandKey(fileName);
                            colors.put(key, list);
                            jsons.put(key, ExternalBrandStore.serialize(key, list));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        externalBrands = colors;
        externalBrandsJson = jsons;
    }

    public static void sendExternalBrands(ServerPlayer sp) {
        scanExternalBrands();
        MardNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                new MardNetwork.SyncExternalBrandsPacket(new LinkedHashMap<>(externalBrandsJson)));
    }

    public static BrandColor lookupBrand(String brand, String code) {
        if (brand == null) return null;
        BrandColor bc = BrandPalette.lookup(brand, code);
        if (bc != null) return bc;
        List<BrandColor> ext = externalBrands.get(brand.trim().toUpperCase(Locale.ROOT));
        if (ext != null) {
            String c = BrandPalette.normalizeCode(code);
            for (BrandColor x : ext) if (x.code().equalsIgnoreCase(c) || BrandPalette.normalizeCode(x.code()).equals(c)) return x;
        }
        return null;
    }

    public static List<BrandColor> brandColors(String system) {
        if (system == null) return null;
        List<BrandColor> b = BrandPalette.brandColors(system);
        if (b != null) return b;
        return externalBrands.get(system.trim().toUpperCase(Locale.ROOT));
    }

    public static String brandDisplay(String brand) {
        if (brand == null) return "";
        String b = brand.toLowerCase(Locale.ROOT);
        if (b.equals("perler")) return "Perler";
        if (b.equals("hama")) return "Hama";
        if (b.equals("artkal")) return "Artkal";
        return brand.toUpperCase(Locale.ROOT);
    }

    public static void giveRequestedItem(ServerPlayer player, String target) {
        if (target == null || target.isBlank()) {
            player.sendSystemMessage(Component.literal("用法：MARD:<色号> / BRAND:<品牌>:<色号> / CUSTOM:<编号> / CUSTOM_RAW:<rgb>").withStyle(ChatFormatting.GRAY));
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

    public static ItemStack buildStack(String target) {
        if (target == null) return ItemStack.EMPTY;
        String t = target.trim();
        if (t.isEmpty()) return ItemStack.EMPTY;
        if (t.startsWith("MARD:")) {
            String code = t.substring(5).trim();
            MardColor mc = MardPalette.byCode(code);
            if (mc == null) return ItemStack.EMPTY;
            Block b = null;
            for (MardBlock mb : MARD_BLOCKS) if (mb.code().equalsIgnoreCase(mc.code())) { b = mb; break; }
            if (b == null) return ItemStack.EMPTY;
            return new ItemStack(b);
        }
        if (t.startsWith("IMPORTED:")) {
            String[] parts = t.substring(9).split(":", 2);
            if (parts.length != 2) return ItemStack.EMPTY;
            ImportedPaletteStore.Palette p = ImportedPaletteStore.get(parts[0]);
            if (p == null) return ItemStack.EMPTY;
            int idx;
            try { idx = Integer.parseInt(parts[1].trim()); } catch (NumberFormatException e) { return ItemStack.EMPTY; }
            if (idx < 1 || idx > p.colors.size()) return ItemStack.EMPTY;
            int rgb = p.colors.get(idx - 1);
            return customStack(null, rgb, p.name + " #" + idx);
        }
        if (t.startsWith("BRAND:")) {
            String[] parts = t.substring(6).split(":", 2);
            if (parts.length != 2) return ItemStack.EMPTY;
            return brandedStack(parts[0].trim(), parts[1].trim());
        }
        if (t.startsWith("CUSTOM_RAW:")) {
            try {
                int rgb = Integer.parseInt(t.substring(11).trim());
                return customStack(null, rgb, null);
            } catch (NumberFormatException e) { return ItemStack.EMPTY; }
        }
        if (t.startsWith("CUSTOM:")) {
            CustomColor cc = CustomColorStore.byCode(t.substring(7).trim());
            if (cc == null) return ItemStack.EMPTY;
            return customStack(cc, cc.rgb(), null);
        }
        MardColor mc = MardPalette.byCode(t);
        if (mc == null) return ItemStack.EMPTY;
        for (MardBlock mb : MARD_BLOCKS) if (mb.code().equalsIgnoreCase(mc.code())) return new ItemStack(mb);
        return ItemStack.EMPTY;
    }

    public static ItemStack brandedStack(String brand, String code) {
        BrandColor bc = lookupBrand(brand, code);
        if (bc == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(CUSTOM_ITEM.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("mard_color", bc.rgb());
        tag.putString("mard_brand", bc.brand());
        tag.putString("mard_brand_code", bc.code());
        stack.setHoverName(Component.literal(brandDisplay(brand) + " " + bc.code()));
        return stack;
    }

    public static ItemStack customStack(CustomColor cc, int rgb, String displayName) {
        ItemStack stack = new ItemStack(CUSTOM_ITEM.get());
        stack.getOrCreateTag().putInt("mard_color", rgb & 0xFFFFFF);
        String name = displayName != null ? displayName
                : cc != null ? cc.displayName()
                : "MARD Custom " + ColorMath.toHex(rgb);
        stack.setHoverName(Component.literal(name));
        return stack;
    }

    public static void switchBagSystem(ServerPlayer player, String system) {
        if (system == null || system.isBlank()) return;
        String sys = system.trim().toUpperCase(Locale.ROOT);
        boolean isMard = sys.equals("MARD");
        boolean isCustom = sys.equals("CUSTOM");
        List<BrandColor> targetBrand = isMard || isCustom ? null : brandColors(system);
        if (!isMard && !isCustom && targetBrand == null) {
            player.sendSystemMessage(Component.literal("色系不存在：" + system).withStyle(ChatFormatting.RED));
            return;
        }
        Inventory inv = player.getInventory();
        int changed = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            int color = colorOf(stack);
            if (color < 0) continue;
            ItemStack target = null;
            if (isMard) {
                MardColor mc = MardPalette.nearest(color);
                target = buildStack("MARD:" + mc.code());
            } else if (isCustom) {
                target = customStack(null, color, "MARD Custom " + ColorMath.toHex(color));
            } else {
                BrandColor nearest = nearestBrandColor(targetBrand, color);
                if (nearest != null) target = brandedStack(nearest.brand(), nearest.code());
            }
            if (target != null) {
                target.setCount(stack.getCount());
                inv.setItem(i, target);
                changed += stack.getCount();
            }
        }
        player.sendSystemMessage(Component.literal("已切换为 " + (isMard ? "MARD" : brandDisplay(sys))
                + " 色系，转换 " + changed + " 个色块").withStyle(ChatFormatting.GREEN));
    }

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

    public static BrandColor nearestBrandColor(List<BrandColor> list, int color) {
        BrandColor best = null;
        double bestD = Double.MAX_VALUE;
        for (BrandColor bc : list) {
            double d = ColorMath.deltaE2000(color, bc.rgb());
            if (d < bestD) { bestD = d; best = bc; }
        }
        return best;
    }

    public static void listBrands(ServerPlayer player) {
        StringBuilder sb = new StringBuilder();
        sb.append("内置品牌：");
        for (String name : BrandPalette.brandNames()) {
            List<BrandColor> list = BrandPalette.brandColors(name);
            sb.append(name).append("(").append(list == null ? 0 : list.size()).append(") ");
        }
        sb.append("\n外部色系：");
        scanExternalBrands();
        for (Map.Entry<String, List<BrandColor>> e : externalBrands.entrySet()) {
            sb.append(e.getKey()).append("(").append(e.getValue().size()).append(") ");
        }
        if (externalBrands.isEmpty()) sb.append("无（把 .json 放到 config/mard_pixel_brands/ 即可导入）");
        player.sendSystemMessage(Component.literal(sb.toString()));
    }
}
