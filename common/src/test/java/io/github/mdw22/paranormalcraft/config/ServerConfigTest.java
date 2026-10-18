package io.github.mdw22.paranormalcraft.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConfigTest {

    @TempDir
    Path dir;

    private final CapturingLogger log = new CapturingLogger();

    private Path file() {

        return this.dir.resolve(ServerConfig.FILE_NAME);
    }

    private void write(String json) throws IOException {

        Files.writeString(file(), json, StandardCharsets.UTF_8);
    }

    private JsonObject readBack() throws IOException {

        return JsonParser.parseString(Files.readString(file(), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void missingFileGivesDefaultsAndWritesThem() throws IOException {

        ServerConfig config = ServerConfig.load(this.dir, this.log);

        assertEquals(ServerConfig.DEFAULTS, config);
        JsonObject written = readBack();
        assertTrue(written.get("hauntingGriefing").getAsBoolean());
        assertEquals(1.0, written.get("ghostSpawnMultiplier").getAsDouble());
        assertEquals(120, written.getAsJsonObject("manifestIntervalSeconds").get("min").getAsInt());
        assertEquals(300, written.getAsJsonObject("manifestIntervalSeconds").get("max").getAsInt());
        assertFalse(written.get("enableDebugCommands").getAsBoolean());
        assertTrue(written.get("hauntingEnabled").getAsBoolean());
        assertEquals(30, written.getAsJsonObject("hauntingIntervalSeconds").get("min").getAsInt());
        assertEquals(90, written.getAsJsonObject("hauntingIntervalSeconds").get("max").getAsInt());
        assertEquals(180, written.getAsJsonObject("hallucinationIntervalSeconds").get("min").getAsInt());
        assertEquals(480, written.getAsJsonObject("hallucinationIntervalSeconds").get("max").getAsInt());
        assertEquals(0, this.log.count(Level.WARN));
    }

    @Test
    void missingKeysAreFilledInAndExistingOnesKept() throws IOException {

        write("{\"hauntingGriefing\": false, \"manifestIntervalSeconds\": {\"max\": 600}}");

        ServerConfig config = ServerConfig.load(this.dir, this.log);

        assertFalse(config.hauntingGriefing());
        assertEquals(1.0, config.ghostSpawnMultiplier());
        assertEquals(120, config.manifestIntervalMinSeconds());
        assertEquals(600, config.manifestIntervalMaxSeconds());
        JsonObject written = readBack();
        assertEquals(1.0, written.get("ghostSpawnMultiplier").getAsDouble());
        assertEquals(120, written.getAsJsonObject("manifestIntervalSeconds").get("min").getAsInt());
        assertEquals(0, this.log.count(Level.WARN));
    }

    @Test
    void outOfRangeValuesAreClampedWithOneWarningEach() throws IOException {

        write("{\"hauntingGriefing\": true, \"ghostSpawnMultiplier\": 9.5, \"manifestIntervalSeconds\": {\"min\": 0, \"max\": 999999}}");

        ServerConfig config = ServerConfig.load(this.dir, this.log);

        assertEquals(4.0, config.ghostSpawnMultiplier());
        assertEquals(ServerConfig.MIN_INTERVAL_SECONDS, config.manifestIntervalMinSeconds());
        assertEquals(ServerConfig.MAX_INTERVAL_SECONDS, config.manifestIntervalMaxSeconds());
        assertEquals(3, this.log.count(Level.WARN));
        assertEquals(4.0, readBack().get("ghostSpawnMultiplier").getAsDouble());
    }

    @Test
    void negativeMultiplierClampsToZero() throws IOException {

        write("{\"ghostSpawnMultiplier\": -1}");

        assertEquals(0.0, ServerConfig.load(this.dir, this.log).ghostSpawnMultiplier());
        assertEquals(1, this.log.count(Level.WARN));
    }

    @Test
    void wrongTypesFallBackToDefaultsWithOneWarningEach() throws IOException {

        write("{\"hauntingGriefing\": \"yes\", \"ghostSpawnMultiplier\": \"lots\", \"manifestIntervalSeconds\": {\"min\": 1.5, \"max\": 300}}");

        ServerConfig config = ServerConfig.load(this.dir, this.log);

        assertTrue(config.hauntingGriefing());
        assertEquals(1.0, config.ghostSpawnMultiplier());
        assertEquals(120, config.manifestIntervalMinSeconds());
        assertEquals(3, this.log.count(Level.WARN));
        assertTrue(readBack().get("hauntingGriefing").getAsBoolean());
    }

    @Test
    void maxBelowMinIsRaisedToMin() throws IOException {

        write("{\"manifestIntervalSeconds\": {\"min\": 200, \"max\": 100}}");

        ServerConfig config = ServerConfig.load(this.dir, this.log);

        assertEquals(200, config.manifestIntervalMinSeconds());
        assertEquals(200, config.manifestIntervalMaxSeconds());
        assertEquals(1, this.log.count(Level.WARN));
        assertEquals(200, readBack().getAsJsonObject("manifestIntervalSeconds").get("max").getAsInt());
    }

    @Test
    void newIntervalsAreCheckedLikeTheManifestOne() throws IOException {

        write("{\"hauntingIntervalSeconds\": {\"min\": 100, \"max\": 50}, \"hallucinationIntervalSeconds\": {\"min\": 0}}");

        ServerConfig config = ServerConfig.load(this.dir, this.log);

        assertEquals(100, config.hauntingIntervalMinSeconds());
        assertEquals(100, config.hauntingIntervalMaxSeconds());
        assertEquals(ServerConfig.MIN_INTERVAL_SECONDS, config.hallucinationIntervalMinSeconds());
        assertEquals(480, config.hallucinationIntervalMaxSeconds());
        assertEquals(2, this.log.count(Level.WARN));
    }

    @Test
    void brokenFileIsBackedUpAndRegenerated() throws IOException {

        String broken = "{\"hauntingGriefing\": false,, oops";
        write(broken);

        ServerConfig config = ServerConfig.load(this.dir, this.log);

        assertEquals(ServerConfig.DEFAULTS, config);
        Path backup = this.dir.resolve(ServerConfig.FILE_NAME + ".bak");
        assertEquals(broken, Files.readString(backup, StandardCharsets.UTF_8));
        assertTrue(readBack().get("hauntingGriefing").getAsBoolean());
        assertTrue(this.log.count(Level.ERROR) >= 1);
    }

    @Test
    void nonObjectFileIsTreatedAsBroken() throws IOException {

        write("[1, 2, 3]");

        assertEquals(ServerConfig.DEFAULTS, ServerConfig.load(this.dir, this.log));
        assertTrue(Files.exists(this.dir.resolve(ServerConfig.FILE_NAME + ".bak")));
    }

    @Test
    void validFileIsNotRewritten() throws IOException {

        String valid = "{\"hauntingGriefing\":false,\"ghostSpawnMultiplier\":2.5,\"manifestIntervalSeconds\":{\"min\":30,\"max\":60},\"enableDebugCommands\":true,"
                + "\"hauntingEnabled\":false,\"hauntingIntervalSeconds\":{\"min\":5,\"max\":10},\"hallucinationIntervalSeconds\":{\"min\":20,\"max\":40}}";
        write(valid);

        ServerConfig config = ServerConfig.load(this.dir, this.log);

        assertEquals(new ServerConfig(false, 2.5, 30, 60, true, false, 5, 10, 20, 40), config);
        assertEquals(valid, Files.readString(file(), StandardCharsets.UTF_8));
        assertEquals(0, this.log.count(Level.WARN));
    }

    @Test
    void unknownKeysAreKeptWhenTheFileIsRewritten() throws IOException {

        write("{\"futureSetting\": 7}");

        ServerConfig.load(this.dir, this.log);

        assertEquals(7, readBack().get("futureSetting").getAsInt());
    }
}
