package io.github.mdw22.paranormalcraft.containment;

import io.github.mdw22.paranormalcraft.Balance;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Containment breaches as plain logic, so they are unit tested: when a Breach starts, when ghosts escape, and in
 * which order.
 */
public final class BreachRules {

    public static final int BREACH_AFTER_TICKS = Balance.Escape.BREACH_AFTER_TICKS;
    public static final int ESCAPE_TICKS = Balance.Escape.BREACH_ESCAPE_TICKS;

    private BreachRules() {
    }

    /** Whether a unit Unstable for {@code unstableTicks} in a row is in Breach. */
    public static boolean breached(int unstableTicks) {

        return unstableTicks >= BREACH_AFTER_TICKS;
    }

    /** Whether one ghost escapes on this tick of the Breach (1 = its first tick). */
    public static boolean escapesOn(int breachTick) {

        return breachTick > 0 && breachTick % ESCAPE_TICKS == 0;
    }

    /**
     * The index of the next ghost to escape: storage order, with the one processing or binding last; -1 if none.
     */
    public static int nextToEscape(List<UUID> stored, @Nullable UUID processing, @Nullable UUID binding) {

        for (int i = 0; i < stored.size(); i++) {
            UUID id = stored.get(i);
            if (!id.equals(processing) && !id.equals(binding)) {
                return i;
            }
        }
        return stored.isEmpty() ? -1 : 0;
    }
}
