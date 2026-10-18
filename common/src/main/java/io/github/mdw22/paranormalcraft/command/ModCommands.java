package io.github.mdw22.paranormalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.debug.RevealDebug;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /paranormalcraft reveal} (op level 2): toggles the caller's debug flag, which shows every ghost to them and
 * draws home radii. Testing only; not saved.
 */
public final class ModCommands {

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
                })));
    }
}
