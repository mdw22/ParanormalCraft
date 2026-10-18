package io.github.mdw22.paranormalcraft.config;

import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Per-player settings, from {@code paranormalcraft-client.json}. Read only on the physical client.
 *
 * @param hallucinationIntensity how strong hallucination effects are
 * @param reduceFlashing         tones down flashing effects
 */
public record ClientConfig(HallucinationIntensity hallucinationIntensity, boolean reduceFlashing) {

    public static final String FILE_NAME = "paranormalcraft-client.json";
    public static final ClientConfig DEFAULTS = new ClientConfig(HallucinationIntensity.FULL, false);

    public enum HallucinationIntensity {
        OFF,
        REDUCED,
        FULL
    }

    public static ClientConfig load(Path configDir, Logger log) {

        ConfigFile file = ConfigFile.open(configDir.resolve(FILE_NAME), log);
        HallucinationIntensity intensity = file.getEnum("hallucinationIntensity", HallucinationIntensity.class, DEFAULTS.hallucinationIntensity);
        boolean reduceFlashing = file.getBoolean("reduceFlashing", DEFAULTS.reduceFlashing);
        file.save();
        return new ClientConfig(intensity, reduceFlashing);
    }
}
