package io.github.mdw22.paranormalcraft.registry;

import net.minecraft.world.item.Item;

public final class ModItems {

    /**
     * Throwaway item that proves registration works on every loader. Remove once real items exist.
     */
    public static final RegistryEntry<Item> DEBUG_WISP = RegistrationHelper.item("debug_wisp", Item::new);

    private ModItems() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
