package io.github.mdw22.paranormalcraft.neoforge.datagen;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModEffects;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Generates {@code en_us.json}: every item, block, entity, tooltip, subtitle and message string.
 */
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

        // Update 9: containment, ward, journal
        addBlock(ModBlocks.CONTAINMENT_UNIT, "Containment Unit");
        addBlock(ModBlocks.WARD_LANTERN, "Ward Lantern");
        addItem(ModItems.LINGERING_ECTOPLASM, "Lingering Ectoplasm");
        addItem(ModItems.SPECTRAL_RESIDUE, "Spectral Residue");
        addItem(ModItems.FIELD_JOURNAL, "Field Journal");
        addEffect(ModEffects.RESTFUL, "Restful");
        add("item.paranormalcraft.spectral_residue.tooltip", "It hums faintly. Someone might know what to do with this.");
        add("container.paranormalcraft.containment_unit", "Containment Unit");
        add("gui.paranormalcraft.containment.process", "Process");
        add("gui.paranormalcraft.containment.release", "Release");
        add("gui.paranormalcraft.containment.bind", "Bind");
        add("gui.paranormalcraft.containment.bind.needs_vessel", "Needs a Spirit Vessel");
        add("gui.paranormalcraft.containment.empty", "No spirits held");
        add("gui.paranormalcraft.containment.charge", "Charge %s");
        add("gui.paranormalcraft.containment.unstable", "UNSTABLE %s");
        add("gui.paranormalcraft.containment.trap", "Trap");
        add("gui.paranormalcraft.containment.fuel", "Fuel");
        add("gui.paranormalcraft.containment.output", "Output");
        add("subtitles.paranormalcraft.containment_unit.hum", "Containment Unit hums");
        add("subtitles.paranormalcraft.containment_unit.load", "Ghost Trap clunks into place");
        add("subtitles.paranormalcraft.containment_unit.bubble", "Containment Unit bubbles");
        add("subtitles.paranormalcraft.containment_unit.alarm", "Containment alarm rings");
        add("subtitles.paranormalcraft.lingering_spirit.ascend", "Spirit ascends");
        add("journal.paranormalcraft.field_notes.title", "Field Notes");
        add("journal.paranormalcraft.field_notes.1", "Notes on the restless dead, kept by whoever carries this book. The EMF Reader climbs as you near one, the thermometer drops where it lingers, and the goggles let you see it.");
        add("journal.paranormalcraft.field_notes.2", "A ghost cannot be killed, only worn down. When it flickers it is weak: hold it with the Tether Wand and lead it over a Ghost Trap.");
        add("journal.paranormalcraft.field_notes.3", "A trapped spirit goes into a Containment Unit. Feed the unit soul sand, then decide: render the spirit down, or let it go.");
        add("journal.paranormalcraft.lingering_spirit.title", "Lingering Spirit");
        add("journal.paranormalcraft.lingering_spirit.stage1", "The needle jumped and would not settle. Something has stayed here long after it should have gone: a Lingering Spirit, by the old accounts. Faint and cold, and the commonest of the restless dead.");
        add("journal.paranormalcraft.lingering_spirit.stage2", "Through the lenses it is a pale shape, like a sheet left out in the rain, drifting just above the floor. It is curious and comes close to watch, but backs away if stared at. It keeps to the coldest corner of a room, and now and then shows itself, whispering.");
        add("journal.paranormalcraft.lingering_spirit.stage3", "It fought the tether, then tired. Wear it down until it flickers, hold the beam, and it goes into a trap with hardly a struggle. A Ward Lantern quiets it: inside the glow it stops its mischief, though it will not leave.");
        add("journal.paranormalcraft.lingering_spirit.stage4", "Rendered down in a Containment Unit it leaves three to five globs of Lingering Ectoplasm, and now and then a strange residue. Set free instead, it rises and is gone, and leaves a calm behind that keeps the Phantoms away for three nights.");
        add("journal.paranormalcraft.locked.1", "Nothing recorded yet.");
        add("journal.paranormalcraft.locked.2", "Not yet seen with my own eyes.");
        add("journal.paranormalcraft.locked.3", "Not yet caught.");
        add("journal.paranormalcraft.locked.4", "Not yet studied.");
        add("journal.paranormalcraft.captures.title", "Captures");
        add("journal.paranormalcraft.captures.empty", "No captures yet.");
        add("journal.paranormalcraft.captures.entry", "%s, %s. Day %s, %s.");
        add("journal.paranormalcraft.outcome.stored", "held");
        add("journal.paranormalcraft.outcome.processed", "processed");
        add("journal.paranormalcraft.outcome.released", "released");
        add("advancements.paranormalcraft.root.title", "Paranormal Craft");
        add("advancements.paranormalcraft.root.description", "Something is out there");
        add("advancements.paranormalcraft.somethings_here.title", "Something's Here");
        add("advancements.paranormalcraft.somethings_here.description", "Read EMF level 5 close to a ghost");
        add("advancements.paranormalcraft.seeing_things.title", "Seeing Things");
        add("advancements.paranormalcraft.seeing_things.description", "See a ghost through Spectral Goggles");
        add("advancements.paranormalcraft.cold_feet.title", "Cold Feet");
        add("advancements.paranormalcraft.cold_feet.description", "Read below 5\u00b0 at a ghost's cold spot");
        add("advancements.paranormalcraft.gotcha.title", "Gotcha!");
        add("advancements.paranormalcraft.gotcha.description", "Capture a ghost in a Ghost Trap");
        add("advancements.paranormalcraft.lockup.title", "Lockup");
        add("advancements.paranormalcraft.lockup.description", "Load a ghost into a Containment Unit");
        add("advancements.paranormalcraft.ectoplasmic.title", "Ectoplasmic");
        add("advancements.paranormalcraft.ectoplasmic.description", "Process a ghost into ectoplasm");
        add("advancements.paranormalcraft.rest_easy.title", "Rest Easy");
        add("advancements.paranormalcraft.rest_easy.description", "Release a ghost and receive its blessing");
        add("advancements.paranormalcraft.warded.title", "Warded");
        add("advancements.paranormalcraft.warded.description", "Place a Ward Lantern");

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
