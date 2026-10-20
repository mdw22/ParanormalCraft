package io.github.mdw22.paranormalcraft.capture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for dropping a Ghost Trap under a tethered ghost ({@link TetherManager#deployTrap}): which inventory slot
 * the trap comes from, and where on the floor it lands.
 */
public final class TrapDeploy {

    /** Vanilla's off-hand slot index in a player inventory. */
    public static final int OFFHAND_SLOT = Inventory.SLOT_OFFHAND;
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_SIZE = 36;

    private TrapDeploy() {
    }

    /**
     * The order slots are searched for a trap: the off-hand, then the hotbar left to right, then the main inventory.
     * Plain data, so it can be unit tested.
     */
    public static List<Integer> searchOrder() {

        List<Integer> order = new ArrayList<>(MAIN_SIZE + 1);
        order.add(OFFHAND_SLOT);
        for (int slot = 0; slot < MAIN_SIZE; slot++) {
            order.add(slot);
        }
        return order;
    }

    /**
     * The first slot in {@link #searchOrder()} holding {@code trap}, or -1.
     */
    public static int findTrapSlot(Inventory inventory, Item trap) {

        for (int slot : searchOrder()) {
            if (inventory.getItem(slot).is(trap)) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * The open position directly under {@code start} that sits on a sturdy floor, searching down at most
     * {@code depth} blocks; null if {@code start} isn't open or there's no floor in reach.
     */
    public static @Nullable BlockPos floorUnder(Level level, BlockPos start, int depth) {

        BlockPos.MutableBlockPos pos = start.mutable();
        if (!isOpen(level.getBlockState(pos))) {
            return null;
        }
        for (int i = 0; i < depth; i++) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.isFaceSturdy(level, below, Direction.UP)) {
                return pos.immutable();
            }
            if (!isOpen(belowState)) {
                return null;
            }
            pos.move(Direction.DOWN);
        }
        return null;
    }

    private static boolean isOpen(BlockState state) {

        return state.isAir() || state.canBeReplaced();
    }
}
