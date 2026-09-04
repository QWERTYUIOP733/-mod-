package com.mard.blueprint.client;

import com.mard.blueprint.MardBlueprintMod;
import com.mard.blueprint.blueprint.ColorMapper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 附属模组客户端主类。
 * 注册按键绑定、颜色映射初始化、UI界面打开。
 */
@Mod.EventBusSubscriber(modid = MardBlueprintMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MardBlueprintClient {

    public static final KeyMapping OPEN_BLUEPRINT_KEY = new KeyMapping(
            "key.mard_pixel_blueprint.open",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.mard_pixel"
    );

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 初始化颜色映射器（从主模组获取MARD 221色数据）
            initColorMapper();
            MardBlueprintMod.LOGGER.info("MARD Pixel Blueprint 客户端初始化完成");
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BLUEPRINT_KEY);
    }

    /**
     * 初始化颜色映射器。
     * 从主模组的MardPalette获取221色数据。
     */
    private static void initColorMapper() {
        try {
            Class<?> mardPaletteClass = Class.forName("com.mard.pixel.common.MardPalette");
            // 获取颜色列表
            java.lang.reflect.Field colorsField = mardPaletteClass.getField("COLORS");
            Object colors = colorsField.get(null);

            if (colors instanceof java.util.List<?> colorList) {
                String[] codes = new String[colorList.size()];
                int[] rgbs = new int[colorList.size()];

                for (int i = 0; i < colorList.size(); i++) {
                    Object color = colorList.get(i);
                    // 调用code()和rgb()方法
                    java.lang.reflect.Method codeMethod = color.getClass().getMethod("code");
                    java.lang.reflect.Method rgbMethod = color.getClass().getMethod("rgb");
                    codes[i] = (String) codeMethod.invoke(color);
                    rgbs[i] = (int) rgbMethod.invoke(color);
                }

                ColorMapper.init(codes, rgbs);
                MardBlueprintMod.LOGGER.info("颜色映射器初始化完成，加载 {} 种MARD颜色", codes.length);
            }
        } catch (Exception e) {
            MardBlueprintMod.LOGGER.error("颜色映射器初始化失败", e);
        }
    }

    /**
     * 打开图纸导入界面。
     */
    public static void openBlueprintScreen() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        mc.setScreen(new BlueprintScreen());
    }
}
