package io.github.mdw22.paranormalcraft.familiar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WispLightTest {

    @Test
    void darkWithoutALightOfItsOwn() {

        assertTrue(WispLight.underlyingDark(0, 0, -1));
        assertTrue(WispLight.underlyingDark(7, 0, -1));
        assertFalse(WispLight.underlyingDark(8, 0, -1), "a torch-lit spot");
        assertFalse(WispLight.underlyingDark(0, 8, -1), "daylight");
        assertTrue(WispLight.underlyingDark(0, 7, -1), "dusk");
    }

    @Test
    void itsOwnLightDoesNotCount() {

        // One block from its own level-11 light: block light 10 is all its own.
        assertTrue(WispLight.underlyingDark(10, 0, 1));
        // Its own light reaches 9 at distance 2; 13 there must come from a torch.
        assertFalse(WispLight.underlyingDark(13, 0, 2));
    }

    @Test
    void farFromItsOldLightItJudgesLikeNew() {

        assertFalse(WispLight.underlyingDark(9, 0, 20));
        assertTrue(WispLight.underlyingDark(3, 0, 20));
    }
}
