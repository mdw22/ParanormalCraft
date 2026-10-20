package io.github.mdw22.paranormalcraft.capture;

import io.github.mdw22.paranormalcraft.Balance;

import java.util.List;

/**
 * Tether steering as plain maths, so it can be unit tested. Each latched tether gives the ghost an anchor point in
 * front of its holder ({@code eye + look × holdDistance}); the ghost is pulled toward every anchor like a spring, the
 * pulls are summed (so two holders pin it between their anchors) and the result is capped at
 * {@link #MAX_STEER_SPEED}. The ghost fights back: a stronger ghost follows more slowly and jerks further off the line.
 */
public final class Steering {

    public static final double FOLLOW_RATE = Balance.Tether.FOLLOW_RATE;
    public static final double MAX_STEER_SPEED = Balance.Tether.MAX_STEER_SPEED;
    public static final double FIGHT_SLOWDOWN = Balance.Tether.FIGHT_SLOWDOWN;
    public static final double FIGHT_OFFSET_MAX = Balance.Tether.FIGHT_OFFSET_MAX;
    public static final double WEAKENED_FIGHT = Balance.Tether.WEAKENED_FIGHT;
    public static final double COOP_FIGHT_FACTOR = Balance.Tether.COOP_FIGHT_FACTOR;

    /** A 3D vector without game types. */
    public record Vec(double x, double y, double z) {

        public static final Vec ZERO = new Vec(0, 0, 0);

        public Vec add(Vec other) {

            return new Vec(this.x + other.x, this.y + other.y, this.z + other.z);
        }

        public Vec subtract(Vec other) {

            return new Vec(this.x - other.x, this.y - other.y, this.z - other.z);
        }

        public Vec scale(double factor) {

            return new Vec(this.x * factor, this.y * factor, this.z * factor);
        }

        public double length() {

            return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        }
    }

    private Steering() {
    }

    /**
     * How hard the ghost fights the tether, 0 to 1: its Presence fraction while not Weakened (at least
     * {@link #WEAKENED_FIGHT}), {@link #WEAKENED_FIGHT} once Weakened, and halved again with two or more tethers.
     */
    public static double fightStrength(float presence, float maxPresence, boolean weakened, int tethers) {

        double strength = weakened ? WEAKENED_FIGHT : Math.max(WEAKENED_FIGHT, Math.min(1.0, presence / maxPresence));
        return tethers >= 2 ? strength * COOP_FIGHT_FACTOR : strength;
    }

    /**
     * The fraction of the gap to an anchor the ghost closes per tick at this fight strength.
     */
    public static double followRate(double fightStrength) {

        return FOLLOW_RATE * (1.0 - FIGHT_SLOWDOWN * fightStrength);
    }

    /**
     * This tick's velocity: a spring toward each anchor (each shifted by the ghost's current jerk {@code offset}), summed
     * and capped at {@link #MAX_STEER_SPEED}.
     *
     * @param anchors       where each holder wants the ghost's centre
     * @param centre        the ghost's centre now
     * @param fightStrength from {@link #fightStrength}
     * @param offset        the ghost's current jerk away from the line (see {@link #jerk})
     */
    public static Vec velocity(List<Vec> anchors, Vec centre, double fightStrength, Vec offset) {

        double rate = followRate(fightStrength);
        Vec total = Vec.ZERO;
        for (Vec anchor : anchors) {
            total = total.add(anchor.add(offset).subtract(centre).scale(rate));
        }
        double speed = total.length();
        return speed > MAX_STEER_SPEED ? total.scale(MAX_STEER_SPEED / speed) : total;
    }

    /**
     * A new jerk offset: a random direction (mostly sideways and up or down) of length up to
     * {@code FIGHT_OFFSET_MAX × fightStrength}.
     *
     * @param unitX  a random value from -1 to 1
     * @param unitY  a random value from -1 to 1
     * @param unitZ  a random value from -1 to 1
     * @param amount a random value from 0 to 1
     */
    public static Vec jerk(double fightStrength, double unitX, double unitY, double unitZ, double amount) {

        Vec direction = new Vec(unitX, unitY * 0.5, unitZ);
        double length = direction.length();
        if (length < 1.0E-6) {
            return Vec.ZERO;
        }
        return direction.scale(FIGHT_OFFSET_MAX * fightStrength * amount / length);
    }
}
