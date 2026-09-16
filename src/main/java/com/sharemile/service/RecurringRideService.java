package com.sharemile.service;

import com.sharemile.dto.RecurringRuleRequest;
import com.sharemile.model.RecurringRule;
import com.sharemile.model.Ride;
import com.sharemile.model.User;
import com.sharemile.repository.RecurringRuleRepository;
import com.sharemile.repository.RideRepository;
import com.sharemile.repository.UserRepository;
import com.sharemile.util.SpatialUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecurringRideService {

    private final RecurringRuleRepository ruleRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;

    public RecurringRideService(RecurringRuleRepository ruleRepository,
                                RideRepository rideRepository,
                                UserRepository userRepository) {
        this.ruleRepository = ruleRepository;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RecurringRule createRule(RecurringRuleRequest request, String driverUsername) {
        User driver = userRepository.findByUsername(driverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        RecurringRule rule = new RecurringRule();
        rule.setDriver(driver);
        rule.setTitle(request.getTitle() != null ? request.getTitle() : "Daily Commute");
        rule.setFrequency(request.getFrequency() != null ? request.getFrequency().toUpperCase() : "DAILY");
        rule.setDayOfWeek(request.getDayOfWeek() != null ? request.getDayOfWeek().toUpperCase() : "ALL");
        rule.setDepartureTime(request.getDepartureTime() != null ? request.getDepartureTime() : LocalTime.of(9, 0));
        rule.setOriginTitle(request.getOriginTitle());
        rule.setOriginLat(request.getOriginLat());
        rule.setOriginLng(request.getOriginLng());
        rule.setDestTitle(request.getDestTitle());
        rule.setDestLat(request.getDestLat());
        rule.setDestLng(request.getDestLng());
        rule.setSeats(request.getSeats() > 0 ? request.getSeats() : 3);
        rule.setPricePerSeat(request.getPricePerSeat());
        rule.setActive(true);

        return ruleRepository.save(rule);
    }

    public List<RecurringRule> getDriverRules(String driverUsername) {
        User driver = userRepository.findByUsername(driverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        return ruleRepository.findByDriverId(driver.getId());
    }

    /**
     * Generates concrete scheduled rides from recurring commute template rules for the specified upcoming days.
     */
    @Transactional
    public List<Ride> generateRidesFromRule(Long ruleId, int daysAhead, String driverUsername) {
        RecurringRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Recurring rule not found: " + ruleId));

        if (!rule.getDriver().getUsername().equals(driverUsername)) {
            throw new IllegalStateException("Only the rule owner can generate rides from this template!");
        }

        List<Ride> generatedRides = new ArrayList<>();
        LocalDate today = LocalDate.now();
        double distance = SpatialUtils.haversineDistanceKm(
                rule.getOriginLat(), rule.getOriginLng(),
                rule.getDestLat(), rule.getDestLng()
        );

        for (int i = 1; i <= Math.min(daysAhead, 14); i++) {
            LocalDate targetDate = today.plusDays(i);

            // Skip weekends if frequency is WEEKDAYS
            if ("WEEKDAYS".equalsIgnoreCase(rule.getFrequency())) {
                if (targetDate.getDayOfWeek().getValue() >= 6) {
                    continue;
                }
            }

            LocalDateTime departureDateTime = LocalDateTime.of(targetDate, rule.getDepartureTime());

            Ride ride = new Ride();
            ride.setDriver(rule.getDriver());
            ride.setOriginTitle(rule.getOriginTitle());
            ride.setOriginLat(rule.getOriginLat());
            ride.setOriginLng(rule.getOriginLng());
            ride.setDestTitle(rule.getDestTitle());
            ride.setDestLat(rule.getDestLat());
            ride.setDestLng(rule.getDestLng());
            ride.setDepartureTime(departureDateTime);
            ride.setTotalSeats(rule.getSeats());
            ride.setAvailableSeats(rule.getSeats());
            ride.setPricePerSeat(rule.getPricePerSeat());
            ride.setStatus("SCHEDULED");
            ride.setRecurringRule(rule);
            ride.setEstimatedDistanceKm(distance);
            ride.setEstimatedDurationMin((int) Math.max(15, (distance / 30.0) * 60));

            generatedRides.add(rideRepository.save(ride));
        }

        return generatedRides;
    }
}
