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
            // UI 点击色块时给予一组（64个）方块
            if (player != null) MardPixelForge.giveRequestedStack(player, p.target);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleHotbar(HotbarPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) MardPixelForge.giveToHotbar(player, p.code);
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

    private MardNetwork() {}
}
