package io.github.mdw22.paranormalcraft;

import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.gametest.ModGameTests;
import io.github.mdw22.paranormalcraft.platform.Services;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModCreativeTabs;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModEffects;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.ModMenus;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import io.github.mdw22.paranormalcraft.registry.ModStructureTypes;
import io.github.mdw22.paranormalcraft.registry.ModTriggers;

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries (GeckoLib's common API). Loader specific concepts go through Services.
/**
 * Common entry point. Each loader calls {@link #init()} from its mod constructor or initializer; it loads the server
 * config and registers all content.
 */
public class ParanormalCraft {

    /**
     * Called once by each loader's entrypoint during mod construction, before any registry is frozen.
     */
    public static void init() {

        Constants.LOG.info("Loading {} on {} ({} environment)", Constants.MOD_NAME, Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());

        ModConfigs.loadServer();

        ModSounds.init();
        ModDataComponents.init();
        ModBlocks.init();
        ModEntities.init();
        ModItems.init();
        ModMenus.init();
        ModEffects.init();
        ModTriggers.init();
        ModCreativeTabs.init();
        ModStructureTypes.init();
        ModGameTests.init();
    }
}
