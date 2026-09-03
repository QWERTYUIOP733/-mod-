package com.mard.pixel.fabric.client;

import com.mard.pixel.common.CustomColorFile;
import com.mard.pixel.common.CustomColorStore;
import com.mard.pixel.fabric.MardBlock;
import com.mard.pixel.fabric.MardCustomBlockEntity;
import com.mard.pixel.fabric.MardPixelFabric;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.glfw.GLFW;

public class MardPixelFabricClient implements ClientModInitializer {

    public static final KeyBinding OPEN_KEY = new KeyBinding(
            "key.mard_pixel.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.mard_pixel");

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(OPEN_KEY);

        ColorProviderRegistry.BLOCK.register((state, view, pos, tint) -> {
            if (view != null && pos != null && view.getBlockEntity(pos) instanceof MardCustomBlockEntity mbe) {
                return mbe.getColor();
            }
            return state.getBlock() instanceof MardBlock mb ? mb.rgb() : 0xFFFFFF;
        }, MardPixelFabric.MARD_BLOCKS.toArray(new net.minecraft.block.Block[0]));
        ColorProviderRegistry.BLOCK.register((state, view, pos, tint) -> {
            if (view != null && pos != null && view.getBlockEntity(pos) instanceof MardCustomBlockEntity mbe) {
                return mbe.getColor();
            }
            return 0xFFFFFF;
        }, MardPixelFabric.CUSTOM_BLOCK);

        for (MardBlock mb : MardPixelFabric.MARD_BLOCKS) {
            ColorProviderRegistry.ITEM.register((stack, tint) -> mb.rgb(), mb);
        }
        ColorProviderRegistry.ITEM.register((stack, tint) -> {
            NbtCompound tag = stack.getNbt();
            return (tag != null && tag.contains("mard_color")) ? tag.getInt("mard_color") & 0xFFFFFF : 0xFFFFFF;
        }, MardPixelFabric.CUSTOM_ITEM);

        ClientPlayNetworking.registerGlobalReceiver(MardPixelFabric.SYNC_CUSTOM, (client, handler, buf, responseSender) -> {
            String json = buf.readString();
            client.execute(() -> CustomColorStore.setAll(CustomColorFile.parse(json)));
        });
        ClientPlayNetworking.registerGlobalReceiver(MardPixelFabric.SYNC_BRANDS, (client, handler, buf, responseSender) -> {
            int n = buf.readInt();
            java.util.LinkedHashMap<String, String> m = new java.util.LinkedHashMap<>();
            for (int i = 0; i < n; i++) m.put(buf.readString(), buf.readString());
            final java.util.Map<String, String> copy = m;
            client.execute(() -> MardPixelFabric.clientExternalBrands = copy);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (OPEN_KEY.wasPressed() && client.player != null) {
                client.setScreen(new MardColorScreen());
            }
        });
    }

    public static void sendRequestItem(String target) {
        var buf = PacketByteBufs.create();
        buf.writeString(target);
        ClientPlayNetworking.send(MardPixelFabric.REQUEST_ITEM, buf);
    }

    public static void sendAddCustom(String name, int rgb) {
        var buf = PacketByteBufs.create();
        buf.writeString(name);
        buf.writeInt(rgb);
        ClientPlayNetworking.send(MardPixelFabric.ADD_CUSTOM, buf);
    }

    public static void sendRemoveCustom(String code) {
        var buf = PacketByteBufs.create();
        buf.writeString(code);
        ClientPlayNetworking.send(MardPixelFabric.REMOVE_CUSTOM, buf);
    }

    public static void sendSwitchSystem(String system) {
        var buf = PacketByteBufs.create();
        buf.writeString(system);
        ClientPlayNetworking.send(MardPixelFabric.SWITCH_SYSTEM, buf);
    }
}
