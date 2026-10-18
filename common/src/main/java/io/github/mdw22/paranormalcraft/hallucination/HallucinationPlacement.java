package io.github.mdw22.paranormalcraft.hallucination;

/**
 * Where a shadow figure may appear relative to the player, as plain maths so it can be unit tested. Angles are in
 * degrees and use Minecraft's yaw convention (0 = +Z/south, 90 = −X/west); a positive relative angle is to the
 * player's right.
 */
public final class HallucinationPlacement {

    public static final double MIN_ANGLE = 60.0;
    public static final double MAX_ANGLE = 80.0;
    public static final double NEAR_MIN_DISTANCE = 10.0;
    public static final double NEAR_MAX_DISTANCE = 16.0;
    /** {@code reduced} intensity keeps the figure further away. */
    public static final double REDUCED_MIN_DISTANCE = 16.0;
    public static final double REDUCED_MAX_DISTANCE = 20.0;
    /** The figure vanishes once the player's view comes this close to it. */
    public static final double VANISH_VIEW_ANGLE = 20.0;

    public record Offset(double dx, double dz) {
    }

    private HallucinationPlacement() {
    }

    /**
     * @param right   which side of the player's view
     * @param unitRandom a uniform value in [0, 1)
     * @return a signed angle off the view direction, 60 to 80° in magnitude
     */
    public static double sampleAngle(boolean right, double unitRandom) {

        double magnitude = MIN_ANGLE + unitRandom * (MAX_ANGLE - MIN_ANGLE);
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
     * Whether a placement is inside the allowed band (60 to 80° to either side, at the right distance).
     */
    public static boolean inBand(double yaw, Offset offset, boolean reduced) {

        double angle = Math.abs(relativeAngle(yaw, offset.dx(), offset.dz()));
        double distance = Math.hypot(offset.dx(), offset.dz());
        double min = reduced ? REDUCED_MIN_DISTANCE : NEAR_MIN_DISTANCE;
        double max = reduced ? REDUCED_MAX_DISTANCE : NEAR_MAX_DISTANCE;
        double epsilon = 1.0E-9;
        return angle >= MIN_ANGLE - epsilon && angle <= MAX_ANGLE + epsilon && distance >= min - epsilon && distance <= max + epsilon;
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
