package io.github.mdw22.paranormalcraft.debug;

import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.tool.Temperature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code /paranormalcraft debug coldspot}: draws a ghost's cold spot (centre column plus the 4-block falloff ring)
 * with particles for {@link #DURATION_TICKS}.
 */
public final class ColdSpotDisplay {

    public static final int DURATION_TICKS = 200;
    private static final int DRAW_INTERVAL_TICKS = 5;
    private static final int RING_POINTS = 32;

    // Ghost -> server tick count when its display ends. Server thread only.
    private static final Map<GhostEntity, Integer> SHOWING = new LinkedHashMap<>();

    private ColdSpotDisplay() {
    }

    public static void show(GhostEntity ghost, MinecraftServer server) {

        SHOWING.put(ghost, server.getTickCount() + DURATION_TICKS);
    }

    public static void tick(MinecraftServer server) {

        if (SHOWING.isEmpty() || server.getTickCount() % DRAW_INTERVAL_TICKS != 0) {
            return;
        }
        Iterator<Map.Entry<GhostEntity, Integer>> entries = SHOWING.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<GhostEntity, Integer> entry = entries.next();
            GhostEntity ghost = entry.getKey();
            BlockPos coldSpot = ghost.getColdSpot();
            if (server.getTickCount() > entry.getValue() || ghost.isRemoved() || coldSpot == null || !(ghost.level() instanceof ServerLevel level)) {
                entries.remove();
                continue;
            }
            double x = coldSpot.getX() + 0.5;
            double y = coldSpot.getY();
            double z = coldSpot.getZ() + 0.5;
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 1.0, z, 12, 0.05, 0.6, 0.05, 0.0);
            for (int i = 0; i < RING_POINTS; i++) {
                float angle = i * Mth.TWO_PI / RING_POINTS;
                level.sendParticles(ParticleTypes.SNOWFLAKE, x + Mth.cos(angle) * Temperature.COLD_SPOT_RADIUS, y + 0.2, z + Mth.sin(angle) * Temperature.COLD_SPOT_RADIUS, 1, 0, 0, 0, 0);
            }
        }
    }

    public static void clear() {

        SHOWING.clear();
    }
}
