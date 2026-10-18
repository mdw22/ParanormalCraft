package io.github.mdw22.paranormalcraft.world;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Keys for the Abandoned Cabin: an oak and a spruce jigsaw structure with the same layout, one structure set, and the
 * {@code #paranormalcraft:abandoned_cabin} structure tag that {@code /locate} uses to find either.
 */
public final class ModStructures {

    public static final ResourceKey<Structure> ABANDONED_CABIN_OAK = ResourceKey.create(Registries.STRUCTURE, Constants.id("abandoned_cabin_oak"));
    public static final ResourceKey<Structure> ABANDONED_CABIN_SPRUCE = ResourceKey.create(Registries.STRUCTURE, Constants.id("abandoned_cabin_spruce"));
    public static final ResourceKey<StructureSet> ABANDONED_CABINS = ResourceKey.create(Registries.STRUCTURE_SET, Constants.id("abandoned_cabins"));
    public static final ResourceKey<StructureTemplatePool> ABANDONED_CABIN_OAK_POOL = ResourceKey.create(Registries.TEMPLATE_POOL, Constants.id("abandoned_cabin/oak"));
    public static final ResourceKey<StructureTemplatePool> ABANDONED_CABIN_SPRUCE_POOL = ResourceKey.create(Registries.TEMPLATE_POOL, Constants.id("abandoned_cabin/spruce"));
    public static final ResourceKey<LootTable> ABANDONED_CABIN_CHEST = ResourceKey.create(Registries.LOOT_TABLE, Constants.id("chests/abandoned_cabin"));

    public static final TagKey<Structure> ABANDONED_CABIN = TagKey.create(Registries.STRUCTURE, Constants.id("abandoned_cabin"));
    public static final TagKey<Biome> HAS_ABANDONED_CABIN_OAK = TagKey.create(Registries.BIOME, Constants.id("has_structure/abandoned_cabin_oak"));
    public static final TagKey<Biome> HAS_ABANDONED_CABIN_SPRUCE = TagKey.create(Registries.BIOME, Constants.id("has_structure/abandoned_cabin_spruce"));

    /** Template ids (data/paranormalcraft/structure/…nbt). */
    public static final String OAK_TEMPLATE = Constants.MOD_ID + ":abandoned_cabin_oak";
    public static final String SPRUCE_TEMPLATE = Constants.MOD_ID + ":abandoned_cabin_spruce";
    /** The structure set's placement salt, unique to this mod. */
    public static final int SALT = 1_627_384_951;
    public static final int SPACING = 32;
    public static final int SEPARATION = 12;

    private ModStructures() {
    }
}
