package com.mard.pixel.forge;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 简单的合成容器实现。
 * 用于在方块实体中进行配方匹配。
 */
public class SimpleCraftingContainer implements CraftingContainer {

    private final int width;
    private final int height;
    private final NonNullList<ItemStack> items;

    public SimpleCraftingContainer(int width, int height) {
        this.width = width;
        this.height = height;
        this.items = NonNullList.withSize(width * height, ItemStack.EMPTY);
    }

    public SimpleCraftingContainer(int width, int height, List<ItemStack> stacks) {
        this(width, height);
        for (int i = 0; i < Math.min(stacks.size(), items.size()); i++) {
            items.set(i, stacks.get(i).copy());
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public @NotNull List<ItemStack> getItems() {
        return items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot >= 0 && slot < items.size()) {
            items.set(slot, stack);
        }
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(items, slot, amount);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public void setChanged() {
        // 不需要通知
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }
}
