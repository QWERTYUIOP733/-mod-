package com.mard.pixel.forge.client;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * MARD Pixel Mod 客户端配置。
 *
 * 配置项：
 * 1. mainTabVisible - 主标签页是否显示（true/false）
 *
 * 配置文件位置：config/mard_pixel_client.properties
 */
public final class MardClientConfig {

    /** 配置文件路径 */
    private static final Path CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("mard_pixel_client.properties");

    /** 主标签页是否显示（默认显示） */
    private static boolean mainTabVisible = true;

    /** 配置是否已加载 */
    private static boolean loaded = false;

    private MardClientConfig() {}

    /**
     * 加载客户端配置。
     * 如果配置文件不存在，使用默认值。
     */
    public static synchronized void load() {
        if (loaded) return;
        loaded = true;

        Properties props = new Properties();
        if (Files.exists(CONFIG_FILE)) {
            try {
                props.load(Files.newBufferedReader(CONFIG_FILE));
                mainTabVisible = Boolean.parseBoolean(props.getProperty("mainTabVisible", "true"));
            } catch (IOException e) {
                // 加载失败，使用默认值
            }
        }
    }

    /**
     * 保存客户端配置到文件。
     */
    public static synchronized void save() {
        Properties props = new Properties();
        props.setProperty("mainTabVisible", String.valueOf(mainTabVisible));
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            props.store(Files.newBufferedWriter(CONFIG_FILE), "MARD Pixel Mod Client Config");
        } catch (IOException e) {
            // 保存失败，忽略
        }
    }

    /**
     * 主标签页是否显示。
     * @return true 如果主标签页显示
     */
    public static boolean isMainTabVisible() {
        load();
        return mainTabVisible;
    }

    /**
     * 设置主标签页是否显示。
     * @param visible true 显示，false 隐藏
     */
    public static synchronized void setMainTabVisible(boolean visible) {
        load();
        if (mainTabVisible != visible) {
            mainTabVisible = visible;
            save();
            // 触发标签页内容重新构建
            rebuildCreativeTabs();
        }
    }

    /**
     * 切换主标签页显示状态。
     * @return 切换后的状态
     */
    public static boolean toggleMainTabVisible() {
        setMainTabVisible(!mainTabVisible);
        return mainTabVisible;
    }

    /**
     * 重新构建所有创造模式标签页的内容。
     * 当配置变更时调用，使主标签页的显示/隐藏立即生效。
     */
    private static void rebuildCreativeTabs() {
        try {
            // 遍历所有创造模式标签页，重新构建内容
            for (net.minecraft.world.item.CreativeModeTab tab :
                    net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB) {
                if (tab != null) {
                    // 调用 buildContents() 重新构建标签页内容
                    // 这是 Forge 1.20.1 中的内部方法
                    try {
                        java.lang.reflect.Method method =
                                net.minecraft.world.item.CreativeModeTab.class.getDeclaredMethod("buildContents");
                        method.setAccessible(true);
                        method.invoke(tab);
                    } catch (Exception e) {
                        // 反射调用失败，忽略
                    }
                }
            }
        } catch (Exception e) {
            // 重新构建失败，忽略
        }
    }
}
