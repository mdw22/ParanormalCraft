package io.github.mdw22.paranormalcraft.neoforge.platform;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.platform.services.IRegistrationHelper;
import io.github.mdw22.paranormalcraft.registry.MenuFactory;
import io.github.mdw22.paranormalcraft.registry.RegistryEntry;
import io.github.mdw22.paranormalcraft.registry.TabContents;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NeoForgeRegistrationHelper implements IRegistrationHelper {

    private static final Map<ResourceKey<? extends Registry<?>>, DeferredRegister<?>> REGISTERS = new HashMap<>();
    private static IEventBus modBus;

    /**
     * Must be called from the mod constructor before common init registers anything.
     */
    public static void setModBus(IEventBus bus) {

        modBus = bus;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends R> RegistryEntry<T> register(Registry<R> registry, String name, Supplier<T> factory) {

        DeferredRegister<R> register = (DeferredRegister<R>) REGISTERS.computeIfAbsent(registry.key(), key -> {
            if (modBus == null) {
                throw new IllegalStateException("Registered " + name + " before the NeoForge mod bus was set");
            }
            DeferredRegister<R> created = DeferredRegister.create(registry, Constants.MOD_ID);
            created.register(modBus);
            return created;
        });
        DeferredHolder<R, T> holder = register.register(name, factory);
        return RegistryEntry.of(holder.getId(), holder);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder(TabContents contents) {

        return CreativeModeTab.builder().displayItems((parameters, output) -> contents.addItems(parameters, output::accept));
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenuType(MenuFactory<T> factory) {

        return new MenuType<>(factory::create, FeatureFlags.VANILLA_SET);
    }
}
