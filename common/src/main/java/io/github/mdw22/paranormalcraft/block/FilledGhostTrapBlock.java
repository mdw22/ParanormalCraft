package io.github.mdw22.paranormalcraft.block;

import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.Presence;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Ghost Trap with a ghost in it. It glows faintly and rattles now and then.
 * <ul>
 *     <li>Break it, or use it with an empty hand while sneaking, to pick it up as an item (the ghost stays inside).</li>
 *     <li>Use it while sneaking with anything in hand to release the ghost: it reappears above the trap at full
 *     Presence, keeping its UUID, name, home and cold spot, and the trap is an armed empty trap again.</li>
 * </ul>
 */
public class FilledGhostTrapBlock extends BaseEntityBlock {

    public static final MapCodec<FilledGhostTrapBlock> CODEC = simpleCodec(FilledGhostTrapBlock::new);
    public static final int EVENT_RATTLE = 1;

    public FilledGhostTrapBlock(Properties properties) {

        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {

        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        return GhostTrapBlock.SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {

        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {

        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel && serverLevel.getBlockEntity(pos) instanceof FilledGhostTrapBlockEntity trap) {
            ItemStack item = itemFor(trap);
            level.removeBlock(pos, false);
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {

        if (!player.isSecondaryUseActive() || stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel serverLevel && serverLevel.getBlockEntity(pos) instanceof FilledGhostTrapBlockEntity trap) {
            release(serverLevel, pos, trap);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * The trap as an item, ghost and all.
     */
    public static ItemStack itemFor(FilledGhostTrapBlockEntity trap) {

        ItemStack item = new ItemStack(ModItems.FILLED_GHOST_TRAP.get());
        if (trap.contents() != null) {
            item.set(ModDataComponents.CAPTURED_GHOST.get(), trap.contents());
        }
        return item;
    }

    /**
     * Lets the ghost out above the trap at full Presence and turns the trap back into an armed empty one.
     *
     * @return the released ghost, or null if there was none or it couldn't be recreated
     */
    public static @Nullable GhostEntity release(ServerLevel level, BlockPos pos, FilledGhostTrapBlockEntity trap) {

        CapturedGhost contents = trap.contents();
        GhostEntity ghost = null;
        if (contents != null) {
            CompoundTag data = contents.entity().copy();
            data.putFloat("Presence", Presence.MAX);
            data.putInt("PresenceTicksSinceHit", Presence.REGEN_DELAY_TICKS);
            Entity entity;
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Constants.LOG)) {
                entity = EntityType.loadEntityRecursive(TagValueInput.create(reporter, level.registryAccess(), data), level, EntitySpawnReason.LOAD, loaded -> loaded);
            }
            if (entity instanceof GhostEntity loaded) {
                loaded.snapTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, loaded.getYRot(), 0.0F);
                level.addFreshEntity(loaded);
                ghost = loaded;
            } else {
                Constants.LOG.error("Could not recreate the ghost in the trap at {} ({})", pos.toShortString(), contents.name());
            }
        }
        GhostEntity released = ghost;
        ServerTrace.event("trap_release", line -> {
            line.addProperty("trap", pos.toShortString());
            line.addProperty("name", contents == null ? "none" : contents.name());
            line.addProperty("ghost", released == null ? "none" : released.getUUID().toString());
        });
        level.playSound(null, pos, ModSounds.TRAP_RELEASE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 20, 0.3, 0.4, 0.3, 0.03);
        trap.setContents(null);
        level.setBlock(pos, ModBlocks.GHOST_TRAP.get().defaultBlockState(), Block.UPDATE_ALL);
        return ghost;
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {

        if (id == EVENT_RATTLE && level.isClientSide()) {
            for (int i = 0; i < 6; i++) {
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.3 + level.getRandom().nextDouble() * 0.4, pos.getY() + 0.32,
                        pos.getZ() + 0.3 + level.getRandom().nextDouble() * 0.4, 0, 0.02, 0);
            }
            return true;
        }
        return super.triggerEvent(state, level, pos, id, param);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {

        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.35,
                    pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0, 0.005, 0);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

        return new FilledGhostTrapBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {

        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.FILLED_GHOST_TRAP_ENTITY.get(), FilledGhostTrapBlockEntity::serverTick);
    }
}
