package io.github.mdw22.paranormalcraft.capture;

import io.github.mdw22.paranormalcraft.Balance;
import org.jspecify.annotations.Nullable;

/**
 * The empty-hand hold that releases a Filled Ghost Trap's ghost, as plain logic so it can be unit tested. While use is
 * held on a block the client repeats it every 4 ticks; each repeat is a {@link #press}. The hold restarts if the
 * presses stop for more than {@link #GAP_TICKS}, and completes after {@link #HOLD_TICKS} of continuous holding.
 *
 * @param start game time the hold started
 * @param last  game time of the latest press
 */
public record ReleaseHold(long start, long last) {

    public static final int HOLD_TICKS = Balance.Trap.RELEASE_HOLD_TICKS;
    public static final int GAP_TICKS = Balance.Trap.RELEASE_GAP_TICKS;

    /**
     * The hold after a press at {@code now}: a fresh one if there was none or it lapsed, otherwise extended.
     */
    public static ReleaseHold press(@Nullable ReleaseHold current, long now) {

        if (current == null || current.lapsed(now)) {
            return new ReleaseHold(now, now);
        }
        return new ReleaseHold(current.start, now);
    }

    /**
     * Whether the player has let go: no press for more than {@link #GAP_TICKS}.
     */
    public boolean lapsed(long now) {

        return now - this.last > GAP_TICKS;
    }

    /**
     * 0 at the first press, 1 once the hold is complete.
     */
    public double progress() {

        return Math.min(1.0, (double) (this.last - this.start) / HOLD_TICKS);
    }

    public boolean complete() {

        return this.last - this.start >= HOLD_TICKS;
    }
}
