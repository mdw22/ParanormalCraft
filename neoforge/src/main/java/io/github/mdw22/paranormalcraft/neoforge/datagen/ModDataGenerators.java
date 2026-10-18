package io.github.mdw22.paranormalcraft.neoforge.datagen;

import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;

/**
 * Run with {@code ./gradlew :neoforge:runData}. Output goes to {@code common/src/generated/resources}.
 */
public class ModDataGenerators {

    public static void gatherClientData(GatherDataEvent.Client event) {

        event.createProvider(ModModelProvider::new);
        event.createProvider(ModLanguageProvider::new);
        event.createProvider(ModRecipeProvider.Runner::new);
        // Worldgen entries first: the tag providers below look up the structures they add.
        event.createDatapackRegistryObjects(ModWorldgen.registries());
        event.createProvider(ModBiomeTagsProvider::new);
        event.createProvider(ModStructureTagsProvider::new);
        event.createProvider((output, lookup) -> new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModChestLoot::new, LootContextParamSets.CHEST)), lookup));
    }
}
