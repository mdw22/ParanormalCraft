package io.github.mdw22.paranormalcraft.journal;

import java.util.Locale;

/**
 * Things a player does that the Field Journal and the Paranormal Craft advancements react to. The first four are also
 * the journal stages for a ghost type, in order.
 */
public enum JournalEvent {
    /** EMF level 5 near a ghost of that type (stage 1). */
    DETECTED,
    /** Saw one with goggles (stage 2). */
    SEEN,
    /** Captured one (stage 3). */
    CAPTURED,
    /** Processed or released one (stage 4). */
    STUDIED,
    /** A thermometer reading below 5° at a cold spot. */
    COLD_SPOT,
    /** Loaded a ghost into a Containment Unit. */
    CONTAINED,
    /** Processed a ghost. */
    PROCESSED,
    /** Released a ghost. */
    RELEASED,
    /** Placed a Ward Lantern. */
    WARD_PLACED,
    /** Bound a ghost into a Spirit Vessel. */
    BOUND,
    /** Summoned a Wisp Familiar. */
    FAMILIAR_SUMMONED,
    /** A ghost that followed this player settled in their home. */
    GHOST_SETTLED,
    /** Captured a ghost that had settled in this player's home. */
    HOUSE_CALL,
    /** Was within 16 blocks of a Containment breach. */
    BREACH_WITNESSED;

    /** The stages, in order (stage n is {@code STAGES[n - 1]}). */
    public static final JournalEvent[] STAGES = {DETECTED, SEEN, CAPTURED, STUDIED};

    public String serialName() {

        return name().toLowerCase(Locale.ROOT);
    }
}
