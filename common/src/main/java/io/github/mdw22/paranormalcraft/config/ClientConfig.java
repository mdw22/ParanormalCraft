package io.github.mdw22.paranormalcraft.config;

import com.google.gson.JsonObject;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Locale;

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
        ClientConfig config = read(file);
        file.save();
        return config;
    }

    /**
     * Reads (and normalises) every key from {@code file}.
     */
    public static ClientConfig read(ConfigFile file) {

        HallucinationIntensity intensity = file.getEnum("hallucinationIntensity", HallucinationIntensity.class, DEFAULTS.hallucinationIntensity);
        boolean reduceFlashing = file.getBoolean("reduceFlashing", DEFAULTS.reduceFlashing);
        TemperatureUnit unit = file.getEnum("temperatureUnit", TemperatureUnit.class, DEFAULTS.temperatureUnit);
        return new ClientConfig(intensity, reduceFlashing, unit);
    }

    /**
     * This config as it would be written to the file.
     */
    public JsonObject toJson() {

        JsonObject json = new JsonObject();
        json.addProperty("hallucinationIntensity", this.hallucinationIntensity.name().toLowerCase(Locale.ROOT));
        json.addProperty("reduceFlashing", this.reduceFlashing);
        json.addProperty("temperatureUnit", this.temperatureUnit.name().toLowerCase(Locale.ROOT));
        return json;
    }
}
