package io.github.mdw22.paranormalcraft.haunting;

import java.util.Locale;
import java.util.Optional;

/**
 * The haunting tricks a ghost can play, with their pick weights. {@link #griefing} actions break or remove something
 * and need {@code hauntingGriefing} and the {@code mobGriefing} game rule.
 */
public enum HauntingAction {
    KNOCK_OFF_DECORATION(2, true),
    SNUFF_LIGHT(2, true),
    FLIP_LEVER(1, true),
    TOGGLE_OPENING(3, false),
    PHANTOM_CHEST(2, false),
    TOSS_ITEMS(1, false),
    FOOTSTEPS(3, false),
    KNOCKING(2, false);

    private final int weight;
    private final boolean griefing;

    HauntingAction(int weight, boolean griefing) {

        this.weight = weight;
        this.griefing = griefing;
    }

    public int weight() {

        return this.weight;
    }

    public boolean griefing() {

        return this.griefing;
    }

    /**
     * Lower-case name used in commands and dumps, e.g. {@code snuff_light}.
     */
    public String id() {

        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<HauntingAction> byId(String id) {

        for (HauntingAction action : values()) {
            if (action.id().equals(id)) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }
}
