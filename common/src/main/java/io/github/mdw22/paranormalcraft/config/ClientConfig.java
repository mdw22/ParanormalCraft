package io.github.mdw22.paranormalcraft.config;

import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Per-player settings, from {@code paranormalcraft-client.json}. Read only on the physical client.
 *
 * @param hallucinationIntensity how strong hallucination effects are
 * @param reduceFlashing         tones down flashing effects
 * @param temperatureUnit        how the Spirit Thermometer shows readings
 */
public record ClientConfig(HallucinationIntensity hallucinationIntensity, boolean reduceFlashing, TemperatureUnit temperatureUnit) {

    public static final String FILE_NAME = "paranormalcraft-client.json";
    public static final ClientConfig DEFAULTS = new ClientConfig(HallucinationIntensity.FULL, false, TemperatureUnit.CELSIUS);

    public enum HallucinationIntensity {
        OFF,
        REDUCED,
        FULL
    }

    public enum TemperatureUnit {
        CELSIUS,
        FAHRENHEIT
    }

    public static ClientConfig load(Path configDir, Logger log) {

        ConfigFile file = ConfigFile.open(configDir.resolve(FILE_NAME), log);
        HallucinationIntensity intensity = file.getEnum("hallucinationIntensity", HallucinationIntensity.class, DEFAULTS.hallucinationIntensity);
        boolean reduceFlashing = file.getBoolean("reduceFlashing", DEFAULTS.reduceFlashing);
        TemperatureUnit unit = file.getEnum("temperatureUnit", TemperatureUnit.class, DEFAULTS.temperatureUnit);
        file.save();
        return new ClientConfig(intensity, reduceFlashing, unit);
    }
}
