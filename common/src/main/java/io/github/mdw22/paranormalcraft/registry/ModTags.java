package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * The mod's block tag keys. The tag JSON lives in {@code data/paranormalcraft/tags/block}.
 */
public final class ModTags {

    /**
     * Doors and thin walls that ghosts drift through.
     */
    public static final TagKey<Block> GHOST_PASSABLE = TagKey.create(Registries.BLOCK, Constants.id("ghost_passable"));

    /**
     * What a player phased by the Spectral Step walks through: {@link #GHOST_PASSABLE} plus every glass block (listed
     * by hand: {@code #minecraft:impermeable} also holds barriers).
     */
    public static final TagKey<Block> PHASE_PASSABLE = TagKey.create(Registries.BLOCK, Constants.id("phase_passable"));

    /** Plain torches a ghost can knock off. Soul and redstone torches are deliberately absent. */
    public static final TagKey<Block> SNUFFABLE_TORCHES = TagKey.create(Registries.BLOCK, Constants.id("haunting/snuffable_torches"));
    /** Lit candles and campfires a ghost can put out (not soul campfires). */
    public static final TagKey<Block> SNUFFABLE_FLAMES = TagKey.create(Registries.BLOCK, Constants.id("haunting/snuffable_flames"));
    /** Wooden doors, trapdoors and fence gates a ghost can swing. Iron and copper ones are deliberately absent. */
    public static final TagKey<Block> HAUNTED_OPENINGS = TagKey.create(Registries.BLOCK, Constants.id("haunting/openings"));
    /** Containers whose lid a ghost can rattle. */
    public static final TagKey<Block> PHANTOM_CONTAINERS = TagKey.create(Registries.BLOCK, Constants.id("haunting/phantom_containers"));

    private ModTags() {
    }
}
