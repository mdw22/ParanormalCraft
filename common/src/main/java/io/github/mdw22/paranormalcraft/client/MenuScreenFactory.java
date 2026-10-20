package io.github.mdw22.paranormalcraft.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Creates the screen for a menu. Mirrors vanilla's {@code MenuScreens.ScreenConstructor}, which common code cannot see.
 */
@FunctionalInterface
public interface MenuScreenFactory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {

    S create(M menu, Inventory inventory, Component title);
}
