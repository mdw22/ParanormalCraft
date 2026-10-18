package io.github.mdw22.paranormalcraft.entity.ghost.goal;

import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostVisibility;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * If a player who can see the ghost stares straight at it for more than {@link #STARE_TICKS}, it backs away from
 * that player toward home.
 */
public class GhostShyGoal extends Goal {

    public static final int STARE_TICKS = 40;
    private static final double STARE_RANGE = 24.0;
    private static final int FLEE_TICKS = 60;
    private static final double FLEE_DISTANCE = 5.0;

    private final GhostEntity ghost;
    private final double speed;
    private @Nullable Player starer;
    private long stareStart;
    private int fleeTicksLeft;

    public GhostShyGoal(GhostEntity ghost, double speed) {

        this.ghost = ghost;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {

        long now = this.ghost.level().getGameTime();
        if (this.starer == null || !isStaring(this.starer)) {
            this.starer = findStarer();
            this.stareStart = now;
            return false;
        }
        return now - this.stareStart > STARE_TICKS;
    }

    @Override
    public boolean canContinueToUse() {

        return this.fleeTicksLeft > 0;
    }

    @Override
    public void start() {

        this.fleeTicksLeft = FLEE_TICKS;
        Player player = this.starer;
        if (player == null) {
            return;
        }
        Vec3 away = this.ghost.position().subtract(player.position()).multiply(1, 0, 1).normalize();
        Vec3 toHome = Vec3.atCenterOf(this.ghost.homeOrHere()).subtract(this.ghost.position()).multiply(1, 0, 1).normalize();
        // Lean toward home at half weight, so the ghost always ends up further from the player (the direction stays
        // within ~30° of straight away), even when home lies between them.
        Vec3 direction = away.add(toHome.scale(0.5)).normalize();
        if (direction.lengthSqr() < 1.0E-4) {
            direction = away;
        }
        Vec3 target = this.ghost.position().add(direction.scale(FLEE_DISTANCE));
        if (!HoverSpots.withinLeash(this.ghost, target, GhostEntity.LEASH_RADIUS)) {
            target = Vec3.atCenterOf(this.ghost.homeOrHere());
        }
        Vec3 spot = HoverSpots.aboveFloor(this.ghost, (int) Math.floor(target.x), (int) Math.floor(this.ghost.getY()), (int) Math.floor(target.z));
        Vec3 goal = spot != null ? spot : target;
        if (!this.ghost.getNavigation().moveTo(goal.x, goal.y, goal.z, this.speed)) {
            this.ghost.getMoveControl().setWantedPosition(goal.x, goal.y, goal.z, this.speed);
        }
    }

    @Override
    public void tick() {

        this.fleeTicksLeft--;
    }

    @Override
    public void stop() {

        this.starer = null;
        this.fleeTicksLeft = 0;
    }

    private @Nullable Player findStarer() {

        for (Player player : this.ghost.level().players()) {
            if (isStaring(player)) {
                return player;
            }
        }
        return null;
    }

    private boolean isStaring(Player player) {

        return player.isAlive()
                && !player.isSpectator()
                && player.distanceToSqr(this.ghost) < STARE_RANGE * STARE_RANGE
                && GhostVisibility.canSee(player, this.ghost)
                && this.ghost.isLookedAtBy(player);
    }
}
