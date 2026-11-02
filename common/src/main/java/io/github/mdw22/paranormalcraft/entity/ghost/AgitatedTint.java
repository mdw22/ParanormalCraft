package io.github.mdw22.paranormalcraft.entity.ghost;

import io.github.mdw22.paranormalcraft.Balance;

/**
 * The colour of an Agitated ghost: clearly red, with green and blue rising and falling in a slow pulse. It replaces the
 * goggles' cyan rather than multiplying it, so it reads as "angry" the same way for every viewer. Visual only.
 */
public final class AgitatedTint {

    private AgitatedTint() {
    }

    /** Green and blue, 0 to 1, at this time (in ticks, partial tick included). */
    public static float greenBlue(float ticks) {

        double phase = 2.0 * Math.PI * ticks / Balance.Escape.AGITATED_TINT_PERIOD_TICKS;
        return Balance.Escape.AGITATED_TINT_GREEN_BLUE + Balance.Escape.AGITATED_TINT_PULSE * (float) Math.sin(phase);
    }

    /** The tint as 0xRRGGBB. */
    public static int rgb(float ticks) {

        int greenBlue = Math.round(greenBlue(ticks) * 255.0F);
        return 0xFF0000 | greenBlue << 8 | greenBlue;
    }
}
