package io.github.mdw22.paranormalcraft.capture;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Where loaded Filled Ghost Traps are, per dimension, so EMF Readers and {@code debug dump} can find them without
 * scanning chunks. Filled trap block entities add themselves when loaded and remove themselves when unloaded or broken.
 * Server thread only.
 */
public final class FilledTraps {

    private static final Map<ResourceKey<Level>, Set<BlockPos>> LOADED = new HashMap<>();

    private FilledTraps() {
    }

    public static void add(Level level, BlockPos pos) {

        LOADED.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(pos.immutable());
    }

    public static void remove(Level level, BlockPos pos) {

        Set<BlockPos> set = LOADED.get(level.dimension());
        if (set != null) {
            set.remove(pos);
        }
    }

    /**
     * Loaded filled traps within {@code radius} of {@code centre}.
     */
    public static List<BlockPos> near(Level level, Vec3 centre, double radius) {

        return LOADED.getOrDefault(level.dimension(), Set.of()).stream()
                .filter(pos -> Vec3.atCenterOf(pos).distanceTo(centre) <= radius)
                .toList();
    }

    public static void clear() {

        LOADED.clear();
    }
}
