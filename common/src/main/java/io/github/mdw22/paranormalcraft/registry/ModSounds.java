package io.github.mdw22.paranormalcraft.registry;

import net.minecraft.sounds.SoundEvent;

/**
 * Sound event registrations. Each id maps to sound files in {@code assets/paranormalcraft/sounds.json} (vanilla
 * sounds for now), with a subtitle key in the lang file.
 */
public final class ModSounds {

    public static final RegistryEntry<SoundEvent> LINGERING_SPIRIT_WHISPER = RegistrationHelper.sound("entity.lingering_spirit.whisper");
    public static final RegistryEntry<SoundEvent> LINGERING_SPIRIT_DISPERSE = RegistrationHelper.sound("entity.lingering_spirit.disperse");
    public static final RegistryEntry<SoundEvent> LINGERING_SPIRIT_HURT = RegistrationHelper.sound("entity.lingering_spirit.hurt");
    public static final RegistryEntry<SoundEvent> SHADOW_FIGURE_VANISH = RegistrationHelper.sound("entity.shadow_figure.vanish");
    public static final RegistryEntry<SoundEvent> SHADOW_FIGURE_PRESENCE = RegistrationHelper.sound("entity.shadow_figure.presence");
    public static final RegistryEntry<SoundEvent> EMF_READER_TICK = RegistrationHelper.sound("item.emf_reader.tick");
    public static final RegistryEntry<SoundEvent> SPECTRAL_GOGGLES_EQUIP = RegistrationHelper.sound("item.spectral_goggles.equip");
    public static final RegistryEntry<SoundEvent> GHOST_RECOVER = RegistrationHelper.sound("entity.lingering_spirit.recover");
    public static final RegistryEntry<SoundEvent> TETHER_HUM = RegistrationHelper.sound("item.tether_wand.hum");
    public static final RegistryEntry<SoundEvent> TETHER_LATCH = RegistrationHelper.sound("item.tether_wand.latch");
    public static final RegistryEntry<SoundEvent> TETHER_OVERHEAT = RegistrationHelper.sound("item.tether_wand.overheat");
    public static final RegistryEntry<SoundEvent> TETHER_BACKFIRE = RegistrationHelper.sound("item.tether_wand.backfire");
    public static final RegistryEntry<SoundEvent> TRAP_ARM = RegistrationHelper.sound("block.ghost_trap.arm");
    public static final RegistryEntry<SoundEvent> TRAP_VACUUM = RegistrationHelper.sound("block.ghost_trap.vacuum");
    public static final RegistryEntry<SoundEvent> TRAP_CAPTURE = RegistrationHelper.sound("block.ghost_trap.capture");
    public static final RegistryEntry<SoundEvent> TRAP_BREAK_FREE = RegistrationHelper.sound("block.ghost_trap.break_free");
    public static final RegistryEntry<SoundEvent> TRAP_RATTLE = RegistrationHelper.sound("block.ghost_trap.rattle");
    public static final RegistryEntry<SoundEvent> TRAP_RELEASE = RegistrationHelper.sound("block.ghost_trap.release");

    private ModSounds() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
