package io.github.mdw22.paranormalcraft.tool;

import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What a Spirit Thermometer at a position reads, in °C. The maths is in {@link Temperature}.
 */
public final class ThermometerReadings {

    private ThermometerReadings() {
    }

    /**
     * A reading with readability noise, as the holder sees it.
     */
    public static double read(ServerLevel level, Vec3 pos, RandomSource random) {

        return readWithoutNoise(level, pos) + (random.nextDouble() * 2.0 - 1.0) * Temperature.NOISE;
    }

    public static double readWithoutNoise(ServerLevel level, Vec3 pos) {

        BlockPos blockPos = BlockPos.containing(pos);
        double biomeTemperature = level.getBiome(blockPos).value().getBaseTemperature();
        return Temperature.celsius(biomeTemperature, level.isDarkOutside(), pos.y, level.getSeaLevel(), nearestColdSpotDistance(level, pos), 0.0);
    }

    /**
     * The cold spot part of a reading alone (0 to −10).
     */
    public static double coldSpotOffset(ServerLevel level, Vec3 pos) {

        return Temperature.coldSpotOffset(nearestColdSpotDistance(level, pos));
    }

    /**
     * Distance from {@code pos} to the nearest cold spot centre (the bottom centre of the cold spot block, i.e. where a
     * player standing on it has their feet). Taking the nearest is the same as taking the strongest.
     */
    public static double nearestColdSpotDistance(ServerLevel level, Vec3 pos) {

        double nearest = Double.POSITIVE_INFINITY;
        // Ghosts wander up to their leash radius from home, so search that far beyond the cold spot radius.
        double search = Temperature.COLD_SPOT_RADIUS + GhostEntity.LEASH_RADIUS + 2;
        for (GhostEntity ghost : level.getEntitiesOfClass(GhostEntity.class, new AABB(pos, pos).inflate(search))) {
            BlockPos coldSpot = ghost.getColdSpot();
            if (coldSpot != null && ghost.isAlive()) {
                nearest = Math.min(nearest, Vec3.atBottomCenterOf(coldSpot).distanceTo(pos));
            }
        }
        return nearest;
    }
}
