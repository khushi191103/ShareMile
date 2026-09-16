package com.sharemile.util;

public final class SpatialUtils {

    public static final double EARTH_RADIUS_KM = 6371.0;
    public static final double CO2_KG_PER_KM = 0.12;

    private SpatialUtils() {}

    /**
     * Calculates the great-circle distance between two geographic coordinates
     * using the Haversine formula as defined in the ShareMile specification:
     * Distance = 2 * r * arcsin(sqrt(sin²(Δφ/2) + cos(φ₁) * cos(φ₂) * sin²(Δλ/2)))
     *
     * @param lat1 Latitude of point 1 in degrees
     * @param lng1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lng2 Longitude of point 2 in degrees
     * @return Distance in kilometers
     */
    public static double haversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLng / 2.0) * Math.sin(dLng / 2.0);

        double c = 2.0 * Math.asin(Math.min(1.0, Math.sqrt(a)));
        return Math.round((EARTH_RADIUS_KM * c) * 100.0) / 100.0;
    }

    /**
     * Computes the estimated environmental carbon footprint offset:
     * CO₂ Saved (kg) = Distance (km) × 0.12 kg/km × (Passengers - 1)
     *
     * @param distanceKm Trip distance in kilometers
     * @param totalPassengers Number of passengers carpooling in the vehicle
     * @return CO2 savings in kg
     */
    public static double calculateCarbonSavingsKg(double distanceKm, int totalPassengers) {
        if (totalPassengers <= 1 || distanceKm <= 0) {
            return 0.0;
        }
        double saved = distanceKm * CO2_KG_PER_KM * (totalPassengers - 1);
        return Math.round(saved * 100.0) / 100.0;
    }
}
