package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.block.GhostMarkerBlock;
import io.github.mdw22.paranormalcraft.block.GhostMarkerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {

    /**
     * Structure-only marker that turns into the structure's ghosts. No item, no drops, no collision.
     */
    public static final RegistryEntry<GhostMarkerBlock> GHOST_MARKER = RegistrationHelper.block("ghost_marker", GhostMarkerBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .noCollision()
                    .noLootTable()
                    .noOcclusion()
                    .instabreak()
                    .pushReaction(PushReaction.DESTROY));

    public static final RegistryEntry<BlockEntityType<GhostMarkerBlockEntity>> GHOST_MARKER_ENTITY = RegistrationHelper.blockEntity("ghost_marker",
            GhostMarkerBlockEntity::new, GHOST_MARKER);

    private ModBlocks() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
