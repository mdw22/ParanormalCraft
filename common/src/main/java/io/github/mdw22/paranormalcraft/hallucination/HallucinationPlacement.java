package io.github.mdw22.paranormalcraft.hallucination;

import org.jspecify.annotations.Nullable;

/**
 * Where a shadow figure may appear relative to the player, as plain maths so it can be unit tested. Angles are in
 * degrees and use Minecraft's yaw convention (0 = +Z/south, 90 = −X/west); a positive relative angle is to the
 * player's right.
 * <p>
 * The figure goes just inside the screen edge: between {@code max(28°, H − 18°)} and {@code H − 6°} off the view,
 * where {@code H} is the horizontal half-FOV (about 51° at FOV 70 in 16:9).
 */
public final class HallucinationPlacement {

    /** Never closer to the view than this, so the player has to turn a little before it vanishes. */
    public static final double MIN_ANGLE_FLOOR = 28.0;
    /** The band's inner and outer edges, measured inward from the screen edge. */
    public static final double INNER_FROM_EDGE = 18.0;
    public static final double OUTER_FROM_EDGE = 6.0;
    public static final double NEAR_MIN_DISTANCE = 8.0;
    public static final double NEAR_MAX_DISTANCE = 13.0;
    /** {@code reduced} intensity keeps the figure further away. */
    public static final double REDUCED_MIN_DISTANCE = 12.0;
    public static final double REDUCED_MAX_DISTANCE = 16.0;
    /** The figure vanishes once the player's view comes this close to it. */
    public static final double VANISH_VIEW_ANGLE = 20.0;

    public record Offset(double dx, double dz) {
    }

    /** Allowed magnitudes of the angle off the view, in degrees. */
    public record Band(double min, double max) {
    }

    private HallucinationPlacement() {
    }

    /**
     * The horizontal half-FOV in degrees for a vertical FOV (Minecraft's FOV setting) and a window aspect ratio.
     */
    public static double halfHorizontalFov(double verticalFov, double aspect) {

        return Math.toDegrees(Math.atan(Math.tan(Math.toRadians(verticalFov / 2.0)) * aspect));
    }

    /**
     * @param halfHorizontalFov see {@link #halfHorizontalFov}
     * @return the angle band for that screen, or null when the screen is too narrow ({@code H − 6° < 28°}) and the
     * figure should be skipped
     */
    public static @Nullable Band band(double halfHorizontalFov) {

        double max = halfHorizontalFov - OUTER_FROM_EDGE;
        if (max < MIN_ANGLE_FLOOR) {
            return null;
        }
        return new Band(Math.max(MIN_ANGLE_FLOOR, halfHorizontalFov - INNER_FROM_EDGE), max);
    }

    /**
     * @param right      which side of the player's view
     * @param unitRandom a uniform value in [0, 1)
     * @return a signed angle off the view direction, inside {@code band} in magnitude
     */
    public static double sampleAngle(Band band, boolean right, double unitRandom) {

        double magnitude = band.min() + unitRandom * (band.max() - band.min());
        return right ? magnitude : -magnitude;
    }

    public static double sampleDistance(boolean reduced, double unitRandom) {

        double min = reduced ? REDUCED_MIN_DISTANCE : NEAR_MIN_DISTANCE;
        double max = reduced ? REDUCED_MAX_DISTANCE : NEAR_MAX_DISTANCE;
        return min + unitRandom * (max - min);
    }

    /**
     * The horizontal offset from the player for a figure {@code relativeAngle} degrees off a view of {@code yaw}.
     */
    public static Offset offset(double yaw, double relativeAngle, double distance) {

        double radians = Math.toRadians(yaw + relativeAngle);
        return new Offset(-Math.sin(radians) * distance, Math.cos(radians) * distance);
    }

    /**
     * The signed horizontal angle between a view of {@code yaw} and the direction ({@code dx}, {@code dz}), in
     * (−180, 180].
     */
    public static double relativeAngle(double yaw, double dx, double dz) {

        double directionYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double difference = (directionYaw - yaw) % 360.0;
        if (difference > 180.0) {
            difference -= 360.0;
        } else if (difference <= -180.0) {
            difference += 360.0;
        }
        return difference;
    }

    /**
     * Whether a placement is inside the angle band (to either side) and at the right distance.
     */
    public static boolean inBand(double yaw, Offset offset, Band band, boolean reduced) {

        double angle = Math.abs(relativeAngle(yaw, offset.dx(), offset.dz()));
        double distance = Math.hypot(offset.dx(), offset.dz());
        double min = reduced ? REDUCED_MIN_DISTANCE : NEAR_MIN_DISTANCE;
        double max = reduced ? REDUCED_MAX_DISTANCE : NEAR_MAX_DISTANCE;
        double epsilon = 1.0E-9;
        return angle >= band.min() - epsilon && angle <= band.max() + epsilon && distance >= min - epsilon && distance <= max + epsilon;
    }

    /**
     * The 3D angle in degrees between two vectors, e.g. the view direction and the direction to the figure.
     */
    public static double angleBetween(double ax, double ay, double az, double bx, double by, double bz) {

        double lengths = Math.sqrt(ax * ax + ay * ay + az * az) * Math.sqrt(bx * bx + by * by + bz * bz);
        if (lengths == 0.0) {
            return 0.0;
        }
        double cos = Math.max(-1.0, Math.min(1.0, (ax * bx + ay * by + az * bz) / lengths));
        return Math.toDegrees(Math.acos(cos));
    }
}
