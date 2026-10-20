package io.github.mdw22.paranormalcraft.world;

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
 * Where loaded blocks of one kind are, per dimension, so checks (ward radius, debug dump) never scan for blocks. The
 * block entity adds itself when it gets its level (loaded or placed) and removes itself when removed (broken or
 * unloaded). Server thread only; cleared on server stop.
 */
public final class LoadedPositions {

    /** Containment Units. */
    public static final LoadedPositions CONTAINMENT_UNITS = new LoadedPositions();
    /** Ward Lanterns. */
    public static final LoadedPositions WARD_LANTERNS = new LoadedPositions();
    /** Ward Lanterns the client has loaded (the boundary preview); cleared when the client leaves a level. */
    public static final LoadedPositions WARD_LANTERNS_CLIENT = new LoadedPositions();

    private final Map<ResourceKey<Level>, Set<BlockPos>> loaded = new HashMap<>();

    private LoadedPositions() {
    }

    public void add(Level level, BlockPos pos) {

        this.loaded.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(pos.immutable());
    }

    public void remove(Level level, BlockPos pos) {

        Set<BlockPos> set = this.loaded.get(level.dimension());
        if (set != null) {
            set.remove(pos);
        }
    }

    /**
     * Loaded positions within {@code radius} of {@code centre} (block centres).
     */
    public List<BlockPos> near(Level level, Vec3 centre, double radius) {

        double radiusSqr = radius * radius;
        return this.loaded.getOrDefault(level.dimension(), Set.of()).stream()
                .filter(pos -> Vec3.atCenterOf(pos).distanceToSqr(centre) <= radiusSqr)
                .toList();
    }

    /**
     * Whether any loaded position is within {@code radius} of {@code centre}.
     */
    public boolean anyWithin(Level level, Vec3 centre, double radius) {

        double radiusSqr = radius * radius;
        for (BlockPos pos : this.loaded.getOrDefault(level.dimension(), Set.of())) {
            if (Vec3.atCenterOf(pos).distanceToSqr(centre) <= radiusSqr) {
                return true;
            }
        }
        return false;
    }

    public void clear() {

        this.loaded.clear();
    }

    /**
     * Clears every registry in this class (server stop).
     */
    public static void clearAll() {

        CONTAINMENT_UNITS.clear();
        WARD_LANTERNS.clear();
    }
}
