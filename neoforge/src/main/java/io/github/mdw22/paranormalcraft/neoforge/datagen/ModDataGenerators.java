package io.github.mdw22.paranormalcraft.neoforge.datagen;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Run with {@code ./gradlew :neoforge:runData}. Output goes to {@code common/src/generated/resources}.
 */
public class ModDataGenerators {

    public static void gatherClientData(GatherDataEvent.Client event) {

        event.createProvider(ModModelProvider::new);
        event.createProvider(ModLanguageProvider::new);
    }
}
