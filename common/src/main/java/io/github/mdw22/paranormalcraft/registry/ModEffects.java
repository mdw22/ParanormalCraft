package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.effect.RestfulEffect;

/**
 * Mob effect registrations: Restful, the Lingering Spirit's blessing. Use {@code RegistrationHelper.holder} for the
 * holder an effect instance needs.
 */
public final class ModEffects {

    public static final RegistryEntry<RestfulEffect> RESTFUL = RegistrationHelper.mobEffect("restful", RestfulEffect::new);

    private ModEffects() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
