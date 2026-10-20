package io.github.mdw22.paranormalcraft.ward;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.world.LoadedPositions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Ward Lantern coverage. A loaded lantern protects everything within {@code wardLanternRadius} (server config) of
 * it: no natural monster spawns, no haunting actions by ghosts inside, no shadow figures for players inside. Ghosts
 * themselves can still be inside a ward, harmless but catchable. Positions come from
 * {@link LoadedPositions#WARD_LANTERNS}, so no check ever scans for blocks.
 */
public final class Wards {

    private Wards() {
    }

    /**
     * The nearest loaded lantern whose ward covers {@code pos}, or null.
     */
    public static @Nullable BlockPos coveringLantern(Level level, Vec3 pos) {

        BlockPos best = null;
        double bestSqr = Double.MAX_VALUE;
        for (BlockPos lantern : LoadedPositions.WARD_LANTERNS.near(level, pos, ModConfigs.server().wardLanternRadius())) {
            double distance = Vec3.atCenterOf(lantern).distanceToSqr(pos);
            if (distance < bestSqr) {
                best = lantern;
                bestSqr = distance;
            }
        }
        return best;
    }

    /**
     * Whether {@code pos} is within {@link Balance.Ward#KEEP_AWAY} of a lantern: ghosts don't choose to go there.
     */
    public static boolean tooClose(Level level, Vec3 pos) {

        return LoadedPositions.WARD_LANTERNS.anyWithin(level, pos, Balance.Ward.KEEP_AWAY);
    }

    public static boolean covers(Level level, Vec3 pos) {

        return LoadedPositions.WARD_LANTERNS.anyWithin(level, pos, ModConfigs.server().wardLanternRadius());
    }

    /**
     * Whether a natural spawn of {@code category} at {@code pos} is blocked: monsters from the biome's own list inside
     * a ward. Structure spawn overrides (witch huts, fortresses, monuments, outposts) are let through, so vanilla
     * farms and progression still work. Called from the natural spawner's position check (see
     * {@code NaturalSpawnerMixin}).
     */
    public static boolean blocksNaturalSpawn(ServerLevel level, MobCategory category, BlockPos pos, StructureManager structures, ChunkGenerator generator) {

        if (category != MobCategory.MONSTER || isStructureOverride(level, category, pos, structures, generator)) {
            return false;
        }
        return blocksBiomeSpawn(level, category, pos);
    }

    /**
     * Whether vanilla would pick {@code pos}'s spawns from a structure's override list rather than the biome's: the
     * Nether fortress special case, or any structure whose spawn override for {@code category} covers {@code pos}
     * ({@code ChunkGenerator.getMobsAt} returns the biome's own list otherwise).
     */
    public static boolean isStructureOverride(ServerLevel level, MobCategory category, BlockPos pos, StructureManager structures, ChunkGenerator generator) {

        if (NaturalSpawner.isInNetherFortressBounds(pos, level, category, structures)) {
            return true;
        }
        Holder<Biome> biome = level.getBiome(pos);
        return generator.getMobsAt(biome, structures, category, pos) != biome.value().getMobSettings().getMobs(category);
    }

    /**
     * The ward part of {@link #blocksNaturalSpawn}, for a biome spawn: blocked inside a ward (with a puff).
     */
    public static boolean blocksBiomeSpawn(Level level, MobCategory category, BlockPos pos) {

        if (category != MobCategory.MONSTER) {
            return false;
        }
        BlockPos lantern = coveringLantern(level, Vec3.atBottomCenterOf(pos));
        if (lantern == null) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel) {
            WardEffects.deniedSpawnPuff(serverLevel, lantern, pos.immutable());
        }
        return true;
    }
}
