package io.github.mdw22.paranormalcraft.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmfLevelsTest {

    @Test
    void noGhostIsTheBaseline() {

        assertEquals(1, EmfLevels.levelForDistance(Double.POSITIVE_INFINITY));
        assertEquals(1, EmfLevels.levelForDistance(24.01));
        assertEquals(1, EmfLevels.levelForDistance(100));
    }

    @Test
    void bandsMatchTheTable() {

        assertEquals(2, EmfLevels.levelForDistance(24.0));
        assertEquals(2, EmfLevels.levelForDistance(16.0));
        assertEquals(3, EmfLevels.levelForDistance(15.99));
        assertEquals(3, EmfLevels.levelForDistance(10.0));
        assertEquals(4, EmfLevels.levelForDistance(9.99));
        assertEquals(4, EmfLevels.levelForDistance(5.0));
        assertEquals(5, EmfLevels.levelForDistance(4.99));
        assertEquals(5, EmfLevels.levelForDistance(0.0));
    }

    @Test
    void levelNeverDropsAsYouGetCloser() {

        int previous = EmfLevels.MIN_LEVEL;
        for (double distance = 40.0; distance >= 0.0; distance -= 0.25) {
            int level = EmfLevels.levelForDistance(distance);
            assertEquals(true, level >= previous, "level dropped at " + distance);
            previous = level;
        }
        assertEquals(EmfLevels.MAX_LEVEL, previous);
    }
}
