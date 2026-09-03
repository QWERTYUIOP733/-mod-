package com.mard.pixel.forge;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class MardBlock extends Block {
    private final String code;
    private final int rgb;

    public MardBlock(String code, int rgb) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(0.6f, 1.0f));
        this.code = code;
        this.rgb = rgb;
    }

    public String code() { return code; }
    public int rgb() { return rgb; }
}
