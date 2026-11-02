package io.github.mdw22.paranormalcraft.entity.ghost;

import io.github.mdw22.paranormalcraft.entity.ghost.goal.GhostFollowGoal;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GhostFollowGoalTest {

    private static final Vec3 PLAYER = new Vec3(10.0, 64.0, 10.0);
    private static final Vec3 SOUTH = new Vec3(0.0, 0.0, 1.0);

    @Test
    void behindIsTheHalfTheyAreNotLookingAt() {

        assertTrue(GhostFollowGoal.behind(PLAYER, SOUTH, PLAYER.add(0.0, 0.0, -5.0)));
        assertFalse(GhostFollowGoal.behind(PLAYER, SOUTH, PLAYER.add(0.0, 0.0, 5.0)));
    }

    /** From straight in front, the path to behind goes round them at the trail distance, never through them. */
    @Test
    void orbitGoesRoundThePlayerAndGetsBehind() {

        Vec3 ghost = PLAYER.add(0.1, 0.0, 14.0);
        for (int move = 0; move < 8; move++) {
            Vec3 next = GhostFollowGoal.orbitStep(PLAYER, ghost, SOUTH, 18.0);
            assertEquals(18.0, next.distanceTo(PLAYER), 1.0E-6);
            ghost = next;
        }
        assertTrue(GhostFollowGoal.behind(PLAYER, SOUTH, ghost));
        assertEquals(0.0, ghost.distanceTo(GhostFollowGoal.trailPoint(PLAYER, SOUTH, 18.0)), 1.0E-6);
    }
}
