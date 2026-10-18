package io.github.mdw22.paranormalcraft.entity.ghost.goal;

import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Idle: a slow random drift to spots near home, 1 to 3 blocks off the floor.
 */
public class GhostWanderGoal extends Goal {

    private final GhostEntity ghost;
    private final double speed;
    private final int interval;

    public GhostWanderGoal(GhostEntity ghost, double speed, int interval) {

        this.ghost = ghost;
        this.speed = speed;
        this.interval = interval;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {

        return this.ghost.getNavigation().isDone() && this.ghost.getRandom().nextInt(reducedTickDelay(this.interval)) == 0;
    }

    @Override
    public boolean canContinueToUse() {

        return !this.ghost.getNavigation().isDone();
    }

    @Override
    public void start() {

        Vec3 spot = HoverSpots.nearHome(this.ghost, GhostEntity.WANDER_RADIUS);
        if (spot != null) {
            this.ghost.getNavigation().moveTo(spot.x, spot.y, spot.z, this.speed);
        }
    }
}
