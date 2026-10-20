package io.github.mdw22.paranormalcraft.block;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.capture.FilledTraps;
import io.github.mdw22.paranormalcraft.capture.ReleaseHold;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Filled Ghost Trap's contents ({@link CapturedGhost}), carried to and from the item as the
 * {@code paranormalcraft:captured_ghost} component. While loaded it rattles every 10–20 s and counts as a weak ghost
 * for EMF Readers (level 3 at most).
 */
public class FilledGhostTrapBlockEntity extends BlockEntity {

    private static final int MIN_RATTLE_TICKS = Balance.Trap.MIN_RATTLE_TICKS;
    private static final int MAX_RATTLE_TICKS = Balance.Trap.MAX_RATTLE_TICKS;

    private @Nullable CapturedGhost contents;
    private int nextRattle = -1;
    private boolean registered;
    /** An empty-hand release hold in progress, and whose it is. Not saved. */
    private @Nullable ReleaseHold hold;
    private @Nullable UUID holder;

    public FilledGhostTrapBlockEntity(BlockPos pos, BlockState state) {

        super(ModBlocks.FILLED_GHOST_TRAP_ENTITY.get(), pos, state);
    }

    public @Nullable CapturedGhost contents() {

        return this.contents;
    }

    public void setContents(@Nullable CapturedGhost contents) {

        this.contents = contents;
        setChanged();
        // Register now rather than on the first tick, so EMF and the dump see a trap filled this tick.
        if (this.level != null && !this.level.isClientSide() && !this.registered && !this.isRemoved()) {
            FilledTraps.add(this.level, this.worldPosition);
            this.registered = true;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FilledGhostTrapBlockEntity trap) {

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!trap.registered) {
            FilledTraps.add(level, pos);
            trap.registered = true;
        }
        if (trap.nextRattle < 0) {
            trap.nextRattle = MIN_RATTLE_TICKS + serverLevel.getRandom().nextInt(MAX_RATTLE_TICKS - MIN_RATTLE_TICKS + 1);
        }
        if (--trap.nextRattle <= 0) {
            trap.nextRattle = -1;
            serverLevel.playSound(null, pos, ModSounds.TRAP_RATTLE.get(), SoundSource.BLOCKS, 1.0F, 0.9F + serverLevel.getRandom().nextFloat() * 0.3F);
            serverLevel.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.35, pos.getZ() + 0.5, 4, 0.2, 0.05, 0.2, 0.01);
            serverLevel.blockEvent(pos, state.getBlock(), FilledGhostTrapBlock.EVENT_RATTLE, 0);
        }
    }

    /**
     * One repeat of {@code player} holding use on this trap with an empty hand. The rattle speeds up and rises in
     * pitch as the hold goes on; once it completes, the ghost is released.
     *
     * @return whether this press released the ghost
     */
    public boolean pressRelease(ServerLevel level, Player player) {

        long now = level.getGameTime();
        if (this.holder != null && !this.holder.equals(player.getUUID()) && this.hold != null && !this.hold.lapsed(now)) {
            return false; // someone else is already holding it
        }
        boolean fresh = this.hold == null || this.hold.lapsed(now) || !player.getUUID().equals(this.holder);
        this.hold = ReleaseHold.press(fresh ? null : this.hold, now);
        this.holder = player.getUUID();
        double progress = this.hold.progress();
        BlockPos pos = this.worldPosition;
        level.playSound(null, pos, ModSounds.TRAP_RATTLE.get(), SoundSource.BLOCKS, 0.6F + 0.4F * (float) progress, 0.8F + 0.8F * (float) progress);
        level.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 1 + (int) (progress * 6), 0.2, 0.1, 0.2, 0.02);
        level.blockEvent(pos, getBlockState().getBlock(), FilledGhostTrapBlock.EVENT_RATTLE, 0);
        if (fresh) {
            ServerTrace.event("trap_release_hold", line -> {
                line.addProperty("trap", pos.toShortString());
                line.addProperty("player", player.getGameProfile().name());
            });
        }
        if (this.hold.complete()) {
            this.hold = null;
            this.holder = null;
            FilledGhostTrapBlock.release(level, pos, this);
            return true;
        }
        return false;
    }

    /**
     * The release hold's progress (0 to 1), or -1 with none in progress.
     */
    public double releaseProgress(long now) {

        return this.hold == null || this.hold.lapsed(now) ? -1 : this.hold.progress();
    }

    @Override
    public void setRemoved() {

        super.setRemoved();
        if (this.level != null && !this.level.isClientSide()) {
            FilledTraps.remove(this.level, this.worldPosition);
        }
        this.registered = false;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {

        super.collectImplicitComponents(components);
        if (this.contents != null) {
            components.set(ModDataComponents.CAPTURED_GHOST.get(), this.contents);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {

        super.applyImplicitComponents(components);
        this.contents = components.get(ModDataComponents.CAPTURED_GHOST.get());
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {

        super.removeComponentsFromTag(output);
        output.discard("Captured");
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        this.contents = input.read("Captured", CapturedGhost.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {

        super.saveAdditional(output);
        if (this.contents != null) {
            output.store("Captured", CapturedGhost.CODEC, this.contents);
        }
    }
}
