package io.github.mdw22.paranormalcraft.config;

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
 */
public record ServerConfig(boolean hauntingGriefing, double ghostSpawnMultiplier, int manifestIntervalMinSeconds, int manifestIntervalMaxSeconds,
                           boolean enableDebugCommands) {

    public static final String FILE_NAME = "paranormalcraft-server.json";
    public static final ServerConfig DEFAULTS = new ServerConfig(true, 1.0, 120, 300, false);

    static final int MIN_INTERVAL_SECONDS = 1;
    static final int MAX_INTERVAL_SECONDS = 86400;

    public static ServerConfig load(Path configDir, Logger log) {

        ConfigFile file = ConfigFile.open(configDir.resolve(FILE_NAME), log);
        boolean griefing = file.getBoolean("hauntingGriefing", DEFAULTS.hauntingGriefing);
        double multiplier = file.getDouble("ghostSpawnMultiplier", DEFAULTS.ghostSpawnMultiplier, 0.0, 4.0);
        ConfigFile.Section interval = file.section("manifestIntervalSeconds");
        int min = interval.getInt("min", DEFAULTS.manifestIntervalMinSeconds, MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS);
        int max = interval.getInt("max", DEFAULTS.manifestIntervalMaxSeconds, MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS);
        if (max < min) {
            max = min;
            interval.fix("max", max, "is less than min");
        }
        boolean debugCommands = file.getBoolean("enableDebugCommands", DEFAULTS.enableDebugCommands);
        file.save();
        return new ServerConfig(griefing, multiplier, min, max, debugCommands);
    }
}
