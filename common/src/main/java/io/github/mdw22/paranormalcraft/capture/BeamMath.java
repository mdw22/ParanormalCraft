package io.github.mdw22.paranormalcraft.capture;

import io.github.mdw22.paranormalcraft.Balance;
import org.jspecify.annotations.Nullable;

/**
 * Tether beam geometry as plain maths, so it can be unit tested: how close a point is to a beam, and where two beams
 * cross.
 */
public final class BeamMath {

    /** Beams this close count as crossing. */
    public static final double CROSSING_DISTANCE = Balance.Tether.CROSSING_DISTANCE;
    /** The last stretch of each beam, where beams on the same target meet, never counts as a crossing. */
    public static final double TARGET_END_ZONE = Balance.Tether.TARGET_END_ZONE;

    public record Point(double x, double y, double z) {

        Point minus(Point other) {

            return new Point(this.x - other.x, this.y - other.y, this.z - other.z);
        }

        Point plus(Point other) {

            return new Point(this.x + other.x, this.y + other.y, this.z + other.z);
        }

        Point times(double scale) {

            return new Point(this.x * scale, this.y * scale, this.z * scale);
        }

        double dot(Point other) {

            return this.x * other.x + this.y * other.y + this.z * other.z;
        }

        double length() {

            return Math.sqrt(dot(this));
        }
    }

    private BeamMath() {
    }

    /**
     * Distance from point {@code p} to the segment {@code a}–{@code b}.
     */
    public static double pointToSegment(Point p, Point a, Point b) {

        Point ab = b.minus(a);
        double lengthSquared = ab.dot(ab);
        double t = lengthSquared == 0.0 ? 0.0 : Math.clamp(p.minus(a).dot(ab) / lengthSquared, 0.0, 1.0);
        return p.minus(a.plus(ab.times(t))).length();
    }

    /**
     * The closest points of segments {@code a0}–{@code a1} and {@code b0}–{@code b1}, as {first, second}.
     */
    public static Point[] closestPoints(Point a0, Point a1, Point b0, Point b1) {

        Point d1 = a1.minus(a0);
        Point d2 = b1.minus(b0);
        Point r = a0.minus(b0);
        double a = d1.dot(d1);
        double e = d2.dot(d2);
        double f = d2.dot(r);
        double s;
        double t;
        if (a <= 1.0E-12 && e <= 1.0E-12) {
            return new Point[]{a0, b0};
        }
        if (a <= 1.0E-12) {
            s = 0.0;
            t = Math.clamp(f / e, 0.0, 1.0);
        } else {
            double c = d1.dot(r);
            if (e <= 1.0E-12) {
                t = 0.0;
                s = Math.clamp(-c / a, 0.0, 1.0);
            } else {
                double b = d1.dot(d2);
                double denominator = a * e - b * b;
                s = denominator > 1.0E-12 ? Math.clamp((b * f - c * e) / denominator, 0.0, 1.0) : 0.0;
                t = (b * s + f) / e;
                if (t < 0.0) {
                    t = 0.0;
                    s = Math.clamp(-c / a, 0.0, 1.0);
                } else if (t > 1.0) {
                    t = 1.0;
                    s = Math.clamp((b - c) / a, 0.0, 1.0);
                }
            }
        }
        return new Point[]{a0.plus(d1.times(s)), b0.plus(d2.times(t))};
    }

    public static double segmentDistance(Point a0, Point a1, Point b0, Point b1) {

        Point[] closest = closestPoints(a0, a1, b0, b1);
        return closest[0].minus(closest[1]).length();
    }

    /**
     * Where two beams (start → end) cross, or null. Each beam's last {@link #TARGET_END_ZONE} blocks are left out, so
     * two tethers on one ghost don't count as crossing where they meet.
     */
    public static @Nullable Point crossing(Point aStart, Point aEnd, Point bStart, Point bEnd) {

        Point aCut = trimEnd(aStart, aEnd);
        Point bCut = trimEnd(bStart, bEnd);
        if (aCut == null || bCut == null) {
            return null;
        }
        Point[] closest = closestPoints(aStart, aCut, bStart, bCut);
        if (closest[0].minus(closest[1]).length() >= CROSSING_DISTANCE) {
            return null;
        }
        return closest[0].plus(closest[1]).times(0.5);
    }

    /**
     * The beam's end moved {@link #TARGET_END_ZONE} back toward its start, or null if the beam is shorter than that.
     */
    private static @Nullable Point trimEnd(Point start, Point end) {

        Point direction = end.minus(start);
        double length = direction.length();
        if (length <= TARGET_END_ZONE) {
            return null;
        }
        return start.plus(direction.times((length - TARGET_END_ZONE) / length));
    }
}
