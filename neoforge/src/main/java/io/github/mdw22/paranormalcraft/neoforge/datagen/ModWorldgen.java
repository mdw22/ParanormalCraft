package io.github.mdw22.paranormalcraft.neoforge.datagen;

import com.mojang.datafixers.util.Pair;
import io.github.mdw22.paranormalcraft.world.DryLandStructure;
import io.github.mdw22.paranormalcraft.world.ModStructures;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.List;
import java.util.Map;

/**
 * The Abandoned Cabin's worldgen entries: one single-piece jigsaw structure per wood variant (same layout), their
 * template pools, and one random-spread structure set holding both.
 */
public final class ModWorldgen {

    private ModWorldgen() {
    }

    public static RegistrySetBuilder registries() {

        return new RegistrySetBuilder()
                .add(Registries.TEMPLATE_POOL, ModWorldgen::pools)
                .add(Registries.STRUCTURE, ModWorldgen::structures)
                .add(Registries.STRUCTURE_SET, ModWorldgen::structureSets);
    }

    private static void pools(BootstrapContext<StructureTemplatePool> context) {

        var empty = context.lookup(Registries.TEMPLATE_POOL).getOrThrow(Pools.EMPTY);
        context.register(ModStructures.ABANDONED_CABIN_OAK_POOL, new StructureTemplatePool(empty,
                List.of(Pair.of(StructurePoolElement.single(ModStructures.OAK_TEMPLATE).apply(StructureTemplatePool.Projection.RIGID), 1))));
        context.register(ModStructures.ABANDONED_CABIN_SPRUCE_POOL, new StructureTemplatePool(empty,
                List.of(Pair.of(StructurePoolElement.single(ModStructures.SPRUCE_TEMPLATE).apply(StructureTemplatePool.Projection.RIGID), 1))));
    }

    private static void structures(BootstrapContext<Structure> context) {

        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        context.register(ModStructures.ABANDONED_CABIN_OAK, cabin(biomes, ModStructures.HAS_ABANDONED_CABIN_OAK, pools, ModStructures.ABANDONED_CABIN_OAK_POOL));
        context.register(ModStructures.ABANDONED_CABIN_SPRUCE, cabin(biomes, ModStructures.HAS_ABANDONED_CABIN_SPRUCE, pools, ModStructures.ABANDONED_CABIN_SPRUCE_POOL));
    }

    /**
     * A single template placed on the surface, sunk one block so the mossy foundation sits in the ground, with a thin
     * beard to blend the terrain. Wrapped in {@link DryLandStructure} so it never starts in water.
     */
    private static Structure cabin(HolderGetter<Biome> biomes, TagKey<Biome> biomeTag, HolderGetter<StructureTemplatePool> pools,
                                   ResourceKey<StructureTemplatePool> pool) {

        Structure.StructureSettings settings = new Structure.StructureSettings(biomes.getOrThrow(biomeTag), Map.of(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.BEARD_THIN);
        return new DryLandStructure(new JigsawStructure(settings, pools.getOrThrow(pool), 1, ConstantHeight.of(VerticalAnchor.absolute(-1)), false,
                Heightmap.Types.WORLD_SURFACE_WG));
    }

    private static void structureSets(BootstrapContext<StructureSet> context) {

        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
        context.register(ModStructures.ABANDONED_CABINS, new StructureSet(
                List.of(StructureSet.entry(structures.getOrThrow(ModStructures.ABANDONED_CABIN_OAK)),
                        StructureSet.entry(structures.getOrThrow(ModStructures.ABANDONED_CABIN_SPRUCE))),
                new RandomSpreadStructurePlacement(ModStructures.SPACING, ModStructures.SEPARATION, RandomSpreadType.LINEAR, ModStructures.SALT)));
    }
}
