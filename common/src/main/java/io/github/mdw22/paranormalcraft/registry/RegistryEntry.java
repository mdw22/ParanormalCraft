package io.github.mdw22.paranormalcraft.registry;

import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * A registered object. On NeoForge {@link #get()} is only valid once registration has run; on Fabric it is valid
 * immediately. Either way, only call it after mod construction.
 */
public interface RegistryEntry<T> extends Supplier<T> {

    Identifier id();

    @Override
    T get();

    static <T> RegistryEntry<T> of(Identifier id, Supplier<T> value) {

        return new RegistryEntry<>() {

            @Override
            public Identifier id() {

                return id;
            }

            @Override
            public T get() {

                return value.get();
            }
        };
    }
}
