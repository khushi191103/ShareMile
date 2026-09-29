package com.sharemile.dto;

import java.time.LocalDateTime;

public class RideSearchRequest {
    private double pickupLat;
    private double pickupLng;
    private double dropLat;
    private double dropLng;
    private LocalDateTime desiredTime;
    private double radiusKm = 10.0; // default 10 km spatial filter radius
    private int seatsNeeded = 1;

    public RideSearchRequest() {}

    public double getPickupLat() { return pickupLat; }
    public void setPickupLat(double pickupLat) { this.pickupLat = pickupLat; }

    public double getPickupLng() { return pickupLng; }
    public void setPickupLng(double pickupLng) { this.pickupLng = pickupLng; }

    public double getDropLat() { return dropLat; }
    public void setDropLat(double dropLat) { this.dropLat = dropLat; }

    public double getDropLng() { return dropLng; }
    public void setDropLng(double dropLng) { this.dropLng = dropLng; }

    public LocalDateTime getDesiredTime() { return desiredTime; }
    public void setDesiredTime(LocalDateTime desiredTime) { this.desiredTime = desiredTime; }

    public double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(double radiusKm) { this.radiusKm = radiusKm; }

    public int getSeatsNeeded() { return seatsNeeded; }
    public void setSeatsNeeded(int seatsNeeded) { this.seatsNeeded = seatsNeeded; }

    private String genderPreference; // ANY, MALE_ONLY, FEMALE_ONLY

    public String getGenderPreference() { return genderPreference; }
    public void setGenderPreference(String genderPreference) { this.genderPreference = genderPreference; }
}
