package io.github.mdw22.paranormalcraft.entity.ghost;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.capture.PresenceRules;

/**
 * A ghost's Presence, the meter that replaces health. Valid hits and tethers drain it, never below
 * {@link PresenceRules#FLOOR}; after {@link #REGEN_DELAY_TICKS} without a drain it comes back by {@link #REGEN_AMOUNT}
 * every {@link #REGEN_INTERVAL_TICKS} (not while Weakened). Only setting it to zero (debug, {@code /kill}) disperses.
 * <p>
 * Plain logic with no game types, so it can be unit tested. {@link GhostEntity} ticks it on the server and syncs the
 * value.
 */
public final class Presence {

    public static final float MAX = Balance.Presence.MAX;
    /** 10 seconds without a valid hit before regeneration starts. */
    public static final int REGEN_DELAY_TICKS = Balance.Presence.REGEN_DELAY_TICKS;
    /** Then 1 Presence every 2 seconds. */
    public static final int REGEN_INTERVAL_TICKS = Balance.Presence.REGEN_INTERVAL_TICKS;
    public static final float REGEN_AMOUNT = Balance.Presence.REGEN_AMOUNT;

    private float value;
    private int ticksSinceHit;

    public Presence() {

        this(MAX, REGEN_DELAY_TICKS);
    }

    public Presence(float value, int ticksSinceHit) {

        this.value = Math.clamp(value, 0.0F, MAX);
        this.ticksSinceHit = Math.max(0, ticksSinceHit);
    }

    public float value() {

        return this.value;
    }

    public int ticksSinceHit() {

        return this.ticksSinceHit;
    }

    public boolean isDepleted() {

        return this.value <= 0.0F;
    }

    /**
     * Drains {@code amount}, never below {@link PresenceRules#FLOOR}, and restarts the regeneration delay.
     */
    public void drain(float amount) {

        if (amount <= 0.0F || isDepleted()) {
            return;
        }
        this.value = PresenceRules.afterDrain(this.value, amount);
        this.ticksSinceHit = 0;
    }

    /**
     * Advances one tick. A depleted Presence never regenerates, and neither does one that may not regenerate now
     * (a Weakened ghost waits for its recovery instead).
     */
    public void tick(boolean mayRegenerate) {

        if (isDepleted()) {
            return;
        }
        if (this.ticksSinceHit < Integer.MAX_VALUE) {
            this.ticksSinceHit++;
        }
        int regenTicks = this.ticksSinceHit - REGEN_DELAY_TICKS;
        if (mayRegenerate && regenTicks > 0 && regenTicks % REGEN_INTERVAL_TICKS == 0 && this.value < MAX) {
            this.value = Math.min(MAX, this.value + REGEN_AMOUNT);
        }
    }
}
