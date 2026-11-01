package io.github.mdw22.paranormalcraft.entity.ghost.goal;

import io.github.mdw22.paranormalcraft.entity.ghost.Attachment;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/**
 * An attached ghost following its player home: it drifts to a point behind them, between {@link Attachment#FOLLOW_MIN}
 * and {@link Attachment#FOLLOW_MAX} away, and holds back while it's already in that band. Teleports and settling are
 * {@link Attachment}'s.
 */
public class GhostFollowGoal extends Goal {

    private static final double TRAIL = (Attachment.FOLLOW_MIN + Attachment.FOLLOW_MAX) / 2.0;

    private final GhostEntity ghost;
    private final double speed;

    public GhostFollowGoal(GhostEntity ghost, double speed) {

        this.ghost = ghost;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {

        return this.ghost.attachment().following() && this.ghost.tetherCount() == 0 && target() != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {

        return true;
    }

    @Override
    public void tick() {

        Player player = target();
        if (player == null || this.ghost.tickCount % 10 != 0) {
            return;
        }
        double distance = this.ghost.distanceTo(player);
        if (distance >= Attachment.FOLLOW_MIN && distance <= Attachment.FOLLOW_MAX) {
            this.ghost.getNavigation().stop();
            return;
        }
        Vec3 behind = trailPoint(player.position(), player.getViewVector(1.0F), TRAIL);
        this.ghost.getNavigation().moveTo(behind.x, behind.y + 1.0, behind.z, this.speed);
    }

    @Override
    public void stop() {

        this.ghost.getNavigation().stop();
    }

    private Player target() {

        UUID id = this.ghost.attachment().player();
        return id == null || !(this.ghost.level() instanceof ServerLevel level) ? null : level.getPlayerByUUID(id);
    }

    /**
     * The point {@code distance} behind a player looking along {@code view}, level with them.
     */
    public static Vec3 trailPoint(Vec3 player, Vec3 view, double distance) {

        Vec3 flat = new Vec3(view.x, 0.0, view.z);
        Vec3 back = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, -1.0) : flat.normalize().scale(-1.0);
        return player.add(back.scale(distance));
    }
}
