package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {

    /**
     * Doors and thin walls that ghosts drift through.
     */
    public static final TagKey<Block> GHOST_PASSABLE = TagKey.create(Registries.BLOCK, Constants.id("ghost_passable"));

    private ModTags() {
    }
}
