package io.github.mdw22.paranormalcraft.containment;

import io.github.mdw22.paranormalcraft.binding.SpiritVesselItem;
import io.github.mdw22.paranormalcraft.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The Containment Unit's menu: the trap input, fuel, six output slots and the Vessel slot, the player's inventory, and the unit's data
 * (charge, processing progress, Unstable, ghost count). Buttons are {@link #clickMenuButton} ids:
 * {@code action × 8 + ghost index}, with {@link #ACTION_PROCESS}, {@link #ACTION_RELEASE}, {@link #ACTION_BIND} and
 * {@link #ACTION_UNBIND}. The ghost list itself
 * comes in a {@code ContainmentStatePayload}, sent from {@link #broadcastChanges}.
 */
public class ContainmentUnitMenu extends AbstractContainerMenu {

    public static final int ACTION_PROCESS = 0;
    public static final int ACTION_RELEASE = 1;
    public static final int ACTION_BIND = 2;
    /** Unbind ignores the ghost index: it acts on the vessel in the slot. */
    public static final int ACTION_UNBIND = 3;
    /** Layout, relative to the screen's top-left corner. */
    public static final int INPUT_X = 8;
    public static final int INPUT_Y = 104;
    public static final int FUEL_X = 44;
    public static final int FUEL_Y = 104;
    public static final int OUTPUT_X = 116;
    public static final int OUTPUT_Y = 104;
    public static final int VESSEL_X = 80;
    public static final int VESSEL_Y = 104;
    public static final int INVENTORY_Y = 148;
    private static final int STATE_INTERVAL = 10;

    private final Container container;
    private final ContainerData data;
    private final @Nullable ContainmentUnitBlockEntity unit;
    private final Player player;
    private int ticks;

    /** Client side. */
    public ContainmentUnitMenu(int containerId, Inventory inventory) {

        this(containerId, inventory, new SimpleContainer(ContainmentUnitBlockEntity.SLOTS), new SimpleContainerData(ContainmentUnitBlockEntity.DATA_SIZE), null);
    }

    /** Server side. */
    public ContainmentUnitMenu(int containerId, Inventory inventory, ContainmentUnitBlockEntity unit, ContainerData data) {

        this(containerId, inventory, unit, data, unit);
    }

    private ContainmentUnitMenu(int containerId, Inventory inventory, Container container, ContainerData data, @Nullable ContainmentUnitBlockEntity unit) {

        super(ModMenus.CONTAINMENT_UNIT.get(), containerId);
        checkContainerSize(container, ContainmentUnitBlockEntity.SLOTS);
        this.container = container;
        this.data = data;
        this.unit = unit;
        this.player = inventory.player;
        container.startOpen(inventory.player);
        this.addSlot(new FilteredSlot(container, ContainmentUnitBlockEntity.INPUT_SLOT, INPUT_X, INPUT_Y));
        this.addSlot(new FilteredSlot(container, ContainmentUnitBlockEntity.FUEL_SLOT, FUEL_X, FUEL_Y));
        for (int i = 0; i < 6; i++) {
            this.addSlot(new OutputSlot(container, ContainmentUnitBlockEntity.OUTPUT_START + i, OUTPUT_X + (i % 3) * 18, OUTPUT_Y + (i / 3) * 18));
        }
        this.addSlot(new FilteredSlot(container, ContainmentUnitBlockEntity.VESSEL_SLOT, VESSEL_X, VESSEL_Y));
        this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        this.addDataSlots(data);
    }

    public int charge() {

        return this.data.get(ContainmentUnitBlockEntity.DATA_CHARGE);
    }

    /** Processing progress, 0 to 1000. */
    public int progress() {

        return this.data.get(ContainmentUnitBlockEntity.DATA_PROCESS);
    }

    public boolean unstable() {

        return this.data.get(ContainmentUnitBlockEntity.DATA_UNSTABLE) != 0;
    }

    public int ghostCount() {

        return this.data.get(ContainmentUnitBlockEntity.DATA_COUNT);
    }

    public int processingIndex() {

        return this.data.get(ContainmentUnitBlockEntity.DATA_PROCESSING_INDEX);
    }

    /** Bind progress, 0 (not binding) to 1000. */
    public int bindProgress() {

        return this.data.get(ContainmentUnitBlockEntity.DATA_BIND);
    }

    /** The stack in the Vessel slot (synced like any slot). */
    public ItemStack vessel() {

        return this.container.getItem(ContainmentUnitBlockEntity.VESSEL_SLOT);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {

        if (this.unit == null || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        int action = id / 8;
        int index = id % 8;
        boolean done = switch (action) {
            case ACTION_PROCESS -> this.unit.queueProcess(index, serverPlayer);
            case ACTION_RELEASE -> this.unit.release(index, serverPlayer);
            case ACTION_BIND -> this.unit.bind(index, serverPlayer);
            case ACTION_UNBIND -> this.unit.unbind(serverPlayer);
            default -> false;
        };
        if (done) {
            this.unit.sendState(serverPlayer, this.containerId);
        }
        return done;
    }

    @Override
    public void broadcastChanges() {

        super.broadcastChanges();
        if (this.unit != null && this.player instanceof ServerPlayer serverPlayer && this.ticks++ % STATE_INTERVAL == 0) {
            this.unit.sendState(serverPlayer, this.containerId);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {

        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int containerSlots = ContainmentUnitBlockEntity.SLOTS;
        if (index < containerSlots) {
            if (!this.moveItemStackTo(stack, containerSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (ContainmentUnitBlockEntity.isLoadable(stack)) {
            if (!this.moveItemStackTo(stack, ContainmentUnitBlockEntity.INPUT_SLOT, ContainmentUnitBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof SpiritVesselItem) {
            int vessel = ContainmentUnitBlockEntity.VESSEL_SLOT;
            if (!this.moveItemStackTo(stack, vessel, vessel + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ContainmentUnitBlockEntity.isFuel(stack)) {
            if (!this.moveItemStackTo(stack, ContainmentUnitBlockEntity.FUEL_SLOT, ContainmentUnitBlockEntity.FUEL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {

        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {

        super.removed(player);
        this.container.stopOpen(player);
    }

    /** Takes only what the unit accepts in that slot. */
    private static final class FilteredSlot extends Slot {

        FilteredSlot(Container container, int slot, int x, int y) {

            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {

            return this.container.canPlaceItem(this.getContainerSlot(), stack);
        }
    }

    /** Take only. */
    private static final class OutputSlot extends Slot {

        OutputSlot(Container container, int slot, int x, int y) {

            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {

            return false;
        }
    }
}
