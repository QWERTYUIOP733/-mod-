package com.mard.pixel.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * MARD 合成台菜单。
 * 布局类似原版工作台：
 * - 左上：3x3 合成网格（9格）
 * - 右侧：结果槽（1格）
 * - 下方：玩家背包（27格）
 * - 最下方：玩家快捷栏（9格）
 */
public class MardCraftingMenu extends AbstractContainerMenu {

    public static final int GRID_START_X = 30;
    public static final int GRID_START_Y = 17;
    public static final int SLOT_SIZE = 18;
    public static final int RESULT_X = 124;
    public static final int RESULT_Y = 35;
    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 84;
    public static final int HOTBAR_Y = 142;

    private final MardCraftingTableBlockEntity blockEntity;
    private final Player player;

    public MardCraftingMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, (MardCraftingTableBlockEntity) playerInventory.player.level()
                .getBlockEntity(buf.readBlockPos()));
    }

    /**
     * 客户端构造函数（MenuType需要）。
     * 实际使用时通过FriendlyByteBuf构造函数传递BlockPos。
     */
    public MardCraftingMenu(int containerId, Inventory playerInventory) {
        super(MardPixelForge.MARD_CRAFTING_MENU.get(), containerId);
        this.blockEntity = null;
        this.player = playerInventory.player;
    }

    private MardCraftingMenu(int containerId, Inventory playerInventory, MardCraftingTableBlockEntity blockEntity) {
        super(MardPixelForge.MARD_CRAFTING_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;

        IItemHandler handler = blockEntity.getInventory();

        // 3x3 合成网格（槽位0-8）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                this.addSlot(new SlotItemHandler(handler, slotIndex,
                        GRID_START_X + col * SLOT_SIZE,
                        GRID_START_Y + row * SLOT_SIZE));
            }
        }

        // 结果槽（槽位9）
        this.addSlot(new SlotItemHandler(handler, MardCraftingTableBlockEntity.RESULT_SLOT,
                RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false; // 结果槽不能手动放入
            }

            @Override
            public void onTake(Player player, @NotNull ItemStack stack) {
                super.onTake(player, stack);
                // 取走结果时消耗材料
                blockEntity.consumeMaterials();
            }
        });

        // 玩家背包（27格）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        PLAYER_INV_X + col * SLOT_SIZE,
                        PLAYER_INV_Y + row * SLOT_SIZE));
            }
        }

        // 玩家快捷栏（9格）
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    PLAYER_INV_X + col * SLOT_SIZE,
                    HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.blockEntity.getBlockPos().distSqr(player.blockPosition()) <= 64.0;
    }

    /**
     * Shift+点击快速移动物品。
     */
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index == MardCraftingTableBlockEntity.RESULT_SLOT) {
                // 从结果槽取出
                if (!this.moveItemStackTo(itemstack1, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 10 && index < 46) {
                // 从玩家背包移到合成网格
                if (!this.moveItemStackTo(itemstack1, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 0 && index < 9) {
                // 从合成网格移到玩家背包
                if (!this.moveItemStackTo(itemstack1, 10, 46, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
            this.broadcastChanges();
        }

        return itemstack;
    }

    public MardCraftingTableBlockEntity getBlockEntity() {
        return blockEntity;
    }

    /**
     * 服务端创建菜单的静态工厂方法。
     */
    public static MardCraftingMenu create(int containerId, Inventory playerInventory, MardCraftingTableBlockEntity blockEntity) {
        return new MardCraftingMenu(containerId, playerInventory, blockEntity);
    }
}
