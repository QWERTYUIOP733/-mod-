package com.mard.pixel.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * MARD Pixel Mod 网络包管理。
 *
 * 网络包列表：
 * 1. RequestItemPacket - 客户端请求物品（UI 中点击获取物品）
 * 2. HotbarPacket - 输入色号后放入快捷栏
 */
public final class MardNetwork {
    public static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MardPixelForge.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void init() {
        int id = 0;
        CHANNEL.messageBuilder(RequestItemPacket.class, id++)
                .encoder(RequestItemPacket::encode)
                .decoder(RequestItemPacket::decode)
                .consumerMainThread(MardNetwork::handleRequestItem)
                .add();
        CHANNEL.messageBuilder(HotbarPacket.class, id++)
                .encoder(HotbarPacket::encode)
                .decoder(HotbarPacket::decode)
                .consumerMainThread(MardNetwork::handleHotbar)
                .add();
        CHANNEL.messageBuilder(CraftItemPacket.class, id++)
                .encoder(CraftItemPacket::encode)
                .decoder(CraftItemPacket::decode)
                .consumerMainThread(MardNetwork::handleCraftItem)
                .add();
        CHANNEL.messageBuilder(SelectCraftingColorPacket.class, id++)
                .encoder(SelectCraftingColorPacket::encode)
                .decoder(SelectCraftingColorPacket::decode)
                .consumerMainThread(MardNetwork::handleSelectCraftingColor)
                .add();
    }

    // ==================== 网络包定义 ====================

    /**
     * 客户端请求物品（UI 中点击获取物品）。
     */
    public static class RequestItemPacket {
        public final String target;
        public RequestItemPacket(String target) { this.target = target; }
        public static void encode(RequestItemPacket p, FriendlyByteBuf b) { b.writeUtf(p.target); }
        public static RequestItemPacket decode(FriendlyByteBuf b) { return new RequestItemPacket(b.readUtf()); }
    }

    // ==================== 网络包处理 ====================

    private static void handleRequestItem(RequestItemPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            // 生存模式下禁用直接获取方块，必须通过合成台或七彩粉末合成
            if (!player.isCreative() && !player.isSpectator()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "生存模式下无法直接获取方块，请使用方块染色台或七彩粉末合成").withStyle(net.minecraft.ChatFormatting.RED));
                return;
            }
            // 创造/旁观模式：UI 点击色块时给予一组（64个）方块
            MardPixelForge.giveRequestedStack(player, p.target);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleHotbar(HotbarPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            // 生存模式下禁用直接放入快捷栏，必须通过合成台或七彩粉末合成
            if (!player.isCreative() && !player.isSpectator()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "生存模式下无法直接获取方块，请使用方块染色台或七彩粉末合成").withStyle(net.minecraft.ChatFormatting.RED));
                return;
            }
            // 创造/旁观模式：输入色号后放入快捷栏
            MardPixelForge.giveToHotbar(player, p.code);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleCraftItem(CraftItemPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) MardPixelForge.craftWithPigment(player, p.code);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleSelectCraftingColor(SelectCraftingColorPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof MardCraftingMenu menu) {
                if (menu.getBlockEntity() != null) {
                    menu.getBlockEntity().selectColor(p.code);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // ==================== 网络包类 ====================

    /**
     * 输入色号后放入快捷栏。
     */
    public static class HotbarPacket {
        public final String code;
        public HotbarPacket(String code) { this.code = code; }
        public static void encode(HotbarPacket p, FriendlyByteBuf buf) { buf.writeUtf(p.code); }
        public static HotbarPacket decode(FriendlyByteBuf buf) { return new HotbarPacket(buf.readUtf()); }
    }

    /**
     * 使用七彩粉末合成色块（UI合成模式下点击色块）。
     */
    public static class CraftItemPacket {
        public final String code;
        public CraftItemPacket(String code) { this.code = code; }
        public static void encode(CraftItemPacket p, FriendlyByteBuf buf) { buf.writeUtf(p.code); }
        public static CraftItemPacket decode(FriendlyByteBuf buf) { return new CraftItemPacket(buf.readUtf()); }
    }

    /**
     * 合成台选择颜色（七彩粉末模式下从右侧列表选择颜色）。
     */
    public static class SelectCraftingColorPacket {
        public final String code;
        public SelectCraftingColorPacket(String code) { this.code = code; }
        public static void encode(SelectCraftingColorPacket p, FriendlyByteBuf buf) { buf.writeUtf(p.code); }
        public static SelectCraftingColorPacket decode(FriendlyByteBuf buf) { return new SelectCraftingColorPacket(buf.readUtf()); }
    }

    private MardNetwork() {}
}
