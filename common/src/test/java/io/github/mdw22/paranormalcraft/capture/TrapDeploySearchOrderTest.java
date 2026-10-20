package io.github.mdw22.paranormalcraft.capture;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrapDeploySearchOrderTest {

    @Test
    void offHandThenHotbarThenMainInventory() {

        List<Integer> order = TrapDeploy.searchOrder();
        assertEquals(37, order.size());
        assertEquals(TrapDeploy.OFFHAND_SLOT, order.getFirst());
        assertEquals(40, order.getFirst(), "vanilla's off-hand slot index");
        for (int slot = 0; slot < 9; slot++) {
            assertEquals(slot, order.get(slot + 1), "hotbar slot " + slot);
        }
        assertEquals(9, order.get(10), "main inventory after the hotbar");
        assertEquals(35, order.getLast());
    }
}
