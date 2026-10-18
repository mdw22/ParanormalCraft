package io.github.mdw22.paranormalcraft.debug.trace;

import com.google.gson.JsonObject;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * The server half of {@code /paranormalcraft debug trace}: haunting actions, goal changes, manifests, Presence
 * changes, hallucinations sent, ghost spawns and dispersals, written to
 * {@code <server dir>/paranormalcraft-trace/server-trace-<timestamp>.jsonl}. Every hook is a no-op while no trace is open.
 */
public final class ServerTrace {

    private static @Nullable TraceWriter writer;
    private static @Nullable MinecraftServer server;

    private ServerTrace() {
    }

    public static boolean active() {

        return writer != null;
    }

    public static Path start(MinecraftServer minecraftServer) throws IOException {

        stop();
        server = minecraftServer;
        writer = TraceWriter.open(minecraftServer.getServerDirectory(), "server-trace-");
        event("trace_start", line -> {
        });
        return writer.path();
    }

    /**
     * Closes the trace and returns its path, or null if none was open.
     */
    public static @Nullable Path stop() {

        if (writer == null) {
            return null;
        }
        event("trace_stop", line -> {
        });
        Path path = writer.path();
        writer.close();
        writer = null;
        server = null;
        return path;
    }

    public static void mark(String label) {

        event("mark", line -> line.addProperty("label", label));
    }

    /**
     * Writes one event if a trace is open. {@code fields} fills in the event-specific fields.
     */
    public static void event(String type, Consumer<JsonObject> fields) {

        if (writer == null || server == null) {
            return;
        }
        JsonObject line = TraceWriter.event(server.overworld().getGameTime(), type);
        fields.accept(line);
        writer.write(line);
    }

    /**
     * An event about one ghost: adds its uuid and position.
     */
    public static void ghost(String type, GhostEntity ghost, Consumer<JsonObject> fields) {

        event(type, line -> {
            line.addProperty("ghost", ghost.getUUID().toString());
            addPos(line, ghost);
            fields.accept(line);
        });
    }

    public static void addPos(JsonObject line, Entity entity) {

        line.addProperty("x", round(entity.getX()));
        line.addProperty("y", round(entity.getY()));
        line.addProperty("z", round(entity.getZ()));
    }

    public static double round(double value) {

        return Math.round(value * 100.0) / 100.0;
    }

    public static void clear() {

        if (writer != null) {
            Constants.LOG.info("Closing server trace {} as the server stops", writer.path());
        }
        stop();
    }
}
