package io.github.mdw22.paranormalcraft.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mdw22.paranormalcraft.capture.TetherManager;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.debug.DebugKit;
import io.github.mdw22.paranormalcraft.debug.DebugScenes;
import io.github.mdw22.paranormalcraft.debug.ScriptRunner;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationScheduler;
import io.github.mdw22.paranormalcraft.network.DebugClientPayload;
import io.github.mdw22.paranormalcraft.platform.Services;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The testing toolkit under {@code /paranormalcraft debug}: screenshot, trace, scene, kit, look, ghost, tether,
 * timers, pace, config and run. Gated like the other debug commands (op level 2 plus {@code enableDebugCommands}, forced on in
 * development). See the README's "How to test" section.
 */
public final class DebugCommands {

    private static final int DEFAULT_SHOT_WIDTH = 854;
    private static final int DEFAULT_SHOT_HEIGHT = 480;
    private static final int DEFAULT_CLEAR_RADIUS = 64;

    private DebugCommands() {
    }

    /**
     * Adds every toolkit command to the {@code debug} literal.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> addTo(LiteralArgumentBuilder<CommandSourceStack> debug) {

        return debug
                .then(screenshot())
                .then(trace())
                .then(Commands.literal("scene")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(DebugScenes.names(), builder))
                                .executes(DebugCommands::scene)))
                .then(Commands.literal("kit")
                        .executes(context -> kit(context, true))
                        .then(Commands.literal("tools").executes(context -> kit(context, true)))
                        .then(Commands.literal("none").executes(context -> kit(context, false))))
                .then(look())
                .then(ghost())
                .then(timers())
                .then(Commands.literal("tether")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.literal("on").executes(context -> tether(context, true)))
                                .then(Commands.literal("off").executes(context -> tether(context, false)))))
                .then(Commands.literal("pace")
                        .then(Commands.literal("fast").executes(context -> pace(context, ModConfigs.FAST_PACE)))
                        .then(Commands.literal("normal").executes(context -> pace(context, 1.0))))
                .then(config())
                .then(Commands.literal("run")
                        .then(Commands.argument("file", StringArgumentType.greedyString()).executes(DebugCommands::run)));
    }

    private static int ok(CommandContext<CommandSourceStack> context, String message) {

        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> context, String message) {

        context.getSource().sendFailure(Component.literal(message));
        return 0;
    }

    // ---- screenshot ----

    private static LiteralArgumentBuilder<CommandSourceStack> screenshot() {

        return Commands.literal("screenshot")
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> screenshot(context, DEFAULT_SHOT_WIDTH, DEFAULT_SHOT_HEIGHT, null))
                        .then(Commands.argument("width", IntegerArgumentType.integer(16, 7680))
                                .then(Commands.argument("height", IntegerArgumentType.integer(16, 4320))
                                        .executes(context -> screenshot(context, IntegerArgumentType.getInteger(context, "width"),
                                                IntegerArgumentType.getInteger(context, "height"), null))
                                        .then(Commands.argument("player", EntityArgument.players())
                                                .executes(context -> screenshot(context, IntegerArgumentType.getInteger(context, "width"),
                                                        IntegerArgumentType.getInteger(context, "height"), EntityArgument.getPlayers(context, "player")))))));
    }

    private static int screenshot(CommandContext<CommandSourceStack> context, int width, int height, Collection<ServerPlayer> targets) throws CommandSyntaxException {

        Collection<ServerPlayer> players = targets != null ? targets : List.of(context.getSource().getPlayerOrException());
        String name = StringArgumentType.getString(context, "name");
        for (ServerPlayer player : players) {
            Services.NETWORK.sendToPlayer(player, DebugClientPayload.of("screenshot", name, Integer.toString(width), Integer.toString(height)));
        }
        return ok(context, "Asked " + players.size() + " client(s) for screenshot " + name + " (" + width + "x" + height
                + "); each replies with its path in <game dir>/paranormalcraft-shots/");
    }

    // ---- trace ----

    private static LiteralArgumentBuilder<CommandSourceStack> trace() {

        return Commands.literal("trace")
                .then(Commands.literal("start").executes(context -> {
                    try {
                        Path path = ServerTrace.start(context.getSource().getServer());
                        toClient(context, DebugClientPayload.of("trace_start"));
                        return ok(context, "Server trace started: " + path);
                    } catch (IOException e) {
                        return fail(context, "Could not start the trace: " + e.getMessage());
                    }
                }))
                .then(Commands.literal("stop").executes(context -> {
                    toClient(context, DebugClientPayload.of("trace_stop"));
                    Path path = ServerTrace.stop();
                    return path == null ? fail(context, "No server trace was running") : ok(context, "Server trace written: " + path);
                }))
                .then(Commands.literal("mark")
                        .then(Commands.argument("label", StringArgumentType.greedyString()).executes(context -> {
                            String label = StringArgumentType.getString(context, "label");
                            ServerTrace.mark(label);
                            toClient(context, DebugClientPayload.of("trace_mark", label));
                            return ok(context, "Marked: " + label);
                        })))
                .then(Commands.literal("snapshot").executes(context -> {
                    toClient(context, DebugClientPayload.of("trace_snapshot"));
                    return ok(context, "Snapshot requested from the client trace");
                }));
    }

    /**
     * Sends a payload to the player running the command, if it is a player.
     */
    private static void toClient(CommandContext<CommandSourceStack> context, DebugClientPayload payload) {

        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            Services.NETWORK.sendToPlayer(player, payload);
        }
    }

    // ---- scene and kit ----

    private static int scene(CommandContext<CommandSourceStack> context) {

        String name = StringArgumentType.getString(context, "name");
        ServerPlayer player = context.getSource().getPlayer();
        return DebugScenes.build(context.getSource().getServer(), name, player)
                .map(placement -> ok(context, "Scene " + name + " built: " + placement.ghosts().size() + " ghost(s)"))
                .orElseGet(() -> fail(context, "Unknown scene " + name + "; try one of " + DebugScenes.names()));
    }

    private static int kit(CommandContext<CommandSourceStack> context, boolean tools) throws CommandSyntaxException {

        DebugKit.give(context.getSource().getPlayerOrException(), tools);
        return ok(context, tools ? "Kit given" : "Inventory cleared");
    }

    // ---- look ----

    private static LiteralArgumentBuilder<CommandSourceStack> look() {

        return Commands.literal("look")
                .then(Commands.argument("target", EntityArgument.entity()).executes(context -> {
                    Entity target = EntityArgument.getEntity(context, "target");
                    return lookAt(context, target.getEyePosition());
                }))
                .then(Commands.argument("pos", Vec3Argument.vec3()).executes(context -> lookAt(context, Vec3Argument.getVec3(context, "pos"))))
                .then(Commands.argument("yaw", FloatArgumentType.floatArg(-360.0F, 360.0F))
                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90.0F, 90.0F)).executes(context ->
                                setView(context, FloatArgumentType.getFloat(context, "yaw"), FloatArgumentType.getFloat(context, "pitch")))));
    }

    private static int lookAt(CommandContext<CommandSourceStack> context, Vec3 point) throws CommandSyntaxException {

        ServerPlayer player = context.getSource().getPlayerOrException();
        Vec3 eye = player.getEyePosition();
        Vec3 delta = point.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
        return setView(context, yaw, pitch);
    }

    private static int setView(CommandContext<CommandSourceStack> context, float yaw, float pitch) throws CommandSyntaxException {

        ServerPlayer player = context.getSource().getPlayerOrException();
        player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.of(), yaw, pitch, false);
        return ok(context, String.format(Locale.ROOT, "Looking at yaw %.1f, pitch %.1f", yaw, pitch));
    }

    // ---- ghost ----

    private static LiteralArgumentBuilder<CommandSourceStack> ghost() {

        return Commands.literal("ghost")
                .then(Commands.literal("spawn")
                        .executes(context -> spawnGhost(context, context.getSource().getPosition()))
                        .then(Commands.argument("pos", Vec3Argument.vec3()).executes(context -> spawnGhost(context, Vec3Argument.getVec3(context, "pos")))))
                .then(Commands.literal("clear")
                        .executes(context -> clearGhosts(context, DEFAULT_CLEAR_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 1024))
                                .executes(context -> clearGhosts(context, IntegerArgumentType.getInteger(context, "radius")))))
                .then(Commands.literal("freeze")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.literal("on").executes(context -> freeze(context, true)))
                                .then(Commands.literal("off").executes(context -> freeze(context, false)))))
                .then(Commands.literal("goal")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("goal", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                List.of("wander", "curious", "shy", "home"), builder))
                                        .executes(DebugCommands::forceGoal))));
    }

    private static List<GhostEntity> ghosts(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        List<GhostEntity> ghosts = new ArrayList<>();
        for (Entity entity : EntityArgument.getEntities(context, "targets")) {
            if (entity instanceof GhostEntity ghost) {
                ghosts.add(ghost);
            }
        }
        return ghosts;
    }

    private static int spawnGhost(CommandContext<CommandSourceStack> context, Vec3 at) {

        ServerLevel level = context.getSource().getLevel();
        LingeringSpiritEntity ghost = ModEntities.LINGERING_SPIRIT.get().create(level, EntitySpawnReason.COMMAND);
        if (ghost == null) {
            return fail(context, "Could not create a ghost");
        }
        ghost.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        ghost.setHome(level, BlockPos.containing(at));
        level.addFreshEntity(ghost);
        return ok(context, "Spawned ghost " + ghost.getUUID() + " homed at " + BlockPos.containing(at).toShortString()
                + ", cold spot " + (ghost.getColdSpot() == null ? "none" : ghost.getColdSpot().toShortString()));
    }

    private static int clearGhosts(CommandContext<CommandSourceStack> context, int radius) {

        Vec3 at = context.getSource().getPosition();
        List<GhostEntity> ghosts = context.getSource().getLevel().getEntitiesOfClass(GhostEntity.class, new AABB(at, at).inflate(radius),
                ghost -> ghost.position().distanceToSqr(at) <= (double) radius * radius);
        ghosts.forEach(Entity::discard);
        return ok(context, "Removed " + ghosts.size() + " ghost(s) within " + radius + " blocks");
    }

    private static int freeze(CommandContext<CommandSourceStack> context, boolean frozen) throws CommandSyntaxException {

        List<GhostEntity> ghosts = ghosts(context);
        for (GhostEntity ghost : ghosts) {
            ghost.setNoAi(frozen);
            ghost.setDeltaMovement(Vec3.ZERO);
            ghost.getNavigation().stop();
        }
        return ok(context, (frozen ? "Froze " : "Unfroze ") + ghosts.size() + " ghost(s)");
    }

    private static int forceGoal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        String name = StringArgumentType.getString(context, "goal");
        GhostEntity.ForcedGoal goal;
        try {
            goal = GhostEntity.ForcedGoal.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fail(context, "Unknown goal " + name + "; use wander, curious, shy or home");
        }
        List<GhostEntity> ghosts = ghosts(context);
        ghosts.forEach(ghost -> ghost.forceGoal(goal));
        return ok(context, "Forced " + name + " on " + ghosts.size() + " ghost(s) for " + GhostEntity.FORCED_GOAL_TICKS / 20 + " s");
    }

    /**
     * Keeps players firing their held Tether Wand without holding use (scripts and tests), or stops them.
     */
    private static int tether(CommandContext<CommandSourceStack> context, boolean on) throws CommandSyntaxException {

        int count = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(context, "players")) {
            if (on && TetherManager.heldWand(player) == null) {
                context.getSource().sendFailure(Component.literal(player.getGameProfile().name() + " isn't holding a Tether Wand"));
                continue;
            }
            TetherManager.setForced(player, on);
            count++;
        }
        return ok(context, (on ? "Firing" : "Stopped") + " the Tether Wand for " + count + " player(s)");
    }

    // ---- timers and pace ----

    private static LiteralArgumentBuilder<CommandSourceStack> timers() {

        return Commands.literal("timers")
                .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.literal("haunt").then(Commands.argument("ticks", IntegerArgumentType.integer(0))
                                .executes(context -> ghostTimer(context, true))))
                        .then(Commands.literal("manifest").then(Commands.argument("ticks", IntegerArgumentType.integer(0))
                                .executes(context -> ghostTimer(context, false))))
                        .then(Commands.literal("hallucination").then(Commands.argument("ticks", IntegerArgumentType.integer(0))
                                .executes(DebugCommands::hallucinationTimer))));
    }

    private static int ghostTimer(CommandContext<CommandSourceStack> context, boolean haunt) throws CommandSyntaxException {

        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        List<GhostEntity> ghosts = ghosts(context);
        for (GhostEntity ghost : ghosts) {
            if (haunt) {
                ghost.haunting().schedule().scheduleNext(ghost.level().getGameTime(), ticks);
            } else {
                ghost.setManifestCooldown(ticks);
            }
        }
        return ok(context, "Next " + (haunt ? "haunt" : "manifest") + " in " + ticks + " ticks for " + ghosts.size() + " ghost(s)");
    }

    private static int hallucinationTimer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        int count = 0;
        for (Entity entity : EntityArgument.getEntities(context, "targets")) {
            if (entity instanceof ServerPlayer player) {
                HallucinationScheduler.setTicksUntilNext(player, ticks);
                count++;
            }
        }
        return ok(context, "Next hallucination attempt in " + ticks + " ticks for " + count + " player(s)");
    }

    /**
     * Sets the pace and rescales the timers already running, so the change applies at once.
     */
    private static int pace(CommandContext<CommandSourceStack> context, double pace) {

        double factor = pace / ModConfigs.pace();
        ModConfigs.setPace(pace);
        MinecraftServer server = context.getSource().getServer();
        int ghosts = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof GhostEntity ghost) {
                    ghosts++;
                    if (ghost.manifestCooldown() > 0) {
                        ghost.setManifestCooldown(Math.max(1, (int) Math.round(ghost.manifestCooldown() * factor)));
                    }
                    if (ghost.haunting().schedule().isScheduled()) {
                        long now = level.getGameTime();
                        long left = Math.max(0, ghost.haunting().schedule().nextAttempt() - now);
                        ghost.haunting().schedule().scheduleNext(now, Math.max(1, Math.round(left * factor)));
                    }
                }
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            long left = HallucinationScheduler.ticksUntilNext(player);
            if (left > 0) {
                HallucinationScheduler.setTicksUntilNext(player, Math.max(1, Math.round(left * factor)));
            }
        }
        return ok(context, (pace == 1.0 ? "Normal pace" : "Fast pace: haunting, manifest and hallucination intervals ×" + pace)
                + " (in memory; resets when the server restarts). Rescaled " + ghosts + " ghost timer(s).");
    }

    // ---- config ----

    private static LiteralArgumentBuilder<CommandSourceStack> config() {

        return Commands.literal("config")
                .then(Commands.literal("get").then(Commands.argument("key", StringArgumentType.word()).executes(context -> {
                    String key = StringArgumentType.getString(context, "key");
                    String value = ModConfigs.get(key);
                    return value == null ? fail(context, "No config key " + key) : ok(context, key + " = " + value
                            + (ModConfigs.isClientKey(key) ? " (this side's client config)" : ""));
                })))
                .then(Commands.literal("set").then(Commands.argument("key", StringArgumentType.word())
                        .then(Commands.argument("value", StringArgumentType.word()).executes(DebugCommands::configSet))))
                .then(Commands.literal("reload").executes(context -> {
                    ModConfigs.loadServer();
                    for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                        Services.NETWORK.sendToPlayer(player, DebugClientPayload.of("config_reload"));
                    }
                    return ok(context, "Server config reloaded from disk; clients asked to reload theirs");
                }));
    }

    private static int configSet(CommandContext<CommandSourceStack> context) {

        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");
        if (ModConfigs.isClientKey(key)) {
            List<ServerPlayer> players = context.getSource().getServer().getPlayerList().getPlayers();
            for (ServerPlayer player : players) {
                Services.NETWORK.sendToPlayer(player, DebugClientPayload.of("config_set", key, value));
            }
            return ok(context, "Client key " + key + " sent to " + players.size() + " client(s) (in memory, not saved)");
        }
        if (!ModConfigs.isServerKey(key)) {
            return fail(context, "No config key " + key);
        }
        String applied = ModConfigs.setServer(key, value);
        return ok(context, key + " = " + applied + " (in memory, not saved)");
    }

    // ---- run ----

    private static int run(CommandContext<CommandSourceStack> context) {

        String file = StringArgumentType.getString(context, "file");
        try {
            ScriptRunner.start(context.getSource(), file, ScriptRunner.replyTo(context.getSource()));
            return ok(context, "Running " + ScriptRunner.scriptPath(context.getSource().getServer(), file));
        } catch (IOException e) {
            return fail(context, "Could not read " + ScriptRunner.scriptPath(context.getSource().getServer(), file) + ": " + e.getMessage());
        }
    }
}
