package io.github.mdw22.paranormalcraft.entity.ghost.goal;

import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * While tethered, the ghost's own movement stops: {@link GhostEntity#travel} steers it toward the holders' anchor points
 * instead (see {@link io.github.mdw22.paranormalcraft.capture.Steering}). This goal holds the move and look flags so no
 * other goal fights the tether, stops pathing, and turns the ghost toward where it's being pulled.
 */
public class GhostTetheredGoal extends Goal {

    private final GhostEntity ghost;

    public GhostTetheredGoal(GhostEntity ghost) {

        this.ghost = ghost;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {

        return this.ghost.tetherCount() > 0 && this.ghost.capturingTrap() == null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {

        return true;
    }

    @Override
    public void start() {

        this.ghost.getNavigation().stop();
    }

    @Override
    public void tick() {

        this.ghost.getNavigation().stop();
        if (!this.ghost.tetherAnchors().isEmpty()) {
            Vec3 anchor = this.ghost.tetherAnchors().getFirst();
            this.ghost.getLookControl().setLookAt(anchor.x, anchor.y, anchor.z);
        }
    }
}
