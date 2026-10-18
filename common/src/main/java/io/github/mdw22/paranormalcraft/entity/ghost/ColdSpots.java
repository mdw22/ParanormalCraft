package io.github.mdw22.paranormalcraft.entity.ghost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks a ghost's cold spot: a standable, roofed (no sky above) position inside its home radius, or home itself if
 * there is none.
 */
public final class ColdSpots {

    private static final int VERTICAL_SEARCH = 4;

    private ColdSpots() {
    }

    public static BlockPos choose(ServerLevel level, BlockPos home, int radius, RandomSource random) {

        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(home.offset(-radius, -VERTICAL_SEARCH, -radius), home.offset(radius, VERTICAL_SEARCH, radius))) {
            if (pos.distSqr(home) <= (double) radius * radius && isRoofedStandable(level, pos)) {
                candidates.add(pos.immutable());
            }
        }
        return candidates.isEmpty() ? home : candidates.get(random.nextInt(candidates.size()));
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
