package io.github.mdw22.paranormalcraft.capture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptureRulesTest {

    @Test
    void presenceFloorHoldsAtOne() {

        assertEquals(1.0F, PresenceRules.afterDrain(3.0F, 10.0F));
        assertEquals(1.0F, PresenceRules.afterDrain(1.0F, 0.5F));
        assertEquals(0.5F, PresenceRules.afterDrain(0.5F, 0.5F), "never raised to the floor");
        assertEquals(17.5F, PresenceRules.afterDrain(20.0F, 2.5F));
        assertEquals(20.0F, PresenceRules.afterDrain(20.0F, 0.0F));
    }

    @Test
    void meleeDrainsHalfRoundedDownToAHalfPointAtLeastAHalf() {

        assertEquals(0.5F, PresenceRules.meleeDrain(1.0F));
        assertEquals(0.5F, PresenceRules.meleeDrain(0.2F));
        assertEquals(1.0F, PresenceRules.meleeDrain(2.0F));
        assertEquals(1.5F, PresenceRules.meleeDrain(3.0F));
        assertEquals(1.5F, PresenceRules.meleeDrain(3.9F));
        assertEquals(4.0F, PresenceRules.meleeDrain(8.0F));
        assertEquals(0.0F, PresenceRules.meleeDrain(0.0F));
    }

    @Test
    void weakenedAtFiveOrBelow() {

        assertTrue(PresenceRules.isWeakened(5.0F));
        assertTrue(PresenceRules.isWeakened(1.0F));
        assertFalse(PresenceRules.isWeakened(5.1F));
    }

    @Test
    void recoversFifteenSecondsAfterTheLastTether() {

        assertFalse(PresenceRules.shouldRecover(1299, 1000));
        assertTrue(PresenceRules.shouldRecover(1300, 1000));
        assertEquals(300, PresenceRules.RECOVERY_TICKS);
        assertEquals(0.1F, PresenceRules.TETHER_DRAIN_PER_TICK, 1.0E-6);
    }

    @Test
    void captureNeedsATetherOrTheCoinFlip() {

        assertTrue(CaptureRules.succeeds(true, 0.99));
        assertTrue(CaptureRules.succeeds(false, 0.49));
        assertFalse(CaptureRules.succeeds(false, 0.5));
        assertFalse(CaptureRules.succeeds(false, 0.9));
    }

    @Test
    void captureZoneIsAThreeHighColumnOneBlockWide() {

        assertTrue(CaptureRules.inZone(0, 1, 0));
        assertTrue(CaptureRules.inZone(1.4, 2.9, -1.4));
        assertFalse(CaptureRules.inZone(1.6, 1, 0));
        assertFalse(CaptureRules.inZone(0, 3.1, 0));
    }
}
