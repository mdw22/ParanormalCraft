package io.github.mdw22.paranormalcraft.world;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.registry.ModStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.List;
import java.util.Optional;

/**
 * A jigsaw structure that refuses to start in water: {@code paranormalcraft:dry_land_jigsaw}. It wraps a vanilla
 * {@link JigsawStructure} (same JSON fields, same settings) and, after the jigsaw has laid out its pieces, rejects the
 * start if the surface block at the start position or at any of the house's four corners is a fluid or waterlogged.
 * <p>
 * "Surface" is the generator's noise surface ({@code WORLD_SURFACE_WG}), which includes seas, rivers and aquifer pools
 * but not lake features, which are decorated later.
 */
public final class DryLandStructure extends Structure {

    public static final MapCodec<DryLandStructure> CODEC = JigsawStructure.CODEC.xmap(DryLandStructure::new, DryLandStructure::jigsaw);
    /**
     * The cabin template's margin around the house on each side (yard ring, porch and fence), so the bounding box
     * inset by this much is the house (plus at most a block of porch).
     */
    public static final int HOUSE_INSET = 3;

    private final JigsawStructure jigsaw;

    public DryLandStructure(JigsawStructure jigsaw) {

        super(new Structure.StructureSettings(jigsaw.biomes(), jigsaw.spawnOverrides(), jigsaw.step(), jigsaw.terrainAdaptation()));
        this.jigsaw = jigsaw;
    }

    public JigsawStructure jigsaw() {

        return this.jigsaw;
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {

        return this.jigsaw.findValidGenerationPoint(context).flatMap(stub -> {
            // Build the pieces once to find the footprint, and hand the built pieces on so they aren't laid out twice.
            StructurePiecesBuilder pieces = stub.getPiecesBuilder();
            if (pieces.isEmpty()) {
                return Optional.empty();
            }
            BoundingBox box = pieces.getBoundingBox();
            BlockPos start = stub.position();
            List<int[]> checks = List.of(
                    new int[]{start.getX(), start.getZ()},
                    new int[]{box.minX() + HOUSE_INSET, box.minZ() + HOUSE_INSET},
                    new int[]{box.maxX() - HOUSE_INSET, box.minZ() + HOUSE_INSET},
                    new int[]{box.minX() + HOUSE_INSET, box.maxZ() - HOUSE_INSET},
                    new int[]{box.maxX() - HOUSE_INSET, box.maxZ() - HOUSE_INSET});
            for (int[] check : checks) {
                if (isWet(context, check[0], check[1])) {
                    return Optional.empty();
                }
            }
            return Optional.of(new Structure.GenerationStub(start, Either.right(pieces)));
        });
    }

    /**
     * Whether the top non-air block of the noise surface at this column is a fluid or holds one.
     */
    public static boolean isWet(Structure.GenerationContext context, int x, int z) {

        int height = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        BlockState surface = column.getBlock(height);
        return !surface.getFluidState().isEmpty();
    }

    @Override
    public StructureType<?> type() {

        return ModStructureTypes.DRY_LAND_JIGSAW.get();
    }
}
