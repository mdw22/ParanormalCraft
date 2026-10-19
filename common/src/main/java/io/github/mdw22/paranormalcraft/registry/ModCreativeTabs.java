package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * The Paranormal Craft creative tab and what it shows.
 */
public final class ModCreativeTabs {

    public static final RegistryEntry<CreativeModeTab> PARANORMAL_CRAFT = RegistrationHelper.creativeTab(Constants.MOD_ID,
            (parameters, output) -> {
                output.accept(new ItemStack(ModItems.SPECTRAL_GOGGLES.get()));
                output.accept(new ItemStack(ModItems.EMF_READER.get()));
                output.accept(new ItemStack(ModItems.SPIRIT_THERMOMETER.get()));
                output.accept(new ItemStack(ModItems.TETHER_WAND.get()));
                output.accept(new ItemStack(ModItems.GHOST_TRAP.get()));
                output.accept(new ItemStack(ModItems.LINGERING_SPIRIT_SPAWN_EGG.get()));
            },
            builder -> builder
                    .title(Component.translatable("itemGroup." + Constants.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.SPECTRAL_GOGGLES.get())));

    private ModCreativeTabs() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
