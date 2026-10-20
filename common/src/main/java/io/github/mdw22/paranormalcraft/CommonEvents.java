package io.github.mdw22.paranormalcraft;

import com.mojang.brigadier.CommandDispatcher;
import io.github.mdw22.paranormalcraft.capture.FilledTraps;
import io.github.mdw22.paranormalcraft.capture.TetherManager;
import io.github.mdw22.paranormalcraft.command.ModCommands;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.debug.ColdSpotDisplay;
import io.github.mdw22.paranormalcraft.debug.RevealDebug;
import io.github.mdw22.paranormalcraft.debug.ScriptRunner;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.entity.ghost.goal.GhostCuriousGoal;
import io.github.mdw22.paranormalcraft.familiar.Familiars;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationScheduler;
import io.github.mdw22.paranormalcraft.haunting.HauntingScheduler;
import io.github.mdw22.paranormalcraft.journal.JournalEvents;
import io.github.mdw22.paranormalcraft.phase.Phasing;
import io.github.mdw22.paranormalcraft.tool.ToolTracker;
import io.github.mdw22.paranormalcraft.ward.WardEffects;
import io.github.mdw22.paranormalcraft.world.LoadedPositions;
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
        TetherManager.tick(server);
        ScriptRunner.tick(server);
        Familiars.tick(server);
    }

    /**
     * Called once per tick for every player on the server, after the player has ticked.
     */
    public static void onPlayerTick(ServerPlayer player) {

        ToolTracker.tick(player);
        HallucinationScheduler.tick(player);
        JournalEvents.tick(player);
        Phasing.tick(player);
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
     * Called once the server has started and its levels are loaded.
     */
    public static void onServerStarted(MinecraftServer server) {

        Familiars.onServerStarted(server);
    }

    /**
     * Called when the server starts stopping, while levels are still loaded (before the final save).
     */
    public static void onServerStopping(MinecraftServer server) {

        Familiars.onServerStopping(server);
    }

    /**
     * Called when a player leaves the server.
     */
    public static void onPlayerLogout(ServerPlayer player) {

        Familiars.onLogout(player);
        Phasing.end(player, "logout");
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
        ScriptRunner.clear();
        TetherManager.clear();
        FilledTraps.clear();
        LoadedPositions.clearAll();
        WardEffects.clear();
        Familiars.clear();
        Phasing.clear();
        ServerTrace.clear();
        ModConfigs.resetSession();
    }
}
