package io.github.mdw22.paranormalcraft.entity.ghost;

/**
 * A ghost's Presence, the meter that replaces health. Valid hits drain it; after {@link #REGEN_DELAY_TICKS} without a
 * valid hit it comes back by {@link #REGEN_AMOUNT} every {@link #REGEN_INTERVAL_TICKS}. At zero the ghost disperses.
 * <p>
 * Plain logic with no game types, so it can be unit tested. {@link GhostEntity} ticks it on the server and syncs the
 * value.
 */
public final class Presence {

    public static final float MAX = 20.0F;
    /** 10 seconds without a valid hit before regeneration starts. */
    public static final int REGEN_DELAY_TICKS = 200;
    /** Then 1 Presence every 2 seconds. */
    public static final int REGEN_INTERVAL_TICKS = 40;
    public static final float REGEN_AMOUNT = 1.0F;

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
     * Applies a valid hit.
     *
     * @return true if this hit depleted the Presence, meaning the ghost should disperse now
     */
    public boolean hit(float damage) {

        if (damage <= 0.0F || isDepleted()) {
            return false;
        }
        this.value = Math.max(0.0F, this.value - damage);
        this.ticksSinceHit = 0;
        return isDepleted();
    }

    /**
     * Advances one tick. A depleted Presence never regenerates.
     */
    public void tick() {

        if (isDepleted()) {
            return;
        }
        if (this.ticksSinceHit < Integer.MAX_VALUE) {
            this.ticksSinceHit++;
        }
        int regenTicks = this.ticksSinceHit - REGEN_DELAY_TICKS;
        if (regenTicks > 0 && regenTicks % REGEN_INTERVAL_TICKS == 0 && this.value < MAX) {
            this.value = Math.min(MAX, this.value + REGEN_AMOUNT);
        }
    }
}
