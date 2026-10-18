package io.github.mdw22.paranormalcraft.client;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.config.ClientConfig;
import io.github.mdw22.paranormalcraft.config.ModConfigs;
import io.github.mdw22.paranormalcraft.network.EmfLevelPayload;
import io.github.mdw22.paranormalcraft.network.ThermometerReadingPayload;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.ModSounds;
import io.github.mdw22.paranormalcraft.tool.EmfLevels;
import io.github.mdw22.paranormalcraft.tool.Temperature;
import io.github.mdw22.paranormalcraft.tool.ToolTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Client side of the EMF Reader and Spirit Thermometer: the last readings the server sent this player, the reader's
 * ticking sound, the thermometer's action bar text and the cold-breath puffs. Everything here is local to this client.
 */
public final class ClientToolState {

    /** Ticks between EMF ticks for levels 1 to 5: one every 2 s at level 1, a crackle at level 5. */
    private static final int[] EMF_TICK_INTERVALS = {40, 20, 10, 5, 2};
    private static final int BREATH_INTERVAL_TICKS = 40;
    /** Readings older than this are stale (the server stopped sending, e.g. the tool was put away). */
    private static final int READING_TIMEOUT_TICKS = 30;
    private static final int COLD_COLOUR = 0x6FB7FF;
    private static final int MILD_COLOUR = 0xFFFFFF;
    private static final int WARM_COLOUR = 0xFFA040;

    private static int emfLevel = EmfLevels.MIN_LEVEL;
    private static int emfPayloadsReceived;
    private static int ticksSinceEmfSound;
    private static double lastCelsius = Double.NaN;
    private static long lastReadingTime = Long.MIN_VALUE;
    private static long lastBreathTime = Long.MIN_VALUE;
    private static Component lastActionBar = Component.empty();

    private ClientToolState() {
    }

    /**
     * The level the local player's reader shows.
     */
    public static int emfLevel() {

        return emfLevel;
    }

    /**
     * How many EMF payloads this client has received since it started (a test hook).
     */
    public static int emfPayloadsReceived() {

        return emfPayloadsReceived;
    }

    public static double lastCelsius() {

        return lastCelsius;
    }

    /**
     * The last action bar text the thermometer showed (a test hook).
     */
    public static Component lastActionBar() {

        return lastActionBar;
    }

    public static void onEmfLevel(EmfLevelPayload payload) {

        int level = Mth.clamp(payload.level(), EmfLevels.MIN_LEVEL, EmfLevels.MAX_LEVEL);
        emfPayloadsReceived++;
        if (level != emfLevel || emfPayloadsReceived == 1) {
            // Only on change, so the log stays quiet but still shows which client got readings.
            Constants.LOG.info("EMF reader now at level {} ({} reading(s) received)", level, emfPayloadsReceived);
        }
        emfLevel = level;
    }

    public static void onThermometerReading(ThermometerReadingPayload payload) {

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        lastCelsius = payload.celsius();
        lastReadingTime = minecraft.level.getGameTime();
        lastActionBar = format(lastCelsius, ModConfigs.client().temperatureUnit());
        minecraft.gui.hud.setOverlayMessage(lastActionBar, false);
    }

    /**
     * The action bar text: one decimal place in the configured unit, coloured by the °C band.
     */
    public static Component format(double celsius, ClientConfig.TemperatureUnit unit) {

        double shown = unit == ClientConfig.TemperatureUnit.FAHRENHEIT ? Temperature.toFahrenheit(celsius) : celsius;
        String suffix = unit == ClientConfig.TemperatureUnit.FAHRENHEIT ? "°F" : "°C";
        int colour = switch (Temperature.band(celsius)) {
            case COLD -> COLD_COLOUR;
            case MILD -> MILD_COLOUR;
            case WARM -> WARM_COLOUR;
        };
        return Component.literal(String.format(Locale.ROOT, "%.1f%s", shown, suffix)).withColor(colour);
    }

    /**
     * Called at the end of every client tick.
     */
    public static void tick(Minecraft minecraft) {

        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        tickEmfSound(player);
        tickBreath(minecraft, player);
    }

    private static void tickEmfSound(LocalPlayer player) {

        if (!ToolTracker.isHolding(player, ModItems.EMF_READER.get())) {
            ticksSinceEmfSound = 0;
            return;
        }
        if (++ticksSinceEmfSound >= EMF_TICK_INTERVALS[emfLevel - 1]) {
            ticksSinceEmfSound = 0;
            float pitch = 0.8F + 0.15F * emfLevel + player.getRandom().nextFloat() * 0.1F;
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.EMF_READER_TICK.get(), SoundSource.PLAYERS, 0.25F, pitch, false);
        }
    }

    private static void tickBreath(Minecraft minecraft, LocalPlayer player) {

        long now = minecraft.level.getGameTime();
        boolean freshCold = now - lastReadingTime <= READING_TIMEOUT_TICKS && lastCelsius < Temperature.BREATH_BELOW;
        if (!freshCold || !ToolTracker.isHolding(player, ModItems.SPIRIT_THERMOMETER.get()) || now - lastBreathTime < BREATH_INTERVAL_TICKS) {
            return;
        }
        lastBreathTime = now;
        Vec3 look = player.getLookAngle();
        Vec3 mouth = player.getEyePosition().add(look.scale(0.45)).add(0, -0.15, 0);
        for (int i = 0; i < 4; i++) {
            minecraft.level.addParticle(ParticleTypes.CLOUD, mouth.x, mouth.y, mouth.z,
                    look.x * 0.03 + (player.getRandom().nextDouble() - 0.5) * 0.01, 0.01, look.z * 0.03 + (player.getRandom().nextDouble() - 0.5) * 0.01);
        }
    }

    /**
     * Forget readings when leaving a world.
     */
    public static void reset() {

        emfLevel = EmfLevels.MIN_LEVEL;
        lastCelsius = Double.NaN;
        lastReadingTime = Long.MIN_VALUE;
        lastBreathTime = Long.MIN_VALUE;
    }
}
