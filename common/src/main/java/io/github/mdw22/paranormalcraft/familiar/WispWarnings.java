package io.github.mdw22.paranormalcraft.familiar;

import io.github.mdw22.paranormalcraft.Balance;
import net.minecraft.world.phys.Vec3;

/**
 * The pure rule behind the Wisp Familiar's warnings: a hostile counts as unseen when it is more than
 * {@link #WARN_ANGLE} degrees from the owner's view or there is no line of sight to it.
 */
public final class WispWarnings {

    public static final double WARN_RANGE = Balance.Familiar.WARN_RANGE;
    public static final double WARN_ANGLE = Balance.Familiar.WARN_ANGLE;

    private WispWarnings() {
    }

    /**
     * The angle in degrees between the owner's view and the direction to the mob (0 straight ahead, 180 behind).
     */
    public static double angle(Vec3 view, Vec3 toMob) {

        double lengths = view.length() * toMob.length();
        if (lengths < 1.0E-9) {
            return 0.0;
        }
        double cos = Math.clamp(view.dot(toMob) / lengths, -1.0, 1.0);
        return Math.toDegrees(Math.acos(cos));
    }

    /**
     * Whether the owner can't see the mob.
     */
    public static boolean unseen(Vec3 view, Vec3 toMob, boolean lineOfSight) {

        return !lineOfSight || angle(view, toMob) > WARN_ANGLE;
    }

    /**
     * Which side of the owner the threat is on, as a horizontal unit vector from the owner toward it.
     */
    public static Vec3 side(Vec3 toMob) {

        Vec3 flat = new Vec3(toMob.x, 0.0, toMob.z);
        return flat.lengthSqr() < 1.0E-9 ? Vec3.ZERO : flat.normalize();
    }
}
