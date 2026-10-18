package io.github.mdw22.paranormalcraft.fabric.platform;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.platform.services.IRegistrationHelper;
import io.github.mdw22.paranormalcraft.registry.MenuFactory;
import io.github.mdw22.paranormalcraft.registry.RegistryEntry;
import io.github.mdw22.paranormalcraft.registry.TabContents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Supplier;

public class FabricRegistrationHelper implements IRegistrationHelper {

    @Override
    public <R, T extends R> RegistryEntry<T> register(Registry<R> registry, String name, Supplier<T> factory) {

        Identifier id = Constants.id(name);
        T value = Registry.register(registry, id, factory.get());
        return RegistryEntry.of(id, () -> value);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder(TabContents contents) {

        return FabricCreativeModeTab.builder().displayItems((parameters, output) -> contents.addItems(parameters, output::accept));
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenuType(MenuFactory<T> factory) {

        return new MenuType<>(factory::create, FeatureFlags.VANILLA_SET);
    }
}
