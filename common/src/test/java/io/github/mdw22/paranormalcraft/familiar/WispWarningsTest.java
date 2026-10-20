package io.github.mdw22.paranormalcraft.familiar;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WispWarningsTest {

    private static final Vec3 SOUTH = new Vec3(0, 0, 1);

    @Test
    void behindIsUnseenAndInFrontIsSeen() {

        assertTrue(WispWarnings.unseen(SOUTH, new Vec3(0, 0, -5), true), "straight behind");
        assertFalse(WispWarnings.unseen(SOUTH, new Vec3(0, 0, 5), true), "straight ahead");
        assertFalse(WispWarnings.unseen(SOUTH, new Vec3(5, 0, 0), true), "90 degrees to the side is still in view");
    }

    @Test
    void theAngleLimitIsOneHundredDegrees() {

        Vec3 at99 = new Vec3(Math.sin(Math.toRadians(99)), 0, Math.cos(Math.toRadians(99)));
        Vec3 at101 = new Vec3(Math.sin(Math.toRadians(101)), 0, Math.cos(Math.toRadians(101)));
        assertEquals(99.0, WispWarnings.angle(SOUTH, at99), 1.0E-6);
        assertFalse(WispWarnings.unseen(SOUTH, at99, true));
        assertTrue(WispWarnings.unseen(SOUTH, at101, true));
    }

    @Test
    void noLineOfSightIsUnseenEvenInFront() {

        assertTrue(WispWarnings.unseen(SOUTH, new Vec3(0, 0, 5), false));
    }

    @Test
    void sideIsHorizontalAndNormalised() {

        Vec3 side = WispWarnings.side(new Vec3(3, 4, 4));
        assertEquals(0.0, side.y, 1.0E-9);
        assertEquals(1.0, side.length(), 1.0E-9);
        assertEquals(Vec3.ZERO, WispWarnings.side(new Vec3(0, 2, 0)));
    }
}
