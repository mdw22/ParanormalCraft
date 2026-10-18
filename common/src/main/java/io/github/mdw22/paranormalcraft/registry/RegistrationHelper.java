package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.platform.Services;
import io.github.mdw22.paranormalcraft.platform.services.IRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * The only way content gets registered. Everything lands in the {@value Constants#MOD_ID} namespace.
 * Call these from static initializers of classes like {@link ModItems}, loaded from {@code ParanormalCraft.init()}.
 */
public final class RegistrationHelper {

    private static final IRegistrationHelper PLATFORM = Services.REGISTRATION;
    private static final List<EntityAttributes<?>> ENTITY_ATTRIBUTES = new ArrayList<>();

    private RegistrationHelper() {
    }

    public static <T extends Item> RegistryEntry<T> item(String name, Function<Item.Properties, T> factory) {

        return item(name, factory, Item.Properties::new);
    }

    public static <T extends Item> RegistryEntry<T> item(String name, Function<Item.Properties, T> factory, Supplier<Item.Properties> properties) {

        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Constants.id(name));
        return PLATFORM.register(BuiltInRegistries.ITEM, name, () -> factory.apply(properties.get().setId(key)));
    }

    public static <T extends Block> RegistryEntry<T> block(String name, Function<BlockBehaviour.Properties, T> factory, Supplier<BlockBehaviour.Properties> properties) {

        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Constants.id(name));
        return PLATFORM.register(BuiltInRegistries.BLOCK, name, () -> factory.apply(properties.get().setId(key)));
    }

    @SafeVarargs
    public static <T extends BlockEntity> RegistryEntry<BlockEntityType<T>> blockEntity(String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block>... validBlocks) {

        return PLATFORM.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, name, () -> {
            Set<Block> blocks = new HashSet<>();
            for (Supplier<? extends Block> block : validBlocks) {
                blocks.add(block.get());
            }
            return new BlockEntityType<>(factory, blocks);
        });
    }

    public static <T extends Entity> RegistryEntry<EntityType<T>> entityType(String name, Supplier<EntityType.Builder<T>> builder) {

        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Constants.id(name));
        return PLATFORM.register(BuiltInRegistries.ENTITY_TYPE, name, () -> builder.get().build(key));
    }

    /**
     * Declares the default attributes of a living entity type. Each loader hands these to its attribute registry.
     */
    public static <T extends LivingEntity> void entityAttributes(Supplier<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes) {

        ENTITY_ATTRIBUTES.add(new EntityAttributes<>(type, attributes));
    }

    /**
     * Every attribute declaration, for the loaders. Only valid once registration has run.
     */
    public static List<EntityAttributes<?>> entityAttributes() {

        return List.copyOf(ENTITY_ATTRIBUTES);
    }

    public record EntityAttributes<T extends LivingEntity>(Supplier<EntityType<T>> type, Supplier<AttributeSupplier.Builder> attributes) {
    }

    public static RegistryEntry<SoundEvent> sound(String name) {

        return PLATFORM.register(BuiltInRegistries.SOUND_EVENT, name, () -> SoundEvent.createVariableRangeEvent(Constants.id(name)));
    }

    public static <T extends AbstractContainerMenu> RegistryEntry<MenuType<T>> menuType(String name, MenuFactory<T> factory) {

        return PLATFORM.register(BuiltInRegistries.MENU, name, () -> PLATFORM.createMenuType(factory));
    }

    /**
     * Registers a creative tab filled by {@code contents}. {@code config} receives a loader-provided builder; set the
     * title and icon on it.
     */
    public static RegistryEntry<CreativeModeTab> creativeTab(String name, TabContents contents, UnaryOperator<CreativeModeTab.Builder> config) {

        return PLATFORM.register(BuiltInRegistries.CREATIVE_MODE_TAB, name, () -> config.apply(PLATFORM.creativeTabBuilder(contents)).build());
    }
}
