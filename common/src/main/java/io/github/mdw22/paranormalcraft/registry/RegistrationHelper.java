package io.github.mdw22.paranormalcraft.registry;

import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.platform.Services;
import io.github.mdw22.paranormalcraft.platform.services.IRegistrationHelper;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
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
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
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

    /**
     * Registers a GameTest function. Pair it with a {@code test_instance} JSON in {@code data/<ns>/test_instance}.
     */
    public static RegistryEntry<Consumer<GameTestHelper>> testFunction(String name, Consumer<GameTestHelper> test) {

        return PLATFORM.register(BuiltInRegistries.TEST_FUNCTION, name, () -> test);
    }

    /**
     * Registers a structure type, whose codec reads structures of {@code "type": "<mod id>:<name>"}.
     */
    public static <S extends Structure> RegistryEntry<StructureType<S>> structureType(String name, Supplier<MapCodec<S>> codec) {

        return PLATFORM.register(BuiltInRegistries.STRUCTURE_TYPE, name, () -> {
            MapCodec<S> resolved = codec.get();
            return () -> resolved;
        });
    }

    /**
     * Registers a data component type; {@code builder} sets its codecs (persistent and/or network-synchronised).
     */
    public static <T> RegistryEntry<DataComponentType<T>> dataComponent(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {

        return PLATFORM.register(BuiltInRegistries.DATA_COMPONENT_TYPE, name, () -> builder.apply(DataComponentType.builder()).build());
    }

    /**
     * Registers a mob effect. Use {@link #holder} to get the {@code Holder} that effect instances need.
     */
    public static <T extends MobEffect> RegistryEntry<T> mobEffect(String name, Supplier<T> effect) {

        return PLATFORM.register(BuiltInRegistries.MOB_EFFECT, name, effect);
    }

    /**
     * The registry holder for a registered mob effect (valid once registration has run).
     */
    public static Holder<MobEffect> holder(RegistryEntry<? extends MobEffect> effect) {

        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.get());
    }

    /**
     * Registers an advancement criterion trigger, referenced from advancement JSON as {@code "trigger": "<mod id>:<name>"}.
     */
    public static <T extends CriterionTrigger<?>> RegistryEntry<T> criterionTrigger(String name, Supplier<T> trigger) {

        return PLATFORM.register(BuiltInRegistries.TRIGGER_TYPES, name, trigger);
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
