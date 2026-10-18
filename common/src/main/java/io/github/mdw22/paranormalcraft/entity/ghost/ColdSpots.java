package io.github.mdw22.paranormalcraft.entity.ghost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks a ghost's cold spot: the standable, roofed (no sky above) position inside its home radius that is nearest
 * home (ties broken at random), or home itself if there is none. Nearest rather than random, so a ghost homed in a
 * cabin's back room keeps its cold spot in that room.
 */
public final class ColdSpots {

    private static final int VERTICAL_SEARCH = 4;

    private ColdSpots() {
    }

    public static BlockPos choose(ServerLevel level, BlockPos home, int radius, RandomSource random) {

        List<BlockPos> nearest = new ArrayList<>();
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(home.offset(-radius, -VERTICAL_SEARCH, -radius), home.offset(radius, VERTICAL_SEARCH, radius))) {
            double distance = pos.distSqr(home);
            if (distance > (double) radius * radius || distance > nearestDistance || !isRoofedStandable(level, pos)) {
                continue;
            }
            if (distance < nearestDistance) {
                nearest.clear();
                nearestDistance = distance;
            }
            nearest.add(pos.immutable());
        }
        return nearest.isEmpty() ? home : nearest.get(random.nextInt(nearest.size()));
    }

    /**
     * Air with air above it, solid ground below, and a roof somewhere overhead.
     */
    public static boolean isRoofedStandable(ServerLevel level, BlockPos pos) {

        return level.isLoaded(pos)
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && isRoofed(level, pos);
    }

    /**
     * Something sky-blocking above. Uses the heightmap rather than sky light, which lags behind block changes.
     */
    public static boolean isRoofed(ServerLevel level, BlockPos pos) {

        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > pos.getY() + 1;
    }
}
