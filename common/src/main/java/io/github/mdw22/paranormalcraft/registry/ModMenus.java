package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.containment.ContainmentUnitMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Menu type registrations. Each loader creates the type ({@code IRegistrationHelper.createMenuType}); screens are
 * registered on the client in {@code ParanormalCraftClient}.
 */
public final class ModMenus {

    public static final RegistryEntry<MenuType<ContainmentUnitMenu>> CONTAINMENT_UNIT = RegistrationHelper.menuType("containment_unit",
            ContainmentUnitMenu::new);

    private ModMenus() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
