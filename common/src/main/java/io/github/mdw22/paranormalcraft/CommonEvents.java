package io.github.mdw22.paranormalcraft;

import com.mojang.brigadier.CommandDispatcher;
import io.github.mdw22.paranormalcraft.command.ModCommands;
import io.github.mdw22.paranormalcraft.debug.ColdSpotDisplay;
import io.github.mdw22.paranormalcraft.debug.RevealDebug;
import io.github.mdw22.paranormalcraft.entity.ghost.goal.GhostCuriousGoal;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationScheduler;
import io.github.mdw22.paranormalcraft.haunting.HauntingScheduler;
import io.github.mdw22.paranormalcraft.tool.ToolTracker;
import net.minecraft.commands.CommandSourceStack;
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

        RevealDebug.tick(server);
        ColdSpotDisplay.tick(server);
        HauntingScheduler.tick(server);
    }

    /**
     * Called once per tick for every player on the server, after the player has ticked.
     */
    public static void onPlayerTick(ServerPlayer player) {

        ToolTracker.tick(player);
        HallucinationScheduler.tick(player);
    }

    /**
     * Called when an entity is added to a server level, including when it is loaded from disk.
     */
    public static void onEntityJoinLevel(Entity entity, ServerLevel level) {

        if (entity instanceof ServerPlayer player) {
            RevealDebug.sync(player);
        }
    }

    /**
     * Called when the server registers its commands.
     */
    public static void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher) {

        ModCommands.register(dispatcher);
    }

    /**
     * Called after the server has stopped, so per-server state doesn't leak into the next world.
     */
    public static void onServerStopped(MinecraftServer server) {

        RevealDebug.clear();
        GhostCuriousGoal.clearClaims();
        ToolTracker.clear();
        ColdSpotDisplay.clear();
        HauntingScheduler.clear();
        HallucinationScheduler.clear();
    }
}
