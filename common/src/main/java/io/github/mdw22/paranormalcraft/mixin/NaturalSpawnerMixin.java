package io.github.mdw22.paranormalcraft.mixin;

import io.github.mdw22.paranormalcraft.ward.Wards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ward Lanterns stop natural monster spawns. The one hook is vanilla's per-position check for natural spawning, which
 * spawners, spawn eggs, commands and structure spawns never go through. Same on both loaders.
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
    private static void paranormalcraft$wardBlocksMonsters(ServerLevel level, MobCategory category, StructureManager structureManager,
                                                            ChunkGenerator generator, MobSpawnSettings.SpawnerData spawnerData,
                                                            BlockPos.MutableBlockPos pos, double nearestPlayerDistanceSqr,
                                                            CallbackInfoReturnable<Boolean> cir) {

        if (Wards.blocksNaturalSpawn(level, category, pos)) {
            cir.setReturnValue(false);
        }
    }
}
