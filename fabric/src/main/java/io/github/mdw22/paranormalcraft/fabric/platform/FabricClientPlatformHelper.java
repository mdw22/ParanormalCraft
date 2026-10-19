package io.github.mdw22.paranormalcraft.fabric.platform;

import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.platform.services.IClientPlatformHelper;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;

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
}
