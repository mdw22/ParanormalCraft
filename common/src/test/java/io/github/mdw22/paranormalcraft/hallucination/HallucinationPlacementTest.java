package io.github.mdw22.paranormalcraft.hallucination;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HallucinationPlacementTest {

    private static final double EPSILON = 1.0E-6;

    /** The default 16:9 window at the default FOV of 70. */
    private static final HallucinationPlacement.Band DEFAULT = HallucinationPlacement.band(HallucinationPlacement.halfHorizontalFov(70, 16.0 / 9.0));

    @Test
    void halfFovMatchesTheScreen() {

        assertEquals(51.2, HallucinationPlacement.halfHorizontalFov(70, 16.0 / 9.0), 0.05);
        // A square window shows half the vertical FOV either side.
        assertEquals(35.0, HallucinationPlacement.halfHorizontalFov(70, 1.0), EPSILON);
        assertEquals(68.5, HallucinationPlacement.halfHorizontalFov(110, 16.0 / 9.0), 0.05);
    }

    @Test
    void bandSitsJustInsideTheScreenEdge() {

        assertEquals(33.2, DEFAULT.min(), 0.05);
        assertEquals(45.2, DEFAULT.max(), 0.05);
        // Narrow screens clamp the inner edge to 28°...
        HallucinationPlacement.Band narrow = HallucinationPlacement.band(40.0);
        assertEquals(28.0, narrow.min(), EPSILON);
        assertEquals(34.0, narrow.max(), EPSILON);
        // ...and skip the figure once the outer edge drops below it.
        assertNotNull(HallucinationPlacement.band(34.0));
        assertNull(HallucinationPlacement.band(33.9));
        assertNull(HallucinationPlacement.band(HallucinationPlacement.halfHorizontalFov(30, 4.0 / 3.0)));
    }

    @Test
    void sampledAnglesAndDistancesStayInTheirBands() {

        Random random = new Random(7);
        for (int i = 0; i < 10_000; i++) {
            boolean right = random.nextBoolean();
            double fov = 30 + random.nextDouble() * 80;
            double aspect = 1.0 + random.nextDouble();
            HallucinationPlacement.Band band = HallucinationPlacement.band(HallucinationPlacement.halfHorizontalFov(fov, aspect));
            if (band == null) {
                continue;
            }
            double angle = HallucinationPlacement.sampleAngle(band, right, random.nextDouble());
            double half = HallucinationPlacement.halfHorizontalFov(fov, aspect);
            assertTrue(Math.abs(angle) >= 28.0 && Math.abs(angle) >= half - 18.0 - EPSILON && Math.abs(angle) <= half - 6.0, "angle " + angle);
            // Always on screen, and never inside the 20° that makes it vanish.
            assertTrue(Math.abs(angle) < half && Math.abs(angle) > HallucinationPlacement.VANISH_VIEW_ANGLE);
            assertEquals(right, angle > 0);
            double near = HallucinationPlacement.sampleDistance(false, random.nextDouble());
            assertTrue(near >= 8.0 && near < 13.0, "near " + near);
            double far = HallucinationPlacement.sampleDistance(true, random.nextDouble());
            assertTrue(far >= 12.0 && far < 16.0, "reduced " + far);
        }
    }

    @Test
    void offsetsFollowMinecraftYaw() {

        // Yaw 0 looks south (+Z); 90° to the right of that is west (−X) in Minecraft's convention.
        HallucinationPlacement.Offset ahead = HallucinationPlacement.offset(0, 0, 10);
        assertEquals(0.0, ahead.dx(), EPSILON);
        assertEquals(10.0, ahead.dz(), EPSILON);
        HallucinationPlacement.Offset right = HallucinationPlacement.offset(0, 90, 10);
        assertEquals(-10.0, right.dx(), EPSILON);
        assertEquals(0.0, right.dz(), EPSILON);
    }

    @Test
    void relativeAngleInvertsOffsetForAnyYaw() {

        Random random = new Random(11);
        for (int i = 0; i < 2_000; i++) {
            double yaw = random.nextDouble() * 720.0 - 360.0;
            double angle = HallucinationPlacement.sampleAngle(DEFAULT, random.nextBoolean(), random.nextDouble());
            double distance = HallucinationPlacement.sampleDistance(random.nextBoolean(), random.nextDouble());
            HallucinationPlacement.Offset offset = HallucinationPlacement.offset(yaw, angle, distance);
            assertEquals(angle, HallucinationPlacement.relativeAngle(yaw, offset.dx(), offset.dz()), 1.0E-6);
            assertEquals(distance, Math.hypot(offset.dx(), offset.dz()), 1.0E-6);
        }
    }

    @Test
    void bandCheckAcceptsOnlyTheRingJustInsideTheScreenEdge() {

        assertTrue(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, 40, 10), DEFAULT, false));
        assertTrue(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, -34, 8), DEFAULT, false));
        assertFalse(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, 30, 10), DEFAULT, false));
        assertFalse(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, 47, 10), DEFAULT, false));
        assertFalse(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, 40, 7), DEFAULT, false));
        assertFalse(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, 40, 11), DEFAULT, true));
        assertTrue(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, 40, 14), DEFAULT, true));
    }

    @Test
    void viewAngleDecidesWhenTheFigureVanishes() {

        assertEquals(0.0, HallucinationPlacement.angleBetween(0, 0, 1, 0, 0, 5), EPSILON);
        assertEquals(90.0, HallucinationPlacement.angleBetween(0, 0, 1, 1, 0, 0), EPSILON);
        double slightlyOff = HallucinationPlacement.angleBetween(0, 0, 1, Math.sin(Math.toRadians(19)), 0, Math.cos(Math.toRadians(19)));
        assertTrue(slightlyOff < HallucinationPlacement.VANISH_VIEW_ANGLE);
        double wellOff = HallucinationPlacement.angleBetween(0, 0, 1, Math.sin(Math.toRadians(65)), 0, Math.cos(Math.toRadians(65)));
        assertTrue(wellOff > HallucinationPlacement.VANISH_VIEW_ANGLE);
    }
}
