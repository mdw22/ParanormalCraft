package io.github.mdw22.paranormalcraft.ward;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.haunting.HauntingScheduler;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * What a Ward Lantern shows when it's working (Update 9 feedback: "make it visible and felt"). Server side; every
 * effect is particles and sounds that all nearby players see.
 * <ul>
 *     <li>{@link #placementRing}: a ring of soul-cyan motes traces the ward's radius once, over 3 s.</li>
 *     <li>{@link #flare}: a ghost inside tried to haunt; the lantern flares (light 15, a chime), a thread of cyan runs
 *     from the ghost to the lantern and the ghost recoils about 2 blocks. Once per lantern per 3 s.</li>
 *     <li>{@link #deniedSpawnPuff}: a natural monster spawn was refused; a small smoke-and-soul puff there. Once per
 *     lantern per 2 s, only near a player.</li>
 * </ul>
 */
public final class WardEffects {

    public static final DustParticleOptions MOTE = new DustParticleOptions(0x6FE7F0, 1.0F);
    private static final DustParticleOptions THREAD = new DustParticleOptions(0x9CF2F2, 0.6F);
    private static final Cooldowns<BlockPos> FLARES = new Cooldowns<>();
    private static final Cooldowns<BlockPos> PUFFS = new Cooldowns<>();

    private WardEffects() {
    }

    /**
     * Traces the ward's boundary once: each tick for {@link Balance.Ward#RING_TICKS} draws the next arc, about two motes
     * per block, on the ground (or at the lantern's height where the ground is far off).
     */
    public static void placementRing(ServerLevel level, BlockPos lantern, int radius) {

        int ticks = Balance.Ward.RING_TICKS;
        int points = Math.max(48, (int) Math.round(4 * Math.PI * radius));
        for (int tick = 0; tick < ticks; tick++) {
            int from = points * tick / ticks;
            int to = points * (tick + 1) / ticks;
            HauntingScheduler.schedule(level, tick, () -> {
                for (int i = from; i < to; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = lantern.getX() + 0.5 + Math.cos(angle) * radius;
                    double z = lantern.getZ() + 0.5 + Math.sin(angle) * radius;
                    double y = groundY(level, x, z, lantern.getY());
                    // Glow motes are full-bright cyan, so the ring reads at night; a dust mote under each for colour by day.
                    level.sendParticles(ParticleTypes.GLOW, true, false, x, y + 0.25, z, 1, 0.05, 0.1, 0.05, 0.0);
                    level.sendParticles(MOTE, true, false, x, y + 0.2, z, 1, 0.05, 0.1, 0.05, 0.0);
                }
            });
        }
        ServerTrace.event("ward_ring", line -> {
            line.addProperty("pos", lantern.toShortString());
            line.addProperty("radius", radius);
        });
    }

    /**
     * The ground at (x, z) if it's within 8 blocks of {@code nearY}, otherwise {@code nearY}.
     */
    public static double groundY(ServerLevel level, double x, double z, int nearY) {

        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        return Math.abs(top - nearY) <= 8 ? top : nearY;
    }

    /**
     * The ward caught a ghost trying to haunt. Rate-limited per lantern.
     *
     * @return whether it flared (false while on cooldown)
     */
    public static boolean flare(ServerLevel level, BlockPos lantern, GhostEntity ghost) {

        if (!FLARES.tryUse(lantern, level.getGameTime(), Balance.Ward.FLARE_COOLDOWN_TICKS)) {
            return false;
        }
        BlockState state = level.getBlockState(lantern);
        if (state.getBlock() instanceof WardLanternBlock) {
            level.setBlock(lantern, state.setValue(WardLanternBlock.FLARE, true), Block.UPDATE_ALL);
            level.scheduleTick(lantern, state.getBlock(), Balance.Ward.FLARE_TICKS);
        }
        level.playSound(null, lantern, ModSounds.WARD_FLARE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        Vec3 from = ghost.getBoundingBox().getCenter();
        Vec3 to = Vec3.atCenterOf(lantern);
        int steps = Math.max(2, (int) Math.ceil(from.distanceTo(to) * 3));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.lerp(to, (double) i / steps);
            level.sendParticles(THREAD, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        Vec3 away = from.subtract(to).multiply(1, 0, 1);
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
        // The ghost's drift keeps half its velocity each tick, so a push of d/2 carries it about d.
        ghost.setDeltaMovement(away.scale(Balance.Ward.RECOIL_DISTANCE / 2.0).add(0, 0.1, 0));
        ghost.hurtMarked = true;
        ServerTrace.ghost("ward_flare", ghost, line -> line.addProperty("lantern", lantern.toShortString()));
        return true;
    }

    /**
     * A natural monster spawn at {@code at} was refused by the ward at {@code lantern}.
     */
    public static void deniedSpawnPuff(ServerLevel level, BlockPos lantern, BlockPos at) {

        double range = Balance.Ward.PUFF_PLAYER_RANGE;
        if (level.getNearestPlayer(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, range, false) == null) {
            return;
        }
        if (!PUFFS.tryUse(lantern, level.getGameTime(), Balance.Ward.PUFF_COOLDOWN_TICKS)) {
            return;
        }
        level.sendParticles(ParticleTypes.SMOKE, at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.01);
        level.sendParticles(ParticleTypes.SOUL, at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, 2, 0.15, 0.2, 0.15, 0.01);
        ServerTrace.event("ward_denied_spawn", line -> {
            line.addProperty("pos", at.toShortString());
            line.addProperty("lantern", lantern.toShortString());
        });
    }

    public static void clear() {

        FLARES.clear();
        PUFFS.clear();
    }
}
