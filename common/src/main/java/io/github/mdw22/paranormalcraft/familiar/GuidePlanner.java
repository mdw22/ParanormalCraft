package io.github.mdw22.paranormalcraft.familiar;

import io.github.mdw22.paranormalcraft.Balance;
import net.minecraft.world.phys.Vec3;

/**
 * The Wisp Familiar's guide route, planned leg by leg from the heightmap, as plain logic so it is unit tested. Each
 * waypoint is {@link #STEP} blocks further toward the cabin (or the cabin itself when closer), at {@link #CLEARANCE}
 * blocks above the highest terrain or canopy anywhere along the leg (sampled every block, a block either side). The
 * wisp climbs to a waypoint's height before flying the leg, so it always passes over hills and trees rather than into
 * them.
 */
public final class GuidePlanner {

    public static final double STEP = Balance.Familiar.GUIDE_AHEAD;
    public static final double CLEARANCE = Balance.Familiar.GUIDE_CLEARANCE;

    /** The top of the terrain or canopy at a column (a heightmap that counts leaves). */
    @FunctionalInterface
    public interface Heights {

        int top(int x, int z);
    }

    private GuidePlanner() {
    }

    /**
     * The next waypoint after {@code from}, toward {@code cabin}.
     */
    public static Vec3 next(Vec3 from, Vec3 cabin, Heights heights) {

        Vec3 flat = new Vec3(cabin.x - from.x, 0.0, cabin.z - from.z);
        double distance = flat.length();
        Vec3 end = distance <= STEP ? new Vec3(cabin.x, 0.0, cabin.z) : from.add(flat.scale(STEP / distance));
        int top = highestAlong(from, end, heights);
        return new Vec3(end.x, top + CLEARANCE, end.z);
    }

    /**
     * The highest top along the horizontal line from {@code a} to {@code b}, a block either side.
     */
    public static int highestAlong(Vec3 a, Vec3 b, Heights heights) {

        double length = Math.hypot(b.x - a.x, b.z - a.z);
        int samples = Math.max(1, (int) Math.ceil(length));
        double sideX = length < 1.0E-6 ? 0.0 : -(b.z - a.z) / length;
        double sideZ = length < 1.0E-6 ? 0.0 : (b.x - a.x) / length;
        int highest = Integer.MIN_VALUE;
        for (int i = 0; i <= samples; i++) {
            double x = a.x + (b.x - a.x) * i / samples;
            double z = a.z + (b.z - a.z) * i / samples;
            for (int side = -1; side <= 1; side++) {
                highest = Math.max(highest, heights.top((int) Math.floor(x + sideX * side), (int) Math.floor(z + sideZ * side)));
            }
        }
        return highest;
    }

    /** Horizontal distance between two points. */
    public static double flatDistance(Vec3 a, Vec3 b) {

        return Math.hypot(a.x - b.x, a.z - b.z);
    }

    /**
     * Horizontal distance from {@code point} to the segment {@code a}–{@code b}: how far the owner has wandered off
     * the route.
     */
    public static double offRoute(Vec3 point, Vec3 a, Vec3 b) {

        double dx = b.x - a.x;
        double dz = b.z - a.z;
        double lengthSqr = dx * dx + dz * dz;
        double t = lengthSqr < 1.0E-9 ? 0.0 : Math.clamp(((point.x - a.x) * dx + (point.z - a.z) * dz) / lengthSqr, 0.0, 1.0);
        return Math.hypot(point.x - (a.x + dx * t), point.z - (a.z + dz * t));
    }

    /**
     * Where the wisp should head to reach {@code waypoint} from {@code at}: straight up first while it's below the
     * waypoint's height, then along the leg.
     */
    public static Vec3 heading(Vec3 at, Vec3 waypoint) {

        return at.y < waypoint.y - 0.5 ? new Vec3(at.x, waypoint.y, at.z) : waypoint;
    }
}
