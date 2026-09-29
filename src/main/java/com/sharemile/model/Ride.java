package com.sharemile.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(nullable = false)
    private String originTitle;

    @Column(nullable = false)
    private double originLat;

    @Column(nullable = false)
    private double originLng;

    @Column(nullable = false)
    private String destTitle;

    @Column(nullable = false)
    private double destLat;

    @Column(nullable = false)
    private double destLng;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private int totalSeats;

    @Column(nullable = false)
    private int availableSeats;

    @Column(nullable = false)
    private double pricePerSeat;

    @Column(nullable = false)
    private String status = "SCHEDULED"; // SCHEDULED, ONGOING, COMPLETED, CANCELLED

    @Column(nullable = false, length = 30)
    private String genderPreference = "ANY"; // ANY, MALE_ONLY, FEMALE_ONLY

    private double driverCurrentLat;
    private double driverCurrentLng;
    private LocalDateTime driverLocationUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_rule_id", nullable = true)
    private RecurringRule recurringRule;

    private double estimatedDistanceKm;
    private int estimatedDurationMin;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Ride() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getDriver() { return driver; }
    public void setDriver(User driver) { this.driver = driver; }

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

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public double getPricePerSeat() { return pricePerSeat; }
    public void setPricePerSeat(double pricePerSeat) { this.pricePerSeat = pricePerSeat; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGenderPreference() { return genderPreference; }
    public void setGenderPreference(String genderPreference) { this.genderPreference = genderPreference; }

    public double getDriverCurrentLat() { return driverCurrentLat; }
    public void setDriverCurrentLat(double driverCurrentLat) { this.driverCurrentLat = driverCurrentLat; }

    public double getDriverCurrentLng() { return driverCurrentLng; }
    public void setDriverCurrentLng(double driverCurrentLng) { this.driverCurrentLng = driverCurrentLng; }

    public LocalDateTime getDriverLocationUpdatedAt() { return driverLocationUpdatedAt; }
    public void setDriverLocationUpdatedAt(LocalDateTime driverLocationUpdatedAt) { this.driverLocationUpdatedAt = driverLocationUpdatedAt; }

    public RecurringRule getRecurringRule() { return recurringRule; }
    public void setRecurringRule(RecurringRule recurringRule) { this.recurringRule = recurringRule; }

    public double getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public void setEstimatedDistanceKm(double estimatedDistanceKm) { this.estimatedDistanceKm = estimatedDistanceKm; }

    public int getEstimatedDurationMin() { return estimatedDurationMin; }
    public void setEstimatedDurationMin(int estimatedDurationMin) { this.estimatedDurationMin = estimatedDurationMin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
