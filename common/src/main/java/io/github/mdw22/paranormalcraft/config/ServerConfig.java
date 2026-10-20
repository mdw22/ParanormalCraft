package io.github.mdw22.paranormalcraft.config;

import com.google.gson.JsonObject;
import io.github.mdw22.paranormalcraft.Balance;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * World rules, from {@code paranormalcraft-server.json}. Only the server reads these, so in multiplayer the server's
 * copy is the one that applies.
 *
 * @param hauntingGriefing          block-changing ghost actions need this and the {@code mobGriefing} game rule
 * @param ghostSpawnMultiplier      scales natural ghost spawning, 0 to 4
 * @param manifestIntervalMinSeconds shortest wait between a ghost's manifests
 * @param manifestIntervalMaxSeconds longest wait between a ghost's manifests
 * @param enableDebugCommands        allows {@code /paranormalcraft debug …} (always on in a development environment)
 * @param hauntingEnabled            turns ghost haunting actions on or off
 * @param hauntingIntervalMinSeconds shortest wait between a ghost's haunting actions
 * @param hauntingIntervalMaxSeconds longest wait between a ghost's haunting actions
 * @param hallucinationIntervalMinSeconds shortest wait between a player's hallucinations
 * @param hallucinationIntervalMaxSeconds longest wait between a player's hallucinations
 * @param wardLanternRadius          blocks a Ward Lantern protects (no monster spawns, haunting or shadow figures)
 */
public record ServerConfig(boolean hauntingGriefing, double ghostSpawnMultiplier, int manifestIntervalMinSeconds, int manifestIntervalMaxSeconds,
                           boolean enableDebugCommands, boolean hauntingEnabled, int hauntingIntervalMinSeconds, int hauntingIntervalMaxSeconds,
                           int hallucinationIntervalMinSeconds, int hallucinationIntervalMaxSeconds, int wardLanternRadius) {

    public static final String FILE_NAME = "paranormalcraft-server.json";
    public static final ServerConfig DEFAULTS = new ServerConfig(true, 1.0, 120, 300, false, true, 30, 90, 180, 480,
            Balance.Ward.DEFAULT_RADIUS);

    static final int MIN_INTERVAL_SECONDS = 1;
    static final int MAX_INTERVAL_SECONDS = 86400;

    public static ServerConfig load(Path configDir, Logger log) {

        ConfigFile file = ConfigFile.open(configDir.resolve(FILE_NAME), log);
        ServerConfig config = read(file);
        file.save();
        return config;
    }

    /**
     * Reads (and normalises) every key from {@code file}.
     */
    public static ServerConfig read(ConfigFile file) {

        boolean griefing = file.getBoolean("hauntingGriefing", DEFAULTS.hauntingGriefing);
        double multiplier = file.getDouble("ghostSpawnMultiplier", DEFAULTS.ghostSpawnMultiplier, 0.0, 4.0);
        int[] manifest = interval(file, "manifestIntervalSeconds", DEFAULTS.manifestIntervalMinSeconds, DEFAULTS.manifestIntervalMaxSeconds);
        boolean debugCommands = file.getBoolean("enableDebugCommands", DEFAULTS.enableDebugCommands);
        boolean haunting = file.getBoolean("hauntingEnabled", DEFAULTS.hauntingEnabled);
        int[] hauntingInterval = interval(file, "hauntingIntervalSeconds", DEFAULTS.hauntingIntervalMinSeconds, DEFAULTS.hauntingIntervalMaxSeconds);
        int[] hallucination = interval(file, "hallucinationIntervalSeconds", DEFAULTS.hallucinationIntervalMinSeconds, DEFAULTS.hallucinationIntervalMaxSeconds);
        return new ServerConfig(griefing, multiplier, manifest[0], manifest[1], debugCommands, haunting, hauntingInterval[0], hauntingInterval[1],
                hallucination[0], hallucination[1], file.getInt("wardLanternRadius", DEFAULTS.wardLanternRadius, 1, 256));
    }

    /**
     * This config as it would be written to the file.
     */
    public JsonObject toJson() {

        JsonObject json = new JsonObject();
        json.addProperty("hauntingGriefing", this.hauntingGriefing);
        json.addProperty("ghostSpawnMultiplier", this.ghostSpawnMultiplier);
        json.add("manifestIntervalSeconds", interval(this.manifestIntervalMinSeconds, this.manifestIntervalMaxSeconds));
        json.addProperty("enableDebugCommands", this.enableDebugCommands);
        json.addProperty("hauntingEnabled", this.hauntingEnabled);
        json.add("hauntingIntervalSeconds", interval(this.hauntingIntervalMinSeconds, this.hauntingIntervalMaxSeconds));
        json.add("hallucinationIntervalSeconds", interval(this.hallucinationIntervalMinSeconds, this.hallucinationIntervalMaxSeconds));
        json.addProperty("wardLanternRadius", this.wardLanternRadius);
        return json;
    }

    private static JsonObject interval(int min, int max) {

        JsonObject json = new JsonObject();
        json.addProperty("min", min);
        json.addProperty("max", max);
        return json;
    }

    /**
     * A copy with a different griefing switch (GameTests).
     */
    public ServerConfig withHauntingGriefing(boolean griefing) {

        return new ServerConfig(griefing, this.ghostSpawnMultiplier, this.manifestIntervalMinSeconds, this.manifestIntervalMaxSeconds,
                this.enableDebugCommands, this.hauntingEnabled, this.hauntingIntervalMinSeconds, this.hauntingIntervalMaxSeconds,
                this.hallucinationIntervalMinSeconds, this.hallucinationIntervalMaxSeconds, this.wardLanternRadius);
    }

    /**
     * A copy with a different haunting interval in seconds (GameTests).
     */
    public ServerConfig withHauntingInterval(int minSeconds, int maxSeconds) {

        return new ServerConfig(this.hauntingGriefing, this.ghostSpawnMultiplier, this.manifestIntervalMinSeconds, this.manifestIntervalMaxSeconds,
                this.enableDebugCommands, this.hauntingEnabled, minSeconds, maxSeconds,
                this.hallucinationIntervalMinSeconds, this.hallucinationIntervalMaxSeconds, this.wardLanternRadius);
    }

    /**
     * Reads a {@code {"min": …, "max": …}} section in seconds, raising max to min if it is smaller.
     */
    private static int[] interval(ConfigFile file, String key, int defaultMin, int defaultMax) {

        ConfigFile.Section section = file.section(key);
        int min = section.getInt("min", defaultMin, MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS);
        int max = section.getInt("max", defaultMax, MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS);
        if (max < min) {
            max = min;
            section.fix("max", max, "is less than min");
        }
        return new int[]{min, max};
    }
}
