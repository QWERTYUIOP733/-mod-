/*
 * Copyright (c) 2026 Color Blocks Extension
 * SPDX-License-Identifier: MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.mard.pixel.forge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * Color Blocks Mod 网络包管理（Forge 1.21.1 ChannelBuilder/SimpleChannel）。
 */
public final class MardNetwork {
    public static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(MardPixelForge.MODID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel();

    public static void init() {
        CHANNEL.messageBuilder(RequestItemPacket.class)
                .encoder(RequestItemPacket::encode)
                .decoder(RequestItemPacket::decode)
                .consumerMainThread(MardNetwork::handleRequestItem)
                .add();
        CHANNEL.messageBuilder(HotbarPacket.class)
                .encoder(HotbarPacket::encode)
                .decoder(HotbarPacket::decode)
                .consumerMainThread(MardNetwork::handleHotbar)
                .add();
        CHANNEL.messageBuilder(CraftItemPacket.class)
                .encoder(CraftItemPacket::encode)
                .decoder(CraftItemPacket::decode)
                .consumerMainThread(MardNetwork::handleCraftItem)
                .add();
        CHANNEL.messageBuilder(SelectCraftingColorPacket.class)
                .encoder(SelectCraftingColorPacket::encode)
                .decoder(SelectCraftingColorPacket::decode)
                .consumerMainThread(MardNetwork::handleSelectCraftingColor)
                .add();
    }

    private static void handleRequestItem(RequestItemPacket p, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;
        if (!player.isCreative() && !player.isSpectator()) {
            player.sendSystemMessage(Component.literal(
                "生存模式下无法直接获取方块，请使用方块染色台或七彩粉末合成").withStyle(ChatFormatting.RED));
            return;
        }
        MardPixelForge.giveRequestedStack(player, p.target);
        ctx.setPacketHandled(true);
    }

    private static void handleHotbar(HotbarPacket p, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;
        if (!player.isCreative() && !player.isSpectator()) {
            player.sendSystemMessage(Component.literal(
                "生存模式下无法直接获取方块，请使用方块染色台或七彩粉末合成").withStyle(ChatFormatting.RED));
            return;
        }
        MardPixelForge.giveToHotbar(player, p.code);
        ctx.setPacketHandled(true);
    }

    private static void handleCraftItem(CraftItemPacket p, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) MardPixelForge.craftWithPigment(player, p.code);
        ctx.setPacketHandled(true);
    }

    private static void handleSelectCraftingColor(SelectCraftingColorPacket p, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null && player.containerMenu instanceof MardCraftingMenu menu) {
            if (menu.getBlockEntity() != null) {
                menu.getBlockEntity().selectColor(p.code);
            }
        }
        ctx.setPacketHandled(true);
    }

    // ==================== 网络包类 ====================

    public static class RequestItemPacket {
        public final String target;
        public RequestItemPacket(String target) { this.target = target; }
        public static void encode(RequestItemPacket p, FriendlyByteBuf b) { b.writeUtf(p.target); }
        public static RequestItemPacket decode(FriendlyByteBuf b) { return new RequestItemPacket(b.readUtf()); }
    }

    public static class HotbarPacket {
        public final String code;
        public HotbarPacket(String code) { this.code = code; }
        public static void encode(HotbarPacket p, FriendlyByteBuf buf) { buf.writeUtf(p.code); }
        public static HotbarPacket decode(FriendlyByteBuf buf) { return new HotbarPacket(buf.readUtf()); }
    }

    public static class CraftItemPacket {
        public final String code;
        public CraftItemPacket(String code) { this.code = code; }
        public static void encode(CraftItemPacket p, FriendlyByteBuf buf) { buf.writeUtf(p.code); }
        public static CraftItemPacket decode(FriendlyByteBuf buf) { return new CraftItemPacket(buf.readUtf()); }
    }

    public static class SelectCraftingColorPacket {
        public final String code;
        public SelectCraftingColorPacket(String code) { this.code = code; }
        public static void encode(SelectCraftingColorPacket p, FriendlyByteBuf buf) { buf.writeUtf(p.code); }
        public static SelectCraftingColorPacket decode(FriendlyByteBuf buf) { return new SelectCraftingColorPacket(buf.readUtf()); }
    }

    private MardNetwork() {}
}
