package io.github.mdw22.paranormalcraft.registry;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Creates a menu on the client side when the server opens it. Mirrors vanilla's {@code MenuType.MenuSupplier},
 * which common code cannot see.
 */
@FunctionalInterface
public interface MenuFactory<T extends AbstractContainerMenu> {

    T create(int containerId, Inventory playerInventory);
}
