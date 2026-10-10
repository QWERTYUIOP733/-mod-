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

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 方块染色台方块实体。
 * 存储3x3合成网格（9格）和结果槽（1格）。
 * 支持七彩粉末颜色选择模式：放入七彩粉末后，可从右侧列表选择颜色合成。
 *
 * 26.3：存档由 CompoundTag 改为 ValueOutput/ValueInput（ContainerHelper 不再接收 HolderLookup.Provider）；
 * level.isClientSide 字段改 isClientSide() 方法；assemble 改单参。
 */
public class MardCraftingTableBlockEntity extends BlockEntity {

    public static final int GRID_SIZE = 9;
    public static final int RESULT_SLOT = 9;
    public static final int TOTAL_SLOTS = 10;

    private String selectedColor = ""; // 当前选择的颜色色号，如"A1"

    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            if (slot < GRID_SIZE && level != null && !level.isClientSide()) {
                updateCraftingResult();
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != RESULT_SLOT;
        }
    };

    private final LazyOptional<IItemHandler> inventoryHandler = LazyOptional.of(() -> inventory);

    public MardCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(MardPixelForge._CRAFTING_TABLE_BE.get(), pos, state);
    }

    public IItemHandler getInventory() {
        return inventory;
    }

    public String getSelectedColor() {
        return selectedColor;
    }

    /**
     * 选择颜色（由客户端网络包调用）。
     */
    public void selectColor(String code) {
        this.selectedColor = code != null ? code : "";
        if (level != null && !level.isClientSide()) {
            updateCraftingResult();
            setChanged();
        }
    }

    /**
     * 检查合成网格中是否有七彩粉末。
     * 使用物品注册名判断，更可靠。
     */
    private boolean hasPigment() {
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && isPigmentItem(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断物品是否为七彩粉末。
     * 使用物品注册名判断，兼容各种情况。
     */
    private boolean isPigmentItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() == MardPixelForge._PIGMENT.get()) return true;
        String registryName = stack.getItem().getDescriptionId();
        return "item.mard_pixel.mard_pigment".equals(registryName);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryHandler.invalidate();
    }

    /**
     * 获取合成网格的物品列表。
     */
    public NonNullList<ItemStack> getGridItems() {
        NonNullList<ItemStack> items = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < GRID_SIZE; i++) {
            items.set(i, inventory.getStackInSlot(i).copy());
        }
        return items;
    }

    /**
     * 更新合成结果。
     * 如果有七彩粉末且选择了颜色，则结果为对应颜色的64个方块。
     * 否则使用原版配方系统，只允许模组内物品。
     */
    private void updateCraftingResult() {
        if (level == null || level.isClientSide()) return;

        // 七彩粉末颜色选择模式
        if (hasPigment() && !selectedColor.isEmpty()) {
            ItemStack result = MardPixelForge.buildStack(selectedColor);
            if (!result.isEmpty()) {
                result.setCount(64);
                inventory.setStackInSlot(RESULT_SLOT, result);
                return;
            }
        }

        // 原版配方系统（只允许模组内物品）
        java.util.List<ItemStack> gridItems = new java.util.ArrayList<>();
        for (int i = 0; i < GRID_SIZE; i++) {
            gridItems.add(inventory.getStackInSlot(i).copy());
        }
        net.minecraft.world.item.crafting.CraftingInput craftingInput =
                net.minecraft.world.item.crafting.CraftingInput.of(3, 3, gridItems);

        Optional<net.minecraft.world.item.crafting.RecipeHolder<CraftingRecipe>> recipe = Optional.empty();
        // Level.getRecipeManager() 移除，改用 ServerLevel.recipeAccess()，其实现即 RecipeManager
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && serverLevel.recipeAccess() instanceof net.minecraft.world.item.crafting.RecipeManager recipeManager) {
            recipe = recipeManager.getRecipeFor(RecipeType.CRAFTING, craftingInput, serverLevel);
        }

        if (recipe.isPresent()) {
            ItemStack result = recipe.get().value().assemble(craftingInput);
            if (isMardPixelItem(result)) {
                inventory.setStackInSlot(RESULT_SLOT, result);
            } else {
                inventory.setStackInSlot(RESULT_SLOT, ItemStack.EMPTY);
            }
        } else {
            inventory.setStackInSlot(RESULT_SLOT, ItemStack.EMPTY);
        }
    }

    /**
     * 检查物品是否属于模组内物品。
     */
    private boolean isMardPixelItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String registryName = stack.getItem().getDescriptionId();
        return registryName.startsWith("item.mard_pixel.") || registryName.startsWith("block.mard_pixel.");
    }

    /**
     * 消耗合成材料（玩家取走结果时调用）。
     * 七彩粉末模式下只消耗七彩粉末，普通模式下消耗所有材料。
     */
    public void consumeMaterials() {
        if (hasPigment() && !selectedColor.isEmpty()) {
            // 七彩粉末模式：只消耗七彩粉末
            for (int i = 0; i < GRID_SIZE; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty() && isPigmentItem(stack)) {
                    stack.shrink(1);
                }
            }
        } else {
            // 普通模式：消耗所有材料
            for (int i = 0; i < GRID_SIZE; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    stack.shrink(1);
                }
            }
        }
        inventory.setStackInSlot(RESULT_SLOT, ItemStack.EMPTY);
        if (level != null && !level.isClientSide()) {
            updateCraftingResult();
        }
    }

    /**
     * 破坏方块时掉落内部所有物品（含结果槽）。
     */
    public void dropContents() {
        if (level == null || level.isClientSide()) return;
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, worldPosition, stack.copy());
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        NonNullList<ItemStack> allItems = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            allItems.set(i, inventory.getStackInSlot(i).copy());
        }
        ContainerHelper.saveAllItems(out, allItems);
        out.putString("SelectedColor", selectedColor);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        NonNullList<ItemStack> allItems = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, allItems);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            inventory.setStackInSlot(i, allItems.get(i));
        }
        selectedColor = in.getStringOr("SelectedColor", "");
    }

    public Component getDisplayName() {
        return Component.translatable("block.mard_pixel.mard_crafting_table");
    }
}
