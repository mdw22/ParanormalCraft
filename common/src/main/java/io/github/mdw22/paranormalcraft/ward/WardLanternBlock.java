package io.github.mdw22.paranormalcraft.ward;

import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.journal.JournalEvent;
import io.github.mdw22.paranormalcraft.journal.JournalEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jspecify.annotations.Nullable;

/**
 * The Ward Lantern: a lantern (hanging or standing, light 12) with slow soul-cyan motes; it flares, pulsing 15/13 for
 * 1.5 s, when it catches a ghost. Its block entity registers
 * the position while it is loaded, which is what {@link Wards} checks.
 */
public class WardLanternBlock extends LanternBlock implements EntityBlock {

    public static final MapCodec<WardLanternBlock> CODEC = simpleCodec(WardLanternBlock::new);
    /** The flare when the ward catches a ghost: 0 steady (light 12); 2 flaring (15); 1 the dimmer half of its pulse (13). */
    public static final IntegerProperty FLARE = IntegerProperty.create("flare", 0, 2);
    private static final int PULSE_TICKS = Balance.Ward.FLARE_PULSE_TICKS;

    public WardLanternBlock(Properties properties) {

        super(properties);
        registerDefaultState(this.defaultBlockState().setValue(FLARE, 0));
    }

    public static int lightLevel(BlockState state) {

        return switch (state.getValue(FLARE)) {
            case 2 -> Balance.Ward.FLARE_LIGHT;
            case 1 -> Balance.Ward.FLARE_DIM_LIGHT;
            default -> Balance.Ward.LIGHT;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        super.createBlockStateDefinition(builder);
        builder.add(FLARE);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        // The flare pulses 15 / 13 every few ticks until its time is up.
        int left = level.getBlockEntity(pos) instanceof WardLanternBlockEntity ward ? ward.tickFlare(PULSE_TICKS) : 0;
        if (left <= 0) {
            if (state.getValue(FLARE) != 0) {
                level.setBlock(pos, state.setValue(FLARE, 0), Block.UPDATE_ALL);
            }
            return;
        }
        level.setBlock(pos, state.setValue(FLARE, state.getValue(FLARE) == 2 ? 1 : 2), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, PULSE_TICKS);
    }

    @Override
    public MapCodec<? extends LanternBlock> codec() {

        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

        return new WardLanternBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {

        super.setPlacedBy(level, pos, state, by, itemStack);
        if (level instanceof ServerLevel serverLevel) {
            WardEffects.placementRing(serverLevel, pos, ModConfigs.server().wardLanternRadius());
        }
        if (by instanceof ServerPlayer player) {
            JournalEvents.fire(player, JournalEvent.WARD_PLACED, null);
            ServerTrace.event("ward_placed", line -> {
                line.addProperty("pos", pos.toShortString());
                line.addProperty("player", player.getGameProfile().name());
            });
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {

        if (state.getValue(FLARE) != 0) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        (random.nextDouble() - 0.5) * 0.1, 0.05, (random.nextDouble() - 0.5) * 0.1);
            }
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8,
                    pos.getY() + 0.3 + random.nextDouble() * 0.5, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8, 0, 0.01, 0);
        }
    }
}
