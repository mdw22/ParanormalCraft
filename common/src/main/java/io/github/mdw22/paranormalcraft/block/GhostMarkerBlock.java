package io.github.mdw22.paranormalcraft.block;

import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Invisible, collision-free marker placed by structures (the Abandoned Cabin) where ghosts should live. On its first
 * server tick it spawns the structure's ghosts, homed here, and removes itself. Not obtainable.
 */
public class GhostMarkerBlock extends BaseEntityBlock {

    public static final MapCodec<GhostMarkerBlock> CODEC = simpleCodec(GhostMarkerBlock::new);

    public GhostMarkerBlock(Properties properties) {

        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {

        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

        return new GhostMarkerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {

        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.GHOST_MARKER_ENTITY.get(), GhostMarkerBlockEntity::serverTick);
    }
}
