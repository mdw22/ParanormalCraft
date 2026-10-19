package io.github.mdw22.paranormalcraft.block;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.capture.CaptureRules;
import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.capture.TetherManager;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.registry.ModBlocks;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import io.github.mdw22.paranormalcraft.world.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.DoubleSupplier;

/**
 * An armed trap's capture logic (server). When a Weakened ghost is in the zone above it (3 high, 1 block out), the
 * trap opens and pulls it down over {@link CaptureRules#CAPTURE_TICKS}, soul particles streaming in while the ghost
 * struggles. The capture succeeds if a tether held the ghost for the whole pull, otherwise on a coin flip
 * ({@link CaptureRules#UNTETHERED_CHANCE}); on success the ghost is stored and the block becomes a Filled Ghost Trap,
 * on failure the ghost breaks free at 8 Presence and the trap closes, still armed.
 */
public class GhostTrapBlockEntity extends BlockEntity {

    private static final int SCAN_INTERVAL = 2;
    /** GameTests set this to fix the no-tether roll; null means roll for real. */
    private @Nullable DoubleSupplier rollOverride;

    private @Nullable UUID capturing;
    private int captureTicks;
    private boolean tetheredThroughout;

    public GhostTrapBlockEntity(BlockPos pos, BlockState state) {

        super(ModBlocks.GHOST_TRAP_ENTITY.get(), pos, state);
    }

    /**
     * Tests only: the value this trap's captures roll against {@link CaptureRules#UNTETHERED_CHANCE}.
     */
    public void setRollOverride(@Nullable DoubleSupplier roll) {

        this.rollOverride = roll;
    }

    public boolean isCapturing() {

        return this.capturing != null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GhostTrapBlockEntity trap) {

        if (level instanceof ServerLevel serverLevel) {
            trap.tick(serverLevel, pos, state);
        }
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {

        if (this.capturing == null) {
            if (level.getGameTime() % SCAN_INTERVAL == 0) {
                GhostEntity ghost = findWeakenedGhost(level, pos);
                if (ghost != null) {
                    start(level, pos, state, ghost);
                }
            }
            return;
        }
        Entity entity = level.getEntity(this.capturing);
        if (!(entity instanceof GhostEntity ghost) || !ghost.isAlive()) {
            reset(level, pos, state);
            return;
        }
        this.captureTicks++;
        this.tetheredThroughout &= ghost.tetherCount() > 0;
        pull(level, pos, ghost);
        if (this.captureTicks >= CaptureRules.CAPTURE_TICKS) {
            double roll = this.rollOverride != null ? this.rollOverride.getAsDouble() : level.getRandom().nextDouble();
            if (CaptureRules.succeeds(this.tetheredThroughout, roll)) {
                capture(level, pos, ghost, roll);
            } else {
                fail(level, pos, state, ghost, roll);
            }
        }
    }

    private static @Nullable GhostEntity findWeakenedGhost(ServerLevel level, BlockPos pos) {

        Vec3 base = Vec3.atBottomCenterOf(pos);
        AABB zone = new AABB(base, base).inflate(CaptureRules.ZONE_HALF_WIDTH, 0, CaptureRules.ZONE_HALF_WIDTH)
                .expandTowards(0, CaptureRules.ZONE_HEIGHT, 0).inflate(0, 0.5, 0);
        for (GhostEntity ghost : level.getEntitiesOfClass(GhostEntity.class, zone)) {
            Vec3 at = ghost.position();
            if (ghost.isAlive() && ghost.isWeakened() && ghost.capturingTrap() == null
                    && CaptureRules.inZone(at.x - base.x, at.y - base.y, at.z - base.z)) {
                return ghost;
            }
        }
        return null;
    }

    private void start(ServerLevel level, BlockPos pos, BlockState state, GhostEntity ghost) {

        this.capturing = ghost.getUUID();
        this.captureTicks = 0;
        this.tetheredThroughout = ghost.tetherCount() > 0;
        ghost.setCapturingTrap(pos);
        level.setBlock(pos, state.setValue(GhostTrapBlock.OPEN, true), Block.UPDATE_ALL);
        level.playSound(null, pos, ModSounds.TRAP_VACUUM.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        ServerTrace.ghost("trap_capture_start", ghost, line -> {
            line.addProperty("trap", pos.toShortString());
            line.addProperty("tethers", ghost.tetherCount());
        });
        setChanged();
    }

    /**
     * Drags the ghost toward the trap, with a struggle, and streams soul particles in.
     */
    private void pull(ServerLevel level, BlockPos pos, GhostEntity ghost) {

        Vec3 target = Vec3.atBottomCenterOf(pos).add(0, 0.1, 0);
        int left = Math.max(1, CaptureRules.CAPTURE_TICKS - this.captureTicks + 1);
        Vec3 next = ghost.position().add(target.subtract(ghost.position()).scale(1.0 / left));
        double struggle = 0.12;
        next = next.add((level.getRandom().nextDouble() - 0.5) * struggle, 0, (level.getRandom().nextDouble() - 0.5) * struggle);
        ghost.setPos(next.x, next.y, next.z);
        ghost.setDeltaMovement(Vec3.ZERO);
        Vec3 centre = ghost.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.SOUL, centre.x, centre.y, centre.z, 3, 0.2, 0.3, 0.2, 0.01);
        level.sendParticles(ParticleTypes.SCULK_SOUL, target.x, target.y + 0.3, target.z, 1, 0.1, 0.1, 0.1, 0.02);
    }

    private void capture(ServerLevel level, BlockPos pos, GhostEntity ghost, double roll) {

        // Released first, so the saved ghost doesn't keep the trap's no-AI hold.
        ghost.setCapturingTrap(null);
        CapturedGhost captured = describe(level, ghost);
        ServerTrace.ghost("trap_capture_success", ghost, line -> {
            line.addProperty("trap", pos.toShortString());
            line.addProperty("name", captured.name());
            line.addProperty("captor", captured.captor());
            line.addProperty("tetheredThroughout", this.tetheredThroughout);
            line.addProperty("roll", Math.round(roll * 1000.0) / 1000.0);
        });
        ghost.discard();
        level.playSound(null, pos, ModSounds.TRAP_CAPTURE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 12, 0.2, 0.1, 0.2, 0.03);
        this.capturing = null;
        level.setBlock(pos, ModBlocks.FILLED_GHOST_TRAP.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof FilledGhostTrapBlockEntity filled) {
            filled.setContents(captured);
        } else {
            Constants.LOG.error("Filled trap at {} has no block entity; {} is lost", pos.toShortString(), captured.name());
        }
    }

    private void fail(ServerLevel level, BlockPos pos, BlockState state, GhostEntity ghost, double roll) {

        ghost.breakFree(level);
        level.playSound(null, pos, ModSounds.TRAP_BREAK_FREE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        ServerTrace.ghost("trap_capture_fail", ghost, line -> {
            line.addProperty("trap", pos.toShortString());
            line.addProperty("tetheredThroughout", this.tetheredThroughout);
            line.addProperty("roll", Math.round(roll * 1000.0) / 1000.0);
        });
        reset(level, pos, state);
    }

    private void reset(ServerLevel level, BlockPos pos, BlockState state) {

        this.capturing = null;
        this.captureTicks = 0;
        if (state.getValue(GhostTrapBlock.OPEN)) {
            level.setBlock(pos, state.setValue(GhostTrapBlock.OPEN, false), Block.UPDATE_ALL);
        }
        setChanged();
    }

    /**
     * Everything the filled trap keeps about the ghost.
     */
    static CapturedGhost describe(ServerLevel level, GhostEntity ghost) {

        CompoundTag data;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Constants.LOG)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, level.registryAccess());
            ghost.save(output);
            data = output.buildResult();
        }
        List<String> holders = TetherManager.holdersOf(ghost);
        String captor = holders.isEmpty() ? nearestPlayer(level, ghost) : holders.getFirst();
        Optional<String> structure = level.structureManager().getStructureWithPieceAt(ghost.homeOrHere(), ModStructures.ABANDONED_CABIN).isValid()
                ? Optional.of("abandoned_cabin") : Optional.empty();
        return new CapturedGhost(BuiltInRegistries.ENTITY_TYPE.getKey(ghost.getType()).toString(), data, ghost.ghostName(), level.getGameTime(),
                captor, level.dimension().identifier().toString(), structure);
    }

    private static String nearestPlayer(ServerLevel level, GhostEntity ghost) {

        ServerPlayer nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            double distance = player.distanceToSqr(ghost);
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest == null ? "nobody" : nearest.getGameProfile().name();
    }

    /**
     * Broken or replaced mid-capture: let the ghost go (it would otherwise keep the trap's no-AI hold).
     */
    @Override
    public void setRemoved() {

        super.setRemoved();
        if (this.capturing != null && this.level instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.capturing) instanceof GhostEntity ghost && this.worldPosition.equals(ghost.capturingTrap())) {
            ghost.setCapturingTrap(null);
        }
        this.capturing = null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        // A capture in progress isn't saved: after a reload the ghost is simply free again.
        this.capturing = null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {

        super.saveAdditional(output);
    }
}
