package com.sharemile.dto;

public class BookingRequest {
    private Long rideId;
    private int seatsBooked = 1;
    private String pickupTitle;
    private double pickupLat;
    private double pickupLng;
    private String dropTitle;
    private double dropLat;
    private double dropLng;

    public BookingRequest() {}

    public Long getRideId() { return rideId; }
    public void setRideId(Long rideId) { this.rideId = rideId; }

    public int getSeatsBooked() { return seatsBooked; }
    public void setSeatsBooked(int seatsBooked) { this.seatsBooked = seatsBooked; }

    public String getPickupTitle() { return pickupTitle; }
    public void setPickupTitle(String pickupTitle) { this.pickupTitle = pickupTitle; }

    public double getPickupLat() { return pickupLat; }
    public void setPickupLat(double pickupLat) { this.pickupLat = pickupLat; }

    public double getPickupLng() { return pickupLng; }
    public void setPickupLng(double pickupLng) { this.pickupLng = pickupLng; }

    public String getDropTitle() { return dropTitle; }
    public void setDropTitle(String dropTitle) { this.dropTitle = dropTitle; }

    public double getDropLat() { return dropLat; }
    public void setDropLat(double dropLat) { this.dropLat = dropLat; }

    public double getDropLng() { return dropLng; }
    public void setDropLng(double dropLng) { this.dropLng = dropLng; }
}
