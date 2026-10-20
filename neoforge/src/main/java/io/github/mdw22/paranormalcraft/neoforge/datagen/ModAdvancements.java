package io.github.mdw22.paranormalcraft.neoforge.datagen;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.journal.GhostEventTrigger;
import io.github.mdw22.paranormalcraft.journal.JournalEvent;
import io.github.mdw22.paranormalcraft.journal.JournalStages;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.ModTriggers;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The "Paranormal Craft" advancement tab (root plus ten, all fired by {@code paranormalcraft:ghost_event}) and the
 * hidden Field Journal stage advancements: {@code journal/<ghost type>/<stage>} for every type in
 * {@link JournalStages#GHOST_TYPES}, plus {@code journal/<ghost type>/bound}, with no display, so they never show or toast.
 */
public class ModAdvancements implements AdvancementSubProvider {

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> output) {

        GhostEventTrigger trigger = ModTriggers.GHOST_EVENT.get();
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(ModItems.FIELD_JOURNAL.get(), title("root"), description("root"),
                        Identifier.withDefaultNamespace("gui/advancements/backgrounds/stone"), AdvancementType.TASK, false, false, false)
                .addCriterion("has_emf_reader", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.EMF_READER.get()))
                .addCriterion("has_field_journal", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.FIELD_JOURNAL.get()))
                .addCriterion("has_spectral_goggles", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SPECTRAL_GOGGLES.get()))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(output, Constants.id("main/root").toString());
        AdvancementHolder somethingsHere = child(output, trigger, root, "somethings_here", ModItems.EMF_READER.get(), JournalEvent.DETECTED, AdvancementType.TASK);
        AdvancementHolder seeingThings = child(output, trigger, somethingsHere, "seeing_things", ModItems.SPECTRAL_GOGGLES.get(), JournalEvent.SEEN, AdvancementType.TASK);
        child(output, trigger, root, "cold_feet", ModItems.SPIRIT_THERMOMETER.get(), JournalEvent.COLD_SPOT, AdvancementType.TASK);
        AdvancementHolder gotcha = child(output, trigger, seeingThings, "gotcha", ModItems.GHOST_TRAP.get(), JournalEvent.CAPTURED, AdvancementType.GOAL);
        AdvancementHolder lockup = child(output, trigger, gotcha, "lockup", ModItems.CONTAINMENT_UNIT.get(), JournalEvent.CONTAINED, AdvancementType.TASK);
        AdvancementHolder ectoplasmic = child(output, trigger, lockup, "ectoplasmic", ModItems.LINGERING_ECTOPLASM.get(), JournalEvent.PROCESSED, AdvancementType.TASK);
        child(output, trigger, lockup, "rest_easy", ModItems.FILLED_GHOST_TRAP.get(), JournalEvent.RELEASED, AdvancementType.GOAL);
        child(output, trigger, ectoplasmic, "warded", ModItems.WARD_LANTERN.get(), JournalEvent.WARD_PLACED, AdvancementType.TASK);
        AdvancementHolder bound = child(output, trigger, ectoplasmic, "bound", ModItems.SPIRIT_VESSEL.get(), JournalEvent.BOUND, AdvancementType.GOAL);
        child(output, trigger, bound, "little_light", ModItems.SPIRIT_VESSEL.get(), JournalEvent.FAMILIAR_SUMMONED, AdvancementType.TASK);

        for (String type : JournalStages.GHOST_TYPES) {
            for (int stage = 1; stage <= JournalEvent.STAGES.length; stage++) {
                Advancement.Builder.advancement()
                        .addCriterion(JournalEvent.STAGES[stage - 1].serialName(),
                                GhostEventTrigger.TriggerInstance.of(trigger, JournalEvent.STAGES[stage - 1], Optional.of(type)))
                        .save(output, JournalStages.advancementId(type, stage).toString());
            }
            Advancement.Builder.advancement()
                    .addCriterion(JournalEvent.BOUND.serialName(), GhostEventTrigger.TriggerInstance.of(trigger, JournalEvent.BOUND, Optional.of(type)))
                    .save(output, JournalStages.boundAdvancementId(type).toString());
        }
    }

    private static AdvancementHolder child(Consumer<AdvancementHolder> output, GhostEventTrigger trigger, AdvancementHolder parent, String name,
                                           ItemLike icon, JournalEvent event, AdvancementType frame) {

        return Advancement.Builder.advancement()
                .parent(parent)
                .display(icon, title(name), description(name), null, frame, true, true, false)
                .addCriterion(event.serialName(), GhostEventTrigger.TriggerInstance.of(trigger, event, Optional.empty()))
                .save(output, Constants.id("main/" + name).toString());
    }

    private static Component title(String name) {

        return Component.translatable("advancements.paranormalcraft." + name + ".title");
    }

    private static Component description(String name) {

        return Component.translatable("advancements.paranormalcraft." + name + ".description");
    }
}
