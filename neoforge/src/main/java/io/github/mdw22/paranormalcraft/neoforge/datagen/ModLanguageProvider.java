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
        add("journal.paranormalcraft.lingering_spirit.title", "Lingering Spirit");
        add("journal.paranormalcraft.captures.title", "Captures");
        add("journal.paranormalcraft.captures.empty", "No captures yet.");
        add("journal.paranormalcraft.captures.day", "Day %s");
        add("journal.paranormalcraft.outcome.stored", "Held");
        add("journal.paranormalcraft.outcome.processed", "Processed");
        add("journal.paranormalcraft.outcome.released", "Released");
        add("journal.paranormalcraft.outcome.bound", "Bound");
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
        add("advancements.paranormalcraft.bound.title", "Bound");
        add("advancements.paranormalcraft.bound.description", "Bind a ghost into a Spirit Vessel");
        add("advancements.paranormalcraft.little_light.title", "Little Light");
        add("advancements.paranormalcraft.little_light.description", "Summon a Wisp Familiar");

        // Update 10: ward feedback, the journal spread, binding, the Wisp Familiar, the Spectral Step
        add("subtitles.paranormalcraft.ward_lantern.flare", "Ward Lantern flares");
        add("journal.paranormalcraft.lingering_spirit.stage1", "The needle jumps and won't settle. Faint, cold and common: a Lingering Spirit.");
        add("journal.paranormalcraft.lingering_spirit.stage2", "A pale sheet drifting near the floor. Curious, but shy if you stare. It loves the coldest corner.");
        add("journal.paranormalcraft.lingering_spirit.stage3", "Wear it down until it flickers, then hold the beam over a trap. Wards quiet it.");
        add("journal.paranormalcraft.lingering_spirit.stage4", "Render it for ectoplasm, set it free for restful nights, or bind it as a guiding Wisp.");
        add("journal.paranormalcraft.lingering_spirit.bound", "Bound, it is a Wisp: a small light in the dark that warns of what's behind you and knows the way to old cabins.");
        add("journal.paranormalcraft.unseen", "Not yet seen");
        add("journal.paranormalcraft.unknown", "Not yet learned");
        add("journal.paranormalcraft.row.found_with", "Found with");
        add("journal.paranormalcraft.row.calmed_by", "Calmed by");
        add("journal.paranormalcraft.row.process", "Process");
        add("journal.paranormalcraft.row.release", "Release");
        add("journal.paranormalcraft.row.bind", "Bind");
        add("journal.paranormalcraft.note.yield", "\u00d7%s\u2013%s");
        add("journal.paranormalcraft.note.nights", "%s nights");
        add("journal.paranormalcraft.diagram.find", "Find");
        add("journal.paranormalcraft.diagram.weaken", "Weaken");
        add("journal.paranormalcraft.diagram.catch", "Catch");
        add("journal.paranormalcraft.diagram.contain", "Contain");
        add("journal.paranormalcraft.diagram.choose", "Then choose");
        add("journal.paranormalcraft.diagram.process", "Process");
        add("journal.paranormalcraft.diagram.process.gives", "ectoplasm");
        add("journal.paranormalcraft.diagram.release", "Release");
        add("journal.paranormalcraft.diagram.release.gives", "a blessing");
        add("journal.paranormalcraft.diagram.bind", "Bind");
        add("journal.paranormalcraft.diagram.bind.gives", "a job");
        addItem(ModItems.SPIRIT_VESSEL, "Spirit Vessel");
        add("item.paranormalcraft.bound_spirit_vessel", "Bound Spirit Vessel");
        add("item.paranormalcraft.spirit_vessel.empty", "Empty. Bind a stored ghost in a Containment Unit.");
        add("item.paranormalcraft.spirit_vessel.bound", "Bound: %s, %s");
        add("job.paranormalcraft.wisp_familiar", "Wisp Familiar");
        add("job.paranormalcraft.wisp_familiar.icon", "Wisp Familiar (from a Bound Spirit Vessel)");
        add("job.paranormalcraft.wisp_familiar.usage", "Use: summon or recall. Sneak + use: guide to a cabin.");
        addEntityType(ModEntities.WISP_FAMILIAR, "Wisp Familiar");
        addItem(ModItems.SPECTRAL_STEP, "Spectral Step");
        add("gui.paranormalcraft.containment.unbind", "Unbind");
        add("gui.paranormalcraft.containment.unbind.full", "No room in storage");
        add("gui.paranormalcraft.containment.bind.busy", "Binding\u2026");
        add("gui.paranormalcraft.containment.bind.needs_charge", "Needs %s charge");
        add("gui.paranormalcraft.containment.bind.select", "Select a stored spirit");
        add("gui.paranormalcraft.containment.vessel", "Vessel");
        add("message.paranormalcraft.wisp_guide_cooldown", "The wisp needs to rest (%s s)");
        add("subtitles.paranormalcraft.containment_unit.bind_tone", "Containment Unit sings");
        add("subtitles.paranormalcraft.containment_unit.bind_done", "Spirit bound");
        add("subtitles.paranormalcraft.wisp_familiar.summon", "Wisp appears");
        add("subtitles.paranormalcraft.wisp_familiar.recall", "Wisp returns");
        add("subtitles.paranormalcraft.wisp_familiar.warn", "Wisp warns");
        add("subtitles.paranormalcraft.wisp_familiar.no", "Wisp hesitates");
        add("subtitles.paranormalcraft.spectral_step.phase_in", "Spectral Step whooshes");
        add("subtitles.paranormalcraft.spectral_step.phase_out", "Footsteps solidify");

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
