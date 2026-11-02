package io.github.mdw22.paranormalcraft.capture;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.journal.CaptureLog;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Filled Trap decay in the world (the maths is {@link TrapIntegrity}). Each place a filled trap can be has its check:
 * <ul>
 *     <li>placed: {@code FilledGhostTrapBlockEntity}'s tick;</li>
 *     <li>in an inventory: {@code FilledGhostTrapItem.inventoryTick};</li>
 *     <li>in a container, or lying on the ground: {@link #tick}, which sweeps the loaded chunks round each player
 *     ({@link #SCAN_RADIUS} chunks), {@link #SCAN_CHUNKS_PER_TICK} chunks a tick, so a trap in a chest is caught soon
 *     after its chunk is loaded near a player.</li>
 * </ul>
 * At 0% the ghost escapes ({@link #escape}): next to the trap, Agitated and visible, then fleeing from the nearest
 * player and following them; the trap is left empty.
 */
public final class TrapDecay {

    public static final int SCAN_CHUNKS_PER_TICK = Balance.Escape.SCAN_CHUNKS_PER_TICK;
    public static final int SCAN_RADIUS = Balance.Escape.SCAN_RADIUS_CHUNKS;
    private static final int SIDE = 2 * SCAN_RADIUS + 1;
    /** Per player, the next chunk of their square to scan. */
    private static final Map<UUID, Integer> CURSORS = new HashMap<>();

    private TrapDecay() {
    }

    /** A fresh seal, from the server config's lifetime. */
    public static TrapSeal freshSeal(ServerLevel level) {

        return TrapIntegrity.fresh(level.getGameTime(), ModConfigs.server().trapIntegrityMinutes());
    }

    /** The stack's seal, adding a fresh one if it has none (traps from before Update 12). */
    public static TrapSeal sealOf(ItemStack stack, ServerLevel level) {

        TrapSeal seal = stack.get(ModDataComponents.TRAP_SEAL.get());
        if (seal == null) {
            seal = freshSeal(level);
            stack.set(ModDataComponents.TRAP_SEAL.get(), seal);
        }
        return seal;
    }

    public static boolean isFilledTrap(ItemStack stack) {

        return stack.is(ModItems.FILLED_GHOST_TRAP.get()) && stack.has(ModDataComponents.CAPTURED_GHOST.get());
    }

    /**
     * The ghost bursts out at {@code at}: Agitated, homed there, attached to the nearest player. The caller replaces
     * the trap with an empty one. {@code where} is for the trace ({@code placed}, {@code inventory}, {@code container},
     * {@code ground}).
     */
    public static void escape(ServerLevel level, CapturedGhost captured, Vec3 at, String where) {

        BlockPos home = BlockPos.containing(at);
        level.playSound(null, at.x, at.y, at.z, ModSounds.TRAP_BURST.get(), SoundSource.BLOCKS, Balance.Escape.BURST_VOLUME, 1.0F);
        LooseGhosts.burst(level, at);
        CaptureLog.get(level.getServer()).setOutcome(captured.id(), "escaped");
        var ghost = LooseGhosts.escape(level, captured, at.add(0.0, 0.5, 0.0), home, "trap_escape");
        ServerTrace.event("trap_escape", line -> {
            line.addProperty("name", captured.name());
            line.addProperty("where", where);
            line.addProperty("pos", home.toShortString());
            line.addProperty("ghost", ghost == null ? "none" : ghost.getUUID().toString());
            line.addProperty("attachedTo", ghost == null || ghost.attachment().player() == null ? "none" : ghost.attachment().playerName());
        });
    }

    /**
     * Checks one stack outside the world (a container slot or the ground). Returns the empty trap to put in its place
     * if the ghost escaped, otherwise null; may rattle.
     *
     * @param chanceScale how many ticks this check stands for (its rattle chance is per tick)
     */
    public static @Nullable ItemStack check(ServerLevel level, ItemStack stack, Vec3 at, String where, double chanceScale) {

        if (!isFilledTrap(stack)) {
            return null;
        }
        TrapSeal seal = sealOf(stack, level);
        float integrity = TrapIntegrity.integrity(seal, level.getGameTime());
        if (integrity <= 0.0F) {
            CapturedGhost captured = stack.get(ModDataComponents.CAPTURED_GHOST.get());
            if (captured != null) {
                escape(level, captured, at, where);
            }
            return new ItemStack(ModItems.GHOST_TRAP.get(), stack.getCount());
        }
        TrapIntegrity.Rattle rattle = TrapIntegrity.rattle(integrity, false);
        if (level.getRandom().nextDouble() < rattle.chancePerTick() * chanceScale) {
            rattleAt(level, at, rattle, integrity);
        }
        return null;
    }

    public static void rattleAt(ServerLevel level, Vec3 at, TrapIntegrity.Rattle rattle, float integrity) {

        level.playSound(null, at.x, at.y, at.z, ModSounds.TRAP_RATTLE.get(), SoundSource.BLOCKS, rattle.volume(), 0.9F + level.getRandom().nextFloat() * 0.3F);
        if (TrapIntegrity.creaks(integrity)) {
            level.playSound(null, at.x, at.y, at.z, ModSounds.TRAP_CREAK.get(), SoundSource.BLOCKS, Balance.Escape.CREAK_VOLUME, 0.8F);
        }
    }

    /** From {@code CommonEvents.onServerTick}: the container and ground sweep. */
    public static void tick(MinecraftServer server) {

        int budget = SCAN_CHUNKS_PER_TICK;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }
        int perPlayer = Math.max(1, budget / players.size());
        // Each chunk is visited about once per this many ticks, which the carried rattle chance makes up for.
        double period = (double) SIDE * SIDE / perPlayer;
        for (ServerPlayer player : players) {
            ServerLevel level = player.level();
            int cursor = CURSORS.getOrDefault(player.getUUID(), 0);
            for (int i = 0; i < perPlayer; i++) {
                int index = (cursor + i) % (SIDE * SIDE);
                int cx = (player.getBlockX() >> 4) + index % SIDE - SCAN_RADIUS;
                int cz = (player.getBlockZ() >> 4) + index / SIDE - SCAN_RADIUS;
                scanChunk(level, cx, cz, period);
            }
            CURSORS.put(player.getUUID(), (cursor + perPlayer) % (SIDE * SIDE));
        }
    }

    /**
     * Checks every container and dropped item in one loaded chunk (does nothing if it isn't loaded).
     */
    public static void scanChunk(ServerLevel level, int cx, int cz, double chanceScale) {

        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) {
            return;
        }
        for (BlockEntity blockEntity : List.copyOf(chunk.getBlockEntities().values())) {
            if (!(blockEntity instanceof Container container) || blockEntity.isRemoved()) {
                continue;
            }
            Vec3 at = Vec3.atCenterOf(blockEntity.getBlockPos().above());
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack replacement = check(level, container.getItem(slot), at, "container", chanceScale);
                if (replacement != null) {
                    container.setItem(slot, replacement);
                    container.setChanged();
                }
            }
        }
        AABB box = new AABB(cx << 4, level.getMinY(), cz << 4, (cx << 4) + 16, level.getMaxY(), (cz << 4) + 16);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box, entity -> isFilledTrap(entity.getItem()))) {
            ItemStack replacement = check(level, item.getItem(), item.position(), "ground", chanceScale);
            if (replacement != null) {
                item.setItem(replacement);
            }
        }
    }

    public static void clear() {

        CURSORS.clear();
    }
}
