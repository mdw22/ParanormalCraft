package io.github.mdw22.paranormalcraft.entity.ghost;

import io.github.mdw22.paranormalcraft.Balance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgitatedTintTest {

    @Test
    void clearlyRedThroughoutThePulse() {

        for (int tick = 0; tick < Balance.Escape.AGITATED_TINT_PERIOD_TICKS; tick++) {
            int rgb = AgitatedTint.rgb(tick);
            assertEquals(0xFF, rgb >> 16 & 0xFF);
            int green = rgb >> 8 & 0xFF;
            assertEquals(green, rgb & 0xFF);
            assertTrue(green >= 0.3 * 255 && green <= 0.6 * 255, "green and blue " + green + " on tick " + tick);
        }
    }

    @Test
    void pulsesRoundThePlannedMiddle() {

        int period = Balance.Escape.AGITATED_TINT_PERIOD_TICKS;
        assertEquals(Balance.Escape.AGITATED_TINT_GREEN_BLUE, AgitatedTint.greenBlue(0.0F), 1.0E-4);
        assertEquals(Balance.Escape.AGITATED_TINT_GREEN_BLUE + Balance.Escape.AGITATED_TINT_PULSE, AgitatedTint.greenBlue(period / 4.0F), 1.0E-4);
        assertEquals(Balance.Escape.AGITATED_TINT_GREEN_BLUE - Balance.Escape.AGITATED_TINT_PULSE, AgitatedTint.greenBlue(period * 3 / 4.0F), 1.0E-4);
    }
}
