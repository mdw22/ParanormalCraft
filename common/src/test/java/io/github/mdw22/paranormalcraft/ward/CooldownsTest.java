package io.github.mdw22.paranormalcraft.ward;

import io.github.mdw22.paranormalcraft.Balance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rate limits behind the ward's denied-spawn puff (1 per 2 s per lantern) and flare (1 per 3 s per lantern).
 */
class CooldownsTest {

    private static final long PUFF = Balance.Ward.PUFF_COOLDOWN_TICKS;
    private static final long FLARE = Balance.Ward.FLARE_COOLDOWN_TICKS;

    @Test
    void puffAtMostOncePerTwoSecondsPerLantern() {

        Cooldowns<String> puffs = new Cooldowns<>();
        assertEquals(40, PUFF);
        assertTrue(puffs.tryUse("lantern", 100, PUFF));
        for (long tick = 101; tick < 140; tick++) {
            assertFalse(puffs.tryUse("lantern", tick, PUFF), "puffed again at tick " + tick);
        }
        assertTrue(puffs.tryUse("lantern", 140, PUFF));
    }

    @Test
    void puffsDenyAMobSpawnEveryTickButShowEveryFortyTicks() {

        Cooldowns<String> puffs = new Cooldowns<>();
        int shown = 0;
        // A spawn denied every tick for 10 s.
        for (long tick = 0; tick < 200; tick++) {
            if (puffs.tryUse("lantern", tick, PUFF)) {
                shown++;
            }
        }
        assertEquals(5, shown);
    }

    @Test
    void eachLanternHasItsOwnCooldown() {

        Cooldowns<String> puffs = new Cooldowns<>();
        assertTrue(puffs.tryUse("a", 0, PUFF));
        assertTrue(puffs.tryUse("b", 1, PUFF));
        assertFalse(puffs.tryUse("a", 2, PUFF));
        assertFalse(puffs.tryUse("b", 2, PUFF));
    }

    @Test
    void flareAtMostOncePerThreeSeconds() {

        Cooldowns<String> flares = new Cooldowns<>();
        assertEquals(60, FLARE);
        assertTrue(flares.tryUse("lantern", 0, FLARE));
        assertFalse(flares.tryUse("lantern", 59, FLARE));
        assertTrue(flares.tryUse("lantern", 60, FLARE));
    }

    @Test
    void clearForgetsEverything() {

        Cooldowns<String> puffs = new Cooldowns<>();
        assertTrue(puffs.tryUse("lantern", 0, PUFF));
        puffs.clear();
        assertTrue(puffs.tryUse("lantern", 1, PUFF));
    }
}
