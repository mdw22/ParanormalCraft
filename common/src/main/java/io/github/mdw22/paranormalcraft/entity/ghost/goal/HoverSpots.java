package io.github.mdw22.paranormalcraft.entity.ghost.goal;

import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Picks positions for a ghost to float to: inside its leash, in open air, about 1 to 3 blocks above the floor.
 */
final class HoverSpots {

    private static final int FLOOR_SEARCH_DEPTH = 10;

    private HoverSpots() {
    }

    /**
     * A random spot within {@code radius} of home, or null if none was found in a few tries.
     */
    static @Nullable Vec3 nearHome(GhostEntity ghost, int radius) {

        return near(ghost, ghost.homeOrHere(), radius);
    }

    /**
     * A random spot within {@code radius} of {@code centre} that is also inside the ghost's wander radius, or null.
     */
    static @Nullable Vec3 near(GhostEntity ghost, BlockPos centre, int radius) {

        RandomSource random = ghost.getRandom();
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            int dy = random.nextInt(7) - 3;
            Vec3 spot = aboveFloor(ghost, centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
            if (spot != null && spot.distanceToSqr(Vec3.atCenterOf(centre)) <= (radius + 1.0) * (radius + 1.0)
                    && withinLeash(ghost, spot, GhostEntity.WANDER_RADIUS)) {
                return spot;
            }
        }
        return null;
    }

    /**
     * Snaps a target to 1 to 3 blocks above the floor below it, or null if there's no floor close enough or no room.
     */
    static @Nullable Vec3 aboveFloor(GhostEntity ghost, int x, int startY, int z) {

        Level level = ghost.level();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, startY, z);
        for (int i = 0; i < FLOOR_SEARCH_DEPTH && pos.getY() > level.getMinY(); i++) {
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty() && !GhostMovement.isPassable(state)) {
                if (i == 0) {
                    // Started inside something solid; look a little higher instead.
                    pos.move(0, 2, 0);
                    if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                        return null;
                    }
                    continue;
                }
                double floorTop = pos.getY() + 1.0;
                double y = floorTop + 1 + ghost.getRandom().nextInt(3);
                Vec3 spot = new Vec3(x + 0.5, y, z + 0.5);
                return hasRoom(ghost, spot) ? spot : null;
            }
            pos.move(0, -1, 0);
        }
        return null;
    }

    static boolean withinLeash(GhostEntity ghost, Vec3 spot, double radius) {

        return spot.distanceToSqr(Vec3.atCenterOf(ghost.homeOrHere())) <= radius * radius;
    }

    private static boolean hasRoom(GhostEntity ghost, Vec3 spot) {

        return ghost.level().noCollision(ghost, ghost.getDimensions(ghost.getPose()).makeBoundingBox(spot));
    }
}
