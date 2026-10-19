package io.github.mdw22.paranormalcraft.neoforge.datagen;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {

        super(output, Constants.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {

        add("itemGroup." + Constants.MOD_ID, Constants.MOD_NAME);
        addItem(ModItems.SPECTRAL_GOGGLES, "Spectral Goggles");
        addItem(ModItems.LINGERING_SPIRIT_SPAWN_EGG, "Lingering Spirit Spawn Egg");
        addItem(ModItems.EMF_READER, "EMF Reader");
        addItem(ModItems.SPIRIT_THERMOMETER, "Spirit Thermometer");
        addEntityType(ModEntities.LINGERING_SPIRIT, "Lingering Spirit");
        addEntityType(ModEntities.SHADOW_FIGURE, "Shadow Figure");
        addItem(ModItems.TETHER_WAND, "Tether Wand");
        addBlock(ModBlocks.GHOST_TRAP, "Ghost Trap");
        addBlock(ModBlocks.FILLED_GHOST_TRAP, "Filled Ghost Trap");
        addEntityType(ModEntities.THROWN_GHOST_TRAP, "Thrown Ghost Trap");
        add("item.paranormalcraft.tether_wand.beam_time", "Beam time left: %s s");
        add("item.paranormalcraft.tether_wand.heat", "Heat: %s%%");
        add("item.paranormalcraft.tether_wand.overheated", "Overheated");
        add("item.paranormalcraft.filled_ghost_trap.ghost", "%s — %s");
        add("item.paranormalcraft.filled_ghost_trap.captor", "Captured by %s");
        add("item.paranormalcraft.filled_ghost_trap.from", "From: %s");
        add("item.paranormalcraft.filled_ghost_trap.empty", "Empty");
        add("structure.paranormalcraft.abandoned_cabin", "Abandoned Cabin");
        add("entity.paranormalcraft.ghost.dispersed", "%s has dispersed.");
        add("subtitles.paranormalcraft.lingering_spirit.recover", "Spirit hisses back to strength");
        add("subtitles.paranormalcraft.tether_wand.hum", "Tether Wand hums");
        add("subtitles.paranormalcraft.tether_wand.latch", "Tether latches on");
        add("subtitles.paranormalcraft.tether_wand.overheat", "Tether Wand overheats");
        add("subtitles.paranormalcraft.tether_wand.backfire", "Tether beams backfire");
        add("subtitles.paranormalcraft.ghost_trap.arm", "Ghost Trap arms");
        add("subtitles.paranormalcraft.ghost_trap.vacuum", "Ghost Trap pulls");
        add("subtitles.paranormalcraft.ghost_trap.capture", "Ghost Trap snaps shut");
        add("subtitles.paranormalcraft.ghost_trap.break_free", "Spirit breaks free");
        add("subtitles.paranormalcraft.ghost_trap.rattle", "Ghost Trap rattles");
        add("subtitles.paranormalcraft.ghost_trap.release", "Spirit released");

        add("subtitles.paranormalcraft.lingering_spirit.whisper", "Something whispers");
        add("subtitles.paranormalcraft.lingering_spirit.disperse", "Spirit disperses");
        add("subtitles.paranormalcraft.lingering_spirit.hurt", "Spirit wavers");
        add("subtitles.paranormalcraft.spectral_goggles.equip", "Goggles click into place");
        add("subtitles.paranormalcraft.shadow_figure.vanish", "Something rushes past");
        add("subtitles.paranormalcraft.shadow_figure.presence", "Something watches");

        add("commands.paranormalcraft.reveal.on", "Ghost reveal on: every ghost is visible to you and home radii are drawn");
        add("commands.paranormalcraft.reveal.off", "Ghost reveal off");
        add("commands.paranormalcraft.debug.manifest", "Forced %s ghost(s) to manifest");
        add("commands.paranormalcraft.debug.presence", "Set Presence on %s ghost(s)");
        add("commands.paranormalcraft.debug.hallucinate", "Sent a shadow figure to %s player(s)");
        add("commands.paranormalcraft.debug.haunt.none", "No ghost could do '%s' here (no target, or griefing is off)");
        add("commands.paranormalcraft.debug.haunt.unknown", "Unknown haunting action '%s'");
        add("commands.paranormalcraft.debug.savestructure", "Saved structure %s to the world's generated folder");
        add("commands.paranormalcraft.debug.savestructure.failed", "No structure block in SAVE mode there, or saving failed");
        add("commands.paranormalcraft.debug.coldspot", "Showing the cold spot of %s ghost(s) for 10 seconds");
    }
}
