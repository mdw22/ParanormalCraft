package io.github.mdw22.paranormalcraft.familiar;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuidePlannerTest {

    /** Flat ground at 64, a 10-high wall across z = 12 and a hill (top 80) around z = 30. */
    private static final GuidePlanner.Heights TERRAIN = (x, z) -> z == 12 ? 74 : z >= 28 && z <= 32 ? 80 : 64;

    @Test
    void waypointsGoEightBlocksTowardTheCabin() {

        Vec3 next = GuidePlanner.next(new Vec3(0.5, 65, 0.5), new Vec3(0.5, 65, 100.5), TERRAIN);
        assertEquals(8.5, next.z, 1.0E-9);
        assertEquals(0.5, next.x, 1.0E-9);
        assertEquals(64 + GuidePlanner.CLEARANCE, next.y, 1.0E-9, "flat leg: clearance above the ground");
    }

    @Test
    void aLegOverAWallOrHillClimbsAboveIt() {

        Vec3 overWall = GuidePlanner.next(new Vec3(0.5, 67, 8.5), new Vec3(0.5, 65, 100.5), TERRAIN);
        assertEquals(74 + GuidePlanner.CLEARANCE, overWall.y, 1.0E-9);
        Vec3 overHill = GuidePlanner.next(new Vec3(0.5, 67, 24.5), new Vec3(0.5, 65, 100.5), TERRAIN);
        assertEquals(80 + GuidePlanner.CLEARANCE, overHill.y, 1.0E-9);
    }

    @Test
    void theLastLegEndsAtTheCabin() {

        Vec3 last = GuidePlanner.next(new Vec3(0.5, 67, 95.5), new Vec3(3.5, 65, 100.5), TERRAIN);
        assertEquals(3.5, last.x, 1.0E-9);
        assertEquals(100.5, last.z, 1.0E-9);
    }

    @Test
    void itClimbsBeforeFlyingTheLeg() {

        Vec3 waypoint = new Vec3(0.5, 77, 16.5);
        assertEquals(new Vec3(0.5, 77, 8.5), GuidePlanner.heading(new Vec3(0.5, 67, 8.5), waypoint));
        assertEquals(waypoint, GuidePlanner.heading(new Vec3(0.5, 77, 8.5), waypoint));
    }

    @Test
    void offRouteIsTheDistanceFromTheLeg() {

        Vec3 a = new Vec3(0, 70, 0);
        Vec3 b = new Vec3(0, 70, 8);
        assertEquals(5.0, GuidePlanner.offRoute(new Vec3(5, 64, 4), a, b), 1.0E-9);
        assertEquals(3.0, GuidePlanner.offRoute(new Vec3(0, 64, -3), a, b), 1.0E-9);
        assertEquals(10.0, GuidePlanner.flatDistance(new Vec3(0, 0, 0), new Vec3(6, 40, 8)), 1.0E-9);
    }
}
