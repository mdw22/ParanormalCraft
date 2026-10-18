package io.github.mdw22.paranormalcraft.entity.ghost.goal;

import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Now and then, when a player is within {@link GhostEntity#PLAYER_RANGE}, the ghost drifts to 4 to 6 blocks from
 * them and hangs there. Only one ghost does this to a given player at a time.
 */
public class GhostCuriousGoal extends Goal {

    private static final double MIN_DISTANCE = 4.0;
    private static final double MAX_DISTANCE = 6.0;
    private static final int MIN_DURATION = 100;
    private static final int MAX_DURATION = 200;
    private static final int REPATH_TICKS = 20;

    // Player UUID -> the ghost currently being curious about them. Server thread only.
    private static final Map<UUID, GhostEntity> CLAIMS = new HashMap<>();

    private final GhostEntity ghost;
    private final double speed;
    private final int chance;
    private @Nullable Player target;
    private int ticksLeft;

    /**
     * @param chance one in this many checks starts the goal while a player is near
     */
    public GhostCuriousGoal(GhostEntity ghost, double speed, int chance) {

        this.ghost = ghost;
        this.speed = speed;
        this.chance = chance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public static void clearClaims() {

        CLAIMS.clear();
    }

    @Override
    public boolean canUse() {

        if (this.ghost.getRandom().nextInt(reducedTickDelay(this.chance)) != 0) {
            return false;
        }
        for (Player player : this.ghost.playersInRange()) {
            if (!isClaimedByOther(player) && hangSpot(player) != null) {
                this.target = player;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {

        Player player = this.target;
        return player != null
                && this.ticksLeft > 0
                && player.isAlive()
                && !player.isSpectator()
                && player.distanceToSqr(this.ghost) < (GhostEntity.PLAYER_RANGE + 2) * (GhostEntity.PLAYER_RANGE + 2)
                && !isClaimedByOther(player);
    }

    @Override
    public void start() {

        this.ticksLeft = MIN_DURATION + this.ghost.getRandom().nextInt(MAX_DURATION - MIN_DURATION + 1);
        if (this.target != null) {
            CLAIMS.put(this.target.getUUID(), this.ghost);
            moveToHangSpot(this.target);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {

        return true;
    }

    @Override
    public void tick() {

        Player player = this.target;
        if (player == null) {
            return;
        }
        this.ticksLeft--;
        this.ghost.getLookControl().setLookAt(player, 10.0F, this.ghost.getMaxHeadXRot());
        double distance = this.ghost.distanceTo(player);
        if (this.ticksLeft % REPATH_TICKS == 0 && (distance < MIN_DISTANCE - 0.5 || distance > MAX_DISTANCE + 0.5)) {
            moveToHangSpot(player);
        }
    }

    @Override
    public void stop() {

        if (this.target != null) {
            CLAIMS.remove(this.target.getUUID(), this.ghost);
        }
        this.target = null;
        this.ghost.getNavigation().stop();
    }

    private void moveToHangSpot(Player player) {

        Vec3 spot = hangSpot(player);
        if (spot == null) {
            this.ticksLeft = 0;
            return;
        }
        this.ghost.getNavigation().moveTo(spot.x, spot.y, spot.z, this.speed);
    }

    /**
     * A spot 4 to 6 blocks from the player on the ghost's side, at about the player's head height, inside the leash.
     */
    private @Nullable Vec3 hangSpot(Player player) {

        Vec3 fromPlayer = this.ghost.position().subtract(player.position()).multiply(1, 0, 1);
        if (fromPlayer.lengthSqr() < 1.0E-4) {
            fromPlayer = new Vec3(1, 0, 0);
        }
        double distance = MIN_DISTANCE + this.ghost.getRandom().nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
        Vec3 spot = player.position().add(fromPlayer.normalize().scale(distance)).add(0, 0.5 + this.ghost.getRandom().nextDouble(), 0);
        if (!HoverSpots.withinLeash(this.ghost, spot, GhostEntity.LEASH_RADIUS)) {
            return null;
        }
        return this.ghost.level().noCollision(this.ghost, this.ghost.getDimensions(this.ghost.getPose()).makeBoundingBox(spot)) ? spot : null;
    }

    private boolean isClaimedByOther(Player player) {

        GhostEntity claimant = CLAIMS.get(player.getUUID());
        if (claimant == null || claimant == this.ghost) {
            return false;
        }
        if (claimant.isRemoved() || !claimant.isAlive()) {
            CLAIMS.remove(player.getUUID());
            return false;
        }
        return true;
    }
}
