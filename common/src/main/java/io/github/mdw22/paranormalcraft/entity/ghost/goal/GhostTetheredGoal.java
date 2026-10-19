package io.github.mdw22.paranormalcraft.entity.ghost.goal;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * While tethered the ghost pulls back: it keeps trying to move away from whoever holds it. With one tether it also
 * jerks about at random; with two or more it can't (and {@link GhostEntity#speedFactor()} halves its speed).
 */
public class GhostTetheredGoal extends Goal {

    private static final double FLEE_DISTANCE = Balance.Ghost.TETHERED_FLEE_DISTANCE;
    private static final int REPATH_TICKS = 10;
    private static final int JERK_CHANCE = Balance.Ghost.TETHERED_JERK_CHANCE;
    private static final double JERK_STRENGTH = Balance.Ghost.TETHERED_JERK_STRENGTH;

    private final GhostEntity ghost;
    private final double speed;
    private int repath;

    public GhostTetheredGoal(GhostEntity ghost, double speed) {

        this.ghost = ghost;
        this.speed = speed;
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

        this.repath = 0;
    }

    @Override
    public void tick() {

        List<Vec3> sources = this.ghost.tetherSources();
        if (sources.isEmpty()) {
            return;
        }
        Vec3 holders = Vec3.ZERO;
        for (Vec3 source : sources) {
            holders = holders.add(source);
        }
        holders = holders.scale(1.0 / sources.size());
        Vec3 away = this.ghost.position().subtract(holders).multiply(1, 0, 1);
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
        if (--this.repath <= 0) {
            this.repath = REPATH_TICKS;
            Vec3 target = this.ghost.position().add(away.scale(FLEE_DISTANCE));
            this.ghost.getMoveControl().setWantedPosition(target.x, this.ghost.getY() + 0.2, target.z, this.speed);
        }
        if (sources.size() == 1 && this.ghost.getRandom().nextInt(JERK_CHANCE) == 0) {
            Vec3 sideways = new Vec3(-away.z, 0, away.x).scale((this.ghost.getRandom().nextDouble() - 0.5) * 2.0);
            Vec3 jerk = away.add(sideways).normalize().scale(JERK_STRENGTH * this.ghost.speedFactor());
            this.ghost.setDeltaMovement(this.ghost.getDeltaMovement().add(jerk.x, (this.ghost.getRandom().nextDouble() - 0.4) * 0.15, jerk.z));
        }
        Vec3 first = sources.getFirst();
        this.ghost.getLookControl().setLookAt(first.x, first.y + 1.6, first.z);
    }
}
