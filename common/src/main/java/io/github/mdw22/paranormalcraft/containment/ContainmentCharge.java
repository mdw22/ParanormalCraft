package io.github.mdw22.paranormalcraft.containment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mdw22.paranormalcraft.Balance;

/**
 * A Containment Unit's charge meter, as plain logic so it can be unit tested. Each stored ghost drains 1 point per
 * {@link #DRAIN_TICKS_PER_GHOST} ticks, and processing drains 1 more per {@link #PROCESS_DRAIN_TICKS}. Fuel (soul sand
 * or soul soil) adds {@link #FUEL_CHARGE} and is taken whenever the charge is at or below {@link #REFUEL_AT_OR_BELOW}.
 * At 0 with ghosts inside the unit is Unstable.
 *
 * @param charge       0 to {@link #MAX_CHARGE}
 * @param drainAccum   ghost-ticks accumulated toward the next storage drain point
 * @param processAccum processing ticks accumulated toward the next processing drain point
 */
public record ContainmentCharge(int charge, int drainAccum, int processAccum) {

    public static final int MAX_CHARGE = Balance.Containment.MAX_CHARGE;
    public static final int FUEL_CHARGE = Balance.Containment.FUEL_CHARGE;
    public static final int REFUEL_AT_OR_BELOW = Balance.Containment.REFUEL_AT_OR_BELOW;
    public static final int DRAIN_TICKS_PER_GHOST = Balance.Containment.DRAIN_TICKS_PER_GHOST;
    public static final int PROCESS_DRAIN_TICKS = Balance.Containment.PROCESS_DRAIN_TICKS;
    public static final ContainmentCharge EMPTY = new ContainmentCharge(0, 0, 0);

    public static final Codec<ContainmentCharge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("charge").forGetter(ContainmentCharge::charge),
            Codec.INT.fieldOf("drain_accum").forGetter(ContainmentCharge::drainAccum),
            Codec.INT.fieldOf("process_accum").forGetter(ContainmentCharge::processAccum)
    ).apply(instance, ContainmentCharge::new));

    /**
     * One tick with {@code ghosts} stored, processing or not.
     */
    public ContainmentCharge tick(int ghosts, boolean processing) {

        int newCharge = this.charge;
        int drain = this.drainAccum + Math.max(0, ghosts);
        while (drain >= DRAIN_TICKS_PER_GHOST) {
            drain -= DRAIN_TICKS_PER_GHOST;
            newCharge--;
        }
        int process = this.processAccum + (processing ? 1 : 0);
        if (process >= PROCESS_DRAIN_TICKS) {
            process -= PROCESS_DRAIN_TICKS;
            newCharge--;
        }
        if (ghosts <= 0) {
            drain = 0;
        }
        return new ContainmentCharge(Math.max(0, newCharge), drain, process);
    }

    public boolean wantsFuel() {

        return this.charge <= REFUEL_AT_OR_BELOW;
    }

    /**
     * One fuel item burnt.
     */
    public ContainmentCharge fuelled() {

        return new ContainmentCharge(Math.min(MAX_CHARGE, this.charge + FUEL_CHARGE), this.drainAccum, this.processAccum);
    }

    public ContainmentCharge withCharge(int value) {

        return new ContainmentCharge(Math.clamp(value, 0, MAX_CHARGE), this.drainAccum, this.processAccum);
    }

    /**
     * Out of charge with ghosts inside: no processing, the alarm sounds (and in part D, breaches).
     */
    public boolean unstable(int ghosts) {

        return this.charge <= 0 && ghosts > 0;
    }
}
