package io.github.mdw22.paranormalcraft.phase;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.network.PhaseStatePayload;
import io.github.mdw22.paranormalcraft.platform.Services;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import io.github.mdw22.paranormalcraft.registry.ModTags;
import io.github.mdw22.paranormalcraft.ward.WardEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * The Spectral Step's phased state. A phased player's collision with {@link ModTags#PHASE_PASSABLE} blocks is
 * skipped (the {@code EntityCollisionContextMixin}) on both sides: the server keeps the state here by UUID, and each
 * client keeps the entity ids it was told about ({@link PhaseStatePayload}), so movement prediction and the server's
 * move check agree. Solid blocks, fluids and entities are never affected.
 * <p>
 * When the phase runs out inside a passable block it is extended tick by tick until the player is clear, for up to
 * {@link #MAX_EXTEND_TICKS}; if they are still inside, they are nudged to the nearest free spot.
 */
public final class Phasing {

    public static final int PHASE_TICKS = Balance.Phase.PHASE_TICKS;
    public static final int MAX_EXTEND_TICKS = Balance.Phase.MAX_EXTEND_TICKS;
    private static final double SYNC_RANGE = Balance.Phase.SYNC_RANGE;

    /** Server: a phase in progress. */
    public static final class State {

        private final long endTick;
        private int extended;

        State(long endTick) {

            this.endTick = endTick;
        }

        public long endTick() {

            return this.endTick;
        }

        public int extended() {

            return this.extended;
        }
    }

    /** Concurrent: in single player the client thread's collision checks read it too. */
    private static final Map<UUID, State> SERVER = new ConcurrentHashMap<>();
    /** Client: phased entity ids. Written from the network thread's handler on the client thread, read by both. */
    private static final Set<Integer> CLIENT = ConcurrentHashMap.newKeySet();

    private Phasing() {
    }

    /** Cheap check for the collision hook: nobody is phased on either side. */
    public static boolean nobodyPhased() {

        return SERVER.isEmpty() && CLIENT.isEmpty();
    }

    /** Whether {@code entity} is phased, on its own side. */
    public static boolean isPhased(@Nullable Entity entity) {

        if (entity == null) {
            return false;
        }
        return entity.level().isClientSide() ? CLIENT.contains(entity.getId()) : SERVER.containsKey(entity.getUUID());
    }

    /** Whether the phased {@code entity} passes through {@code state}. */
    public static boolean passes(@Nullable Entity entity, BlockState state) {

        return state.is(ModTags.PHASE_PASSABLE) && isPhased(entity);
    }

    public static @Nullable State state(ServerPlayer player) {

        return SERVER.get(player.getUUID());
    }

    /**
     * Starts a phase for {@code player}, whoosh and sync included.
     */
    public static void start(ServerPlayer player) {

        start(player, PHASE_TICKS);
    }

    /**
     * Starts a phase of {@code ticks} (tests use short ones).
     */
    public static void start(ServerPlayer player, int ticks) {

        ServerLevel level = player.level();
        SERVER.put(player.getUUID(), new State(level.getGameTime() + ticks));
        sync(player, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PHASE_IN.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        ServerTrace.event("phase_start", line -> {
            line.addProperty("player", player.getGameProfile().name());
            line.addProperty("ticks", ticks);
            ServerTrace.addPos(line, player);
        });
    }

    /**
     * Ends {@code player}'s phase now, if they have one.
     */
    public static void end(ServerPlayer player, String reason) {

        State state = SERVER.remove(player.getUUID());
        if (state == null) {
            return;
        }
        sync(player, false);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PHASE_OUT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        ServerTrace.event("phase_end", line -> {
            line.addProperty("player", player.getGameProfile().name());
            line.addProperty("reason", reason);
            line.addProperty("extended", state.extended);
            ServerTrace.addPos(line, player);
        });
    }

    /** From {@code CommonEvents.onPlayerTick}. */
    public static void tick(ServerPlayer player) {

        State state = SERVER.get(player.getUUID());
        if (state == null) {
            return;
        }
        ServerLevel level = player.level();
        if (!player.isAlive()) {
            end(player, "death");
            return;
        }
        long now = level.getGameTime();
        if (now % 2 == 0) {
            level.sendParticles(WardEffects.MOTE, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.3, 0.5, 0.3, 0.0);
        }
        if (now < state.endTick) {
            if (now % Balance.Phase.RESYNC_TICKS == 0) {
                sync(player, true);
            }
            return;
        }
        if (!insidePassable(level, player.getBoundingBox())) {
            end(player, state.extended > 0 ? "extended" : "expired");
            return;
        }
        if (state.extended < MAX_EXTEND_TICKS) {
            state.extended++;
            return;
        }
        Vec3 free = freeSpot(level, player);
        if (free != null) {
            player.connection.teleport(free.x, free.y, free.z, player.getYRot(), player.getXRot());
        }
        end(player, free != null ? "nudged" : "stuck");
    }

    /**
     * Whether {@code box} overlaps the real collision shape of a passable block.
     */
    public static boolean insidePassable(ServerLevel level, AABB box) {

        AABB inner = box.deflate(1.0E-4);
        return BlockPos.betweenClosedStream(inner).anyMatch(pos -> {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModTags.PHASE_PASSABLE)) {
                return false;
            }
            // The context-free shape is the real one: the phase hook only answers for an entity context.
            VoxelShape shape = state.getCollisionShape(level, pos);
            return shape.toAabbs().stream().anyMatch(part -> part.move(pos).intersects(inner));
        });
    }

    /**
     * The nearest spot within {@code NUDGE_RADIUS} where the player's box collides with nothing (real shapes).
     */
    public static @Nullable Vec3 freeSpot(ServerLevel level, ServerPlayer player) {

        int radius = Balance.Phase.NUDGE_RADIUS;
        BlockPos origin = player.blockPosition();
        AABB box = player.getBoundingBox();
        return BlockPos.betweenClosedStream(origin.offset(-radius, -1, -radius), origin.offset(radius, radius, radius))
                .map(BlockPos::immutable)
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(origin)))
                .map(pos -> new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))
                .filter(feet -> {
                    AABB moved = box.move(feet.subtract(player.position()));
                    return !level.getBlockCollisions(null, moved).iterator().hasNext() && level.getFluidState(BlockPos.containing(feet)).isEmpty();
                })
                .findFirst().orElse(null);
    }

    private static void sync(ServerPlayer player, boolean phased) {

        PhaseStatePayload payload = new PhaseStatePayload(player.getId(), phased);
        Stream.concat(Stream.of(player), player.level().players().stream()
                        .filter(other -> other != player && other.distanceTo(player) <= SYNC_RANGE))
                .forEach(target -> Services.NETWORK.sendToPlayer(target, payload));
    }

    /** Client handler for {@link PhaseStatePayload}. */
    public static void handleClient(PhaseStatePayload payload) {

        if (payload.phased()) {
            CLIENT.add(payload.entityId());
        } else {
            CLIENT.remove(payload.entityId());
        }
    }

    /** For the debug dump: phased players and their phase. */
    public static Map<UUID, State> serverStates() {

        return Map.copyOf(SERVER);
    }

    /** Client: leaving a world. */
    public static void resetClient() {

        CLIENT.clear();
    }

    public static void clear() {

        SERVER.clear();
    }
}
