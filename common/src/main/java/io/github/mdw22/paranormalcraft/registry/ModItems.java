package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.item.FilledGhostTrapItem;
import io.github.mdw22.paranormalcraft.item.GhostTrapItem;
import io.github.mdw22.paranormalcraft.item.TetherWandItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

public final class ModItems {

    public static final ResourceKey<EquipmentAsset> SPECTRAL_GOGGLES_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Constants.id("spectral_goggles"));

    /**
     * Head-slot wearable that reveals ghosts. No protection, no durability, no enchanting component; only the two
     * curses can be applied (it is in the {@code enchantable/equippable} and {@code enchantable/vanishing} tags, like
     * a carved pumpkin). The vignette is the equippable's camera overlay.
     */
    public static final RegistryEntry<Item> SPECTRAL_GOGGLES = RegistrationHelper.item("spectral_goggles", Item::new, () -> new Item.Properties()
            .stacksTo(1)
            .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
                    .setEquipSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.SPECTRAL_GOGGLES_EQUIP.get()))
                    .setAsset(SPECTRAL_GOGGLES_ASSET)
                    .setCameraOverlay(Constants.id("misc/spectral_goggles_vignette"))
                    .setDamageOnHurt(false)
                    .build()));

    public static final RegistryEntry<SpawnEggItem> LINGERING_SPIRIT_SPAWN_EGG = RegistrationHelper.item("lingering_spirit_spawn_egg", SpawnEggItem::new,
            () -> new Item.Properties().spawnEgg(ModEntities.LINGERING_SPIRIT.get()));

    /**
     * Shows ghost proximity as lights 1 to 5 while held in either hand. The level reaches only the holder, by payload.
     */
    public static final RegistryEntry<Item> EMF_READER = RegistrationHelper.item("emf_reader", Item::new, () -> new Item.Properties().stacksTo(1));

    /**
     * Shows the temperature in the action bar while held in either hand; cold spots read colder.
     */
    public static final RegistryEntry<Item> SPIRIT_THERMOMETER = RegistrationHelper.item("spirit_thermometer", Item::new, () -> new Item.Properties().stacksTo(1));

    /**
     * Hold use to fire a tether beam at a visible ghost; 300 s of beam time, repaired with copper.
     */
    public static final RegistryEntry<TetherWandItem> TETHER_WAND = RegistrationHelper.item("tether_wand", TetherWandItem::new,
            () -> new Item.Properties().durability(300).repairable(Items.COPPER_INGOT));

    /** Throw it, or sneak-place it, to arm a Ghost Trap. */
    public static final RegistryEntry<GhostTrapItem> GHOST_TRAP = RegistrationHelper.item("ghost_trap",
            properties -> new GhostTrapItem(ModBlocks.GHOST_TRAP.get(), properties), () -> new Item.Properties().useBlockDescriptionPrefix());

    /** A trap with a ghost in it; one per stack, since each holds a different ghost. */
    public static final RegistryEntry<FilledGhostTrapItem> FILLED_GHOST_TRAP = RegistrationHelper.item("filled_ghost_trap",
            properties -> new FilledGhostTrapItem(ModBlocks.FILLED_GHOST_TRAP.get(), properties),
            () -> new Item.Properties().stacksTo(1).useBlockDescriptionPrefix());

    private ModItems() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
