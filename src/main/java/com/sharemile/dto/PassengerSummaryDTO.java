package com.sharemile.dto;

public class PassengerSummaryDTO {
    private String passengerName;
    private String gender;
    private int seatsBooked;
    private String passengerNamesDetail;
    private String pickupTitle;
    private String dropTitle;

    public PassengerSummaryDTO() {}

    public PassengerSummaryDTO(String passengerName, String gender, int seatsBooked, String passengerNamesDetail, String pickupTitle, String dropTitle) {
        this.passengerName = passengerName;
        this.gender = gender;
        this.seatsBooked = seatsBooked;
        this.passengerNamesDetail = passengerNamesDetail;
        this.pickupTitle = pickupTitle;
        this.dropTitle = dropTitle;
    }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getSeatsBooked() { return seatsBooked; }
    public void setSeatsBooked(int seatsBooked) { this.seatsBooked = seatsBooked; }

    public String getPassengerNamesDetail() { return passengerNamesDetail; }
    public void setPassengerNamesDetail(String passengerNamesDetail) { this.passengerNamesDetail = passengerNamesDetail; }

    public String getPickupTitle() { return pickupTitle; }
    public void setPickupTitle(String pickupTitle) { this.pickupTitle = pickupTitle; }

    public String getDropTitle() { return dropTitle; }
    public void setDropTitle(String dropTitle) { this.dropTitle = dropTitle; }
}
