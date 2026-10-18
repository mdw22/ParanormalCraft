package io.github.mdw22.paranormalcraft.debug;

import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.network.RevealFlagPayload;
import io.github.mdw22.paranormalcraft.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The {@code /paranormalcraft reveal} debug flag. The server keeps the set of revealed players and tells each player
 * their own flag; nothing is saved, so a restart clears it.
 */
public final class RevealDebug {

    private static final int RING_INTERVAL_TICKS = 10;
    private static final int RING_POINTS = 48;
    private static final double RING_VIEW_DISTANCE = 64.0;

    private static final Set<UUID> SERVER_REVEALED = new HashSet<>();
    private static boolean clientRevealed;

    private RevealDebug() {
    }

    public static boolean isRevealed(Player player) {

        return player.level().isClientSide() ? clientRevealed : SERVER_REVEALED.contains(player.getUUID());
    }

    /**
     * @return the new state
     */
    public static boolean toggle(ServerPlayer player) {

        UUID id = player.getUUID();
        boolean revealed = !SERVER_REVEALED.remove(id);
        if (revealed) {
            SERVER_REVEALED.add(id);
        }
        sync(player);
        return revealed;
    }

    /**
     * Sends the player's flag. Called on toggle and whenever the player joins a level, so a relogged client is never
     * out of step with the server.
     */
    public static void sync(ServerPlayer player) {

        Services.NETWORK.sendToPlayer(player, new RevealFlagPayload(SERVER_REVEALED.contains(player.getUUID())));
    }

    public static void handleClientFlag(RevealFlagPayload payload) {

        clientRevealed = payload.revealed();
    }

    public static void clear() {

        SERVER_REVEALED.clear();
    }

    /**
     * Draws each nearby ghost's wander and leash radius as particle rings, for revealed players only.
     */
    public static void tick(MinecraftServer server) {

        if (SERVER_REVEALED.isEmpty() || server.getTickCount() % RING_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!SERVER_REVEALED.contains(player.getUUID())) {
                continue;
            }
            ServerLevel level = player.level();
            for (GhostEntity ghost : level.getEntitiesOfClass(GhostEntity.class, player.getBoundingBox().inflate(RING_VIEW_DISTANCE))) {
                BlockPos home = ghost.getHome();
                if (home != null) {
                    drawRing(level, player, home, GhostEntity.WANDER_RADIUS, ParticleTypes.SOUL_FIRE_FLAME);
                    drawRing(level, player, home, GhostEntity.LEASH_RADIUS, ParticleTypes.SMALL_FLAME);
                    level.sendParticles(player, ParticleTypes.END_ROD, true, true, home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    private static void drawRing(ServerLevel level, ServerPlayer player, BlockPos centre, int radius, SimpleParticleType particle) {

        for (int i = 0; i < RING_POINTS; i++) {
            float angle = i * Mth.TWO_PI / RING_POINTS;
            double x = centre.getX() + 0.5 + Mth.cos(angle) * radius;
            double z = centre.getZ() + 0.5 + Mth.sin(angle) * radius;
            level.sendParticles(player, particle, true, true, x, centre.getY() + 0.5, z, 1, 0, 0, 0, 0);
        }
    }
}
