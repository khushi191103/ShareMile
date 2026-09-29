package com.sharemile.dto;

import com.sharemile.model.Ride;
import java.util.ArrayList;
import java.util.List;

public class RideSearchResult {
    private Ride ride;
    private double pickupDistanceKm;
    private double dropDistanceKm;
    private double matchScore; // 0 - 100%
    private double routeOverlapScore;
    private double timeProximityScore;
    private double driverRatingScore;
    private double estimatedCarbonSavingsKg;

    // Enhanced live location and co-passenger transparency attributes
    private double driverCurrentLat;
    private double driverCurrentLng;
    private double driverDistanceKm;
    private int driverEtaMinutes;
    private String genderPreference = "ANY";
    private List<PassengerSummaryDTO> confirmedPassengers = new ArrayList<>();

    public RideSearchResult() {}

    public RideSearchResult(Ride ride, double pickupDistanceKm, double dropDistanceKm, double matchScore,
                            double routeOverlapScore, double timeProximityScore, double driverRatingScore,
                            double estimatedCarbonSavingsKg) {
        this.ride = ride;
        this.pickupDistanceKm = pickupDistanceKm;
        this.dropDistanceKm = dropDistanceKm;
        this.matchScore = matchScore;
        this.routeOverlapScore = routeOverlapScore;
        this.timeProximityScore = timeProximityScore;
        this.driverRatingScore = driverRatingScore;
        this.estimatedCarbonSavingsKg = estimatedCarbonSavingsKg;
    }

    public Ride getRide() { return ride; }
    public void setRide(Ride ride) { this.ride = ride; }

    public double getPickupDistanceKm() { return pickupDistanceKm; }
    public void setPickupDistanceKm(double pickupDistanceKm) { this.pickupDistanceKm = pickupDistanceKm; }

    public double getDropDistanceKm() { return dropDistanceKm; }
    public void setDropDistanceKm(double dropDistanceKm) { this.dropDistanceKm = dropDistanceKm; }

    public double getMatchScore() { return matchScore; }
    public void setMatchScore(double matchScore) { this.matchScore = matchScore; }

    public double getRouteOverlapScore() { return routeOverlapScore; }
    public void setRouteOverlapScore(double routeOverlapScore) { this.routeOverlapScore = routeOverlapScore; }

    public double getTimeProximityScore() { return timeProximityScore; }
    public void setTimeProximityScore(double timeProximityScore) { this.timeProximityScore = timeProximityScore; }

    public double getDriverRatingScore() { return driverRatingScore; }
    public void setDriverRatingScore(double driverRatingScore) { this.driverRatingScore = driverRatingScore; }

    public double getEstimatedCarbonSavingsKg() { return estimatedCarbonSavingsKg; }
    public void setEstimatedCarbonSavingsKg(double estimatedCarbonSavingsKg) { this.estimatedCarbonSavingsKg = estimatedCarbonSavingsKg; }

    public double getDriverCurrentLat() { return driverCurrentLat; }
    public void setDriverCurrentLat(double driverCurrentLat) { this.driverCurrentLat = driverCurrentLat; }

    public double getDriverCurrentLng() { return driverCurrentLng; }
    public void setDriverCurrentLng(double driverCurrentLng) { this.driverCurrentLng = driverCurrentLng; }

    public double getDriverDistanceKm() { return driverDistanceKm; }
    public void setDriverDistanceKm(double driverDistanceKm) { this.driverDistanceKm = driverDistanceKm; }

    public int getDriverEtaMinutes() { return driverEtaMinutes; }
    public void setDriverEtaMinutes(int driverEtaMinutes) { this.driverEtaMinutes = driverEtaMinutes; }

    public String getGenderPreference() { return genderPreference; }
    public void setGenderPreference(String genderPreference) { this.genderPreference = genderPreference; }

    public List<PassengerSummaryDTO> getConfirmedPassengers() { return confirmedPassengers; }
    public void setConfirmedPassengers(List<PassengerSummaryDTO> confirmedPassengers) { this.confirmedPassengers = confirmedPassengers; }
}
