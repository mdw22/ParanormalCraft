package io.github.mdw22.paranormalcraft.debug;

import com.google.gson.JsonObject;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.network.DebugClientPayload;
import io.github.mdw22.paranormalcraft.platform.Services;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

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
 *     <li>{@code # comment} and blank lines are skipped;</li>
 *     <li>{@code expect trace <type> [field<op>value …] [within <ticks>]} and {@code expect dump <path><op><value> …}:
 *     see {@link Expectation}. Trace expectations look at events since the script's last command, from the server
 *     and from the client of the player who started the script (mirrored while the script runs).</li>
 * </ul>
 * A script stops at the first line that doesn't parse or whose command fails, and reports that line's number. Every
 * line runs from where the script was started: a scene that teleports the player doesn't move later commands' {@code ~}
 * or {@code @e[sort=nearest]}, so name coordinates explicitly after a scene. A failed expectation stops the script
 * and reports the line, the expectation and the closest event seen.
 */
public final class ScriptRunner {

    public static final String SCRIPT_DIR = "paranormalcraft-scripts";
    /** {@code expect dump} looks this far around the player who started the script. */
    private static final double DUMP_RADIUS = 48.0;

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
        /** Game time of the last command run; trace expectations look at events from then on. */
        private long lastCommandTime;
        private Expectation pending;
        private int pendingLine;
        private long pendingDeadline;

        private Run(String name, List<String> lines, CommandSourceStack source, Consumer<Result> onDone) {

            this.name = name;
            this.lines = lines;
            this.source = source;
            this.onDone = onDone;
            this.startTime = source.getLevel().getGameTime();
            this.lastCommandTime = this.startTime;
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

        ServerTrace.startBuffering(source.getServer());
        mirror(source, true);
        RUNNING.add(new Run(name, List.copyOf(lines), source, onDone));
    }

    /**
     * Turns client trace mirroring on or off for the player who started a script.
     */
    private static void mirror(CommandSourceStack source, boolean on) {

        if (source.getEntity() instanceof ServerPlayer player) {
            Services.NETWORK.sendToPlayer(player, DebugClientPayload.of("trace_mirror", Boolean.toString(on)));
        }
    }

    private static void finish(Run run, Result result) {

        RUNNING.remove(run);
        ServerTrace.stopBuffering();
        mirror(run.source, false);
        run.onDone.accept(result);
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
                finish(run, result);
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
        if (run.pending != null) {
            String failure = check(run, now);
            if (failure != null) {
                return now >= run.pendingDeadline ? error(run, run.pendingLine, failure, now) : null;
            }
            run.pending = null;
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
                try {
                    run.pending = Expectation.parse(line.substring(7));
                } catch (IllegalArgumentException e) {
                    return error(run, lineNumber, e.getMessage(), now);
                }
                run.pendingLine = lineNumber;
                run.pendingDeadline = now + run.pending.within();
                String failure = check(run, now);
                if (failure == null) {
                    run.pending = null;
                    continue;
                }
                if (now >= run.pendingDeadline) {
                    return error(run, lineNumber, failure, now);
                }
                return null;
            }
            String failure = execute(server, run.source, line);
            run.lastCommandTime = now;
            if (failure != null) {
                return error(run, lineNumber, failure, now);
            }
        }
        return new Result(run.name, true, 0, "finished", now - run.startTime);
    }

    /**
     * Null if the pending expectation holds now, otherwise why not (with the closest thing seen).
     */
    private static String check(Run run, long now) {

        return evaluate(run.pending, run.source, run.lastCommandTime);
    }

    /**
     * Null if {@code expectation} holds now for {@code source} (a dump around its entity, or a trace event since game
     * time {@code since}), otherwise why not, with the closest thing seen. Trace events need buffering on
     * ({@link ServerTrace#startBuffering}). Also used by playtest auto-checks.
     */
    public static @Nullable String evaluate(Expectation expectation, CommandSourceStack source, long since) {

        if (expectation.kind() == Expectation.Kind.DUMP) {
            Vec3 at = source.getEntity() != null ? source.getEntity().position() : source.getPosition();
            JsonObject dump = DebugDump.build(source.getLevel(), at, DUMP_RADIUS, DebugDump.SILENT);
            if (expectation.matchesDump(dump)) {
                return null;
            }
            List<String> actual = new ArrayList<>();
            for (Expectation.Condition condition : expectation.conditions()) {
                actual.add(condition.path() + " is " + Expectation.resolve(dump, condition.path()));
            }
            return "expect " + expectation.describe() + " failed; " + String.join(", ", actual);
        }
        String player = source.getEntity() instanceof ServerPlayer serverPlayer ? serverPlayer.getGameProfile().name() : null;
        List<JsonObject> events = new ArrayList<>();
        for (JsonObject event : ServerTrace.since(since)) {
            boolean fromClient = event.has("side") && event.get("side").getAsString().equals("client");
            if (!fromClient || (player != null && player.equals(event.get("player").getAsString()))) {
                events.add(event);
            }
        }
        for (JsonObject event : events) {
            if (expectation.matchesEvent(event)) {
                return null;
            }
        }
        JsonObject closest = expectation.closest(events);
        return "expect " + expectation.describe() + " not met (" + events.size() + " event(s) since the last command); closest: "
                + (closest == null ? "none" : closest);
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
        // Redirecting commands (execute … run …) keep the command on the last child context.
        CommandContextBuilder<CommandSourceStack> last = parse.getContext().getLastChild();
        if (parse.getReader().canRead() || !parse.getExceptions().isEmpty() || last.getCommand() == null) {
            String reason = parse.getExceptions().isEmpty() ? "unknown or incomplete command" : parse.getExceptions().values().iterator().next().getMessage();
            return "'" + command + "': " + reason;
        }
        server.getCommands().performCommand(parse, command);
        return failed[0] ? "'" + command + "' failed" : null;
    }

    public static void clear() {

        for (int i = 0; i < RUNNING.size(); i++) {
            ServerTrace.stopBuffering();
        }
        RUNNING.clear();
    }
}
