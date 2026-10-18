package io.github.mdw22.paranormalcraft.tool;

import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What an EMF Reader at a position reads. Distance only: EMF passes through walls and doesn't care who can see the
 * ghost.
 */
public final class EmfReadings {

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

    public static int levelAt(ServerLevel level, Vec3 pos) {

        return EmfLevels.levelForDistance(nearestGhostDistance(level, pos));
    }
}
