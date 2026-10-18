package io.github.mdw22.paranormalcraft.platform.services;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;

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
}
