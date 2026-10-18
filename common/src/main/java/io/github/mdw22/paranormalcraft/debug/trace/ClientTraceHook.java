package io.github.mdw22.paranormalcraft.debug.trace;

import com.google.gson.JsonObject;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Lets common entity code (the client-only shadow figure) write client trace events without referencing client
 * classes. Client init installs the real hooks; on a server they stay no-ops.
 */
public final class ClientTraceHook {

    private static BiConsumer<String, Consumer<JsonObject>> event = (type, fields) -> {
    };
    private static Runnable markNextSoundLocal = () -> {
    };

    private ClientTraceHook() {
    }

    public static void install(BiConsumer<String, Consumer<JsonObject>> eventHook, Runnable localSoundHook) {

        event = eventHook;
        markNextSoundLocal = localSoundHook;
    }

    public static void event(String type, Consumer<JsonObject> fields) {

        event.accept(type, fields);
    }

    public static void markNextSoundLocal() {

        markNextSoundLocal.run();
    }

    public static void addPos(JsonObject line, double x, double y, double z) {

        line.addProperty("x", Math.round(x * 1000.0) / 1000.0);
        line.addProperty("y", Math.round(y * 1000.0) / 1000.0);
        line.addProperty("z", Math.round(z * 1000.0) / 1000.0);
    }
}
