package io.github.mdw22.paranormalcraft.neoforge.datagen;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.world.ModStructures;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.world.level.biome.Biomes;

import java.util.concurrent.CompletableFuture;

/**
 * Where cabins may appear: oak cabins in plains and broadleaf forests, spruce cabins in taigas. Never oceans, rivers or
 * beaches.
 */
public class ModBiomeTagsProvider extends BiomeTagsProvider {

    public ModBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {

        super(output, lookup, Constants.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {

        tag(ModStructures.HAS_ABANDONED_CABIN_OAK)
                .add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST);
        tag(ModStructures.HAS_ABANDONED_CABIN_SPRUCE)
                .add(Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.SNOWY_TAIGA);
    }
}
