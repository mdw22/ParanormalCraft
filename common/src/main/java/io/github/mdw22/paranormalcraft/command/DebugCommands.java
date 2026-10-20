package io.github.mdw22.paranormalcraft.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.binding.SpiritVesselItem;
import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.capture.TetherManager;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.containment.ContainmentCharge;
import io.github.mdw22.paranormalcraft.containment.ContainmentUnitBlockEntity;
import io.github.mdw22.paranormalcraft.debug.DebugKit;
import io.github.mdw22.paranormalcraft.debug.DebugScenes;
import io.github.mdw22.paranormalcraft.debug.ScriptRunner;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.entity.LingeringSpiritEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.familiar.Familiars;
import io.github.mdw22.paranormalcraft.familiar.WispFamiliarEntity;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationScheduler;
import io.github.mdw22.paranormalcraft.item.FieldJournalItem;
import io.github.mdw22.paranormalcraft.journal.CaptureLog;
import io.github.mdw22.paranormalcraft.journal.JournalEvent;
import io.github.mdw22.paranormalcraft.journal.JournalStages;
import io.github.mdw22.paranormalcraft.network.DebugClientPayload;
import io.github.mdw22.paranormalcraft.phase.Phasing;
import io.github.mdw22.paranormalcraft.platform.Services;
import io.github.mdw22.paranormalcraft.registry.ModDataComponents;
import io.github.mdw22.paranormalcraft.registry.ModEntities;
import io.github.mdw22.paranormalcraft.world.ModStructures;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
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
 * timers, pace, config, journal, containment, bind, familiar, nearcabin, phase and run. Gated like the other debug commands (op level 2 plus {@code enableDebugCommands}, forced on in
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
                                .then(Commands.literal("off").executes(context -> tether(context, false)))
                                .then(Commands.literal("deploy").executes(DebugCommands::deployTrap))))
                .then(Commands.literal("pace")
                        .then(Commands.literal("fast").executes(context -> pace(context, ModConfigs.FAST_PACE)))
                        .then(Commands.literal("normal").executes(context -> pace(context, 1.0))))
                .then(config())
                .then(journal())
                .then(containment())
                .then(Commands.literal("bind")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("index", IntegerArgumentType.integer(0, ContainmentUnitBlockEntity.CAPACITY - 1))
                                        .executes(context -> containment(context, unit -> unit.debugBind(context.getSource().getLevel(),
                                                IntegerArgumentType.getInteger(context, "index"), context.getSource().getPlayerOrException())
                                                ? "bound into the vessel" : "can't bind that ghost")))))
                .then(familiar())
                .then(Commands.literal("nearcabin")
                        .executes(context -> nearCabin(context, DEFAULT_CABIN_DISTANCE))
                        .then(Commands.argument("distance", IntegerArgumentType.integer(8, 256))
                                .executes(context -> nearCabin(context, IntegerArgumentType.getInteger(context, "distance")))))
                .then(Commands.literal("phase")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("start").executes(context -> {
                                    Phasing.start(EntityArgument.getPlayer(context, "player"));
                                    return ok(context, "Phased for " + Phasing.PHASE_TICKS + " ticks");
                                }))
                                .then(Commands.literal("end").executes(context -> {
                                    Phasing.end(EntityArgument.getPlayer(context, "player"), "command");
                                    return ok(context, "Phase ended");
                                }))))
                .then(Commands.literal("run")
                        .then(Commands.argument("file", StringArgumentType.greedyString()).executes(DebugCommands::run)));
    }

    // ---- journal ----

    private static LiteralArgumentBuilder<CommandSourceStack> journal() {

        return Commands.literal("journal")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("reset").executes(DebugCommands::journalReset))
                        .then(Commands.literal("open").executes(context -> {
                            FieldJournalItem.open(EntityArgument.getPlayer(context, "player"));
                            return ok(context, "Opened the journal");
                        }))
                        .then(Commands.argument("ghostType", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        JournalStages.GHOST_TYPES.stream().map(type -> Identifier.parse(type).getPath()), builder))
                                .then(Commands.argument("stage", IntegerArgumentType.integer(0, JournalEvent.STAGES.length + 1))
                                        .executes(DebugCommands::journalStage))));
    }

    private static int journalStage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String type = StringArgumentType.getString(context, "ghostType");
        String id = type.contains(":") ? type : Constants.MOD_ID + ":" + type;
        if (!JournalStages.GHOST_TYPES.contains(id)) {
            context.getSource().sendFailure(Component.literal("No journal page for " + id));
            return 0;
        }
        int stage = IntegerArgumentType.getInteger(context, "stage");
        JournalStages.setStage(player, id, stage);
        return ok(context, "Journal: " + id + " at stage " + JournalStages.stage(player, id) + (JournalStages.bound(player, id) ? " (bound)" : "")
                + " for " + player.getGameProfile().name());
    }

    private static int journalReset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        for (String type : JournalStages.GHOST_TYPES) {
            JournalStages.setStage(player, type, 0);
        }
        CaptureLog.get(player.level().getServer()).reset(player.getUUID());
        return ok(context, "Journal reset for " + player.getGameProfile().name());
    }

    // ---- containment ----

    private static LiteralArgumentBuilder<CommandSourceStack> containment() {

        return Commands.literal("containment")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.literal("charge").then(Commands.argument("value", IntegerArgumentType.integer(0, ContainmentCharge.MAX_CHARGE))
                                .executes(context -> containment(context, unit -> {
                                    unit.setCharge(IntegerArgumentType.getInteger(context, "value"));
                                    return "charge " + unit.charge();
                                }))))
                        .then(Commands.literal("fill").then(Commands.argument("count", IntegerArgumentType.integer(1, ContainmentUnitBlockEntity.CAPACITY))
                                .executes(context -> containment(context, unit -> "added "
                                        + unit.fillForDebug(context.getSource().getLevel(), IntegerArgumentType.getInteger(context, "count")) + " ghost(s)"))))
                        .then(Commands.literal("unstable").executes(context -> containment(context, unit -> {
                            if (unit.stored().isEmpty()) {
                                unit.fillForDebug(context.getSource().getLevel(), 1);
                            }
                            unit.setCharge(0);
                            return "charge 0 with " + unit.stored().size() + " ghost(s): Unstable next tick";
                        })))
                        .then(Commands.literal("process").then(Commands.argument("index", IntegerArgumentType.integer(0, ContainmentUnitBlockEntity.CAPACITY - 1))
                                .executes(context -> containment(context, unit -> unit.queueProcess(IntegerArgumentType.getInteger(context, "index"),
                                        context.getSource().getPlayerOrException()) ? "queued for processing" : "can't queue that ghost"))))
                        .then(Commands.literal("advance").then(Commands.argument("ticks", IntegerArgumentType.integer(1, ContainmentUnitBlockEntity.PROCESS_TICKS))
                                .executes(context -> containment(context, unit -> "processing at "
                                        + unit.debugAdvanceProcessing(IntegerArgumentType.getInteger(context, "ticks")) + " ticks"))))
                        .then(Commands.literal("release").then(Commands.argument("index", IntegerArgumentType.integer(0, ContainmentUnitBlockEntity.CAPACITY - 1))
                                .executes(context -> containment(context, unit -> unit.release(IntegerArgumentType.getInteger(context, "index"),
                                        context.getSource().getPlayerOrException()) ? "released" : "can't release that ghost"))))
                        .then(Commands.literal("bindstart").then(Commands.argument("index", IntegerArgumentType.integer(0, ContainmentUnitBlockEntity.CAPACITY - 1))
                                .executes(context -> containment(context, unit -> unit.bind(IntegerArgumentType.getInteger(context, "index"),
                                        context.getSource().getPlayerOrException()) ? "binding started" : "can't bind that ghost"))))
                        .then(Commands.literal("unbind").executes(context -> containment(context, unit ->
                                unit.unbind(context.getSource().getPlayerOrException()) ? "unbound into storage" : "nothing to unbind, or no room"))));
    }

    // ---- familiar ----

    private static LiteralArgumentBuilder<CommandSourceStack> familiar() {

        return Commands.literal("familiar")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("summon").executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            ItemStack vessel = boundVessel(player);
                            if (vessel.isEmpty()) {
                                return fail(context, "No Bound Spirit Vessel in " + player.getGameProfile().name() + "'s inventory");
                            }
                            WispFamiliarEntity out = Familiars.of(player);
                            CapturedGhost ghost = vessel.get(ModDataComponents.CAPTURED_GHOST.get());
                            if (out != null && ghost != null && ghost.id().equals(out.ghost())) {
                                return ok(context, "Already out");
                            }
                            return ghost != null && Familiars.summon(player, ghost) != null ? ok(context, "Summoned " + ghost.name())
                                    : fail(context, "That vessel's ghost has no familiar job");
                        }))
                        .then(Commands.literal("recall").executes(context -> {
                            WispFamiliarEntity out = Familiars.of(EntityArgument.getPlayer(context, "player"));
                            if (out == null) {
                                return fail(context, "No familiar out");
                            }
                            Familiars.recall(out, "command");
                            return ok(context, "Recalled");
                        }))
                        .then(Commands.literal("guide").executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            if (Familiars.of(player) == null) {
                                return fail(context, "No familiar out");
                            }
                            Familiars.resetGuideCooldown(player);
                            BlockPos cabin = Familiars.guide(player);
                            return ok(context, cabin == null ? "No cabin within range: the wisp shakes no" : "Guiding toward the cabin at " + cabin.toShortString());
                        })));
    }

    private static final int DEFAULT_CABIN_DISTANCE = 48;
    private static final int CABIN_SEARCH_CHUNKS = 100;

    /**
     * Teleports the player {@code distance} blocks from the nearest Abandoned Cabin, on the surface, facing away from
     * it (for testing the wisp's guide).
     */
    private static int nearCabin(CommandContext<CommandSourceStack> context, int distance) throws CommandSyntaxException {

        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.level();
        BlockPos cabin = level.findNearestMapStructure(ModStructures.ABANDONED_CABIN, player.blockPosition(), CABIN_SEARCH_CHUNKS, false);
        if (cabin == null) {
            return fail(context, "No Abandoned Cabin within " + CABIN_SEARCH_CHUNKS + " chunks");
        }
        Vec3 away = new Vec3(player.getX() - cabin.getX(), 0.0, player.getZ() - cabin.getZ());
        away = away.lengthSqr() < 1.0 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
        int x = (int) Math.floor(cabin.getX() + away.x * distance);
        int z = (int) Math.floor(cabin.getZ() + away.z * distance);
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        float yaw = (float) Math.toDegrees(Math.atan2(-away.x, away.z));
        player.teleportTo(level, x + 0.5, y, z + 0.5, Set.of(), yaw, 0.0F, true);
        return ok(context, "Moved " + distance + " blocks from the cabin at " + cabin.toShortString() + ", facing away from it");
    }

    private static ItemStack boundVessel(ServerPlayer player) {

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (SpiritVesselItem.isBound(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @FunctionalInterface
    private interface UnitAction {

        String apply(ContainmentUnitBlockEntity unit) throws CommandSyntaxException;
    }

    private static int containment(CommandContext<CommandSourceStack> context, UnitAction action) throws CommandSyntaxException {

        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (!(context.getSource().getLevel().getBlockEntity(pos) instanceof ContainmentUnitBlockEntity unit)) {
            context.getSource().sendFailure(Component.literal("No Containment Unit at " + pos.toShortString()));
            return 0;
        }
        return ok(context, "Containment Unit at " + pos.toShortString() + ": " + action.apply(unit));
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
    /**
     * What attack does while firing: drop a trap under each player's latched ghost.
     */
    private static int deployTrap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        int count = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(context, "players")) {
            if (TetherManager.deployTrap(player)) {
                count++;
            }
        }
        return ok(context, "Deployed " + count + " trap(s)");
    }

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
