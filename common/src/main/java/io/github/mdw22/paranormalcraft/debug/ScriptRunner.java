package io.github.mdw22.paranormalcraft.debug;

import com.mojang.brigadier.ParseResults;
import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * {@code /paranormalcraft debug run <file>}: runs {@code <server dir>/paranormalcraft-scripts/<file>.txt} line by line
 * on the server tick, as whoever started it.
 * <ul>
 *     <li>a command, without the leading {@code /};</li>
 *     <li>{@code wait <ticks>}: game ticks, so it follows {@code /tick freeze}, {@code step} and {@code sprint};</li>
 *     <li>{@code # comment} and blank lines are skipped.</li>
 * </ul>
 * A script stops at the first line that doesn't parse or whose command fails, and reports that line's number. Every
 * line runs from where the script was started: a scene that teleports the player doesn't move later commands' {@code ~}
 * or {@code @e[sort=nearest]}, so name coordinates explicitly after a scene.
 * ({@code expect} lines aren't supported yet; use {@code debug trace} and read the file afterwards.)
 */
public final class ScriptRunner {

    public static final String SCRIPT_DIR = "paranormalcraft-scripts";

    /**
     * How a script ended. {@code line} is the 1-based line of the error, or 0 when it finished.
     */
    public record Result(String name, boolean ok, int line, String message, long gameTicks) {
    }

    private static final List<Run> RUNNING = new ArrayList<>();

    private static final class Run {

        private final String name;
        private final List<String> lines;
        private final CommandSourceStack source;
        private final Consumer<Result> onDone;
        private final long startTime;
        private int next;
        private long waitUntil = Long.MIN_VALUE;

        private Run(String name, List<String> lines, CommandSourceStack source, Consumer<Result> onDone) {

            this.name = name;
            this.lines = lines;
            this.source = source;
            this.onDone = onDone;
            this.startTime = source.getLevel().getGameTime();
        }
    }

    private ScriptRunner() {
    }

    public static Path scriptPath(MinecraftServer server, String file) {

        String name = file.endsWith(".txt") ? file : file + ".txt";
        return server.getServerDirectory().resolve(SCRIPT_DIR).resolve(name).toAbsolutePath().normalize();
    }

    /**
     * Reads a script file and starts it; it begins on the next server tick.
     */
    public static void start(CommandSourceStack source, String file, Consumer<Result> onDone) throws IOException {

        Path path = scriptPath(source.getServer(), file);
        start(source, file, Files.readAllLines(path, StandardCharsets.UTF_8), onDone);
    }

    public static void start(CommandSourceStack source, String name, List<String> lines, Consumer<Result> onDone) {

        RUNNING.add(new Run(name, List.copyOf(lines), source, onDone));
    }

    /**
     * Reports a result to the source that started the script, and to the log.
     */
    public static Consumer<Result> replyTo(CommandSourceStack source) {

        return result -> {
            if (result.ok()) {
                Constants.LOG.info("Script {} finished after {} game ticks", result.name(), result.gameTicks());
                source.sendSuccess(() -> Component.literal("Script " + result.name() + " finished after " + result.gameTicks() + " game ticks"), false);
            } else {
                Constants.LOG.warn("Script {} stopped at line {}: {}", result.name(), result.line(), result.message());
                source.sendFailure(Component.literal("Script " + result.name() + " stopped at line " + result.line() + ": " + result.message()));
            }
        };
    }

    /**
     * Called every server tick.
     */
    public static void tick(MinecraftServer server) {

        if (RUNNING.isEmpty()) {
            return;
        }
        for (Run run : List.copyOf(RUNNING)) {
            Result result = advance(server, run);
            if (result != null) {
                RUNNING.remove(run);
                run.onDone.accept(result);
            }
        }
    }

    /**
     * Runs lines until a wait or the end. Returns the result once the script is done, or null to continue next tick.
     */
    private static Result advance(MinecraftServer server, Run run) {

        long now = run.source.getLevel().getGameTime();
        if (now < run.waitUntil) {
            return null;
        }
        while (run.next < run.lines.size()) {
            int lineNumber = run.next + 1;
            String line = run.lines.get(run.next++).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("/")) {
                line = line.substring(1);
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("wait ")) {
                try {
                    int ticks = Integer.parseInt(line.substring(5).strip());
                    if (ticks < 0) {
                        throw new NumberFormatException();
                    }
                    run.waitUntil = now + ticks;
                    return null;
                } catch (NumberFormatException e) {
                    return error(run, lineNumber, "wait needs a whole number of ticks, got '" + line.substring(5).strip() + "'", now);
                }
            }
            if (lower.startsWith("expect ")) {
                return error(run, lineNumber, "expect isn't supported yet", now);
            }
            String failure = execute(server, run.source, line);
            if (failure != null) {
                return error(run, lineNumber, failure, now);
            }
        }
        return new Result(run.name, true, 0, "finished", now - run.startTime);
    }

    private static Result error(Run run, int line, String message, long now) {

        return new Result(run.name, false, line, message, now - run.startTime);
    }

    /**
     * Runs one command; returns why it failed, or null if it worked.
     */
    private static String execute(MinecraftServer server, CommandSourceStack source, String command) {

        boolean[] failed = {false};
        CommandSourceStack tracked = source.withCallback((success, result) -> failed[0] |= !success);
        ParseResults<CommandSourceStack> parse = server.getCommands().getDispatcher().parse(command, tracked);
        if (parse.getReader().canRead() || !parse.getExceptions().isEmpty() || parse.getContext().getCommand() == null) {
            String reason = parse.getExceptions().isEmpty() ? "unknown or incomplete command" : parse.getExceptions().values().iterator().next().getMessage();
            return "'" + command + "': " + reason;
        }
        server.getCommands().performCommand(parse, command);
        return failed[0] ? "'" + command + "' failed" : null;
    }

    public static void clear() {

        RUNNING.clear();
    }
}
