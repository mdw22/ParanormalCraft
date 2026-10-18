package io.github.mdw22.paranormalcraft.debug.trace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.mdw22.paranormalcraft.Constants;
import org.jspecify.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One JSON Lines trace file: each line is an object with the game time {@code t}, an event {@code type} and the
 * event's fields. Used by both the server and the client trace. Not thread-safe; each side writes from its own main
 * thread.
 */
public final class TraceWriter {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeSpecialFloatingPointValues().create();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path path;
    private @Nullable BufferedWriter writer;

    private TraceWriter(Path path, BufferedWriter writer) {

        this.path = path;
        this.writer = writer;
    }

    /**
     * Opens {@code <dir>/paranormalcraft-trace/<prefix><timestamp>.jsonl}. The client uses {@code trace-} and the
     * server {@code server-trace-}, so they don't mix when both write to one folder (single player).
     */
    public static TraceWriter open(Path gameDir, String prefix) throws IOException {

        Path dir = gameDir.resolve("paranormalcraft-trace");
        Files.createDirectories(dir);
        String stamp = LocalDateTime.now().format(STAMP);
        Path path = dir.resolve(prefix + stamp + ".jsonl");
        for (int i = 1; Files.exists(path); i++) {
            path = dir.resolve(prefix + stamp + "-" + i + ".jsonl");
        }
        return new TraceWriter(path.toAbsolutePath().normalize(), Files.newBufferedWriter(path, StandardCharsets.UTF_8));
    }

    public Path path() {

        return this.path;
    }

    /**
     * Starts an event line; add fields to the returned object, then pass it to {@link #write}.
     */
    public static JsonObject event(long gameTime, String type) {

        JsonObject line = new JsonObject();
        line.addProperty("t", gameTime);
        line.addProperty("type", type);
        return line;
    }

    public void write(JsonObject line) {

        if (this.writer == null) {
            return;
        }
        try {
            this.writer.write(GSON.toJson(line));
            this.writer.newLine();
            this.writer.flush();
        } catch (IOException e) {
            Constants.LOG.error("Could not write to trace {}", this.path, e);
            close();
        }
    }

    public void close() {

        if (this.writer == null) {
            return;
        }
        try {
            this.writer.close();
        } catch (IOException e) {
            Constants.LOG.error("Could not close trace {}", this.path, e);
        }
        this.writer = null;
    }
}
