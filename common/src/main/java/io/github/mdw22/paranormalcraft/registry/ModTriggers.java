package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.journal.GhostEventTrigger;

/**
 * Advancement criterion trigger registrations: {@code paranormalcraft:ghost_event}, used by every Field Journal stage and
 * Paranormal Craft advancement.
 */
public final class ModTriggers {

    public static final RegistryEntry<GhostEventTrigger> GHOST_EVENT = RegistrationHelper.criterionTrigger("ghost_event", GhostEventTrigger::new);

    private ModTriggers() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
