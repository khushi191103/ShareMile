package com.sharemile.dto;

import java.util.Map;

public class AnalyticsResponse {
    private long totalUsers;
    private long verifiedUsers;
    private long totalDrivers;
    private long totalPassengers;
    private long totalRidesPublished;
    private long activeRides;
    private long completedRides;
    private long totalBookings;
    private long acceptedBookings;
    private long cancelledBookings;
    private double overallOccupancyRatePercent;
    private double totalCarbonOffsetKg;
    private double totalFareVolume;
    private double averagePlatformRating;
    private Map<String, Long> hourlyDemandBreakdown;
    private Map<String, Long> popularRoutes;

    public AnalyticsResponse() {}

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getVerifiedUsers() { return verifiedUsers; }
    public void setVerifiedUsers(long verifiedUsers) { this.verifiedUsers = verifiedUsers; }

    public long getTotalDrivers() { return totalDrivers; }
    public void setTotalDrivers(long totalDrivers) { this.totalDrivers = totalDrivers; }

    public long getTotalPassengers() { return totalPassengers; }
    public void setTotalPassengers(long totalPassengers) { this.totalPassengers = totalPassengers; }

    public long getTotalRidesPublished() { return totalRidesPublished; }
    public void setTotalRidesPublished(long totalRidesPublished) { this.totalRidesPublished = totalRidesPublished; }

    public long getActiveRides() { return activeRides; }
    public void setActiveRides(long activeRides) { this.activeRides = activeRides; }

    public long getCompletedRides() { return completedRides; }
    public void setCompletedRides(long completedRides) { this.completedRides = completedRides; }

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public long getAcceptedBookings() { return acceptedBookings; }
    public void setAcceptedBookings(long acceptedBookings) { this.acceptedBookings = acceptedBookings; }

    public long getCancelledBookings() { return cancelledBookings; }
    public void setCancelledBookings(long cancelledBookings) { this.cancelledBookings = cancelledBookings; }

    public double getOverallOccupancyRatePercent() { return overallOccupancyRatePercent; }
    public void setOverallOccupancyRatePercent(double overallOccupancyRatePercent) { this.overallOccupancyRatePercent = overallOccupancyRatePercent; }

    public double getTotalCarbonOffsetKg() { return totalCarbonOffsetKg; }
    public void setTotalCarbonOffsetKg(double totalCarbonOffsetKg) { this.totalCarbonOffsetKg = totalCarbonOffsetKg; }

    public double getTotalFareVolume() { return totalFareVolume; }
    public void setTotalFareVolume(double totalFareVolume) { this.totalFareVolume = totalFareVolume; }

    public double getAveragePlatformRating() { return averagePlatformRating; }
    public void setAveragePlatformRating(double averagePlatformRating) { this.averagePlatformRating = averagePlatformRating; }

    public Map<String, Long> getHourlyDemandBreakdown() { return hourlyDemandBreakdown; }
    public void setHourlyDemandBreakdown(Map<String, Long> hourlyDemandBreakdown) { this.hourlyDemandBreakdown = hourlyDemandBreakdown; }

    public Map<String, Long> getPopularRoutes() { return popularRoutes; }
    public void setPopularRoutes(Map<String, Long> popularRoutes) { this.popularRoutes = popularRoutes; }
}
