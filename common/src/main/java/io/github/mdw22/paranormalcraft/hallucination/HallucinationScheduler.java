package io.github.mdw22.paranormalcraft.hallucination;

import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.config.ServerConfig;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.network.HallucinationPayload;
import io.github.mdw22.paranormalcraft.platform.Services;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server side: each player has a hidden timer ({@code hallucinationIntervalSeconds}). When it fires and the player is
 * within {@link #GHOST_RANGE} of a ghost at block light {@link #MAX_BLOCK_LIGHT} or less, that player alone is sent a
 * shadow figure. Otherwise it retries every {@link #RETRY_TICKS}. Not saved.
 */
public final class HallucinationScheduler {

    public static final double GHOST_RANGE = 24.0;
    public static final int MAX_BLOCK_LIGHT = 7;
    public static final int RETRY_TICKS = 200;

    // Player UUID -> game time of their next hallucination attempt. Server thread only.
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    private HallucinationScheduler() {
    }

    public static void tick(ServerPlayer player) {

        long now = player.level().getGameTime();
        Long next = NEXT.get(player.getUUID());
        if (next == null) {
            NEXT.put(player.getUUID(), now + rollInterval(player));
            return;
        }
        if (now < next || player.isSpectator()) {
            return;
        }
        if (conditionsMet(player)) {
            send(player);
            NEXT.put(player.getUUID(), now + rollInterval(player));
        } else {
            NEXT.put(player.getUUID(), now + RETRY_TICKS);
        }
    }

    public static boolean conditionsMet(ServerPlayer player) {

        boolean ghostNearby = !player.level().getEntitiesOfClass(GhostEntity.class, player.getBoundingBox().inflate(GHOST_RANGE),
                ghost -> ghost.isAlive() && ghost.distanceToSqr(player) <= GHOST_RANGE * GHOST_RANGE).isEmpty();
        return ghostNearby && player.level().getBrightness(LightLayer.BLOCK, player.blockPosition()) <= MAX_BLOCK_LIGHT;
    }

    /**
     * Sends a shadow figure to this player only (also used by {@code debug hallucinate}, which skips the checks).
     */
    public static void send(ServerPlayer player) {

        Services.NETWORK.sendToPlayer(player, new HallucinationPayload(HallucinationKind.SHADOW_FIGURE, player.getRandom().nextLong()));
    }

    /**
     * Ticks until this player's next attempt, or -1 if not scheduled yet. For debug output.
     */
    public static long ticksUntilNext(ServerPlayer player) {

        Long next = NEXT.get(player.getUUID());
        return next == null ? -1 : Math.max(0, next - player.level().getGameTime());
    }

    private static long rollInterval(ServerPlayer player) {

        ServerConfig config = ModConfigs.server();
        int min = config.hallucinationIntervalMinSeconds() * 20;
        int max = config.hallucinationIntervalMaxSeconds() * 20;
        return min + player.getRandom().nextInt(max - min + 1);
    }

    public static void clear() {

        NEXT.clear();
    }
}
