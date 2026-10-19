package io.github.mdw22.paranormalcraft.capture;

import io.github.mdw22.paranormalcraft.Balance;

/**
 * The Milestone 2 Presence rules, as plain logic so they can be unit tested.
 * <ul>
 *     <li>Damage (melee, arrows, the Tether drain) never takes Presence below {@link #FLOOR}: hitting a ghost no longer
 *     disperses it. Only {@code /kill}, the void and {@code debug presence 0} do.</li>
 *     <li>Melee drains half its damage, rounded down to a half point, at least {@link #MIN_MELEE_DRAIN}.</li>
 *     <li>At {@link #WEAKENED_AT} or below the ghost is Weakened: it flickers for everyone, moves at half speed and
 *     heads home.</li>
 *     <li>A Weakened ghost not trapped within {@link #RECOVERY_TICKS} of its last tether recovers to
 *     {@link #RECOVERED_PRESENCE}.</li>
 * </ul>
 */
public final class PresenceRules {

    public static final float FLOOR = Balance.Presence.FLOOR;
    public static final float MIN_MELEE_DRAIN = Balance.Presence.MIN_MELEE_DRAIN;
    public static final float WEAKENED_AT = Balance.Presence.WEAKENED_AT;
    /** 15 s. */
    public static final int RECOVERY_TICKS = Balance.Presence.RECOVERY_TICKS;
    public static final float RECOVERED_PRESENCE = Balance.Presence.RECOVERED_PRESENCE;
    /** A ghost that breaks free of a trap is left at this. */
    public static final float BREAK_FREE_PRESENCE = Balance.Presence.BREAK_FREE_PRESENCE;
    /** Each latched Tether drains this much per second (0.1 per tick). */
    public static final float TETHER_DRAIN_PER_SECOND = Balance.Presence.TETHER_DRAIN_PER_SECOND;
    public static final float TETHER_DRAIN_PER_TICK = TETHER_DRAIN_PER_SECOND / 20.0F;

    private PresenceRules() {
    }

    /**
     * What a melee hit of {@code damage} drains: half, rounded down to a multiple of 0.5, at least 0.5.
     */
    public static float meleeDrain(float damage) {

        if (damage <= 0.0F) {
            return 0.0F;
        }
        float half = (float) Math.floor(damage / 2.0F * 2.0F) / 2.0F;
        return Math.max(MIN_MELEE_DRAIN, half);
    }

    /**
     * Presence after draining {@code drain}, never below {@link #FLOOR} (and never raised if already below it).
     */
    public static float afterDrain(float current, float drain) {

        if (drain <= 0.0F || current <= FLOOR) {
            return current;
        }
        return Math.max(FLOOR, current - drain);
    }

    public static boolean isWeakened(float presence) {

        return presence <= WEAKENED_AT;
    }

    /**
     * Whether a Weakened ghost recovers now: {@link #RECOVERY_TICKS} since it was last tethered (or became Weakened).
     */
    public static boolean shouldRecover(long now, long lastTetherOrWeakened) {

        return now - lastTetherOrWeakened >= RECOVERY_TICKS;
    }
}
