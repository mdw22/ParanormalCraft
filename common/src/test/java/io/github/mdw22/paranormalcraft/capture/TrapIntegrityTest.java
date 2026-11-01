package io.github.mdw22.paranormalcraft.capture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrapIntegrityTest {

    @Test
    void drainsEvenlyOverTheConfiguredLifetime() {

        TrapSeal seal = TrapIntegrity.fresh(1_000, 20);
        assertEquals(24_000, seal.lifetimeTicks(), "20 real minutes is one in-game day");
        assertEquals(100.0F, TrapIntegrity.integrity(seal, 1_000), 1.0E-4);
        assertEquals(50.0F, TrapIntegrity.integrity(seal, 13_000), 1.0E-4);
        assertEquals(0.0F, TrapIntegrity.integrity(seal, 25_000), 1.0E-4);
        assertTrue(TrapIntegrity.broken(seal, 25_000));
        assertFalse(TrapIntegrity.broken(seal, 24_999));
    }

    @Test
    void offlineTimeCountsTheSame() {

        // Sealed, then nothing ticked it for most of a day (an unloaded chunk, a closed chest): read once, much later.
        TrapSeal seal = TrapIntegrity.fresh(0, 20);
        assertEquals(25.0F, TrapIntegrity.integrity(seal, 18_000), 1.0E-4);
        assertEquals(0.0F, TrapIntegrity.integrity(seal, 10_000_000), 1.0E-4, "never below 0");
        assertEquals(100.0F, TrapIntegrity.integrity(seal, -50), 1.0E-4, "never above the start");
    }

    @Test
    void configScalesTheLifetime() {

        TrapSeal quick = TrapIntegrity.fresh(0, 1);
        TrapSeal slow = TrapIntegrity.fresh(0, 60);
        assertEquals(0.0F, TrapIntegrity.integrity(quick, 1_200), 1.0E-4);
        assertEquals(100.0F - 100.0F / 60.0F, TrapIntegrity.integrity(slow, 1_200), 1.0E-3);
    }

    @Test
    void aSealSetPartwayDrainsFromThere() {

        TrapSeal seal = TrapIntegrity.at(500, 30.0F, 24_000);
        assertEquals(30.0F, TrapIntegrity.integrity(seal, 500), 1.0E-4);
        assertEquals(20.0F, TrapIntegrity.integrity(seal, 500 + 2_400), 1.0E-4);
        assertEquals(31, TrapIntegrity.percent(30.2F), "rounds up, so 0% shows only when broken");
    }

    @Test
    void rattleScheduleGetsWorseAsIntegrityDrops() {

        TrapIntegrity.Rattle healthyPlaced = TrapIntegrity.rattle(100.0F, true);
        assertTrue(healthyPlaced.allowed());
        assertEquals(900, healthyPlaced.minTicks(), "at most one rattle every 45 s");
        assertEquals(1800, healthyPlaced.maxTicks());
        assertTrue(healthyPlaced.volume() < 0.5F, "soft");
        assertFalse(TrapIntegrity.rattle(80.0F, false).allowed(), "silent in an inventory or chest at 50% and above");
        TrapIntegrity.Rattle middle = TrapIntegrity.rattle(37.5F, false);
        assertTrue(middle.allowed() && middle.minTicks() < 900 && middle.minTicks() > 200, "carried below 50%: in between");
        TrapIntegrity.Rattle urgent = TrapIntegrity.rattle(10.0F, false);
        assertEquals(200, urgent.minTicks(), "every 10–20 s below 25%, anywhere");
        assertEquals(400, urgent.maxTicks());
        assertEquals(1.0F, urgent.volume(), 1.0E-4);
        assertEquals(1.0 / 300.0, urgent.chancePerTick(), 1.0E-9);
    }

    @Test
    void emfAndCreak() {

        assertEquals(3, TrapIntegrity.emfCap(60.0F));
        assertEquals(4, TrapIntegrity.emfCap(24.9F));
        assertTrue(TrapIntegrity.creaks(9.0F));
        assertFalse(TrapIntegrity.creaks(10.0F));
    }

    @Test
    void barGoesFromGreenToRed() {

        assertEquals(0x00FF00, TrapIntegrity.barColour(100.0F));
        assertEquals(0xFF0000, TrapIntegrity.barColour(0.0F));
        assertEquals(0xFFFF00, TrapIntegrity.barColour(50.0F));
    }
}
