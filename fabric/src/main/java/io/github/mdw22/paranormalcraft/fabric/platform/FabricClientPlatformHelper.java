package io.github.mdw22.paranormalcraft.fabric.platform;

import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.platform.services.IClientPlatformHelper;
import io.github.mdw22.paranormalcraft.client.MenuScreenFactory;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

/**
 * Client hooks on Fabric: range-select item model properties go straight into vanilla's id mapper, which Fabric
 * API's access wideners make public.
 */
public class FabricClientPlatformHelper implements IClientPlatformHelper {

    @Override
    public void registerRangeSelectProperty(Identifier id, MapCodec<? extends RangeSelectItemModelProperty> codec) {

        // Fabric API's transitive access wideners make the vanilla id mapper public.
        RangeSelectItemModelProperties.ID_MAPPER.put(id, codec);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerMenuScreen(Supplier<? extends MenuType<? extends M>> type,
                                                                                                      MenuScreenFactory<M, S> factory) {

        // Fabric API's transitive access wideners make the vanilla registration method public.
        MenuScreens.register((MenuType) type.get(), (MenuScreens.ScreenConstructor<M, S>) factory::create);
    }
}
