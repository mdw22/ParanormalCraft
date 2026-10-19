package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.block.FilledGhostTrapBlock;
import io.github.mdw22.paranormalcraft.block.FilledGhostTrapBlockEntity;
import io.github.mdw22.paranormalcraft.block.GhostMarkerBlock;
import io.github.mdw22.paranormalcraft.block.GhostMarkerBlockEntity;
import io.github.mdw22.paranormalcraft.block.GhostTrapBlock;
import io.github.mdw22.paranormalcraft.block.GhostTrapBlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/**
 * Block and block entity registrations: the ghost marker, the Ghost Trap and the Filled Ghost Trap.
 */
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

    /** An armed Ghost Trap (thrown or placed). Small, no collision, needs a solid floor. */
    public static final RegistryEntry<GhostTrapBlock> GHOST_TRAP = RegistrationHelper.block("ghost_trap", GhostTrapBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .noCollision()
                    .noOcclusion()
                    .strength(0.5F)
                    .sound(SoundType.COPPER)
                    .pushReaction(PushReaction.DESTROY));

    public static final RegistryEntry<BlockEntityType<GhostTrapBlockEntity>> GHOST_TRAP_ENTITY = RegistrationHelper.blockEntity("ghost_trap",
            GhostTrapBlockEntity::new, GHOST_TRAP);

    /** A Ghost Trap holding a ghost: glows faintly, rattles now and then. */
    public static final RegistryEntry<FilledGhostTrapBlock> FILLED_GHOST_TRAP = RegistrationHelper.block("filled_ghost_trap", FilledGhostTrapBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .noCollision()
                    .noOcclusion()
                    .strength(0.5F)
                    .sound(SoundType.COPPER)
                    .lightLevel(state -> 3)
                    .pushReaction(PushReaction.DESTROY));

    public static final RegistryEntry<BlockEntityType<FilledGhostTrapBlockEntity>> FILLED_GHOST_TRAP_ENTITY = RegistrationHelper.blockEntity("filled_ghost_trap",
            FilledGhostTrapBlockEntity::new, FILLED_GHOST_TRAP);

    private ModBlocks() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
