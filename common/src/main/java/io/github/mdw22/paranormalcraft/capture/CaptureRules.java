package io.github.mdw22.paranormalcraft.capture;

import io.github.mdw22.paranormalcraft.Balance;

/**
 * Ghost Trap capture rules, as plain logic so they can be unit tested.
 */
public final class CaptureRules {

    /** A capture pulls the ghost down for this long (1.5 s). */
    public static final int CAPTURE_TICKS = Balance.Trap.CAPTURE_TICKS;
    /** Without a tether held for the whole pull, a capture succeeds this often. */
    public static final double UNTETHERED_CHANCE = Balance.Trap.UNTETHERED_CHANCE;
    /** The capture zone: this far above the trap's floor, and this far out horizontally from its centre. */
    public static final double ZONE_HEIGHT = Balance.Trap.ZONE_HEIGHT;
    public static final double ZONE_HALF_WIDTH = Balance.Trap.ZONE_HALF_WIDTH;

    private CaptureRules() {
    }

    /**
     * @param tetheredThroughout a tether was latched on the ghost for every tick of the pull
     * @param roll               a uniform value in [0, 1)
     */
    public static boolean succeeds(boolean tetheredThroughout, double roll) {

        return tetheredThroughout || roll < UNTETHERED_CHANCE;
    }

    /**
     * Whether a ghost at ({@code dx}, {@code dy}, {@code dz}) from the trap block's bottom centre is in its zone: up to
     * 3 blocks above it and within 1 block horizontally of the trap's own block.
     */
    public static boolean inZone(double dx, double dy, double dz) {

        return dy >= -0.5 && dy <= ZONE_HEIGHT && Math.abs(dx) <= ZONE_HALF_WIDTH && Math.abs(dz) <= ZONE_HALF_WIDTH;
    }
}
