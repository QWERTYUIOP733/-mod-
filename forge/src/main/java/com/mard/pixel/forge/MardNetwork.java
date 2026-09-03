package com.mard.pixel.forge;

import com.mard.pixel.common.CustomColor;
import com.mard.pixel.common.CustomColorStore;
import com.mard.pixel.common.CustomColorFile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.function.Supplier;

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
        CHANNEL.messageBuilder(SwitchBagSystemPacket.class, id++)
                .encoder(SwitchBagSystemPacket::encode)
                .decoder(SwitchBagSystemPacket::decode)
                .consumerMainThread(MardNetwork::handleSwitchBag)
                .add();
        CHANNEL.messageBuilder(SyncExternalBrandsPacket.class, id++)
                .encoder(SyncExternalBrandsPacket::encode)
                .decoder(SyncExternalBrandsPacket::decode)
                .consumerMainThread(MardNetwork::handleSyncExternal)
                .add();
    }

    public static class RequestItemPacket {
        public final String target;
        public RequestItemPacket(String target) { this.target = target; }
        public static void encode(RequestItemPacket p, FriendlyByteBuf b) { b.writeUtf(p.target); }
        public static RequestItemPacket decode(FriendlyByteBuf b) { return new RequestItemPacket(b.readUtf()); }
    }

    public static class SyncCustomPacket {
        public final String json;
        public SyncCustomPacket(String json) { this.json = json; }
        public static void encode(SyncCustomPacket p, FriendlyByteBuf b) { b.writeUtf(p.json); }
        public static SyncCustomPacket decode(FriendlyByteBuf b) { return new SyncCustomPacket(b.readUtf()); }
    }

    public static class AddCustomPacket {
        public final String name; public final int rgb;
        public AddCustomPacket(String name, int rgb) { this.name = name; this.rgb = rgb; }
        public static void encode(AddCustomPacket p, FriendlyByteBuf b) { b.writeUtf(p.name); b.writeInt(p.rgb); }
        public static AddCustomPacket decode(FriendlyByteBuf b) { return new AddCustomPacket(b.readUtf(), b.readInt()); }
    }

    public static class RemoveCustomPacket {
        public final String code;
        public RemoveCustomPacket(String code) { this.code = code; }
        public static void encode(RemoveCustomPacket p, FriendlyByteBuf b) { b.writeUtf(p.code); }
        public static RemoveCustomPacket decode(FriendlyByteBuf b) { return new RemoveCustomPacket(b.readUtf()); }
    }

    public static class SwitchBagSystemPacket {
        public final String system;
        public SwitchBagSystemPacket(String system) { this.system = system; }
        public static void encode(SwitchBagSystemPacket p, FriendlyByteBuf b) { b.writeUtf(p.system); }
        public static SwitchBagSystemPacket decode(FriendlyByteBuf b) { return new SwitchBagSystemPacket(b.readUtf()); }
    }

    public static class SyncExternalBrandsPacket {
        public final Map<String, String> brands;
        public SyncExternalBrandsPacket(Map<String, String> brands) { this.brands = brands; }
        public static void encode(SyncExternalBrandsPacket p, FriendlyByteBuf b) {
            b.writeInt(p.brands.size());
            for (Map.Entry<String, String> e : p.brands.entrySet()) {
                b.writeUtf(e.getKey()); b.writeUtf(e.getValue());
            }
        }
        public static SyncExternalBrandsPacket decode(FriendlyByteBuf b) {
            int n = b.readInt();
            java.util.HashMap<String, String> m = new java.util.HashMap<>();
            for (int i = 0; i < n; i++) m.put(b.readUtf(), b.readUtf());
            return new SyncExternalBrandsPacket(m);
        }
    }

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

    private static void handleSwitchBag(SwitchBagSystemPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) MardPixelForge.switchBagSystem(player, p.system);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleSyncExternal(SyncExternalBrandsPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MardPixelForge.clientExternalBrands = new java.util.LinkedHashMap<>(p.brands);
        });
        ctx.get().setPacketHandled(true);
    }

    private MardNetwork() {}
}
