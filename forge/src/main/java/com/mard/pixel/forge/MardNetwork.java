package com.mard.pixel.forge;

import com.mard.pixel.common.CustomColorFile;
import com.mard.pixel.common.CustomColorStore;
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
 * 1. RequestItemPacket - 客户端请求物品（/mardp give 的 UI 版本）
 * 2. SyncCustomPacket - 服务端→客户端同步自定义色列表
 * 3. AddCustomPacket - 客户端→服务端新增自定义色
 * 4. RemoveCustomPacket - 客户端→服务端删除自定义色
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
        CHANNEL.messageBuilder(SyncCustomPacket.class, id++)
                .encoder(SyncCustomPacket::encode)
                .decoder(SyncCustomPacket::decode)
                .consumerMainThread(MardNetwork::handleSyncCustom)
                .add();
        CHANNEL.messageBuilder(AddCustomPacket.class, id++)
                .encoder(AddCustomPacket::encode)
                .decoder(AddCustomPacket::decode)
                .consumerMainThread(MardNetwork::handleAddCustom)
                .add();
        CHANNEL.messageBuilder(RemoveCustomPacket.class, id++)
                .encoder(RemoveCustomPacket::encode)
                .decoder(RemoveCustomPacket::decode)
                .consumerMainThread(MardNetwork::handleRemoveCustom)
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

    /**
     * 服务端→客户端同步自定义色列表（JSON 格式）。
     */
    public static class SyncCustomPacket {
        public final String json;
        public SyncCustomPacket(String json) { this.json = json; }
        public static void encode(SyncCustomPacket p, FriendlyByteBuf b) { b.writeUtf(p.json); }
        public static SyncCustomPacket decode(FriendlyByteBuf b) { return new SyncCustomPacket(b.readUtf()); }
    }

    /**
     * 客户端→服务端新增自定义色。
     */
    public static class AddCustomPacket {
        public final String name;
        public final int rgb;
        public AddCustomPacket(String name, int rgb) { this.name = name; this.rgb = rgb; }
        public static void encode(AddCustomPacket p, FriendlyByteBuf b) { b.writeUtf(p.name); b.writeInt(p.rgb); }
        public static AddCustomPacket decode(FriendlyByteBuf b) { return new AddCustomPacket(b.readUtf(), b.readInt()); }
    }

    /**
     * 客户端→服务端删除自定义色。
     */
    public static class RemoveCustomPacket {
        public final String code;
        public RemoveCustomPacket(String code) { this.code = code; }
        public static void encode(RemoveCustomPacket p, FriendlyByteBuf b) { b.writeUtf(p.code); }
        public static RemoveCustomPacket decode(FriendlyByteBuf b) { return new RemoveCustomPacket(b.readUtf()); }
    }

    // ==================== 网络包处理 ====================

    private static void handleRequestItem(RequestItemPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) MardPixelForge.giveRequestedItem(player, p.target);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleSyncCustom(SyncCustomPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CustomColorStore.setAll(CustomColorFile.parse(p.json));
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleAddCustom(AddCustomPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) MardPixelForge.addCustom(player, p.name, p.rgb);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleRemoveCustom(RemoveCustomPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) MardPixelForge.removeCustom(player, p.code);
        });
        ctx.get().setPacketHandled(true);
    }

    private MardNetwork() {}
}
