package io.github.mdw22.paranormalcraft.familiar;

import com.mojang.serialization.Codec;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every light block a Wisp Familiar has placed and not yet removed, saved with the world
 * ({@code data/paranormalcraft_wisp_lights.dat}) the moment it changes. A light comes off this list only once it has
 * been removed from a loaded chunk, so a crash, an unload or a stop can't leave an orphan the list forgets:
 * <ul>
 *     <li>a wisp removes its light when it moves, is recalled, unloads or the server stops;</li>
 *     <li>{@link #sweepLoaded} (every few seconds) removes listed lights no wisp owns once their chunk is loaded;</li>
 *     <li>{@link #sweepAll} at server start loads and clears every listed light.</li>
 * </ul>
 * Only a light block still at {@link WispLight#LEVEL} is removed, so a block the player put there since is kept.
 * <p>
 * Same vanilla-fixer note as {@code CaptureLog}.
 */
public final class WispLights extends SavedData {

    private static final Codec<WispLights> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG.listOf())
            .xmap(WispLights::new, WispLights::toMap)
            .fieldOf("lights").codec();
    public static final SavedDataType<WispLights> TYPE = new SavedDataType<>(Constants.id("wisp_lights"), WispLights::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, Set<Long>> lights = new HashMap<>();

    private WispLights() {
    }

    private WispLights(Map<String, List<Long>> loaded) {

        loaded.forEach((dimension, list) -> this.lights.put(dimension, new HashSet<>(list)));
    }

    private Map<String, List<Long>> toMap() {

        Map<String, List<Long>> map = new HashMap<>();
        this.lights.forEach((dimension, set) -> map.put(dimension, new ArrayList<>(set)));
        return map;
    }

    public static WispLights get(MinecraftServer server) {

        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    private static String key(Level level) {

        return level.dimension().identifier().toString();
    }

    public int count() {

        return this.lights.values().stream().mapToInt(Set::size).sum();
    }

    public List<String> describe() {

        List<String> list = new ArrayList<>();
        this.lights.forEach((dimension, set) -> set.forEach(pos -> list.add(dimension + " " + BlockPos.of(pos).toShortString())));
        return list;
    }

    /**
     * Places a wisp light at {@code pos} if it is air, and lists it. Returns whether it placed one.
     */
    public static boolean place(ServerLevel level, BlockPos pos) {

        BlockState here = level.getBlockState(pos);
        if (!here.isAir() || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        level.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, WispLight.LEVEL), Block.UPDATE_CLIENTS);
        WispLights lights = get(level.getServer());
        lights.lights.computeIfAbsent(key(level), k -> new HashSet<>()).add(pos.asLong());
        lights.setDirty();
        return true;
    }

    /**
     * Removes a listed wisp light. If its chunk isn't loaded it stays listed for a later sweep. Returns whether the
     * light is gone from the list.
     */
    public static boolean remove(ServerLevel level, BlockPos pos) {

        if (!level.isLoaded(pos)) {
            return false;
        }
        if (isWispLight(level.getBlockState(pos))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        WispLights lights = get(level.getServer());
        Set<Long> set = lights.lights.get(key(level));
        if (set != null && set.remove(pos.asLong())) {
            lights.setDirty();
        }
        return true;
    }

    public static boolean isWispLight(BlockState state) {

        return state.is(Blocks.LIGHT) && state.getValue(LightBlock.LEVEL) == WispLight.LEVEL;
    }

    /**
     * Removes listed lights that no live wisp owns, in loaded chunks only.
     */
    public static int sweepLoaded(MinecraftServer server, Set<String> owned) {

        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            Set<Long> set = get(server).lights.get(key(level));
            if (set == null) {
                continue;
            }
            for (Long packed : List.copyOf(set)) {
                BlockPos pos = BlockPos.of(packed);
                if (!owned.contains(key(level) + " " + packed) && level.isLoaded(pos) && remove(level, pos)) {
                    removed++;
                }
            }
        }
        return removed;
    }

    /**
     * Server start: every listed light is an orphan (wisps are never saved), so each is loaded and removed.
     */
    public static int sweepAll(MinecraftServer server) {

        WispLights lights = get(server);
        int listed = lights.count();
        int removed = 0;
        for (Map.Entry<String, Set<Long>> entry : Map.copyOf(lights.lights).entrySet()) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(entry.getKey())));
            for (Long packed : List.copyOf(entry.getValue())) {
                BlockPos pos = BlockPos.of(packed);
                if (level != null) {
                    // getBlockState loads the chunk, so the light is checked even far from any player.
                    if (isWispLight(level.getBlockState(pos))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        removed++;
                    }
                }
            }
        }
        lights.lights.clear();
        lights.setDirty();
        int found = removed;
        ServerTrace.event("wisp_light_sweep", line -> {
            line.addProperty("listed", listed);
            line.addProperty("removed", found);
        });
        if (listed > 0) {
            Constants.LOG.info("Wisp light sweep: {} listed, {} orphan light blocks removed", listed, found);
        }
        return removed;
    }
}
