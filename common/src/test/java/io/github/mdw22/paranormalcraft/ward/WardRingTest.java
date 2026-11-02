package io.github.mdw22.paranormalcraft.ward;

import io.github.mdw22.paranormalcraft.Balance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WardRingTest {

    private static final double CENTRE = 0.5;
    private static final double RANGE = Balance.Ward.PREVIEW_EDGE_RANGE;
    private static final int BUDGET = Balance.Ward.PREVIEW_MAX_MOTES;
    private static final int[] RADII = {2, 4, 8, 12, 16, 24, 32, 48, 64};

    /** The player's positions, relative to the lantern, for a ring of this radius. */
    private static double[][] players(int radius) {

        return new double[][] {
                {0.0, 0.0},
                {radius, 0.0},
                {radius + 0.5, 3.0},
                {radius - 1.0, -2.0},
                {-radius, 0.0},
                {0.0, radius},
                {radius * 0.7, -radius * 0.7},
                {radius + 20.0, 0.0},
                {3.0, -radius - 5.0},
        };
    }

    /** The spacing a round was drawn at: the circle's length over its point count, worked out from the round itself. */
    private static double spacing(List<WardRing.Point> round) {

        double closest = Double.MAX_VALUE;
        for (int i = 1; i < round.size(); i++) {
            closest = Math.min(closest, Math.hypot(round.get(i).x() - round.get(i - 1).x(), round.get(i).z() - round.get(i - 1).z()));
        }
        return closest;
    }

    @Test
    void everyPreviewRoundIsClosedWithinTheBudget() {

        for (int radius : RADII) {
            for (double[] offset : players(radius)) {
                double px = CENTRE + offset[0];
                double pz = CENTRE + offset[1];
                List<WardRing.Point> round = WardRing.previewRound(CENTRE, CENTRE, radius, px, pz, RANGE, BUDGET);
                String where = "radius " + radius + " at " + offset[0] + "," + offset[1];
                assertTrue(WardRing.motes(round) <= BUDGET, where + ": " + WardRing.motes(round) + " motes");
                if (round.size() < 2) {
                    continue;
                }
                double gap = WardRing.widestGap(round, CENTRE, CENTRE, radius, px, pz, RANGE);
                assertTrue(gap <= spacing(round) * 1.5, where + ": a gap of " + gap + " at spacing " + spacing(round));
                assertCovered(round, radius, px, pz, spacing(round), where);
            }
        }
    }

    /** Walks the circle in small steps, across the 0/2π seam too: every point in range has a mote close by. */
    private static void assertCovered(List<WardRing.Point> round, int radius, double px, double pz, double spacing, String where) {

        int steps = 720;
        for (int step = 0; step < steps; step++) {
            double angle = 2 * Math.PI * step / steps;
            double x = CENTRE + Math.cos(angle) * radius;
            double z = CENTRE + Math.sin(angle) * radius;
            // Just inside the range, as the very ends of an arc stop at the last whole point.
            if (Math.hypot(x - px, z - pz) > RANGE - spacing) {
                continue;
            }
            double closest = round.stream().mapToDouble(point -> Math.hypot(point.x() - x, point.z() - z)).min().orElse(Double.MAX_VALUE);
            assertTrue(closest <= spacing * 1.5, where + ": nothing within " + closest + " of angle " + angle);
        }
    }

    /** What the user saw at radius 12: a full round is 227 points and 38 post motes, over the old cut-off of 240. */
    @Test
    void aRoundOverBudgetGetsSparserInsteadOfLosingItsEnd() {

        int full = WardRing.pointCount(12, Balance.Ward.PREVIEW_MOTES_PER_BLOCK);
        assertTrue(WardRing.motes(WardRing.circle(CENTRE, CENTRE, 12, full)) > BUDGET, "this radius should be over budget at full density");
        List<WardRing.Point> round = WardRing.previewRound(CENTRE, CENTRE, 12, CENTRE, CENTRE, RANGE, BUDGET);
        assertTrue(round.size() < full);
        // The last point is one step short of the first, across the seam.
        WardRing.Point first = round.getFirst();
        WardRing.Point last = round.getLast();
        assertEquals(CENTRE + 12, first.x(), 1.0E-9);
        assertTrue(Math.hypot(first.x() - last.x(), first.z() - last.z()) <= spacing(round) * 1.5);
    }

    @Test
    void fullRingEndsOneStepBeforeItsStart() {

        for (int radius : RADII) {
            int points = WardRing.pointCount(radius, Balance.Ward.RING_MOTES_PER_BLOCK);
            List<WardRing.Point> ring = WardRing.circle(CENTRE, CENTRE, radius, points);
            assertEquals(points, ring.size());
            assertTrue(WardRing.widestGap(ring, CENTRE, CENTRE, radius, CENTRE, CENTRE, radius * 2.0) <= 2 * Math.PI * radius / points * 1.01, "radius " + radius);
        }
    }

    @Test
    void postsComeAboutEverySpacingBlocks() {

        List<WardRing.Point> ring = WardRing.circle(CENTRE, CENTRE, 16, WardRing.pointCount(16, Balance.Ward.PREVIEW_MOTES_PER_BLOCK));
        long posts = ring.stream().filter(WardRing.Point::post).count();
        double expected = 2 * Math.PI * 16 / Balance.Ward.PREVIEW_POST_SPACING;
        assertTrue(Math.abs(posts - expected) <= 1.0, posts + " posts, expected about " + expected);
    }

    @Test
    void anOutOfRangeRingDrawsNothingAndSharesNoGap() {

        List<WardRing.Point> round = WardRing.previewRound(CENTRE, CENTRE, 8, 200.0, 200.0, RANGE, BUDGET);
        assertTrue(round.isEmpty());
        assertEquals(0.0, WardRing.widestGap(round, CENTRE, CENTRE, 8, 200.0, 200.0, RANGE));
        assertFalse(WardRing.previewRound(CENTRE, CENTRE, 8, CENTRE, CENTRE, RANGE, 1).size() > 1);
    }
}
