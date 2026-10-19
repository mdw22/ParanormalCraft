package io.github.mdw22.paranormalcraft.capture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BeamMathTest {

    private static BeamMath.Point p(double x, double y, double z) {

        return new BeamMath.Point(x, y, z);
    }

    @Test
    void pointToSegmentClampsToTheEnds() {

        assertEquals(1.0, BeamMath.pointToSegment(p(5, 1, 0), p(0, 0, 0), p(10, 0, 0)), 1.0E-9);
        assertEquals(5.0, BeamMath.pointToSegment(p(-3, 4, 0), p(0, 0, 0), p(10, 0, 0)), 1.0E-9);
        assertEquals(2.0, BeamMath.pointToSegment(p(12, 0, 0), p(0, 0, 0), p(10, 0, 0)), 1.0E-9);
    }

    @Test
    void segmentDistanceForCrossingSkewAndParallel() {

        assertEquals(0.0, BeamMath.segmentDistance(p(-5, 0, 0), p(5, 0, 0), p(0, 0, -5), p(0, 0, 5)), 1.0E-9);
        assertEquals(0.3, BeamMath.segmentDistance(p(-5, 0, 0), p(5, 0, 0), p(0, 0.3, -5), p(0, 0.3, 5)), 1.0E-9);
        assertEquals(2.0, BeamMath.segmentDistance(p(0, 0, 0), p(10, 0, 0), p(0, 2, 0), p(10, 2, 0)), 1.0E-9);
        // Non-overlapping colinear segments: the gap between their ends.
        assertEquals(1.0, BeamMath.segmentDistance(p(0, 0, 0), p(4, 0, 0), p(5, 0, 0), p(9, 0, 0)), 1.0E-9);
    }

    @Test
    void crossingBeamsBackfireAtTheCrossingPoint() {

        BeamMath.Point at = BeamMath.crossing(p(-6, 1, 0), p(6, 1, 0), p(0, 1.2, -6), p(0, 1.2, 6));
        assertNotNull(at);
        assertEquals(0.0, at.x(), 1.0E-9);
        assertEquals(1.1, at.y(), 1.0E-9);
        assertEquals(0.0, at.z(), 1.0E-9);
        assertNull(BeamMath.crossing(p(-6, 1, 0), p(6, 1, 0), p(0, 1.5, -6), p(0, 1.5, 6)), "0.5 apart is not a crossing");
    }

    @Test
    void beamsMeetingAtASharedTargetDoNotCount() {

        // Two players either side, both latched on a ghost at the origin: the beams meet only at their ends.
        assertNull(BeamMath.crossing(p(-8, 1, 0), p(0, 1, 0), p(8, 1, 0), p(0, 1, 0)));
        // They converge within the last 1.5 blocks only: still no crossing.
        assertNull(BeamMath.crossing(p(-8, 1, 0.5), p(0, 1, 0), p(8, 1, 0.5), p(0, 1, 0)));
        // A beam crossing the other 3 blocks from its end does count.
        assertNotNull(BeamMath.crossing(p(-8, 1, 0), p(4, 1, 0), p(0, 1, -8), p(0, 1, 4)));
    }

    @Test
    void shortBeamsNeverCross() {

        assertNull(BeamMath.crossing(p(0, 0, 0), p(1, 0, 0), p(0.5, 0, -0.5), p(0.5, 0, 0.5)));
    }
}
