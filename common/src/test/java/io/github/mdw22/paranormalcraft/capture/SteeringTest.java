package io.github.mdw22.paranormalcraft.capture;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SteeringTest {

    private static final Steering.Vec ORIGIN = new Steering.Vec(0, 0, 0);

    @Test
    void fightStrengthFollowsPresenceAndWeakenedAndCoop() {

        assertEquals(1.0, Steering.fightStrength(20, 20, false, 1), 1e-9);
        assertEquals(0.5, Steering.fightStrength(10, 20, false, 1), 1e-9);
        assertEquals(0.25, Steering.fightStrength(2, 20, false, 1), 1e-9, "never below the Weakened strength");
        assertEquals(0.25, Steering.fightStrength(5, 20, true, 1), 1e-9);
        assertEquals(0.5, Steering.fightStrength(20, 20, false, 2), 1e-9, "two tethers halve it");
        assertEquals(0.125, Steering.fightStrength(5, 20, true, 3), 1e-9);
    }

    @Test
    void strongerGhostsFollowMoreSlowly() {

        assertTrue(Steering.followRate(1.0) < Steering.followRate(0.25));
        assertEquals(Steering.FOLLOW_RATE * 0.5, Steering.followRate(1.0), 1e-9);
    }

    @Test
    void velocityIsASpringCappedAtTopSpeed() {

        Steering.Vec near = Steering.velocity(List.of(new Steering.Vec(1, 0, 0)), ORIGIN, 0.0, Steering.Vec.ZERO);
        assertEquals(Steering.FOLLOW_RATE, near.x(), 1e-9);
        Steering.Vec far = Steering.velocity(List.of(new Steering.Vec(100, 0, 0)), ORIGIN, 0.0, Steering.Vec.ZERO);
        assertEquals(Steering.MAX_STEER_SPEED, far.length(), 1e-9);
    }

    @Test
    void twoAnchorsSettleTheGhostBetweenThem() {

        List<Steering.Vec> anchors = List.of(new Steering.Vec(-2, 0, 0), new Steering.Vec(2, 0, 4));
        Steering.Vec ghost = new Steering.Vec(5, 3, -5);
        for (int tick = 0; tick < 200; tick++) {
            ghost = ghost.add(Steering.velocity(anchors, ghost, 0.5, Steering.Vec.ZERO));
        }
        assertEquals(0.0, ghost.x(), 1e-3);
        assertEquals(0.0, ghost.y(), 1e-3);
        assertEquals(2.0, ghost.z(), 1e-3);
    }

    @Test
    void aNinetyDegreeSweepAtSixBlocksTakesAboutHalfASecondWhenWeakened() {

        Steering.Vec anchor = new Steering.Vec(6, 0, 0);
        Steering.Vec ghost = new Steering.Vec(0, 0, 6);
        int ticks = 0;
        while (new Steering.Vec(anchor.x() - ghost.x(), anchor.y() - ghost.y(), anchor.z() - ghost.z()).length() > 1.5 && ticks < 100) {
            ghost = ghost.add(Steering.velocity(List.of(anchor), ghost, Steering.WEAKENED_FIGHT, Steering.Vec.ZERO));
            ticks++;
        }
        assertTrue(ticks >= 6 && ticks <= 14, "took " + ticks + " ticks");
    }

    @Test
    void jerksAreBoundedByFightStrength() {

        Steering.Vec full = Steering.jerk(1.0, 1, 0, 0, 1.0);
        assertEquals(Steering.FIGHT_OFFSET_MAX, full.length(), 1e-9);
        Steering.Vec weak = Steering.jerk(0.25, 0, 0, 1, 1.0);
        assertEquals(Steering.FIGHT_OFFSET_MAX * 0.25, weak.length(), 1e-9);
        assertEquals(0.0, Steering.jerk(1.0, 0, 0, 0, 1.0).length(), 1e-9);
    }
}
