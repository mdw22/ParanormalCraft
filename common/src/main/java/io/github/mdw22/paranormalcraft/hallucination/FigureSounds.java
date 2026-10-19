package io.github.mdw22.paranormalcraft.hallucination;

import io.github.mdw22.paranormalcraft.Balance;

/**
 * When and how loud the shadow figure's presence sound plays, as plain logic so it can be unit tested. It plays once
 * when the figure appears, then every 20–30 ticks at {@code full} (about half as loud, every 40 ticks, at
 * {@code reduced}) until the figure vanishes.
 */
public final class FigureSounds {

    public static final int FULL_MIN_INTERVAL = Balance.Hallucination.SOUND_FULL_MIN_INTERVAL;
    public static final int FULL_MAX_INTERVAL = Balance.Hallucination.SOUND_FULL_MAX_INTERVAL;
    public static final int REDUCED_INTERVAL = Balance.Hallucination.SOUND_REDUCED_INTERVAL;
    public static final float FULL_VOLUME = Balance.Hallucination.SOUND_FULL_VOLUME;
    public static final float REDUCED_VOLUME = Balance.Hallucination.SOUND_REDUCED_VOLUME;

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
