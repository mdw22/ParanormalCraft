package io.github.mdw22.paranormalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.debug.ColdSpotDisplay;
import io.github.mdw22.paranormalcraft.debug.DebugDump;
import io.github.mdw22.paranormalcraft.debug.RevealDebug;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.Presence;
import io.github.mdw22.paranormalcraft.hallucination.HallucinationScheduler;
import io.github.mdw22.paranormalcraft.haunting.HauntingAction;
import io.github.mdw22.paranormalcraft.haunting.HauntingController;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@code /paranormalcraft reveal} (op level 2) toggles the caller's debug reveal flag.
 * <p>
 * {@code /paranormalcraft debug …} (op level 2, and only when {@code enableDebugCommands} is on or in a development
 * environment): {@code dump [radius]}, {@code manifest <targets>}, {@code presence <targets> <value>},
 * {@code haunt <targets> [action]}, {@code hallucinate <player>}, {@code coldspot <targets>} and
 * {@code savestructure <pos>} (saves a structure block's template to disk, for building structures).
 */
public final class ModCommands {

    private static final int DEFAULT_DUMP_RADIUS = 32;
    private static final DynamicCommandExceptionType UNKNOWN_ACTION = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.paranormalcraft.debug.haunt.unknown", id));

    private ModCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal(Constants.MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("reveal").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    boolean revealed = RevealDebug.toggle(player);
                    context.getSource().sendSuccess(() -> Component.translatable(revealed
                            ? "commands.paranormalcraft.reveal.on"
                            : "commands.paranormalcraft.reveal.off"), false);
                    return revealed ? 1 : 0;
                }))
                .then(debugCommands()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> debugCommands() {

        return DebugCommands.addTo(Commands.literal("debug")
                .requires(source -> ModConfigs.debugCommandsEnabled()))
                .then(Commands.literal("dump")
                        .executes(context -> dump(context, DEFAULT_DUMP_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
                                .executes(context -> dump(context, IntegerArgumentType.getInteger(context, "radius")))))
                .then(Commands.literal("manifest")
                        .then(Commands.argument("targets", EntityArgument.entities()).executes(context -> {
                            List<GhostEntity> ghosts = ghosts(context);
                            for (GhostEntity ghost : ghosts) {
                                ghost.manifest((ServerLevel) ghost.level());
                            }
                            return report(context, "commands.paranormalcraft.debug.manifest", ghosts.size());
                        })))
                .then(Commands.literal("presence")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, Presence.MAX)).executes(context -> {
                                    float value = FloatArgumentType.getFloat(context, "value");
                                    List<GhostEntity> ghosts = ghosts(context);
                                    for (GhostEntity ghost : ghosts) {
                                        ghost.setPresence((ServerLevel) ghost.level(), value);
                                    }
                                    return report(context, "commands.paranormalcraft.debug.presence", ghosts.size());
                                }))))
                .then(Commands.literal("haunt")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(context -> haunt(context, null))
                                .then(Commands.argument("action", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(HauntingAction.values()).map(HauntingAction::id), builder))
                                        .executes(context -> {
                                            String id = StringArgumentType.getString(context, "action");
                                            HauntingAction action = HauntingAction.byId(id).orElseThrow(() -> UNKNOWN_ACTION.create(id));
                                            return haunt(context, action);
                                        }))))
                .then(Commands.literal("hallucinate")
                        .then(Commands.argument("player", EntityArgument.players()).executes(context -> {
                            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
                            players.forEach(HallucinationScheduler::send);
                            return report(context, "commands.paranormalcraft.debug.hallucinate", players.size());
                        })))
                .then(Commands.literal("savestructure")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> {
                            // Same as the structure block screen's SAVE button: writes the template to the world's
                            // generated/ folder. Used to (re)build structure templates such as the Abandoned Cabin.
                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                            if (!(context.getSource().getLevel().getBlockEntity(pos) instanceof StructureBlockEntity structureBlock)
                                    || !structureBlock.saveStructure(true)) {
                                context.getSource().sendFailure(Component.translatable("commands.paranormalcraft.debug.savestructure.failed"));
                                return 0;
                            }
                            String name = structureBlock.getStructureName();
                            context.getSource().sendSuccess(() -> Component.translatable("commands.paranormalcraft.debug.savestructure", name), true);
                            return 1;
                        })))
                .then(Commands.literal("coldspot")
                        .then(Commands.argument("targets", EntityArgument.entities()).executes(context -> {
                            List<GhostEntity> ghosts = ghosts(context);
                            for (GhostEntity ghost : ghosts) {
                                ColdSpotDisplay.show(ghost, context.getSource().getServer());
                            }
                            return report(context, "commands.paranormalcraft.debug.coldspot", ghosts.size());
                        })));
    }

    private static int haunt(CommandContext<CommandSourceStack> context, HauntingAction action) throws CommandSyntaxException {

        int done = 0;
        for (GhostEntity ghost : ghosts(context)) {
            Optional<HauntingController.Record> record = ghost.haunting().force((ServerLevel) ghost.level(), action);
            if (record.isPresent()) {
                done++;
                String line = record.get().action().id() + ": " + record.get().detail();
                context.getSource().sendSuccess(() -> Component.literal(line), true);
            } else {
                context.getSource().sendFailure(Component.translatable("commands.paranormalcraft.debug.haunt.none",
                        action == null ? "any" : action.id()));
            }
        }
        return done;
    }

    private static int dump(CommandContext<CommandSourceStack> context, int radius) {

        CommandSourceStack source = context.getSource();
        DebugDump.dump(source.getLevel(), source.getPosition(), radius, line -> source.sendSuccess(() -> line, false));
        return 1;
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

    private static int report(CommandContext<CommandSourceStack> context, String key, int count) {

        context.getSource().sendSuccess(() -> Component.translatable(key, count), true);
        return count;
    }
}
