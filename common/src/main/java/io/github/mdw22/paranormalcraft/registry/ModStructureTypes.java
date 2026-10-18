package io.github.mdw22.paranormalcraft.registry;

import io.github.mdw22.paranormalcraft.world.DryLandStructure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class ModStructureTypes {

    /** A jigsaw structure that won't start in water; see {@link DryLandStructure}. */
    public static final RegistryEntry<StructureType<DryLandStructure>> DRY_LAND_JIGSAW = RegistrationHelper.structureType("dry_land_jigsaw", () -> DryLandStructure.CODEC);

    private ModStructureTypes() {
    }

    /**
     * Loads this class so its entries are registered.
     */
    public static void init() {
    }
}
