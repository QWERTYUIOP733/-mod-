package com.mard.pixel.fabric;

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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.ColorArgument;
import net.minecraft.command.argument.StringArgumentType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MardPixelFabric implements ModInitializer {
    public static final String MODID = "mard_pixel";

    public static final Identifier SYNC_CUSTOM = new Identifier(MODID, "sync_custom");
    public static final Identifier SYNC_BRANDS = new Identifier(MODID, "sync_brands");
    public static final Identifier REQUEST_ITEM = new Identifier(MODID, "request_item");
    public static final Identifier ADD_CUSTOM = new Identifier(MODID, "add_custom");
    public static final Identifier REMOVE_CUSTOM = new Identifier(MODID, "remove_custom");
    public static final Identifier SWITCH_SYSTEM = new Identifier(MODID, "switch_system");

    public static final List<MardBlock> MARD_BLOCKS = new ArrayList<>();
    public static MardCustomBlock CUSTOM_BLOCK;
    public static net.minecraft.block.entity.BlockEntityType<MardCustomBlockEntity> CUSTOM_BE_TYPE;
    public static net.minecraft.item.Item CUSTOM_ITEM;

    public static final Path CUSTOM_FILE = FabricLoader.getInstance().getConfigDir().resolve("mard_pixel_custom.json");
    public static final Path EXTERNAL_DIR = FabricLoader.getInstance().getConfigDir().resolve("mard_pixel_brands");

    public static Map<String, List<BrandColor>> externalBrands = new LinkedHashMap<>();
    public static Map<String, String> externalBrandsJson = new LinkedHashMap<>();
    public static Map<String, String> clientExternalBrands = new LinkedHashMap<>();

    public static MinecraftServer SERVER;

    @Override
    public void onInitialize() {
        for (MardColor mc : MardPalette.COLORS) {
            Identifier id = new Identifier(MODID, mc.blockName());
            MardBlock block = Registry.register(Registries.BLOCK, id, new MardBlock(mc.code(), mc.rgb()));
            Registry.register(Registries.ITEM, id, new BlockItem(block, new net.minecraft.item.Item.Settings()));
            MARD_BLOCKS.add(block);
        }
        CUSTOM_BLOCK = Registry.register(Registries.BLOCK, new Identifier(MODID, "mard_custom"), new MardCustomBlock());
        CUSTOM_ITEM = Registry.register(Registries.ITEM, new Identifier(MODID, "mard_custom"),
                new BlockItem(CUSTOM_BLOCK, new net.minecraft.item.Item.Settings()));
        CUSTOM_BE_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MODID, "mard_custom_be"),
                net.minecraft.block.entity.BlockEntityType.Builder.create(MardCustomBlockEntity::new, CUSTOM_BLOCK).build(null));

        Registry.register(Registries.ITEM_GROUP, new Identifier(MODID, "mard_pixel"),
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.mard_pixel"))
                        .icon(() -> new ItemStack(CUSTOM_ITEM))
                        .entries((context, entries) -> {
                            for (MardBlock mb : MARD_BLOCKS) entries.add(new ItemStack(mb));
                            entries.add(new ItemStack(CUSTOM_ITEM));
                        })
                        .build());

        BrandPalette.load();
        loadCustomFromFile();
        scanExternalBrands();
        ImportedPaletteStore.init(FabricLoader.getInstance().getConfigDir().resolve("mard_pixel"));

        registerCommands();
        registerNetwork();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("mardp")
                    .then(CommandManager.literal("brands")
                            .executes(ctx -> { listBrands(ctx.getSource().getPlayerOrThrow()); return 1; }))
                    .then(CommandManager.literal("find")
                            .then(CommandManager.argument("hex", ColorArgument.color())
                                    .executes(ctx -> {
                                        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                        MardColor nearest = MardPalette.nearest(ColorArgument.getColor(ctx, "hex"));
                                        p.sendMessage(Text.literal("最近 MARD 色：" + nearest.code() + " " + ColorMath.toHex(nearest.rgb())), false);
                                        return 1;
                                    })))
                    .then(CommandManager.literal("convert")
                            .then(CommandManager.argument("brand", StringArgumentType.word())
                                    .then(CommandManager.argument("code", StringArgumentType.word())
                                            .executes(ctx -> {
                                                ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                                String brand = StringArgumentType.getString(ctx, "brand");
                                                String code = StringArgumentType.getString(ctx, "code");
                                                BrandColor bc = lookupBrand(brand, code);
                                                if (bc == null) {
                                                    p.sendMessage(Text.literal("未找到品牌色 " + brand + " " + code).formatted(Formatting.RED), false);
                                                } else {
                                                    MardColor nearest = MardPalette.nearest(bc.rgb());
                                                    p.sendMessage(Text.literal(brandDisplay(brand) + " " + bc.code()
                                                            + " (" + ColorMath.toHex(bc.rgb()) + ") → MARD "
                                                            + nearest.code() + " " + ColorMath.toHex(nearest.rgb())), false);
                                                }
                                                return 1;
                                            }))))
                    .then(CommandManager.literal("give")
                            .then(CommandManager.argument("target", StringArgumentType.greedyString())
                                    .executes(ctx -> {
                                        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                        giveRequestedItem(p, StringArgumentType.getString(ctx, "target"));
                                        return 1;
                                    })))
                    .then(CommandManager.literal("switch")
                            .then(CommandManager.argument("system", StringArgumentType.word())
                                    .executes(ctx -> {
                                        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                        switchBagSystem(p, StringArgumentType.getString(ctx, "system"));
                                        return 1;
                                    })))
                    .then(CommandManager.literal("reload")
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                scanExternalBrands();
                                p.sendMessage(Text.literal("已重新扫描外部色系：" + externalBrands.size() + " 个"), false);
                                return 1;
                            })));
        });
    }

    private void registerNetwork() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_ITEM, (server, player, handler, buf, responseSender) -> {
            String target = buf.readString();
            server.execute(() -> giveRequestedItem(player, target));
        });
        ServerPlayNetworking.registerGlobalReceiver(ADD_CUSTOM, (server, player, handler, buf, responseSender) -> {
            String name = buf.readString();
            int rgb = buf.readInt();
            server.execute(() -> addCustom(player, name, rgb));
        });
        ServerPlayNetworking.registerGlobalReceiver(REMOVE_CUSTOM, (server, player, handler, buf, responseSender) -> {
            String code = buf.readString();
            server.execute(() -> removeCustom(player, code));
        });
        ServerPlayNetworking.registerGlobalReceiver(SWITCH_SYSTEM, (server, player, handler, buf, responseSender) -> {
            String system = buf.readString();
            server.execute(() -> switchBagSystem(player, system));
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            SERVER = server;
            ServerPlayerEntity p = handler.getPlayer();
            loadCustomFromFile();
            sendSyncCustom(p);
            sendExternalBrands(p);
        });
    }

    public static void loadCustomFromFile() {
        CustomColorStore.loadFrom(CustomColorFile.load(CUSTOM_FILE));
    }

    public static void addCustom(ServerPlayerEntity player, String name, int rgb) {
        CustomColor c = CustomColorStore.add(name, rgb);
        if (c == null) {
            player.sendMessage(Text.literal("自定义色已满（9999）或参数非法").formatted(Formatting.RED), false);
            return;
        }
        CustomColorFile.save(CUSTOM_FILE, CustomColorStore.all());
        broadcastCustom();
        player.sendMessage(Text.literal("已新增自定义色 " + c.code() + " " + ColorMath.toHex(rgb)), false);
    }

    public static void removeCustom(ServerPlayerEntity player, String code) {
        boolean ok = CustomColorStore.remove(code);
        if (ok) {
            CustomColorFile.save(CUSTOM_FILE, CustomColorStore.all());
            broadcastCustom();
            player.sendMessage(Text.literal("已删除自定义色 " + code), false);
        } else {
            player.sendMessage(Text.literal("未找到自定义色 " + code).formatted(Formatting.RED), false);
        }
    }

    public static void broadcastCustom() {
        String json = CustomColorFile.serialize(CustomColorStore.all());
        if (SERVER != null) {
            for (ServerPlayerEntity sp : SERVER.getPlayerManager().getPlayerList()) {
                sendSyncCustom(sp);
            }
        }
    }

    public static void sendSyncCustom(ServerPlayerEntity sp) {
        String json = CustomColorFile.serialize(CustomColorStore.all());
        var buf = PacketByteBufs.create();
        buf.writeString(json);
        ServerPlayNetworking.send(sp, SYNC_CUSTOM, buf);
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

    public static void sendExternalBrands(ServerPlayerEntity sp) {
        scanExternalBrands();
        var buf = PacketByteBufs.create();
        buf.writeInt(externalBrandsJson.size());
        for (Map.Entry<String, String> e : externalBrandsJson.entrySet()) {
            buf.writeString(e.getKey());
            buf.writeString(e.getValue());
        }
        ServerPlayNetworking.send(sp, SYNC_BRANDS, buf);
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

    public static void giveRequestedItem(ServerPlayerEntity player, String target) {
        if (target == null || target.isBlank()) {
            player.sendMessage(Text.literal("用法：MARD:<色号> / BRAND:<品牌>:<色号> / CUSTOM:<编号> / CUSTOM_RAW:<rgb>").formatted(Formatting.GRAY), false);
            return;
        }
        ItemStack stack = buildStack(target);
        if (stack == null || stack.isEmpty()) {
            player.sendMessage(Text.literal("无法生成物品：" + target).formatted(Formatting.RED), false);
            return;
        }
        player.getInventory().offerOrDrop(stack);
        player.sendMessage(Text.literal("已给予 ").append(stack.getName()).append(" x" + stack.getCount()), false);
    }

    public static ItemStack buildStack(String target) {
        if (target == null) return ItemStack.EMPTY;
        String t = target.trim();
        if (t.isEmpty()) return ItemStack.EMPTY;
        if (t.startsWith("MARD:")) {
            String code = t.substring(5).trim();
            MardColor mc = MardPalette.byCode(code);
            if (mc == null) return ItemStack.EMPTY;
            for (MardBlock mb : MARD_BLOCKS) if (mb.code().equalsIgnoreCase(mc.code())) return new ItemStack(mb);
            return ItemStack.EMPTY;
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
            try { return customStack(null, Integer.parseInt(t.substring(11).trim()), null); }
            catch (NumberFormatException e) { return ItemStack.EMPTY; }
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
        ItemStack stack = new ItemStack(CUSTOM_ITEM);
        NbtCompound tag = stack.getOrCreateNbt();
        tag.putInt("mard_color", bc.rgb());
        tag.putString("mard_brand", bc.brand());
        tag.putString("mard_brand_code", bc.code());
        stack.setCustomName(Text.literal(brandDisplay(brand) + " " + bc.code()));
        return stack;
    }

    public static ItemStack customStack(CustomColor cc, int rgb, String displayName) {
        ItemStack stack = new ItemStack(CUSTOM_ITEM);
        stack.getOrCreateNbt().putInt("mard_color", rgb & 0xFFFFFF);
        String name = displayName != null ? displayName
                : cc != null ? cc.displayName()
                : "MARD Custom " + ColorMath.toHex(rgb);
        stack.setCustomName(Text.literal(name));
        return stack;
    }

    public static void switchBagSystem(ServerPlayerEntity player, String system) {
        if (system == null || system.isBlank()) return;
        String sys = system.trim().toUpperCase(Locale.ROOT);
        boolean isMard = sys.equals("MARD");
        boolean isCustom = sys.equals("CUSTOM");
        List<BrandColor> targetBrand = isMard || isCustom ? null : brandColors(system);
        if (!isMard && !isCustom && targetBrand == null) {
            player.sendMessage(Text.literal("色系不存在：" + system).formatted(Formatting.RED), false);
            return;
        }
        List<ItemStack> main = player.getInventory().main;
        int changed = 0;
        for (int i = 0; i < main.size(); i++) {
            ItemStack stack = main.get(i);
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
                main.set(i, target);
                changed += stack.getCount();
            }
        }
        player.sendMessage(Text.literal("已切换为 " + (isMard ? "MARD" : brandDisplay(sys))
                + " 色系，转换 " + changed + " 个色块").formatted(Formatting.GREEN), false);
    }

    public static int colorOf(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        var item = stack.getItem();
        if (item instanceof BlockItem bi && bi.getBlock() instanceof MardBlock mb) return mb.rgb();
        if (item == CUSTOM_ITEM) {
            NbtCompound tag = stack.getNbt();
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

    public static void listBrands(ServerPlayerEntity player) {
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
        player.sendMessage(Text.literal(sb.toString()), false);
    }
}
