package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {

    public static final RegistryEntry<CreativeModeTab> PARANORMAL_CRAFT = RegistrationHelper.creativeTab(Constants.MOD_ID,
            (parameters, output) -> output.accept(new ItemStack(ModItems.DEBUG_WISP.get())),
            builder -> builder
                    .title(Component.translatable("itemGroup." + Constants.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.DEBUG_WISP.get())));

    private ModCreativeTabs() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
