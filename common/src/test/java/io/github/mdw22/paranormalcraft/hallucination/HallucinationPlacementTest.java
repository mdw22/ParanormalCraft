package io.github.mdw22.paranormalcraft.hallucination;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HallucinationPlacementTest {

    private static final double EPSILON = 1.0E-6;

    @Test
    void sampledAnglesAndDistancesStayInTheirBands() {

        Random random = new Random(7);
        for (int i = 0; i < 10_000; i++) {
            boolean right = random.nextBoolean();
            double angle = HallucinationPlacement.sampleAngle(right, random.nextDouble());
            assertTrue(Math.abs(angle) >= 60.0 && Math.abs(angle) < 80.0, "angle " + angle);
            assertEquals(right, angle > 0);
            double near = HallucinationPlacement.sampleDistance(false, random.nextDouble());
            assertTrue(near >= 10.0 && near < 16.0, "near " + near);
            double far = HallucinationPlacement.sampleDistance(true, random.nextDouble());
            assertTrue(far >= 16.0 && far < 20.0, "reduced " + far);
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
            double angle = HallucinationPlacement.sampleAngle(random.nextBoolean(), random.nextDouble());
            double distance = HallucinationPlacement.sampleDistance(random.nextBoolean(), random.nextDouble());
            HallucinationPlacement.Offset offset = HallucinationPlacement.offset(yaw, angle, distance);
            assertEquals(angle, HallucinationPlacement.relativeAngle(yaw, offset.dx(), offset.dz()), 1.0E-6);
            assertEquals(distance, Math.hypot(offset.dx(), offset.dz()), 1.0E-6);
        }
    }

    @Test
    void bandCheckAcceptsOnlyTheSixtyToEightyDegreeRing() {

        assertTrue(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, 70, 12), false));
        assertTrue(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, -60, 10), false));
        assertFalse(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, 50, 12), false));
        assertFalse(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, 85, 12), false));
        assertFalse(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, 70, 9), false));
        assertFalse(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, 70, 12), true));
        assertTrue(HallucinationPlacement.inBand(30, HallucinationPlacement.offset(30, 70, 18), true));
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
