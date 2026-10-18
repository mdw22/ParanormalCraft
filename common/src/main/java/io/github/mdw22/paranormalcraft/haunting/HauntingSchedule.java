package io.github.mdw22.paranormalcraft.haunting;

import java.util.EnumMap;
import java.util.Map;

/**
 * When a ghost may next haunt, and each action's cooldown, in game ticks. Plain logic so it can be unit tested.
 */
public final class HauntingSchedule {

    /** Each action waits at least this long before it can repeat (60 s). */
    public static final long ACTION_COOLDOWN_TICKS = 1200;
    private static final long UNSCHEDULED = Long.MIN_VALUE;

    private long nextAttempt = UNSCHEDULED;
    private final Map<HauntingAction, Long> cooldownUntil = new EnumMap<>(HauntingAction.class);

    public boolean isScheduled() {

        return this.nextAttempt != UNSCHEDULED;
    }

    public long nextAttempt() {

        return this.nextAttempt;
    }

    public void scheduleNext(long now, long intervalTicks) {

        this.nextAttempt = now + intervalTicks;
    }

    public boolean isDue(long now) {

        return isScheduled() && now >= this.nextAttempt;
    }

    public boolean offCooldown(HauntingAction action, long now) {

        return now >= this.cooldownUntil.getOrDefault(action, UNSCHEDULED);
    }

    /**
     * Ticks until the action can be used again (0 if it can be used now).
     */
    public long cooldownRemaining(HauntingAction action, long now) {

        return Math.max(0, this.cooldownUntil.getOrDefault(action, now) - now);
    }

    /**
     * An action happened: start its cooldown and wait a full interval before the next attempt.
     */
    public void recordAction(HauntingAction action, long now, long nextIntervalTicks) {

        this.cooldownUntil.put(action, now + ACTION_COOLDOWN_TICKS);
        scheduleNext(now, nextIntervalTicks);
    }
}
