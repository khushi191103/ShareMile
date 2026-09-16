package com.sharemile.util;

import java.time.Duration;
import java.time.LocalDateTime;

public final class MatchScoreCalculator {

    private MatchScoreCalculator() {}

    /**
     * Calculates the dynamic match score (0 - 100%) defined in the ShareMile specification:
     * Match Score = (Route Overlap % × 0.5) + (Time Proximity Score × 0.3) + (Driver Rating Score × 0.2)
     *
     * @param originDistKm Distance from passenger pickup to driver origin
     * @param destDistKm Distance from passenger dropoff to driver destination
     * @param maxRadiusKm Maximum search radius threshold
     * @param requestedTime Desired departure time
     * @param rideTime Scheduled ride departure time
     * @param driverRating Driver's rating (1.0 to 5.0)
     * @return Match score percentage rounded to one decimal place (0.0 to 100.0)
     */
    public static double computeMatchScore(
            double originDistKm,
            double destDistKm,
            double maxRadiusKm,
            LocalDateTime requestedTime,
            LocalDateTime rideTime,
            double driverRating) {

        // 1. Route Overlap Component (0 - 100)
        double totalDist = originDistKm + destDistKm;
        double maxAcceptableDist = maxRadiusKm * 2.0;
        double routeOverlapScore = Math.max(0.0, 100.0 * (1.0 - (totalDist / maxAcceptableDist)));

        // 2. Time Proximity Component (0 - 100)
        double timeProximityScore = 100.0;
        if (requestedTime != null && rideTime != null) {
            long minutesDiff = Math.abs(Duration.between(requestedTime, rideTime).toMinutes());
            if (minutesDiff <= 15) {
                timeProximityScore = 100.0;
            } else if (minutesDiff <= 45) {
                timeProximityScore = 85.0;
            } else if (minutesDiff <= 90) {
                timeProximityScore = 70.0;
            } else if (minutesDiff <= 180) {
                timeProximityScore = 40.0;
            } else {
                timeProximityScore = Math.max(10.0, 100.0 - (minutesDiff / 5.0));
            }
        }

        // 3. Driver Rating Component (0 - 100)
        double normalizedRating = Math.max(1.0, Math.min(5.0, driverRating));
        double driverRatingScore = (normalizedRating / 5.0) * 100.0;

        // Weighted aggregation
        double compositeScore = (routeOverlapScore * 0.5) + (timeProximityScore * 0.3) + (driverRatingScore * 0.2);
        return Math.round(Math.max(0.0, Math.min(100.0, compositeScore)) * 10.0) / 10.0;
    }
}
