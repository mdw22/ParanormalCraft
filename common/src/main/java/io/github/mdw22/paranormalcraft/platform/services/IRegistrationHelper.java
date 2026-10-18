package io.github.mdw22.paranormalcraft.platform.services;

import io.github.mdw22.paranormalcraft.registry.MenuFactory;
import io.github.mdw22.paranormalcraft.registry.RegistryEntry;
import io.github.mdw22.paranormalcraft.registry.TabContents;
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Supplier;

/**
 * Loader side of {@link io.github.mdw22.paranormalcraft.registry.RegistrationHelper}. Use that class instead of this one.
 */
public interface IRegistrationHelper {

    /**
     * Registers an object in the mod's namespace. NeoForge defers creation to its RegisterEvent; Fabric creates and
     * registers immediately.
     */
    <R, T extends R> RegistryEntry<T> register(Registry<R> registry, String name, Supplier<T> factory);

    /**
     * A creative tab builder that the loader can place and page correctly, with its display items wired to
     * {@code contents}.
     */
    CreativeModeTab.Builder creativeTabBuilder(TabContents contents);

    /**
     * Creates a menu type. The vanilla constructor is not visible from common code.
     */
    <T extends AbstractContainerMenu> MenuType<T> createMenuType(MenuFactory<T> factory);
}
