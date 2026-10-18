package io.github.mdw22.paranormalcraft.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.platform.Services;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * The loaded configs, read at startup. {@code /paranormalcraft debug config} can change values in memory (never
 * written to disk) and re-read the files; {@code debug pace} scales the timers in memory.
 */
public final class ModConfigs {

    /** {@code debug pace fast} multiplies the haunting, manifest and hallucination intervals by this. */
    public static final double FAST_PACE = 0.1;

    private static ServerConfig server = ServerConfig.DEFAULTS;
    private static ClientConfig client = ClientConfig.DEFAULTS;
    private static double pace = 1.0;

    private ModConfigs() {
    }

    public static ServerConfig server() {

        return server;
    }

    /**
     * Replaces the server config in memory: GameTests and {@code debug config set}. Never written to disk.
     */
    public static void setServerForTesting(ServerConfig config) {

        server = config;
    }

    public static ClientConfig client() {

        return client;
    }

    public static void setClient(ClientConfig config) {

        client = config;
    }

    /**
     * Whether {@code /paranormalcraft debug …} is available: the config switch, forced on in development.
     */
    public static boolean debugCommandsEnabled() {

        return server.enableDebugCommands() || Services.PLATFORM.isDevelopmentEnvironment();
    }

    /**
     * The timer multiplier from {@code debug pace}: 1 normally, {@link #FAST_PACE} when fast. In memory only; reset
     * when the server stops.
     */
    public static double pace() {

        return pace;
    }

    public static void setPace(double value) {

        pace = value;
    }

    /**
     * Drops the in-memory session state (the pace) when the server stops.
     */
    public static void resetSession() {

        pace = 1.0;
    }

    /**
     * A timer length in ticks, scaled by the current pace (at least 1 tick).
     */
    public static int paced(int ticks) {

        return pace == 1.0 ? ticks : Math.max(1, (int) Math.round(ticks * pace));
    }

    /**
     * Called from common init on both physical sides (the integrated server needs the server config too), and by
     * {@code debug config reload}.
     */
    public static void loadServer() {

        server = ServerConfig.load(Services.PLATFORM.getConfigDir(), Constants.LOG);
    }

    /**
     * Called from client init only, and when the server asks the client to reload.
     */
    public static void loadClient() {

        client = ClientConfig.load(Services.PLATFORM.getConfigDir(), Constants.LOG);
    }

    /**
     * Whether {@code key} (e.g. {@code temperatureUnit}) is a client config key rather than a server one.
     */
    public static boolean isClientKey(String key) {

        return ClientConfig.DEFAULTS.toJson().has(topLevel(key));
    }

    public static boolean isServerKey(String key) {

        return lookup(ServerConfig.DEFAULTS.toJson(), key) != null;
    }

    /**
     * The current value of a key, as JSON text. Section keys use a dot, e.g. {@code manifestIntervalSeconds.min}.
     * Null if there is no such key.
     */
    public static @Nullable String get(String key) {

        JsonElement value = lookup(isClientKey(key) ? client.toJson() : server.toJson(), key);
        return value == null ? null : value.toString();
    }

    /**
     * Sets a server key in memory, normalised exactly as the file would be (clamped, defaults for bad values).
     *
     * @return the value it ended up with, as JSON text
     */
    public static String setServer(String key, String value) {

        JsonObject json = server.toJson();
        put(json, key, parse(value));
        server = ServerConfig.read(ConfigFile.inMemory(json, ServerConfig.FILE_NAME + " (in memory)", Constants.LOG));
        return String.valueOf(get(key));
    }

    /**
     * Sets a client key in memory on this side (the client handler of {@code debug config set}).
     */
    public static String setClient(String key, String value) {

        JsonObject json = client.toJson();
        put(json, key, parse(value));
        client = ClientConfig.read(ConfigFile.inMemory(json, ClientConfig.FILE_NAME + " (in memory)", Constants.LOG));
        return String.valueOf(lookup(client.toJson(), key));
    }

    private static String topLevel(String key) {

        int dot = key.indexOf('.');
        return dot < 0 ? key : key.substring(0, dot);
    }

    private static @Nullable JsonElement lookup(JsonObject json, String key) {

        JsonElement current = json;
        for (String part : key.split("\\.")) {
            if (!(current instanceof JsonObject object) || !object.has(part)) {
                return null;
            }
            current = object.get(part);
        }
        return current;
    }

    private static void put(JsonObject json, String key, JsonElement value) {

        String[] parts = key.split("\\.");
        JsonObject target = json;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!(target.get(parts[i]) instanceof JsonObject next)) {
                return;
            }
            target = next;
        }
        target.add(parts[parts.length - 1], value);
    }

    /**
     * {@code true}/{@code false} become booleans, numbers become numbers, anything else a (lower-case) string.
     */
    static JsonPrimitive parse(String value) {

        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return new JsonPrimitive(Boolean.parseBoolean(value.toLowerCase(Locale.ROOT)));
        }
        try {
            double number = Double.parseDouble(value);
            if (number == Math.rint(number) && Math.abs(number) < Integer.MAX_VALUE) {
                return new JsonPrimitive((int) number);
            }
            return new JsonPrimitive(number);
        } catch (NumberFormatException e) {
            return new JsonPrimitive(value.toLowerCase(Locale.ROOT));
        }
    }
}
