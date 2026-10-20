package io.github.mdw22.paranormalcraft.containment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainmentChargeTest {

    private static ContainmentCharge run(ContainmentCharge charge, int ghosts, boolean processing, int ticks) {

        for (int tick = 0; tick < ticks; tick++) {
            charge = charge.tick(ghosts, processing);
        }
        return charge;
    }

    @Test
    void eachGhostDrainsOnePointPerThirtySeconds() {

        ContainmentCharge full = ContainmentCharge.EMPTY.withCharge(100);
        assertEquals(99, run(full, 1, false, 600).charge());
        assertEquals(100, run(full, 1, false, 599).charge());
        assertEquals(96, run(full, 4, false, 600).charge());
    }

    @Test
    void fourGhostsDrainAFullUnitInTwelveAndAHalfMinutes() {

        ContainmentCharge full = ContainmentCharge.EMPTY.withCharge(100);
        assertEquals(1, run(full, 4, false, 15_000 - 150).charge());
        assertEquals(0, run(full, 4, false, 15_000).charge());
    }

    @Test
    void emptyUnitDoesNotDrain() {

        assertEquals(50, run(ContainmentCharge.EMPTY.withCharge(50), 0, false, 10_000).charge());
    }

    @Test
    void processingDrainsOneExtraPointAMinute() {

        ContainmentCharge full = ContainmentCharge.EMPTY.withCharge(100);
        // 1 ghost: 2 points from storage in a minute, plus 1 from processing.
        assertEquals(97, run(full, 1, true, 1200).charge());
        assertEquals(98, run(full, 1, false, 1200).charge());
    }

    @Test
    void fuelTopsUpByTwentyAtOrBelowEighty() {

        assertTrue(ContainmentCharge.EMPTY.withCharge(80).wantsFuel());
        assertFalse(ContainmentCharge.EMPTY.withCharge(81).wantsFuel());
        assertEquals(100, ContainmentCharge.EMPTY.withCharge(80).fuelled().charge());
        assertEquals(100, ContainmentCharge.EMPTY.withCharge(95).fuelled().charge(), "capped at 100");
        assertEquals(20, ContainmentCharge.EMPTY.fuelled().charge());
    }

    @Test
    void unstableOnlyAtZeroWithGhosts() {

        assertTrue(ContainmentCharge.EMPTY.unstable(1));
        assertFalse(ContainmentCharge.EMPTY.unstable(0));
        assertFalse(ContainmentCharge.EMPTY.withCharge(1).unstable(4));
        assertEquals(0, run(ContainmentCharge.EMPTY, 4, true, 5000).charge(), "never below zero");
    }
}
