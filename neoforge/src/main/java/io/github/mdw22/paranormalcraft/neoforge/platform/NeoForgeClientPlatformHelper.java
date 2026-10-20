package io.github.mdw22.paranormalcraft.neoforge.platform;

import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.platform.services.IClientPlatformHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import io.github.mdw22.paranormalcraft.client.MenuScreenFactory;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Client hooks on NeoForge: range-select item model properties are queued and registered in
 * {@code RegisterRangeSelectItemModelPropertyEvent}.
 */
public class NeoForgeClientPlatformHelper implements IClientPlatformHelper {

    // Common client init runs before NeoForge fires the registration event, so properties wait here until then.
    private static final Map<Identifier, MapCodec<? extends RangeSelectItemModelProperty>> RANGE_PROPERTIES = new LinkedHashMap<>();

    @Override
    public void registerRangeSelectProperty(Identifier id, MapCodec<? extends RangeSelectItemModelProperty> codec) {

        RANGE_PROPERTIES.put(id, codec);
    }

    /**
     * Mod bus listener.
     */
    public static void registerRangeSelectProperties(RegisterRangeSelectItemModelPropertyEvent event) {

        RANGE_PROPERTIES.forEach(event::register);
    }

    // Menu screens, queued the same way for RegisterMenuScreensEvent.
    private static final List<Consumer<RegisterMenuScreensEvent>> MENU_SCREENS = new ArrayList<>();

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerMenuScreen(Supplier<? extends MenuType<? extends M>> type,
                                                                                                      MenuScreenFactory<M, S> factory) {

        MENU_SCREENS.add(event -> event.register((MenuType) type.get(), (MenuScreens.ScreenConstructor<M, S>) factory::create));
    }

    /**
     * Mod bus listener.
     */
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {

        MENU_SCREENS.forEach(registration -> registration.accept(event));
    }
}
