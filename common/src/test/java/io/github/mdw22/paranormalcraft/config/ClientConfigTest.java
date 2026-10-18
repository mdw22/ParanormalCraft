package io.github.mdw22.paranormalcraft.config;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientConfigTest {

    @TempDir
    Path dir;

    private final CapturingLogger log = new CapturingLogger();

    private void write(String json) throws IOException {

        Files.writeString(this.dir.resolve(ClientConfig.FILE_NAME), json, StandardCharsets.UTF_8);
    }

    @Test
    void defaults() {

        assertEquals(ClientConfig.DEFAULTS, ClientConfig.load(this.dir, this.log));
        assertEquals(ClientConfig.HallucinationIntensity.FULL, ClientConfig.DEFAULTS.hallucinationIntensity());
        assertTrue(Files.exists(this.dir.resolve(ClientConfig.FILE_NAME)));
    }

    @Test
    void readsEnumCaseInsensitively() throws IOException {

        write("{\"hallucinationIntensity\": \"REDUCED\", \"reduceFlashing\": true}");

        ClientConfig config = ClientConfig.load(this.dir, this.log);

        assertEquals(ClientConfig.HallucinationIntensity.REDUCED, config.hallucinationIntensity());
        assertTrue(config.reduceFlashing());
        assertEquals(0, this.log.count(Level.WARN));
    }

    @Test
    void unknownEnumValueFallsBackWithAWarning() throws IOException {

        write("{\"hallucinationIntensity\": \"loud\", \"reduceFlashing\": false}");

        ClientConfig config = ClientConfig.load(this.dir, this.log);

        assertEquals(ClientConfig.HallucinationIntensity.FULL, config.hallucinationIntensity());
        assertEquals(1, this.log.count(Level.WARN));
        String written = Files.readString(this.dir.resolve(ClientConfig.FILE_NAME), StandardCharsets.UTF_8);
        assertEquals("full", JsonParser.parseString(written).getAsJsonObject().get("hallucinationIntensity").getAsString());
    }

    @Test
    void temperatureUnitDefaultsToCelsiusAndReadsFahrenheit() throws IOException {

        assertEquals(ClientConfig.TemperatureUnit.CELSIUS, ClientConfig.DEFAULTS.temperatureUnit());
        write("{\"temperatureUnit\": \"fahrenheit\"}");

        assertEquals(ClientConfig.TemperatureUnit.FAHRENHEIT, ClientConfig.load(this.dir, this.log).temperatureUnit());
        assertEquals(0, this.log.count(Level.WARN));
    }

    @Test
    void brokenFileRecovers() throws IOException {

        write("not json at all {");

        assertEquals(ClientConfig.DEFAULTS, ClientConfig.load(this.dir, this.log));
        assertTrue(Files.exists(this.dir.resolve(ClientConfig.FILE_NAME + ".bak")));
    }
}
