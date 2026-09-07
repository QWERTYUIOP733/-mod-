package com.mard.pixel.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 方块染色台菜单。
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

    private MardCraftingTableBlockEntity blockEntity;
    private final Player player;
    private IItemHandler inventoryHandler;

    /**
     * 客户端构造函数（通过IForgeMenuType调用，从FriendlyByteBuf读取BlockPos）。
     * 必须注册所有槽位，否则会导致IndexOutOfBoundsException。
     *
     * 重要：客户端始终使用独立的ItemStackHandler作为槽位副本，
     * 不直接使用方块实体的inventory（客户端方块实体inventory为空，
     * 物品只存在于服务端，通过AbstractContainerMenu同步机制更新客户端副本）。
     */
    public MardCraftingMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(MardPixelForge.MARD_CRAFTING_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.blockEntity = null;
        // 客户端使用独立的inventory副本，通过槽位同步机制更新
        this.inventoryHandler = new ItemStackHandler(MardCraftingTableBlockEntity.TOTAL_SLOTS);

        // 尝试从buf读取BlockPos并获取方块实体引用（仅用于stillValid等检查，不使用其inventory）
        if (buf != null && buf.isReadable()) {
            try {
                BlockPos pos = buf.readBlockPos();
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof MardCraftingTableBlockEntity craftingTable) {
                    this.blockEntity = craftingTable;
                }
            } catch (Exception e) {
                // 读取失败，blockEntity保持null，stillValid返回true
            }
        }

        registerSlots(playerInventory);
    }

    /**
     * MenuType需要的构造函数（后备方案，也必须注册槽位）。
     */
    public MardCraftingMenu(int containerId, Inventory playerInventory) {
        super(MardPixelForge.MARD_CRAFTING_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.blockEntity = null;
        this.inventoryHandler = new ItemStackHandler(MardCraftingTableBlockEntity.TOTAL_SLOTS);
        registerSlots(playerInventory);
    }

    /**
     * 服务端创建菜单的构造函数。
     */
    public MardCraftingMenu(int containerId, Inventory playerInventory, MardCraftingTableBlockEntity blockEntity) {
        super(MardPixelForge.MARD_CRAFTING_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        this.inventoryHandler = blockEntity.getInventory();
        registerSlots(playerInventory);
    }

    /**
     * 注册所有槽位。必须在所有构造函数中调用。
     */
    private void registerSlots(Inventory playerInventory) {
        // 3x3 合成网格（槽位0-8）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                this.addSlot(new SlotItemHandler(inventoryHandler, slotIndex,
                        GRID_START_X + col * SLOT_SIZE,
                        GRID_START_Y + row * SLOT_SIZE));
            }
        }

        // 结果槽（槽位9）
        this.addSlot(new SlotItemHandler(inventoryHandler, MardCraftingTableBlockEntity.RESULT_SLOT,
                RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, @NotNull ItemStack stack) {
                super.onTake(player, stack);
                if (blockEntity != null) {
                    blockEntity.consumeMaterials();
                }
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
        if (blockEntity == null) return true;
        return blockEntity.getBlockPos().distSqr(player.blockPosition()) <= 64.0;
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
                // 从结果槽取出到玩家背包
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
     * 客户端选择颜色（发送网络包到服务端）。
     */
    public void selectColor(String code) {
        if (player.level().isClientSide) {
            MardNetwork.CHANNEL.sendToServer(new MardNetwork.SelectCraftingColorPacket(code));
        }
    }

    /**
     * 检查合成网格中是否有七彩粉末（客户端判断是否显示颜色选择列表）。
     * 使用物品注册名判断，避免实例比较在客户端/服务端不同步的问题。
     */
    public boolean hasPigment() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = getSlot(i).getItem();
            if (!stack.isEmpty() && isPigmentItem(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断物品是否为七彩粉末。
     * 使用物品注册名判断，兼容客户端和服务端。
     */
    private boolean isPigmentItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // 优先使用实例比较（最快）
        if (stack.getItem() == MardPixelForge.MARD_PIGMENT.get()) return true;
        // 回退：使用物品注册名判断（最可靠）
        String registryName = stack.getItem().getDescriptionId();
        return "item.mard_pixel.mard_pigment".equals(registryName);
    }

    /**
     * 服务端创建菜单的静态工厂方法。
     */
    public static MardCraftingMenu create(int containerId, Inventory playerInventory, MardCraftingTableBlockEntity blockEntity) {
        return new MardCraftingMenu(containerId, playerInventory, blockEntity);
    }
}
