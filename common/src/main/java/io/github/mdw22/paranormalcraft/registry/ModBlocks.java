package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.block.FilledGhostTrapBlock;
import io.github.mdw22.paranormalcraft.block.FilledGhostTrapBlockEntity;
import io.github.mdw22.paranormalcraft.block.GhostMarkerBlock;
import io.github.mdw22.paranormalcraft.block.GhostMarkerBlockEntity;
import io.github.mdw22.paranormalcraft.block.GhostTrapBlock;
import io.github.mdw22.paranormalcraft.block.GhostTrapBlockEntity;
import io.github.mdw22.paranormalcraft.containment.ContainmentUnitBlock;
import io.github.mdw22.paranormalcraft.containment.ContainmentUnitBlockEntity;
import io.github.mdw22.paranormalcraft.ward.WardLanternBlock;
import io.github.mdw22.paranormalcraft.ward.WardLanternBlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Block and block entity registrations: the ghost marker, the Ghost Trap, the Filled Ghost Trap, the Containment Unit
 * and the Ward Lantern.
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

    /** The Containment Unit: stores, fuels, processes and releases ghosts. */
    public static final RegistryEntry<ContainmentUnitBlock> CONTAINMENT_UNIT = RegistrationHelper.block("containment_unit", ContainmentUnitBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(ContainmentUnitBlock::lightLevel));

    public static final RegistryEntry<BlockEntityType<ContainmentUnitBlockEntity>> CONTAINMENT_UNIT_ENTITY = RegistrationHelper.blockEntity("containment_unit",
            ContainmentUnitBlockEntity::new, CONTAINMENT_UNIT);

    /** The Ward Lantern: a lantern that keeps monsters, haunting and shadow figures away. */
    public static final RegistryEntry<WardLanternBlock> WARD_LANTERN = RegistrationHelper.block("ward_lantern", WardLanternBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .forceSolidOn()
                    .strength(3.5F)
                    .sound(SoundType.LANTERN)
                    .lightLevel(WardLanternBlock::lightLevel)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    public static final RegistryEntry<BlockEntityType<WardLanternBlockEntity>> WARD_LANTERN_ENTITY = RegistrationHelper.blockEntity("ward_lantern",
            WardLanternBlockEntity::new, WARD_LANTERN);

    private ModBlocks() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
