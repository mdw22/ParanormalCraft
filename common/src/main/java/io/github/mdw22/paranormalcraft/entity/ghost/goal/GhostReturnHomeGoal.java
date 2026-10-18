package io.github.mdw22.paranormalcraft.entity.ghost.goal;

import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Beyond the leash radius (for example after being knocked back) the ghost drifts home until it is well inside.
 */
public class GhostReturnHomeGoal extends Goal {

    private static final double ARRIVED_DISTANCE = GhostEntity.WANDER_RADIUS / 2.0;

    private final GhostEntity ghost;
    private final double speed;

    public GhostReturnHomeGoal(GhostEntity ghost, double speed) {

        this.ghost = ghost;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {

        return this.ghost.getHome() != null && this.ghost.distanceToHomeSqr() > GhostEntity.LEASH_RADIUS * GhostEntity.LEASH_RADIUS;
    }

    @Override
    public boolean canContinueToUse() {

        return this.ghost.distanceToHomeSqr() > ARRIVED_DISTANCE * ARRIVED_DISTANCE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {

        return true;
    }

    @Override
    public void start() {

        moveHome();
    }

    @Override
    public void tick() {

        if (this.ghost.getNavigation().isDone()) {
            moveHome();
        }
    }

    @Override
    public void stop() {

        this.ghost.getNavigation().stop();
    }

    private void moveHome() {

        Vec3 home = Vec3.atBottomCenterOf(this.ghost.homeOrHere()).add(0, 1, 0);
        if (!this.ghost.getNavigation().moveTo(home.x, home.y, home.z, this.speed)) {
            // No path (e.g. sealed in): float straight there, still colliding with solid blocks.
            this.ghost.getMoveControl().setWantedPosition(home.x, home.y, home.z, this.speed);
        }
    }
}
