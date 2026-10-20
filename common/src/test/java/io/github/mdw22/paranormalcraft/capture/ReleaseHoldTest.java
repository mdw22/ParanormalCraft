package io.github.mdw22.paranormalcraft.capture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseHoldTest {

    @Test
    void completesAfterFortyTicksOfPressesEveryFourTicks() {

        ReleaseHold hold = null;
        for (long tick = 100; tick < 140; tick += 4) {
            hold = ReleaseHold.press(hold, tick);
            assertFalse(hold.complete(), "complete early at " + tick);
        }
        hold = ReleaseHold.press(hold, 140);
        assertTrue(hold.complete());
        assertEquals(1.0, hold.progress());
    }

    @Test
    void aGapRestartsTheHold() {

        ReleaseHold hold = ReleaseHold.press(null, 0);
        hold = ReleaseHold.press(hold, 4);
        hold = ReleaseHold.press(hold, 8);
        assertTrue(hold.lapsed(15));
        hold = ReleaseHold.press(hold, 20);
        assertEquals(20, hold.start(), "restarted after a 12-tick gap");
        for (long tick = 24; tick < 60; tick += 4) {
            hold = ReleaseHold.press(hold, tick);
            assertFalse(hold.complete(), "complete early at " + tick);
        }
        assertTrue(ReleaseHold.press(hold, 60).complete(), "40 ticks after the restart");
    }

    @Test
    void progressRises() {

        ReleaseHold hold = ReleaseHold.press(null, 0);
        assertEquals(0.0, hold.progress());
        for (long tick = 4; tick <= 20; tick += 4) {
            hold = ReleaseHold.press(hold, tick);
        }
        assertEquals(0.5, hold.progress());
    }
}
