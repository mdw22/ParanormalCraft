package io.github.mdw22.paranormalcraft.containment;

import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Containment Unit: an iron-and-glass cabinet. Its glow brightens with each stored ghost ({@link #GHOSTS}, also
 * its light level) and flickers red while Unstable. Use opens its menu; a comparator reads the stored ghost count.
 * All the logic is in {@link ContainmentUnitBlockEntity}.
 */
public class ContainmentUnitBlock extends BaseEntityBlock {

    public static final MapCodec<ContainmentUnitBlock> CODEC = simpleCodec(ContainmentUnitBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty GHOSTS = IntegerProperty.create("ghosts", 0, ContainmentUnitBlockEntity.CAPACITY);
    public static final BooleanProperty UNSTABLE = BooleanProperty.create("unstable");
    private static final DustParticleOptions RED = new DustParticleOptions(0xFF3020, 0.8F);

    public ContainmentUnitBlock(Properties properties) {

        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GHOSTS, 0).setValue(UNSTABLE, false));
    }

    /**
     * Light level: 2 per stored ghost, plus a red 5 while Unstable.
     */
    public static int lightLevel(BlockState state) {

        return Math.min(15, state.getValue(GHOSTS) * 2 + (state.getValue(UNSTABLE) ? 5 : 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {

        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(FACING, GHOSTS, UNSTABLE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {

        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof ContainmentUnitBlockEntity unit) {
            player.openMenu(unit);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {

        return true;
    }

    /**
     * 0–4 stored ghosts read as 0, 4, 8, 11, 15.
     */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {

        return comparatorSignal(state.getValue(GHOSTS));
    }

    public static int comparatorSignal(int ghosts) {

        return Math.round(ghosts * 15.0F / ContainmentUnitBlockEntity.CAPACITY);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {

        if (state.getValue(UNSTABLE) && random.nextInt(3) == 0) {
            level.addParticle(RED, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.3 + random.nextDouble() * 0.6,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0, 0);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

        return new ContainmentUnitBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {

        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.CONTAINMENT_UNIT_ENTITY.get(), ContainmentUnitBlockEntity::serverTick);
    }
}
