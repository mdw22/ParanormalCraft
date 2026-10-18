package io.github.mdw22.paranormalcraft.entity.ghost;

import io.github.mdw22.paranormalcraft.registry.ModTags;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lets ghosts drift through doors and thin walls (the {@code paranormalcraft:ghost_passable} block tag) while still
 * colliding with everything else. {@code noPhysics} stays off; instead pathfinding treats passable blocks as open air
 * and {@link GhostEntity#move} skips collision when only passable blocks are in the way.
 */
public final class GhostMovement {

    private GhostMovement() {
    }

    public static boolean isPassable(BlockState state) {

        return state.is(ModTags.GHOST_PASSABLE);
    }

    /**
     * True when {@code box} touches at least one passable block and no other solid block, i.e. the ghost may move
     * there without collision.
     */
    public static boolean onlyPassableBlocksIn(Level level, AABB box) {

        boolean touchesPassable = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    if (!level.isLoaded(pos)) {
                        return false;
                    }
                    BlockState state = level.getBlockState(pos);
                    VoxelShape shape = state.getCollisionShape(level, pos);
                    if (shape.isEmpty() || !shape.move(pos).bounds().intersects(box)) {
                        continue;
                    }
                    if (!isPassable(state)) {
                        return false;
                    }
                    touchesPassable = true;
                }
            }
        }
        return touchesPassable;
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
