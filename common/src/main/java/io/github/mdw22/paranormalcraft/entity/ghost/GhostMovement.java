package io.github.mdw22.paranormalcraft.entity.ghost;

import io.github.mdw22.paranormalcraft.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Lets ghosts drift through doors and thin walls (the {@code paranormalcraft:ghost_passable} block tag) while still
 * colliding with everything else. {@code noPhysics} stays off; instead pathfinding treats passable blocks as open air
 * and {@link GhostEntity#move} ignores passable blocks when resolving collisions (see {@link #moveThroughPassable}).
 */
public final class GhostMovement {

    private GhostMovement() {
    }

    public static boolean isPassable(BlockState state) {

        return state.is(ModTags.GHOST_PASSABLE);
    }

    /**
     * Moves {@code box} by {@code delta}, colliding only with non-passable blocks, one axis at a time (Y, X, Z) so the
     * ghost can slide along a lintel or frame while drifting through a door. Returns null when the move doesn't touch
     * any passable block, so vanilla collision applies instead.
     */
    public static @Nullable Vec3 moveThroughPassable(Level level, AABB box, Vec3 delta) {

        if (!scan(level, box.expandTowards(delta)).touchesPassable()) {
            return null;
        }
        AABB moved = box;
        double[] allowed = new double[3];
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.Y, Direction.Axis.X, Direction.Axis.Z}) {
            double wanted = delta.get(axis);
            // Try the full step, then shorter ones, so the ghost stops close to a solid face instead of short of it.
            for (double fraction = 1.0; fraction > 0.1 && wanted != 0.0; fraction /= 2.0) {
                Vec3 step = switch (axis) {
                    case X -> new Vec3(wanted * fraction, 0, 0);
                    case Y -> new Vec3(0, wanted * fraction, 0);
                    case Z -> new Vec3(0, 0, wanted * fraction);
                };
                AABB candidate = moved.move(step);
                if (!scan(level, candidate).hitsSolid()) {
                    moved = candidate;
                    allowed[axis.ordinal()] = wanted * fraction;
                    break;
                }
            }
        }
        return new Vec3(allowed[0], allowed[1], allowed[2]);
    }

    /** What a box overlaps: passable blocks, and solid blocks that aren't passable (or unloaded chunks). */
    private record Contact(boolean touchesPassable, boolean hitsSolid) {
    }

    private static Contact scan(Level level, AABB box) {

        boolean touchesPassable = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);
        boolean hitsSolid = false;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    if (!level.isLoaded(pos)) {
                        return new Contact(touchesPassable, true);
                    }
                    BlockState state = level.getBlockState(pos);
                    VoxelShape shape = state.getCollisionShape(level, pos);
                    if (shape.isEmpty() || !intersects(shape, pos, box)) {
                        continue;
                    }
                    if (isPassable(state)) {
                        touchesPassable = true;
                    } else {
                        hitsSolid = true;
                    }
                }
            }
        }
        return new Contact(touchesPassable, hitsSolid);
    }

    private static boolean intersects(VoxelShape shape, BlockPos pos, AABB box) {

        for (AABB part : shape.toAabbs()) {
            if (part.move(pos).intersects(box)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Flying navigation whose paths may go through passable blocks.
     */
    public static FlyingPathNavigation createNavigation(Mob mob, Level level) {

        FlyingPathNavigation navigation = new FlyingPathNavigation(mob, level) {

            @Override
            protected PathFinder createPathFinder(int maxVisitedNodes) {

                this.nodeEvaluator = new PassableFlyNodeEvaluator();
                return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
            }
        };
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    private static final class PassableFlyNodeEvaluator extends FlyNodeEvaluator {

        @Override
        public PathType getPathType(PathfindingContext context, int x, int y, int z) {

            if (isPassable(context.getBlockState(new BlockPos(x, y, z)))) {
                return PathType.OPEN;
            }
            return super.getPathType(context, x, y, z);
        }
    }
}
