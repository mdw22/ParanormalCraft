package io.github.mdw22.paranormalcraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;

/**
 * Reads one JSON config file and normalises it: missing keys get their defaults, wrong types are reset, out-of-range
 * values are clamped (one warning per bad value), and a file that can't be parsed is backed up as {@code .bak} and
 * regenerated. Never throws for a bad file.
 * <p>
 * Usage: {@link #open}, read every key with the typed getters in the order they should be written, then {@link #save()}.
 */
public final class ConfigFile {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path path;
    private final Logger log;
    private final JsonObject source;
    private final JsonObject normalised = new JsonObject();
    private boolean changed;
    private boolean inMemory;

    private ConfigFile(Path path, Logger log, JsonObject source, boolean changed) {

        this.path = path;
        this.log = log;
        this.source = source;
        this.changed = changed;
    }

    /**
     * Normalises an in-memory object (e.g. a config with one key changed by {@code debug config set}). {@link #save()}
     * on it never writes anything.
     */
    public static ConfigFile inMemory(JsonObject source, String label, Logger log) {

        ConfigFile file = new ConfigFile(Path.of(label), log, source, false);
        file.inMemory = true;
        return file;
    }

    /**
     * The normalised values read so far, as JSON.
     */
    public JsonObject normalised() {

        return this.normalised.deepCopy();
    }

    public static ConfigFile open(Path path, Logger log) {

        if (!Files.exists(path)) {
            log.info("Creating config file {}", path);
            return new ConfigFile(path, log, new JsonObject(), true);
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed.isJsonObject()) {
                return new ConfigFile(path, log, parsed.getAsJsonObject(), false);
            }
            log.error("Config file {} is not a JSON object", path);
        } catch (Exception e) {
            log.error("Config file {} could not be read: {}", path, e.getMessage());
        }

        Path backup = path.resolveSibling(path.getFileName() + ".bak");
        try {
            Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
            log.error("Moved the broken config to {} and regenerated it with defaults", backup);
        } catch (IOException e) {
            log.error("Could not back up the broken config {}", path, e);
        }
        return new ConfigFile(path, log, new JsonObject(), true);
    }

    /**
     * True when the file was missing, broken or had values that had to be fixed.
     */
    public boolean changed() {

        return this.changed;
    }

    public boolean getBoolean(String key, boolean defaultValue) {

        return getBoolean(this.source, this.normalised, key, key, defaultValue);
    }

    public double getDouble(String key, double defaultValue, double min, double max) {

        return getDouble(this.source, this.normalised, key, key, defaultValue, min, max);
    }

    public int getInt(String key, int defaultValue, int min, int max) {

        return getInt(this.source, this.normalised, key, key, defaultValue, min, max);
    }

    /**
     * Reads an enum stored as its lower-case name.
     */
    public <E extends Enum<E>> E getEnum(String key, Class<E> type, E defaultValue) {

        JsonElement element = this.source.get(key);
        E value = defaultValue;
        if (element == null) {
            this.changed = true;
        } else {
            value = null;
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String name = element.getAsString().toUpperCase(Locale.ROOT);
                for (E constant : type.getEnumConstants()) {
                    if (constant.name().equals(name)) {
                        value = constant;
                    }
                }
            }
            if (value == null) {
                warnInvalid(key, element, defaultValue.name().toLowerCase(Locale.ROOT));
                value = defaultValue;
            }
        }
        this.normalised.addProperty(key, value.name().toLowerCase(Locale.ROOT));
        return value;
    }

    /**
     * A nested object of ints, e.g. {@code {"min": 120, "max": 300}}.
     */
    public Section section(String key) {

        JsonElement element = this.source.get(key);
        JsonObject sourceSection;
        if (element == null) {
            this.changed = true;
            sourceSection = new JsonObject();
        } else if (element.isJsonObject()) {
            sourceSection = element.getAsJsonObject();
        } else {
            this.log.warn("Config {}: '{}' should be an object, got {}; using defaults", this.path.getFileName(), key, element);
            this.changed = true;
            sourceSection = new JsonObject();
        }
        JsonObject out = new JsonObject();
        this.normalised.add(key, out);
        return new Section(key, sourceSection, out);
    }

    /**
     * Writes the normalised file if anything was missing or fixed. Keys this config doesn't know are kept.
     */
    public void save() {

        if (!this.changed || this.inMemory) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : this.source.entrySet()) {
            if (!this.normalised.has(entry.getKey())) {
                this.normalised.add(entry.getKey(), entry.getValue());
            }
        }
        try {
            Files.createDirectories(this.path.getParent());
            try (Writer writer = Files.newBufferedWriter(this.path, StandardCharsets.UTF_8)) {
                GSON.toJson(this.normalised, writer);
            }
        } catch (IOException e) {
            this.log.error("Could not write config file {}", this.path, e);
        }
    }

    public final class Section {

        private final String name;
        private final JsonObject source;
        private final JsonObject out;

        private Section(String name, JsonObject source, JsonObject out) {

            this.name = name;
            this.source = source;
            this.out = out;
        }

        public int getInt(String key, int defaultValue, int min, int max) {

            return ConfigFile.this.getInt(this.source, this.out, key, this.name + "." + key, defaultValue, min, max);
        }

        /**
         * Replaces an already-read value, e.g. after a cross-field check. Logs a warning and marks the file changed.
         */
        public void fix(String key, int value, String reason) {

            ConfigFile.this.log.warn("Config {}: '{}.{}' {}; using {}", ConfigFile.this.path.getFileName(), this.name, key, reason, value);
            ConfigFile.this.changed = true;
            this.out.addProperty(key, value);
        }
    }

    private boolean getBoolean(JsonObject in, JsonObject out, String key, String label, boolean defaultValue) {

        JsonElement element = in.get(key);
        boolean value = defaultValue;
        if (element == null) {
            this.changed = true;
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            value = element.getAsBoolean();
        } else {
            warnInvalid(label, element, defaultValue);
        }
        out.addProperty(key, value);
        return value;
    }

    private double getDouble(JsonObject in, JsonObject out, String key, String label, double defaultValue, double min, double max) {

        JsonElement element = in.get(key);
        double value = defaultValue;
        if (element == null) {
            this.changed = true;
        } else if (isNumber(element) && Double.isFinite(element.getAsDouble())) {
            value = element.getAsDouble();
            if (value < min || value > max) {
                double clamped = Math.clamp(value, min, max);
                warnClamped(label, value, min, max, clamped);
                value = clamped;
            }
        } else {
            warnInvalid(label, element, defaultValue);
        }
        out.addProperty(key, value);
        return value;
    }

    private int getInt(JsonObject in, JsonObject out, String key, String label, int defaultValue, int min, int max) {

        JsonElement element = in.get(key);
        int value = defaultValue;
        if (element == null) {
            this.changed = true;
        } else if (isNumber(element) && isWhole(element.getAsDouble())) {
            double raw = element.getAsDouble();
            if (raw < min || raw > max) {
                int clamped = (int) Math.clamp((long) raw, min, max);
                warnClamped(label, element.getAsNumber(), min, max, clamped);
                value = clamped;
            } else {
                value = (int) raw;
            }
        } else {
            warnInvalid(label, element, defaultValue);
        }
        out.addProperty(key, value);
        return value;
    }

    private static boolean isNumber(JsonElement element) {

        return element instanceof JsonPrimitive primitive && primitive.isNumber();
    }

    private static boolean isWhole(double value) {

        return Double.isFinite(value) && value == Math.rint(value);
    }

    private void warnInvalid(String label, JsonElement found, Object defaultValue) {

        this.log.warn("Config {}: '{}' has invalid value {}; using default {}", this.path.getFileName(), label, found, defaultValue);
        this.changed = true;
    }

    private void warnClamped(String label, Object found, Object min, Object max, Object clamped) {

        this.log.warn("Config {}: '{}' value {} is outside {} to {}; using {}", this.path.getFileName(), label, found, min, max, clamped);
        this.changed = true;
    }
}
