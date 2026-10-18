package io.github.mdw22.paranormalcraft.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Registers entity renderers. Implemented by each loader's client code.
 */
@FunctionalInterface
public interface EntityRendererRegistrar {

    <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
}
