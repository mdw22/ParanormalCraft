package io.github.mdw22.paranormalcraft.tool;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.block.FilledGhostTrapBlockEntity;
import io.github.mdw22.paranormalcraft.capture.FilledTraps;
import io.github.mdw22.paranormalcraft.capture.TrapIntegrity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What an EMF Reader at a position reads. Distance only: EMF passes through walls and doesn't care who can see the
 * ghost.
 */
public final class EmfReadings {

    /** A trapped ghost reads at most this. */
    public static final int TRAPPED_MAX_LEVEL = Balance.Trap.TRAPPED_EMF_MAX_LEVEL;

    private EmfReadings() {
    }

    /**
     * Distance to the nearest ghost within {@link EmfLevels#RANGE}, or {@link Double#POSITIVE_INFINITY}.
     */
    public static double nearestGhostDistance(ServerLevel level, Vec3 pos) {

        double nearest = Double.POSITIVE_INFINITY;
        for (GhostEntity ghost : level.getEntitiesOfClass(GhostEntity.class, new AABB(pos, pos).inflate(EmfLevels.RANGE))) {
            if (ghost.isAlive()) {
                nearest = Math.min(nearest, ghost.position().distanceTo(pos));
            }
        }
        return nearest;
    }

    /**
     * The reading: from the nearest free ghost, or from a Filled Ghost Trap (which reads like a ghost but never above
     * {@link #TRAPPED_MAX_LEVEL}, or 4 once its integrity is below 25%), whichever is higher.
     */
    public static int levelAt(ServerLevel level, Vec3 pos) {

        int ghosts = EmfLevels.levelForDistance(nearestGhostDistance(level, pos));
        int traps = 0;
        for (BlockPos trap : FilledTraps.near(level, pos, EmfLevels.RANGE)) {
            // A decaying trap reads louder: level 3 at most, 4 below 25% integrity.
            int cap = level.getBlockEntity(trap) instanceof FilledGhostTrapBlockEntity filled
                    ? TrapIntegrity.emfCap(filled.integrity(level.getGameTime())) : TRAPPED_MAX_LEVEL;
            traps = Math.max(traps, Math.min(cap, EmfLevels.levelForDistance(Vec3.atCenterOf(trap).distanceTo(pos))));
        }
        return Math.max(ghosts, traps);
    }
}
