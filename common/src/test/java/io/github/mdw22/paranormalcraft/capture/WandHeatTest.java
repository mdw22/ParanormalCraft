package io.github.mdw22.paranormalcraft.capture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WandHeatTest {

    @Test
    void overheatsAfterEightSecondsOfContinuousFire() {

        WandHeat heat = WandHeat.COLD;
        for (int tick = 1; tick < 160; tick++) {
            heat = heat.fire();
            assertTrue(heat.canFire(), "locked out early at tick " + tick);
        }
        heat = heat.fire();
        assertTrue(heat.overheated());
        assertEquals(WandHeat.LOCKOUT_TICKS, heat.lockout());
        assertEquals(heat, heat.fire(), "firing while locked out does nothing");
    }

    @Test
    void coolsTwiceAsFastAndUnlocksAfterFourSeconds() {

        WandHeat heat = WandHeat.COLD;
        for (int tick = 0; tick < 160; tick++) {
            heat = heat.fire();
        }
        for (int tick = 1; tick < 80; tick++) {
            heat = heat.cool();
            assertFalse(heat.canFire(), "unlocked early at tick " + tick);
        }
        heat = heat.cool();
        assertTrue(heat.canFire());
        assertEquals(0, heat.heat());
        WandHeat warm = new WandHeat(40, 0, 0);
        for (int tick = 0; tick < 20; tick++) {
            warm = warm.cool();
        }
        assertEquals(0, warm.heat(), "40 heat cools in 20 ticks");
    }

    @Test
    void chargesOneDurabilityPerSecondOfBeam() {

        WandHeat heat = WandHeat.COLD;
        int charges = 0;
        for (int tick = 0; tick < 100; tick++) {
            heat = heat.fire();
            if (heat.durabilityDue()) {
                heat = heat.charged();
                charges++;
            }
        }
        assertEquals(5, charges);
    }

    @Test
    void crossedBeamsForceAnOverheat() {

        WandHeat heat = new WandHeat(10, 0, 3).overheat();
        assertTrue(heat.overheated());
        assertEquals(WandHeat.MAX_HEAT, heat.heat());
        assertEquals(1.0F, heat.fraction());
    }
}
