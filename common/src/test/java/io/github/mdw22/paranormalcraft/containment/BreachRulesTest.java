package io.github.mdw22.paranormalcraft.containment;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreachRulesTest {

    @Test
    void breachAfterAMinuteUnstableThenAnEscapeEveryThirtySeconds() {

        assertFalse(BreachRules.breached(1199));
        assertTrue(BreachRules.breached(1200));
        assertFalse(BreachRules.escapesOn(0));
        assertFalse(BreachRules.escapesOn(599));
        assertTrue(BreachRules.escapesOn(600));
        assertTrue(BreachRules.escapesOn(1200));
    }

    @Test
    void storageOrderWithTheBusyGhostLast() {

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        assertEquals(0, BreachRules.nextToEscape(List.of(a, b, c), null, null));
        assertEquals(1, BreachRules.nextToEscape(List.of(a, b, c), a, null), "the one processing goes last");
        assertEquals(2, BreachRules.nextToEscape(List.of(a, b, c), a, b), "and the one binding");
        assertEquals(0, BreachRules.nextToEscape(List.of(a), a, null), "until it's the only one left");
        assertEquals(-1, BreachRules.nextToEscape(List.of(), null, null));
    }
}
