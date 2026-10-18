package io.github.mdw22.paranormalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.debug.ColdSpotDisplay;
import io.github.mdw22.paranormalcraft.debug.DebugDump;
import io.github.mdw22.paranormalcraft.debug.RevealDebug;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.Presence;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /paranormalcraft reveal} (op level 2) toggles the caller's debug reveal flag.
 * <p>
 * {@code /paranormalcraft debug …} (op level 2, and only when {@code enableDebugCommands} is on or in a development
 * environment): {@code dump [radius]}, {@code manifest <targets>}, {@code presence <targets> <value>} and
 * {@code coldspot <targets>}.
 */
public final class ModCommands {

    private static final int DEFAULT_DUMP_RADIUS = 32;

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

        return Commands.literal("debug")
                .requires(source -> ModConfigs.debugCommandsEnabled())
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
                .then(Commands.literal("coldspot")
                        .then(Commands.argument("targets", EntityArgument.entities()).executes(context -> {
                            List<GhostEntity> ghosts = ghosts(context);
                            for (GhostEntity ghost : ghosts) {
                                ColdSpotDisplay.show(ghost, context.getSource().getServer());
                            }
                            return report(context, "commands.paranormalcraft.debug.coldspot", ghosts.size());
                        })));
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
