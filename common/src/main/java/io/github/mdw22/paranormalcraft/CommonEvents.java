package io.github.mdw22.paranormalcraft;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Loader-neutral game event handlers. Each loader forwards its own events here, server side only,
 * so gameplay code never touches NeoForge or Fabric event types.
 */
public class CommonEvents {

    /**
     * Called at the end of every server tick.
     */
    public static void onServerTick(MinecraftServer server) {
    }

    /**
     * Called once per tick for every player on the server, after the player has ticked.
     */
    public static void onPlayerTick(ServerPlayer player) {
    }

    /**
     * Called when an entity is added to a server level, including when it is loaded from disk.
     */
    public static void onEntityJoinLevel(Entity entity, ServerLevel level) {
    }
}
