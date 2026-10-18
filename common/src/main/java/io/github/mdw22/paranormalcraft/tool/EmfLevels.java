package io.github.mdw22.paranormalcraft.tool;

/**
 * The EMF Reader's distance-to-level table. Plain logic so it can be unit tested.
 */
public final class EmfLevels {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 5;
    /** Ghosts further than this don't register; the reader shows its baseline light. */
    public static final double RANGE = 24.0;

    private EmfLevels() {
    }

    /**
     * @param distance distance to the nearest ghost, or {@link Double#POSITIVE_INFINITY} if there is none
     */
    public static int levelForDistance(double distance) {

        if (distance < 5.0) {
            return 5;
        }
        if (distance < 10.0) {
            return 4;
        }
        if (distance < 16.0) {
            return 3;
        }
        if (distance <= RANGE) {
            return 2;
        }
        return MIN_LEVEL;
    }
}
