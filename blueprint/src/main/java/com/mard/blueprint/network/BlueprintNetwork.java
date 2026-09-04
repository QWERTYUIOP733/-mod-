package com.mard.blueprint.network;

import com.mard.blueprint.MardBlueprintMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 附属模组网络包。
 * 用于客户端向服务端发送图纸生成请求。
 */
public class BlueprintNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MardBlueprintMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++, GenerateBlueprintPacket.class,
                GenerateBlueprintPacket::encode, GenerateBlueprintPacket::decode, GenerateBlueprintPacket::handle);
    }

    /**
     * 生成图纸请求包。
     * 客户端发送给服务端，请求在指定位置生成图纸。
     */
    public static class GenerateBlueprintPacket {
        private final int startX;
        private final int startY;
        private final int startZ;
        private final int width;
        private final int height;
        private final String[] colorCodes; // 扁平化的颜色码数组

        public GenerateBlueprintPacket(int startX, int startY, int startZ, int width, int height, String[] colorCodes) {
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
            this.width = width;
            this.height = height;
            this.colorCodes = colorCodes;
        }

        public static void encode(GenerateBlueprintPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.startX);
            buf.writeInt(msg.startY);
            buf.writeInt(msg.startZ);
            buf.writeInt(msg.width);
            buf.writeInt(msg.height);
            buf.writeInt(msg.colorCodes.length);
            for (String code : msg.colorCodes) {
                buf.writeUtf(code == null ? "" : code);
            }
        }

        public static GenerateBlueprintPacket decode(FriendlyByteBuf buf) {
            int startX = buf.readInt();
            int startY = buf.readInt();
            int startZ = buf.readInt();
            int width = buf.readInt();
            int height = buf.readInt();
            int length = buf.readInt();
            String[] colorCodes = new String[length];
            for (int i = 0; i < length; i++) {
                String code = buf.readUtf();
                colorCodes[i] = code.isEmpty() ? null : code;
            }
            return new GenerateBlueprintPacket(startX, startY, startZ, width, height, colorCodes);
        }

        public static void handle(GenerateBlueprintPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                // 在服务端生成图纸方块
                BlockPos startPos = new BlockPos(msg.startX, msg.startY, msg.startZ);
                for (int y = 0; y < msg.height; y++) {
                    for (int x = 0; x < msg.width; x++) {
                        String code = msg.colorCodes[y * msg.width + x];
                        if (code == null) continue;

                        BlockPos pos = startPos.offset(x, 0, y);
                        // 调用主模组的方块注册，放置对应颜色的方块
                        // 实际实现需要通过主模组的API获取对应方块
                        placeMardBlock(player, pos, code);
                    }
                }

                MardBlueprintMod.LOGGER.info("图纸生成完成: {}x{} at {}", msg.width, msg.height, startPos);
            });
            ctx.get().setPacketHandled(true);
        }

        /**
         * 放置MARD色块。
         * 通过反射或主模组API获取对应方块并放置。
         */
        private static void placeMardBlock(ServerPlayer player, BlockPos pos, String code) {
            try {
                // 通过主模组的方块注册表获取对应方块
                // 实际实现需要主模组提供API
                Class<?> mardBlockClass = Class.forName("com.mard.pixel.forge.MardPixelForge");
                // 这里预留主模组API调用
                // 实际实现时调用主模组的getBlockByCode(code)方法
                MardBlueprintMod.LOGGER.debug("放置MARD色块 {} at {}", code, pos);
            } catch (ClassNotFoundException e) {
                MardBlueprintMod.LOGGER.error("未找到主模组MardPixelForge类", e);
            }
        }
    }

    /**
     * 发送生成图纸请求到服务端。
     */
    public static void sendGenerateRequest(int startX, int startY, int startZ, int width, int height, String[] colorCodes) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(),
                new GenerateBlueprintPacket(startX, startY, startZ, width, height, colorCodes));
    }
}
