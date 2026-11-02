package io.github.mdw22.paranormalcraft.entity.ghost.goal;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.entity.ghost.Attachment;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A ghost that just got out of a trap or a Containment Unit: it hangs where it is while it shows itself, then drifts
 * away from its player, visibly and at a shy ghost's speed. The stages and their timing are {@link Attachment}'s.
 */
public class GhostEscapeGoal extends Goal {

    private static final int REPLAN_TICKS = Balance.Escape.ESCAPE_REPLAN_TICKS;
    /** Straight away from the player first, then turning aside, if the way is blocked. */
    private static final double[] TURNS = {0.0, 45.0, -45.0, 90.0, -90.0, 135.0, -135.0};

    private final GhostEntity ghost;
    private final double speed;

    public GhostEscapeGoal(GhostEntity ghost, double speed) {

        this.ghost = ghost;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {

        return this.ghost.attachment().gettingAway() && this.ghost.tetherCount() == 0 && target() != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {

        return true;
    }

    @Override
    public void tick() {

        Player player = target();
        if (player == null || this.ghost.attachment().escape() != Attachment.Escape.FLEE) {
            this.ghost.getNavigation().stop();
            return;
        }
        if (this.ghost.attachment().escapeTicks() % REPLAN_TICKS != 1) {
            return;
        }
        Vec3 spot = fleeSpot(player);
        if (spot != null && !this.ghost.getNavigation().moveTo(spot.x, spot.y, spot.z, this.speed)) {
            this.ghost.getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, this.speed);
        }
    }

    @Override
    public void stop() {

        this.ghost.getNavigation().stop();
    }

    private @Nullable Vec3 fleeSpot(Player player) {

        Vec3 away = this.ghost.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
        // Right on top of the player, any way out will do.
        Vec3 direction = away.lengthSqr() < 1.0E-4 ? player.getViewVector(1.0F).multiply(1.0, 0.0, 1.0).normalize() : away.normalize();
        for (double turn : TURNS) {
            Vec3 target = player.position().add(direction.yRot((float) Math.toRadians(turn)).scale(Balance.Escape.ESCAPE_FLEE_TARGET));
            BlockPos column = BlockPos.containing(target.x, this.ghost.getY(), target.z);
            if (!this.ghost.level().isLoaded(column)) {
                continue;
            }
            Vec3 spot = HoverSpots.aboveFloor(this.ghost, column.getX(), column.getY(), column.getZ());
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }

    private @Nullable Player target() {

        UUID id = this.ghost.attachment().player();
        return id == null || !(this.ghost.level() instanceof ServerLevel level) ? null : level.getPlayerByUUID(id);
    }
}
