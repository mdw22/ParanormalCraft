package io.github.mdw22.paranormalcraft.haunting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HauntingScheduleTest {

    @Test
    void notDueUntilScheduledAndTheIntervalPasses() {

        HauntingSchedule schedule = new HauntingSchedule();
        assertFalse(schedule.isDue(1_000));

        schedule.scheduleNext(1_000, 600);

        assertFalse(schedule.isDue(1_599));
        assertTrue(schedule.isDue(1_600));
    }

    @Test
    void anActionStartsASixtySecondCooldownAndANewInterval() {

        HauntingSchedule schedule = new HauntingSchedule();
        schedule.recordAction(HauntingAction.KNOCKING, 2_000, 600);

        assertFalse(schedule.offCooldown(HauntingAction.KNOCKING, 2_000 + HauntingSchedule.ACTION_COOLDOWN_TICKS - 1));
        assertTrue(schedule.offCooldown(HauntingAction.KNOCKING, 2_000 + HauntingSchedule.ACTION_COOLDOWN_TICKS));
        assertTrue(schedule.offCooldown(HauntingAction.FOOTSTEPS, 2_000));
        assertEquals(2_600, schedule.nextAttempt());
        assertEquals(1_200, HauntingSchedule.ACTION_COOLDOWN_TICKS);
    }

    @Test
    void cooldownRemainingCountsDown() {

        HauntingSchedule schedule = new HauntingSchedule();
        assertEquals(0, schedule.cooldownRemaining(HauntingAction.PHANTOM_CHEST, 50));

        schedule.recordAction(HauntingAction.PHANTOM_CHEST, 100, 600);

        assertEquals(1_200, schedule.cooldownRemaining(HauntingAction.PHANTOM_CHEST, 100));
        assertEquals(200, schedule.cooldownRemaining(HauntingAction.PHANTOM_CHEST, 1_100));
        assertEquals(0, schedule.cooldownRemaining(HauntingAction.PHANTOM_CHEST, 5_000));
    }
}
