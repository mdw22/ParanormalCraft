package io.github.mdw22.paranormalcraft.hallucination;

import io.github.mdw22.paranormalcraft.Balance;

/**
 * When a shadow figure that couldn't be placed is tried again, as plain logic so it can be unit tested. A request is
 * tried at once and then every {@link #RETRY_INTERVAL} ticks, as the player turns, for up to {@link #GIVE_UP_TICKS}.
 * Indoors, from {@link #SOUND_FIRST_AFTER} ticks on, a try that finds no visible spot places the figure "sound
 * first": somewhere standable but out of sight, so the player hears it before seeing it.
 */
public final class FigureRetry {

    public static final int RETRY_INTERVAL = Balance.Hallucination.RETRY_INTERVAL;
    /** 2 s of failed tries indoors before falling back to sound first. */
    public static final int SOUND_FIRST_AFTER = Balance.Hallucination.SOUND_FIRST_AFTER;
    /** 10 s, after which the request is dropped. */
    public static final int GIVE_UP_TICKS = Balance.Hallucination.GIVE_UP_TICKS;
    /** Sound-first spots: this many blocks out. */
    public static final double SOUND_FIRST_MIN_DISTANCE = Balance.Hallucination.SOUND_FIRST_MIN_DISTANCE;
    public static final double SOUND_FIRST_MAX_DISTANCE = Balance.Hallucination.SOUND_FIRST_MAX_DISTANCE;
    /** Indoors, a visible spot inside (the next room) may be this close. */
    public static final double INDOOR_MIN_DISTANCE = Balance.Hallucination.INDOOR_MIN_DISTANCE;

    public enum Step {
        /** Nothing this tick. */
        WAIT,
        /** Look for a visible spot. */
        TRY_VISIBLE,
        /** Look for a visible spot, then fall back to a sound-first one. */
        TRY_VISIBLE_THEN_SOUND_FIRST,
        /** Out of time: report it skipped. */
        GIVE_UP
    }

    private FigureRetry() {
    }

    /**
     * @param age     ticks since the request arrived (0 on arrival)
     * @param indoors whether the player is under a roof right now
     */
    public static Step step(int age, boolean indoors) {

        if (age > GIVE_UP_TICKS) {
            return Step.GIVE_UP;
        }
        if (age % RETRY_INTERVAL != 0) {
            return Step.WAIT;
        }
        return indoors && age >= SOUND_FIRST_AFTER ? Step.TRY_VISIBLE_THEN_SOUND_FIRST : Step.TRY_VISIBLE;
    }
}
