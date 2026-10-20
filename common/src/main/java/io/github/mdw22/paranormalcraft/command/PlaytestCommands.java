package io.github.mdw22.paranormalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.playtest.Checklist;
import io.github.mdw22.paranormalcraft.playtest.Checklists;
import io.github.mdw22.paranormalcraft.playtest.Playtests;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /pt}: playtest mode ({@link Playtests}). Same gate as the debug commands: op level 2, and
 * {@code enableDebugCommands} on or a development run.
 * <ul>
 *     <li>{@code start <checklist>}, {@code list}, {@code status}, {@code stop};</li>
 *     <li>{@code setup}, {@code pass}, {@code fail [note]}, {@code note <text>}, {@code skip}, {@code back} (what the chat
 *     buttons run).</li>
 * </ul>
 */
public final class PlaytestCommands {

    private PlaytestCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("pt")
                .requires(source -> ModConfigs.debugCommandsEnabled() && Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .then(Commands.literal("start")
                        .then(Commands.argument("checklist", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        Checklists.load(context.getSource().getServer()).checklists().keySet(), builder))
                                .executes(PlaytestCommands::start)))
                .then(Commands.literal("list").executes(PlaytestCommands::list))
                .then(Commands.literal("status").executes(context -> run(context, Playtests::status)))
                .then(Commands.literal("stop").executes(context -> run(context, Playtests::stop)))
                .then(Commands.literal("setup").executes(context -> {
                    Playtests.setup(context.getSource().getPlayerOrException(), context.getSource());
                    return 1;
                }))
                .then(Commands.literal("pass").executes(context -> run(context, Playtests::pass)))
                .then(Commands.literal("skip").executes(context -> run(context, Playtests::skip)))
                .then(Commands.literal("back").executes(context -> run(context, Playtests::back)))
                .then(Commands.literal("fail")
                        .executes(context -> run(context, player -> Playtests.fail(player, null)))
                        .then(Commands.argument("note", StringArgumentType.greedyString())
                                .executes(context -> run(context, player -> Playtests.fail(player, StringArgumentType.getString(context, "note"))))))
                .then(Commands.literal("note")
                        .then(Commands.argument("note", StringArgumentType.greedyString())
                                .executes(context -> run(context, player -> Playtests.note(player, StringArgumentType.getString(context, "note")))))));
    }

    @FunctionalInterface
    private interface PlayerAction {

        void run(ServerPlayer player);
    }

    private static int run(CommandContext<CommandSourceStack> context, PlayerAction action) throws CommandSyntaxException {

        action.run(context.getSource().getPlayerOrException());
        return 1;
    }

    private static int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "checklist");
        Checklists.Loaded loaded = Checklists.load(context.getSource().getServer());
        Checklist checklist = loaded.checklists().get(name);
        if (checklist == null) {
            String problem = loaded.invalid().stream().filter(e -> e.file().endsWith("/" + name + ".json")).map(Throwable::getMessage).findFirst()
                    .orElse("no checklist called " + name + " (see /pt list)");
            context.getSource().sendFailure(Component.literal(problem));
            return 0;
        }
        Playtests.start(player, checklist);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {

        Checklists.Loaded loaded = Checklists.load(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal("Playtest checklists:").withStyle(ChatFormatting.GOLD), false);
        for (Checklist checklist : loaded.checklists().values()) {
            context.getSource().sendSuccess(() -> Playtests.button("[Start]", "/pt start " + checklist.name(), "Start " + checklist.name(),
                    ChatFormatting.AQUA, false).append(Component.literal(" " + checklist.name() + ": " + checklist.title() + " ("
                    + checklist.steps().size() + " steps)").withStyle(ChatFormatting.WHITE)), false);
        }
        for (Checklist.InvalidChecklistException invalid : loaded.invalid()) {
            context.getSource().sendSuccess(() -> Component.literal("Invalid: " + invalid.getMessage()).withStyle(ChatFormatting.RED), false);
        }
        return loaded.checklists().size();
    }
}
