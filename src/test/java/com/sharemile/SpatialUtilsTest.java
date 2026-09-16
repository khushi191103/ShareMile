package com.sharemile;

import com.sharemile.util.SpatialUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpatialUtilsTest {

    @Test
    @DisplayName("Haversine formula should return zero distance for identical coordinates")
    void testHaversineDistanceZero() {
        double dist = SpatialUtils.haversineDistanceKm(18.5913, 73.7389, 18.5913, 73.7389);
        assertEquals(0.0, dist, 0.01);
    }

    @Test
    @DisplayName("Haversine formula should compute accurate distance between Hinjawadi and Shivajinagar")
    void testHaversineDistanceKnownCities() {
        // Hinjawadi (18.5913, 73.7389) to Shivajinagar (18.5314, 73.8446) is roughly ~13-15 km
        double dist = SpatialUtils.haversineDistanceKm(18.5913, 73.7389, 18.5314, 73.8446);
        assertTrue(dist > 12.0 && dist < 16.0, "Computed distance should be between 12 and 16 km, was: " + dist);
    }

    @Test
    @DisplayName("Carbon footprint savings formula should yield 0 for single occupant")
    void testCarbonSavingsSinglePassenger() {
        // CO2 Saved = Distance * 0.12 * (Passengers - 1)
        double savings = SpatialUtils.calculateCarbonSavingsKg(20.0, 1);
        assertEquals(0.0, savings, 0.001);
    }

    @Test
    @DisplayName("Carbon footprint savings should scale with passengers and distance")
    void testCarbonSavingsMultiplePassengers() {
        // 20 km trip with 4 passengers carpooling: 20 * 0.12 * (4 - 1) = 20 * 0.12 * 3 = 7.20 kg
        double savings = SpatialUtils.calculateCarbonSavingsKg(20.0, 4);
        assertEquals(7.20, savings, 0.01);
    }
}
