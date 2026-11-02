package io.github.mdw22.paranormalcraft.ward;

import io.github.mdw22.paranormalcraft.Balance;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the motes of a ward's edge go, as plain logic, so it's unit tested. Points are evenly spaced round the whole
 * circle, so the last one meets the first across the 0/2π seam. When a preview round would go over its mote budget the
 * whole round gets sparser: it's never cut short, which would leave a gap at the end of the circle.
 */
public final class WardRing {

    /** A point on the edge. {@code post}: it carries a preview post. */
    public record Point(double x, double z, boolean post) {
    }

    private static final double RANGE_SLACK = 1.0E-6;

    private WardRing() {
    }

    /** Evenly spaced points for {@code motesPerBlock} round a circle of {@code radius}. */
    public static int pointCount(int radius, double motesPerBlock) {

        return Math.max(1, (int) Math.ceil(2 * Math.PI * radius * motesPerBlock));
    }

    /** The whole circle in {@code points} evenly spaced points, from angle 0. */
    public static List<Point> circle(double centreX, double centreZ, int radius, int points) {

        double arc = 2 * Math.PI * radius / points;
        int spacing = Balance.Ward.PREVIEW_POST_SPACING;
        List<Point> ring = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            boolean post = i == 0 || (int) (arc * i / spacing) != (int) (arc * (i - 1) / spacing);
            ring.add(new Point(centreX + Math.cos(angle) * radius, centreZ + Math.sin(angle) * radius, post));
        }
        return ring;
    }

    /**
     * One preview round: the points of the circle within {@code range} of the player, in at most {@code budget} motes
     * counting the posts.
     */
    public static List<Point> previewRound(double centreX, double centreZ, int radius, double playerX, double playerZ, double range, int budget) {

        int points = pointCount(radius, Balance.Ward.PREVIEW_MOTES_PER_BLOCK);
        while (true) {
            List<Point> visible = new ArrayList<>();
            for (Point point : circle(centreX, centreZ, radius, points)) {
                if (inRange(point, playerX, playerZ, range)) {
                    visible.add(point);
                }
            }
            int motes = motes(visible);
            if (motes <= budget || points <= 1) {
                return visible;
            }
            points = Math.max(1, Math.min(points - 1, (int) (points * (double) budget / motes)));
        }
    }

    /** How many motes these points take, posts included. */
    public static int motes(List<Point> points) {

        int motes = points.size();
        for (Point point : points) {
            motes += point.post() ? Balance.Ward.PREVIEW_POST_HEIGHT - 1 : 0;
        }
        return motes;
    }

    /**
     * The widest gap between neighbouring points of a round, in blocks, across the seam as well. A pair of neighbours
     * either side of the part of the circle out of the player's range doesn't count: that's no gap in what they see.
     */
    public static double widestGap(List<Point> points, double centreX, double centreZ, int radius, double playerX, double playerZ, double range) {

        if (points.isEmpty()) {
            return 0.0;
        }
        double[] angles = points.stream().mapToDouble(point -> Math.atan2(point.z() - centreZ, point.x() - centreX)).sorted().toArray();
        double widest = 0.0;
        for (int i = 0; i < angles.length; i++) {
            double from = angles[i];
            double to = i + 1 < angles.length ? angles[i + 1] : angles[0] + 2 * Math.PI;
            double middle = (from + to) / 2.0;
            Point between = new Point(centreX + Math.cos(middle) * radius, centreZ + Math.sin(middle) * radius, false);
            if (inRange(between, playerX, playerZ, range)) {
                widest = Math.max(widest, (to - from) * radius);
            }
        }
        return widest;
    }

    private static boolean inRange(Point point, double x, double z, double range) {

        // A little slack: standing at the lantern with the radius equal to the range, rounding mustn't drop points.
        return Math.hypot(point.x() - x, point.z() - z) <= range + RANGE_SLACK;
    }
}
