package io.github.mdw22.paranormalcraft.fabric.platform;

import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.platform.services.IClientPlatformHelper;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;

public class FabricClientPlatformHelper implements IClientPlatformHelper {

    @Override
    public void registerRangeSelectProperty(Identifier id, MapCodec<? extends RangeSelectItemModelProperty> codec) {

        // Fabric API's transitive access wideners make the vanilla id mapper public.
        RangeSelectItemModelProperties.ID_MAPPER.put(id, codec);
    }
}
