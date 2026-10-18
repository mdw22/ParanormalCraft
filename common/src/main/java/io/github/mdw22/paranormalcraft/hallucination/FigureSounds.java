package io.github.mdw22.paranormalcraft.hallucination;

/**
 * When and how loud the shadow figure's presence sound plays, as plain logic so it can be unit tested. It plays once
 * when the figure appears, then every 20–30 ticks at {@code full} (about half as loud, every 40 ticks, at
 * {@code reduced}) until the figure vanishes.
 */
public final class FigureSounds {

    public static final int FULL_MIN_INTERVAL = 20;
    public static final int FULL_MAX_INTERVAL = 30;
    public static final int REDUCED_INTERVAL = 40;
    public static final float FULL_VOLUME = 0.6F;
    public static final float REDUCED_VOLUME = 0.3F;

    private FigureSounds() {
    }

    /**
     * Ticks until the next presence sound.
     *
     * @param unitRandom a uniform value in [0, 1)
     */
    public static int nextInterval(boolean reduced, double unitRandom) {

        if (reduced) {
            return REDUCED_INTERVAL;
        }
        return FULL_MIN_INTERVAL + (int) Math.floor(unitRandom * (FULL_MAX_INTERVAL - FULL_MIN_INTERVAL + 1));
    }

    public static float volume(boolean reduced) {

        return reduced ? REDUCED_VOLUME : FULL_VOLUME;
    }

    /**
     * Whether a sound is due this tick. {@code nextSoundTick} is the figure's age at which the next one plays; the
     * first plays at age 0.
     */
    public static boolean due(int age, int nextSoundTick, boolean vanishing) {

        return !vanishing && age >= nextSoundTick;
    }
}
