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

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 安全的合成容器实现，用于在方块实体中查找配方。
 * 不关联任何菜单，setChanged()为空实现，避免空指针崩溃。
 */
public class SafeCraftingContainer implements CraftingContainer {

    private final int width;
    private final int height;
    private final NonNullList<ItemStack> items;

    public SafeCraftingContainer(int width, int height) {
        this.width = width;
        this.height = height;
        this.items = NonNullList.withSize(width * height, ItemStack.EMPTY);
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
        if (slot >= 0 && slot < items.size()) {
            return items.get(slot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot >= 0 && slot < items.size()) {
            ItemStack stack = items.get(slot);
            ItemStack result = stack.copy();
            result.setCount(Math.min(amount, stack.getCount()));
            stack.shrink(amount);
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (slot >= 0 && slot < items.size()) {
            ItemStack stack = items.get(slot);
            items.set(slot, ItemStack.EMPTY);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot >= 0 && slot < items.size()) {
            items.set(slot, stack);
        }
    }

    @Override
    public void setChanged() {
        // 空实现，不关联菜单，避免空指针
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public @NotNull NonNullList<ItemStack> getItems() {
        return items;
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
    public void fillStackedContents(@NotNull net.minecraft.world.entity.player.StackedContents helper) {
        for (ItemStack stack : items) {
            helper.accountSimpleStack(stack);
        }
    }
}
