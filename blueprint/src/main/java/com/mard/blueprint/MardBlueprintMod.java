package com.mard.blueprint;

import com.mard.blueprint.network.BlueprintNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MARD Pixel Blueprint 主类。
 *
 * 附属模组：导入拼豆图纸图片，自动映射MARD颜色，一键生成像素画。
 * 依赖主模组 mard_pixel。
 */
@Mod(MardBlueprintMod.MOD_ID)
public class MardBlueprintMod {

    public static final String MOD_ID = "mard_pixel_blueprint";
    public static final Logger LOGGER = LoggerFactory.getLogger("MARD Blueprint");

    public MardBlueprintMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("MARD Pixel Blueprint 初始化完成");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BlueprintNetwork.register();
            LOGGER.info("MARD Pixel Blueprint 网络包注册完成");
        });
    }
}
