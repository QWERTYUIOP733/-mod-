package com.mard.pixel.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * MARD 合成台方块实体。
 * 存储3x3合成网格（9格）和结果槽（1格）。
 * 合成结果只允许模组内物品。
 */
public class MardCraftingTableBlockEntity extends BlockEntity {

    public static final int GRID_SIZE = 9;
    public static final int RESULT_SLOT = 9;
    public static final int TOTAL_SLOTS = 10;

    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            if (slot < GRID_SIZE && level != null && !level.isClientSide) {
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
        super(MardPixelForge.MARD_CRAFTING_TABLE_BE.get(), pos, state);
    }

    public IItemHandler getInventory() {
        return inventory;
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
     * 更新合成结果。只允许模组内物品。
     */
    private void updateCraftingResult() {
        if (level == null || level.isClientSide) return;

        TransientCraftingContainer craftingContainer = new TransientCraftingContainer(null, 3, 3);
        NonNullList<ItemStack> gridItems = getGridItems();
        for (int i = 0; i < GRID_SIZE; i++) {
            craftingContainer.setItem(i, gridItems.get(i));
        }

        Optional<CraftingRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftingContainer, level);

        if (recipe.isPresent()) {
            ItemStack result = recipe.get().assemble(craftingContainer, level.registryAccess());
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
     */
    public void consumeMaterials() {
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stack.shrink(1);
            }
        }
        inventory.setStackInSlot(RESULT_SLOT, ItemStack.EMPTY);
        if (level != null && !level.isClientSide) {
            updateCraftingResult();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        NonNullList<ItemStack> allItems = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            allItems.set(i, inventory.getStackInSlot(i).copy());
        }
        ContainerHelper.saveAllItems(tag, allItems);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        NonNullList<ItemStack> allItems = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, allItems);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            inventory.setStackInSlot(i, allItems.get(i));
        }
    }

    public Component getDisplayName() {
        return Component.translatable("block.mard_pixel.mard_crafting_table");
    }
}
