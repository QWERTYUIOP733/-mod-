package com.mard.pixel.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MardCustomBlockEntity extends BlockEntity {
    private int color = 0xFFFFFF;

    public MardCustomBlockEntity(BlockPos pos, BlockState state) {
        super(MardPixelFabric.CUSTOM_BE_TYPE, pos, state);
    }

    public void setColor(int c) { this.color = c & 0xFFFFFF; setChanged(); }
    public int getColor() { return color; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("mard_color", color);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("mard_color")) color = tag.getInt("mard_color") & 0xFFFFFF;
    }
}
