package io.github.mdw22.paranormalcraft.tool;

/**
 * The Spirit Thermometer's maths, in °C. Plain logic so it can be unit tested; {@link ThermometerReadings} feeds it
 * from the world.
 */
public final class Temperature {

    public static final double DEGREES_PER_BIOME_UNIT = 20.0;
    public static final double NIGHT_OFFSET = -3.0;
    public static final double BLOCKS_PER_DEGREE_OF_ALTITUDE = 16.0;
    public static final double COLD_SPOT_STRENGTH = 10.0;
    public static final double COLD_SPOT_RADIUS = 4.0;
    public static final double NOISE = 0.3;
    /** Below this the holder's breath shows. */
    public static final double BREATH_BELOW = 3.0;
    /** Display bands: blue below, orange above. */
    public static final double COLD_BELOW = 5.0;
    public static final double WARM_ABOVE = 25.0;

    public enum Band {
        COLD,
        MILD,
        WARM
    }

    private Temperature() {
    }

    /**
     * @param biomeTemperature  the biome's base temperature (plains 0.8, desert 2.0)
     * @param night             whether it is night
     * @param y                 the reading height
     * @param seaLevel          the level's sea level
     * @param coldSpotDistance  distance to the nearest cold spot centre, or {@link Double#POSITIVE_INFINITY}
     * @param noise             readability noise, expected in [-{@link #NOISE}, {@link #NOISE}]
     */
    public static double celsius(double biomeTemperature, boolean night, double y, int seaLevel, double coldSpotDistance, double noise) {

        return biomeTemperature * DEGREES_PER_BIOME_UNIT
                + (night ? NIGHT_OFFSET : 0.0)
                + altitudeOffset(y, seaLevel)
                + coldSpotOffset(coldSpotDistance)
                + noise;
    }

    /**
     * −1° per 16 blocks above sea level; nothing below it.
     */
    public static double altitudeOffset(double y, int seaLevel) {

        return -Math.max(0.0, y - seaLevel) / BLOCKS_PER_DEGREE_OF_ALTITUDE;
    }

    /**
     * −10° at the centre, rising linearly to 0 at {@link #COLD_SPOT_RADIUS} blocks out.
     */
    public static double coldSpotOffset(double distance) {

        if (!(distance < COLD_SPOT_RADIUS)) {
            return 0.0;
        }
        return -COLD_SPOT_STRENGTH * (1.0 - Math.max(0.0, distance) / COLD_SPOT_RADIUS);
    }

    /**
     * Overlapping cold spots don't stack: the strongest (coldest) one wins.
     */
    public static double strongestColdSpotOffset(double... distances) {

        double strongest = 0.0;
        for (double distance : distances) {
            strongest = Math.min(strongest, coldSpotOffset(distance));
        }
        return strongest;
    }

    public static double toFahrenheit(double celsius) {

        return celsius * 9.0 / 5.0 + 32.0;
    }

    public static Band band(double celsius) {

        if (celsius < COLD_BELOW) {
            return Band.COLD;
        }
        return celsius > WARM_ABOVE ? Band.WARM : Band.MILD;
    }
}
