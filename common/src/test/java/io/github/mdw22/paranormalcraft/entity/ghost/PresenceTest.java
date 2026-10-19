package io.github.mdw22.paranormalcraft.entity.ghost;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresenceTest {

    private static void tick(Presence presence, int ticks) {

        for (int i = 0; i < ticks; i++) {
            presence.tick(true);
        }
    }

    @Test
    void startsFull() {

        Presence presence = new Presence();

        assertEquals(Presence.MAX, presence.value());
        assertFalse(presence.isDepleted());
    }

    @Test
    void hitsDrainByTheDamageDealt() {

        Presence presence = new Presence();

        presence.drain(6.5F);
        assertEquals(13.5F, presence.value());
    }

    @Test
    void nonPositiveHitsAreIgnored() {

        Presence presence = new Presence();
        tick(presence, 300);

        presence.drain(0.0F);
        presence.drain(-3.0F);

        assertEquals(Presence.MAX, presence.value());
        assertEquals(Presence.REGEN_DELAY_TICKS + 300, presence.ticksSinceHit());
    }

    @Test
    void drainsStopAtTheFloorOfOneAndNeverDisperse() {

        Presence presence = new Presence();

        presence.drain(7.0F);
        presence.drain(7.0F);
        presence.drain(7.0F);
        assertEquals(1.0F, presence.value());
        assertFalse(presence.isDepleted());
        presence.drain(5.0F);
        assertEquals(1.0F, presence.value());
    }

    @Test
    void noRegenerationWhileItMayNot() {

        Presence presence = new Presence();
        presence.drain(16.0F);
        for (int i = 0; i < Presence.REGEN_DELAY_TICKS + Presence.REGEN_INTERVAL_TICKS * 3; i++) {
            presence.tick(false);
        }
        assertEquals(4.0F, presence.value());
    }

    @Test
    void noRegenerationForTenSecondsAfterAHit() {

        Presence presence = new Presence();
        presence.drain(10.0F);

        tick(presence, Presence.REGEN_DELAY_TICKS);

        assertEquals(10.0F, presence.value());
    }

    @Test
    void regeneratesOnePerTwoSecondsAfterTheDelay() {

        Presence presence = new Presence();
        presence.drain(10.0F);
        tick(presence, Presence.REGEN_DELAY_TICKS);

        tick(presence, Presence.REGEN_INTERVAL_TICKS - 1);
        assertEquals(10.0F, presence.value());
        tick(presence, 1);
        assertEquals(11.0F, presence.value());
        tick(presence, Presence.REGEN_INTERVAL_TICKS * 3);
        assertEquals(14.0F, presence.value());
    }

    @Test
    void regenerationStopsAtMax() {

        Presence presence = new Presence();
        presence.drain(1.5F);

        tick(presence, Presence.REGEN_DELAY_TICKS + Presence.REGEN_INTERVAL_TICKS * 5);

        assertEquals(Presence.MAX, presence.value());
    }

    @Test
    void aValidHitRestartsTheRegenerationDelay() {

        Presence presence = new Presence();
        presence.drain(5.0F);
        tick(presence, Presence.REGEN_DELAY_TICKS + Presence.REGEN_INTERVAL_TICKS - 1);

        presence.drain(1.0F);
        tick(presence, Presence.REGEN_DELAY_TICKS);

        assertEquals(14.0F, presence.value());
        tick(presence, Presence.REGEN_INTERVAL_TICKS);
        assertEquals(15.0F, presence.value());
    }

    @Test
    void aDepletedPresenceNeverRegenerates() {

        Presence presence = new Presence(0.0F, 0);
        tick(presence, 10_000);

        assertEquals(0.0F, presence.value());
    }

    @Test
    void restoredValuesAreClamped() {

        assertEquals(Presence.MAX, new Presence(50.0F, 0).value());
        assertEquals(0.0F, new Presence(-4.0F, 0).value());
        assertEquals(0, new Presence(10.0F, -7).ticksSinceHit());
    }
}
