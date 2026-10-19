package io.github.mdw22.paranormalcraft.hallucination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FigureRetryTest {

    @Test
    void triesAtOnceThenEveryTenTicks() {

        assertEquals(FigureRetry.Step.TRY_VISIBLE, FigureRetry.step(0, false));
        for (int age = 1; age < 10; age++) {
            assertEquals(FigureRetry.Step.WAIT, FigureRetry.step(age, false));
        }
        assertEquals(FigureRetry.Step.TRY_VISIBLE, FigureRetry.step(10, false));
        assertEquals(FigureRetry.Step.TRY_VISIBLE, FigureRetry.step(190, false));
    }

    @Test
    void outdoorsNeverFallsBackToSoundFirst() {

        for (int age = 0; age <= FigureRetry.GIVE_UP_TICKS; age += 10) {
            assertEquals(FigureRetry.Step.TRY_VISIBLE, FigureRetry.step(age, false), "age " + age);
        }
    }

    @Test
    void indoorsFallsBackToSoundFirstAfterTwoSeconds() {

        assertEquals(FigureRetry.Step.TRY_VISIBLE, FigureRetry.step(30, true));
        assertEquals(FigureRetry.Step.TRY_VISIBLE_THEN_SOUND_FIRST, FigureRetry.step(40, true));
        assertEquals(FigureRetry.Step.TRY_VISIBLE_THEN_SOUND_FIRST, FigureRetry.step(200, true));
        // Sound first by 2.5 s at the latest (the GameTest's bound).
        assertEquals(true, FigureRetry.SOUND_FIRST_AFTER + FigureRetry.RETRY_INTERVAL <= 50);
    }

    @Test
    void givesUpAfterTenSeconds() {

        assertEquals(FigureRetry.Step.GIVE_UP, FigureRetry.step(201, false));
        assertEquals(FigureRetry.Step.GIVE_UP, FigureRetry.step(201, true));
    }

    @Test
    void soundFirstDistancesMatchTheSpec() {

        assertEquals(6.0, FigureRetry.SOUND_FIRST_MIN_DISTANCE);
        assertEquals(13.0, FigureRetry.SOUND_FIRST_MAX_DISTANCE);
    }
}
