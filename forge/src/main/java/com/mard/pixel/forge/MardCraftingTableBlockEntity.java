package com.mard.pixel.forge;

import com.mard.pixel.common.MardPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
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
 * 方块染色台方块实体。
 * 存储3x3合成网格（9格）和结果槽（1格）。
 * 支持七彩粉末颜色选择模式：放入七彩粉末后，可从右侧列表选择颜色合成。
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

    public String getSelectedColor() {
        return selectedColor;
    }

    /**
     * 选择颜色（由客户端网络包调用）。
     */
    public void selectColor(String code) {
        this.selectedColor = code != null ? code : "";
        if (level != null && !level.isClientSide) {
            updateCraftingResult();
            setChanged();
        }
    }

    /**
     * 检查合成网格中是否有七彩粉末。
     */
    private boolean hasPigment() {
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == MardPixelForge.MARD_PIGMENT.get()) {
                return true;
            }
        }
        return false;
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
        if (level == null || level.isClientSide) return;

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
        SafeCraftingContainer craftingContainer = new SafeCraftingContainer(3, 3);
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
     * 七彩粉末模式下只消耗七彩粉末，普通模式下消耗所有材料。
     */
    public void consumeMaterials() {
        if (hasPigment() && !selectedColor.isEmpty()) {
            // 七彩粉末模式：只消耗七彩粉末
            for (int i = 0; i < GRID_SIZE; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == MardPixelForge.MARD_PIGMENT.get()) {
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
        tag.putString("SelectedColor", selectedColor);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        NonNullList<ItemStack> allItems = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, allItems);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            inventory.setStackInSlot(i, allItems.get(i));
        }
        selectedColor = tag.getString("SelectedColor");
    }

    public Component getDisplayName() {
        return Component.translatable("block.mard_pixel.mard_crafting_table");
    }
}
