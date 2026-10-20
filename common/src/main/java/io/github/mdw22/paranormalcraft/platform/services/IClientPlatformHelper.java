package io.github.mdw22.paranormalcraft.platform.services;

import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.client.MenuScreenFactory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

/**
 * Client-only loader hooks. Loaded through {@link io.github.mdw22.paranormalcraft.client.ClientServices}, never from
 * code that runs on a dedicated server.
 */
public interface IClientPlatformHelper {

    /**
     * Registers an item model range property. Vanilla keeps its registry private: NeoForge exposes it through
     * {@code RegisterRangeSelectItemModelPropertyEvent}, Fabric through a transitive access widener.
     */
    void registerRangeSelectProperty(Identifier id, MapCodec<? extends RangeSelectItemModelProperty> codec);

    /**
     * Registers the screen a menu type opens. Vanilla's {@code MenuScreens.register} is private: NeoForge exposes it
     * through {@code RegisterMenuScreensEvent}, Fabric through a transitive access widener.
     */
    <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerMenuScreen(Supplier<? extends MenuType<? extends M>> type,
                                                                                               MenuScreenFactory<M, S> factory);
}
