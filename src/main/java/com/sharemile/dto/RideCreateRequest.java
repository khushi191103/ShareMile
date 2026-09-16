package com.sharemile.dto;

import java.time.LocalDateTime;

public class RideCreateRequest {
    private String originTitle;
    private double originLat;
    private double originLng;
    private String destTitle;
    private double destLat;
    private double destLng;
    private LocalDateTime departureTime;
    private int totalSeats;
    private double pricePerSeat;
    private Long recurringRuleId;

    public RideCreateRequest() {}

    public String getOriginTitle() { return originTitle; }
    public void setOriginTitle(String originTitle) { this.originTitle = originTitle; }

    public double getOriginLat() { return originLat; }
    public void setOriginLat(double originLat) { this.originLat = originLat; }

    public double getOriginLng() { return originLng; }
    public void setOriginLng(double originLng) { this.originLng = originLng; }

    public String getDestTitle() { return destTitle; }
    public void setDestTitle(String destTitle) { this.destTitle = destTitle; }

    public double getDestLat() { return destLat; }
    public void setDestLat(double destLat) { this.destLat = destLat; }

    public double getDestLng() { return destLng; }
    public void setDestLng(double destLng) { this.destLng = destLng; }

    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public double getPricePerSeat() { return pricePerSeat; }
    public void setPricePerSeat(double pricePerSeat) { this.pricePerSeat = pricePerSeat; }

    public Long getRecurringRuleId() { return recurringRuleId; }
    public void setRecurringRuleId(Long recurringRuleId) { this.recurringRuleId = recurringRuleId; }
}
