package io.github.mdw22.paranormalcraft.ward;

import com.mojang.serialization.MapCodec;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.journal.JournalEvent;
import io.github.mdw22.paranormalcraft.journal.JournalEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Ward Lantern: a lantern (hanging or standing, light 12) with slow soul-cyan motes. Its block entity registers
 * the position while it is loaded, which is what {@link Wards} checks.
 */
public class WardLanternBlock extends LanternBlock implements EntityBlock {

    public static final MapCodec<WardLanternBlock> CODEC = simpleCodec(WardLanternBlock::new);

    public WardLanternBlock(Properties properties) {

        super(properties);
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

        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8,
                    pos.getY() + 0.3 + random.nextDouble() * 0.5, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8, 0, 0.01, 0);
        }
    }
}
