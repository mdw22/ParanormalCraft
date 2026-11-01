package io.github.mdw22.paranormalcraft.capture;

import io.github.mdw22.paranormalcraft.Balance;

import java.util.function.LongSupplier;

/**
 * Filled Trap integrity (Milestone 2 part D), as plain logic so it is unit tested: 100% when sealed, falling evenly
 * to 0% over the seal's lifetime ({@code trapIntegrityMinutes} of real time, 1 in-game day by default), worked out
 * from game time, so time passes for a trap in a chest or an unloaded chunk too. At 0 the ghost escapes. Loading the
 * trap into a Containment Unit is the fix: storage doesn't decay.
 * <p>
 * Rattles get rarer and quieter while integrity is high, and urgent below {@link #URGENT_BELOW}%.
 */
public final class TrapIntegrity {

    public static final int URGENT_BELOW = Balance.Escape.URGENT_BELOW;
    public static final int CARRIED_RATTLE_BELOW = Balance.Escape.CARRIED_RATTLE_BELOW;
    public static final int CREAK_BELOW = Balance.Escape.CREAK_BELOW;
    public static final int TICKS_PER_MINUTE = 1200;

    /** Client side: the current game time, for the item bar and tooltip (set by client init). */
    private static LongSupplier clientClock = () -> 0L;

    /**
     * When a trap may rattle and how loudly; {@code allowed} false means it stays quiet.
     */
    public record Rattle(boolean allowed, int minTicks, int maxTicks, float volume) {

        static final Rattle NONE = new Rattle(false, 0, 0, 0.0F);

        /** The chance per tick of a rattle, for things nothing keeps a timer for (carried traps). */
        public double chancePerTick() {

            return this.allowed ? 2.0 / (this.minTicks + this.maxTicks) : 0.0;
        }
    }

    private TrapIntegrity() {
    }

    public static TrapSeal fresh(long now, int lifetimeMinutes) {

        return new TrapSeal(now, 100.0F, Math.max(1, lifetimeMinutes) * TICKS_PER_MINUTE);
    }

    /** A seal at {@code integrity}% from now on, with the same lifetime per 100%. */
    public static TrapSeal at(long now, float integrity, int lifetimeTicks) {

        return new TrapSeal(now, Math.clamp(integrity, 0.0F, 100.0F), lifetimeTicks);
    }

    /**
     * Integrity at {@code now}, 0 to 100.
     */
    public static float integrity(TrapSeal seal, long now) {

        long elapsed = Math.max(0, now - seal.sealedAt());
        double lost = elapsed * 100.0 / Math.max(1, seal.lifetimeTicks());
        return (float) Math.clamp(seal.startIntegrity() - lost, 0.0, 100.0);
    }

    public static boolean broken(TrapSeal seal, long now) {

        return integrity(seal, now) <= 0.0F;
    }

    /** Whole percent for display ("Integrity 64%"): rounded up, so it shows 0% only once broken. */
    public static int percent(float integrity) {

        return (int) Math.ceil(integrity);
    }

    /**
     * The rattle schedule. A placed trap: soft and rare (every {@code QUIET_RATTLE} ticks) at 50% and above, tightening
     * evenly to urgent at {@link #URGENT_BELOW}%. A carried trap (an inventory or a container): silent at
     * {@link #CARRIED_RATTLE_BELOW}% and above, then the same as placed. Below {@link #URGENT_BELOW}%: loud, every 10–20 s,
     * anywhere.
     */
    public static Rattle rattle(float integrity, boolean placed) {

        if (integrity < URGENT_BELOW) {
            return new Rattle(true, Balance.Escape.URGENT_RATTLE_MIN_TICKS, Balance.Escape.URGENT_RATTLE_MAX_TICKS, Balance.Escape.URGENT_VOLUME);
        }
        if (integrity >= CARRIED_RATTLE_BELOW) {
            return placed ? new Rattle(true, Balance.Escape.QUIET_RATTLE_MIN_TICKS, Balance.Escape.QUIET_RATTLE_MAX_TICKS, Balance.Escape.QUIET_VOLUME)
                    : Rattle.NONE;
        }
        // 25–50%: from quiet (at 50) to urgent (at 25).
        double t = (CARRIED_RATTLE_BELOW - integrity) / (double) (CARRIED_RATTLE_BELOW - URGENT_BELOW);
        int min = (int) Math.round(lerp(Balance.Escape.QUIET_RATTLE_MIN_TICKS, Balance.Escape.URGENT_RATTLE_MIN_TICKS, t));
        int max = (int) Math.round(lerp(Balance.Escape.QUIET_RATTLE_MAX_TICKS, Balance.Escape.URGENT_RATTLE_MAX_TICKS, t));
        float volume = (float) lerp(Balance.Escape.QUIET_VOLUME, Balance.Escape.URGENT_VOLUME, t);
        return new Rattle(true, min, max, volume);
    }

    /** The highest EMF level a trap at {@code integrity} reads as. */
    public static int emfCap(float integrity) {

        return integrity < URGENT_BELOW ? Balance.Escape.URGENT_EMF_LEVEL : Balance.Trap.TRAPPED_EMF_MAX_LEVEL;
    }

    public static boolean creaks(float integrity) {

        return integrity < CREAK_BELOW;
    }

    /** Item bar colour: green at 100% through yellow to red at 0%. */
    public static int barColour(float integrity) {

        float t = Math.clamp(integrity / 100.0F, 0.0F, 1.0F);
        int red = t < 0.5F ? 255 : Math.round((1.0F - t) * 2.0F * 255.0F);
        int green = t > 0.5F ? 255 : Math.round(t * 2.0F * 255.0F);
        return red << 16 | green << 8;
    }

    private static double lerp(double from, double to, double t) {

        return from + (to - from) * Math.clamp(t, 0.0, 1.0);
    }

    public static void setClientClock(LongSupplier clock) {

        clientClock = clock;
    }

    public static long clientNow() {

        return clientClock.getAsLong();
    }
}
