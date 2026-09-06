package com.mard.pixel.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * MARD 合成台方块实体。
 * 存储3x3合成网格（9格）和结果槽（1格）。
 * 合成结果只允许模组内物品。
 */
public class MardCraftingTableBlockEntity extends BlockEntity {

    public static final int GRID_SIZE = 9; // 3x3 合成网格
    public static final int RESULT_SLOT = 9; // 结果槽索引
    public static final int TOTAL_SLOTS = 10; // 总槽位数

    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            if (slot < GRID_SIZE) {
                updateCraftingResult();
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == RESULT_SLOT) {
                return false; // 结果槽不能手动放入
            }
            return true;
        }
    };

    public MardCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(MardPixelForge.MARD_CRAFTING_TABLE_BE.get(), pos, state);
    }

    public IItemHandler getInventory() {
        return inventory;
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
     * 只允许合成模组内物品。
     */
    private void updateCraftingResult() {
        if (level == null || level.isClientSide) return;

        // 创建临时合成容器
        SimpleCraftingContainer craftingContainer = new SimpleCraftingContainer(3, 3, getGridItems());

        // 查找匹配的配方
        Optional<CraftingRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftingContainer, level);

        if (recipe.isPresent()) {
            ItemStack result = recipe.get().assemble(craftingContainer, level.registryAccess());
            // 只允许模组内物品
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
        String itemId = stack.getItem().getDescriptionId();
        return itemId.startsWith("item.mard_pixel.") || itemId.startsWith("block.mard_pixel.");
    }

    /**
     * 消耗合成材料（玩家取走结果时调用）。
     */
    public void consumeMaterials() {
        // 消耗合成网格中的材料
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }

        inventory.setStackInSlot(RESULT_SLOT, ItemStack.EMPTY);
        updateCraftingResult();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, getGridItems());
        // 保存结果槽
        if (!inventory.getStackInSlot(RESULT_SLOT).isEmpty()) {
            CompoundTag resultTag = new CompoundTag();
            inventory.getStackInSlot(RESULT_SLOT).save(resultTag);
            tag.put("Result", resultTag);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        NonNullList<ItemStack> items = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < GRID_SIZE; i++) {
            inventory.setStackInSlot(i, items.get(i));
        }
        // 加载结果槽
        if (tag.contains("Result")) {
            ItemStack result = ItemStack.of(tag.getCompound("Result"));
            inventory.setStackInSlot(RESULT_SLOT, result);
        }
    }

    public Component getDisplayName() {
        return Component.translatable("block.mard_pixel.mard_crafting_table");
    }
}
