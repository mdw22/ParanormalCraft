package io.github.mdw22.paranormalcraft.registry;

import net.minecraft.sounds.SoundEvent;

public final class ModSounds {

    public static final RegistryEntry<SoundEvent> LINGERING_SPIRIT_WHISPER = RegistrationHelper.sound("entity.lingering_spirit.whisper");
    public static final RegistryEntry<SoundEvent> LINGERING_SPIRIT_DISPERSE = RegistrationHelper.sound("entity.lingering_spirit.disperse");
    public static final RegistryEntry<SoundEvent> LINGERING_SPIRIT_HURT = RegistrationHelper.sound("entity.lingering_spirit.hurt");
    public static final RegistryEntry<SoundEvent> SPECTRAL_GOGGLES_EQUIP = RegistrationHelper.sound("item.spectral_goggles.equip");

    private ModSounds() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
