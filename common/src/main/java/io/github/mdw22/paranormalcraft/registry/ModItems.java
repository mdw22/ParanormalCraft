package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
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

    private ModItems() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
