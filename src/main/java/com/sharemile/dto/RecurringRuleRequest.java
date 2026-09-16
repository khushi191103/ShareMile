package com.sharemile.dto;

import java.time.LocalTime;

public class RecurringRuleRequest {
    private String title;
    private String frequency; // DAILY, WEEKDAYS, WEEKLY
    private String dayOfWeek;
    private LocalTime departureTime;
    private String originTitle;
    private double originLat;
    private double originLng;
    private String destTitle;
    private double destLat;
    private double destLng;
    private int seats;
    private double pricePerSeat;

    public RecurringRuleRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }

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

    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }

    public double getPricePerSeat() { return pricePerSeat; }
    public void setPricePerSeat(double pricePerSeat) { this.pricePerSeat = pricePerSeat; }
}
