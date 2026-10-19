package io.github.mdw22.paranormalcraft.neoforge.platform;

import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.platform.services.IClientPlatformHelper;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;

import java.util.LinkedHashMap;
import java.util.Map;

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
}
