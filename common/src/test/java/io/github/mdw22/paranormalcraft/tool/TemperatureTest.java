package io.github.mdw22.paranormalcraft.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemperatureTest {

    private static final double EPSILON = 1.0E-9;
    private static final double NONE = Double.POSITIVE_INFINITY;

    @Test
    void biomeBaseTemperatures() {

        assertEquals(16.0, Temperature.celsius(0.8, false, 63, 63, NONE, 0), EPSILON);
        assertEquals(0.0, Temperature.celsius(0.0, false, 63, 63, NONE, 0), EPSILON);
        assertEquals(40.0, Temperature.celsius(2.0, false, 63, 63, NONE, 0), EPSILON);
    }

    @Test
    void nightIsThreeDegreesColder() {

        assertEquals(13.0, Temperature.celsius(0.8, true, 63, 63, NONE, 0), EPSILON);
    }

    @Test
    void altitudeOnlyCountsAboveSeaLevel() {

        assertEquals(-1.0, Temperature.altitudeOffset(79, 63), EPSILON);
        assertEquals(-4.0, Temperature.altitudeOffset(127, 63), EPSILON);
        assertEquals(0.0, Temperature.altitudeOffset(10, 63), EPSILON);
        assertEquals(14.0, Temperature.celsius(0.8, false, 95, 63, NONE, 0), EPSILON);
    }

    @Test
    void coldSpotFallsOffLinearlyToFourBlocks() {

        assertEquals(-10.0, Temperature.coldSpotOffset(0.0), EPSILON);
        assertEquals(-7.5, Temperature.coldSpotOffset(1.0), EPSILON);
        assertEquals(-5.0, Temperature.coldSpotOffset(2.0), EPSILON);
        assertEquals(-2.5, Temperature.coldSpotOffset(3.0), EPSILON);
        assertEquals(0.0, Temperature.coldSpotOffset(4.0), EPSILON);
        assertEquals(0.0, Temperature.coldSpotOffset(5.0), EPSILON);
        assertEquals(0.0, Temperature.coldSpotOffset(NONE), EPSILON);
    }

    @Test
    void overlappingColdSpotsUseTheStrongest() {

        assertEquals(-7.5, Temperature.strongestColdSpotOffset(3.0, 1.0, 2.0), EPSILON);
        assertEquals(0.0, Temperature.strongestColdSpotOffset(), EPSILON);
        assertEquals(0.0, Temperature.strongestColdSpotOffset(6.0, NONE), EPSILON);
    }

    @Test
    void everythingCombines() {

        // Plains at night, 16 blocks up, standing on a cold spot's centre, with +0.2 noise.
        assertEquals(16.0 - 3.0 - 1.0 - 10.0 + 0.2, Temperature.celsius(0.8, true, 79, 63, 0.0, 0.2), EPSILON);
    }

    @Test
    void unitsAndBands() {

        assertEquals(32.0, Temperature.toFahrenheit(0.0), EPSILON);
        assertEquals(212.0, Temperature.toFahrenheit(100.0), EPSILON);
        assertEquals(Temperature.Band.COLD, Temperature.band(4.9));
        assertEquals(Temperature.Band.MILD, Temperature.band(5.0));
        assertEquals(Temperature.Band.MILD, Temperature.band(25.0));
        assertEquals(Temperature.Band.WARM, Temperature.band(25.1));
    }
}
