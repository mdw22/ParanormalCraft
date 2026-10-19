package io.github.mdw22.paranormalcraft.block;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Spawns a structure's ghosts the first time its chunk ticks: the first with probability
 * {@code min(1, ghostSpawnMultiplier)}, a second with {@code min(1, 0.3 × ghostSpawnMultiplier)}, both homed at the
 * marker. Then the marker removes itself, so this happens once per structure.
 */
public class GhostMarkerBlockEntity extends BlockEntity {

    public static final double SECOND_GHOST_FACTOR = Balance.Ghost.SECOND_GHOST_FACTOR;

    public GhostMarkerBlockEntity(BlockPos pos, BlockState state) {

        super(ModBlocks.GHOST_MARKER_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GhostMarkerBlockEntity marker) {

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        double multiplier = ModConfigs.server().ghostSpawnMultiplier();
        RandomSource random = serverLevel.getRandom();
        if (random.nextDouble() < Math.min(1.0, multiplier)) {
            spawnGhost(serverLevel, pos);
        }
        if (random.nextDouble() < Math.min(1.0, SECOND_GHOST_FACTOR * multiplier)) {
            spawnGhost(serverLevel, pos);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    public static void spawnGhost(ServerLevel level, BlockPos home) {

        LingeringSpiritEntity ghost = ModEntities.LINGERING_SPIRIT.get().create(level, EntitySpawnReason.STRUCTURE);
        if (ghost == null) {
            return;
        }
        ghost.snapTo(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        ghost.setHome(level, home);
        level.addFreshEntity(ghost);
    }
}
