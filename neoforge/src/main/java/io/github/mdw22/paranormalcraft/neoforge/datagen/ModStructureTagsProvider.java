package io.github.mdw22.paranormalcraft.neoforge.datagen;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.world.ModStructures;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.StructureTagsProvider;

import java.util.concurrent.CompletableFuture;

/**
 * {@code #paranormalcraft:abandoned_cabin} groups both wood variants, so {@code /locate} finds either.
 */
public class ModStructureTagsProvider extends StructureTagsProvider {

    public ModStructureTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {

        super(output, lookup, Constants.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {

        tag(ModStructures.ABANDONED_CABIN).add(ModStructures.ABANDONED_CABIN_OAK, ModStructures.ABANDONED_CABIN_SPRUCE);
    }
}
