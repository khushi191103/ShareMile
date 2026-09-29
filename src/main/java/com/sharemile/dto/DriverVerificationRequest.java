package com.sharemile.dto;

import jakarta.validation.constraints.NotBlank;

public class DriverVerificationRequest {

    @NotBlank(message = "Driving License Number is mandatory")
    private String driverLicenseNumber;

    @NotBlank(message = "Car Name / Model is mandatory")
    private String vehicleModel;

    @NotBlank(message = "Car Registration Number is mandatory")
    private String vehicleNumber;

    @NotBlank(message = "Car Color is mandatory")
    private String vehicleColor;

    public DriverVerificationRequest() {}

    public DriverVerificationRequest(String driverLicenseNumber, String vehicleModel, String vehicleNumber, String vehicleColor) {
        this.driverLicenseNumber = driverLicenseNumber;
        this.vehicleModel = vehicleModel;
        this.vehicleNumber = vehicleNumber;
        this.vehicleColor = vehicleColor;
    }

    public String getDriverLicenseNumber() { return driverLicenseNumber; }
    public void setDriverLicenseNumber(String driverLicenseNumber) { this.driverLicenseNumber = driverLicenseNumber; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getVehicleColor() { return vehicleColor; }
    public void setVehicleColor(String vehicleColor) { this.vehicleColor = vehicleColor; }
}
