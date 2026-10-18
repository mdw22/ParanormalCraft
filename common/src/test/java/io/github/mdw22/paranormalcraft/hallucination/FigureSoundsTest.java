package io.github.mdw22.paranormalcraft.hallucination;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FigureSoundsTest {

    @Test
    void fullPlaysEveryTwentyToThirtyTicksCoveringTheWholeRange() {

        Random random = new Random(3);
        boolean sawMin = false;
        boolean sawMax = false;
        for (int i = 0; i < 10_000; i++) {
            int interval = FigureSounds.nextInterval(false, random.nextDouble());
            assertTrue(interval >= 20 && interval <= 30, "interval " + interval);
            sawMin |= interval == 20;
            sawMax |= interval == 30;
        }
        assertTrue(sawMin && sawMax);
        assertEquals(20, FigureSounds.nextInterval(false, 0.0));
        assertEquals(30, FigureSounds.nextInterval(false, 0.999999));
    }

    @Test
    void reducedIsHalfAsLoudEveryFortyTicks() {

        assertEquals(40, FigureSounds.nextInterval(true, 0.0));
        assertEquals(40, FigureSounds.nextInterval(true, 0.999));
        assertEquals(FigureSounds.volume(false) / 2.0F, FigureSounds.volume(true), 1.0E-6);
    }

    @Test
    void firstSoundOnAppearingThenOnCadenceAndNoneWhileVanishing() {

        // Simulate a full-intensity figure for 200 ticks with fixed 25-tick intervals.
        int next = 0;
        int count = 0;
        int last = -1;
        for (int age = 0; age < 200; age++) {
            if (FigureSounds.due(age, next, false)) {
                if (last >= 0) {
                    assertEquals(25, age - last);
                } else {
                    assertEquals(0, age, "the first sound plays as it appears");
                }
                last = age;
                count++;
                next = age + FigureSounds.nextInterval(false, 0.5);
            }
        }
        assertEquals(8, count);
        assertFalse(FigureSounds.due(500, 0, true), "no sound once it is vanishing");
    }
}
